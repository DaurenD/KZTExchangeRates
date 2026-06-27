"""
Task 3 — Metrics Aggregation
Acceptance criteria:
  AC1: empty punch list → all metric fields are zero / empty
  AC2: PPM = total_punches / (duration_ms / 60_000)
  AC3: 2+ punches within 1500ms window = 1 combination
  AC4: max_combo_length tracks the longest combo correctly
  AC5: active_ratio never exceeds 1.0 regardless of overlapping windows
  AC6: punch_breakdown has correct count per type
"""

import pytest

from ml.analysis.metrics import aggregate_metrics
from ml.detection.punch_detector import DetectedPunch


def _punch(ts: int, punch_type: str = "JAB", hand: str = "LEFT") -> DetectedPunch:
    return DetectedPunch(timestamp_ms=ts, punch_type=punch_type, hand=hand, speed_estimate=0.5, confidence=0.9)


# ── AC1 ──────────────────────────────────────────────────────────────────────

def test_empty_punch_list_returns_zeros():
    result = aggregate_metrics([], duration_ms=60_000)
    assert result["total_punches"] == 0
    assert result["punches_per_minute"] == 0.0
    assert result["total_combinations"] == 0
    assert result["max_combo_length"] == 0
    assert result["active_ratio"] == 0.0
    assert result["punch_breakdown"] == {}


# ── AC2 ──────────────────────────────────────────────────────────────────────

def test_ppm_is_punches_per_minute():
    punches = [_punch(i * 2000) for i in range(30)]   # 30 punches spread over 60s
    result = aggregate_metrics(punches, duration_ms=60_000)
    assert result["punches_per_minute"] == pytest.approx(30.0, rel=0.01)


# ── AC2 edge case ─────────────────────────────────────────────────────────────

def test_ppm_rounds_correctly():
    punches = [_punch(0), _punch(30_000), _punch(60_000)]   # 3 punches in 2 min
    result = aggregate_metrics(punches, duration_ms=120_000)
    assert result["punches_per_minute"] == pytest.approx(1.5, rel=0.01)


# ── AC3 ──────────────────────────────────────────────────────────────────────

def test_two_close_punches_form_combination():
    punches = [_punch(0), _punch(500)]   # 500ms apart → inside 1500ms window
    result = aggregate_metrics(punches, duration_ms=10_000)
    assert result["total_combinations"] == 1


def test_punches_far_apart_not_a_combination():
    punches = [_punch(0), _punch(2000)]  # 2000ms apart → outside window
    result = aggregate_metrics(punches, duration_ms=10_000)
    assert result["total_combinations"] == 0


# ── AC4 ──────────────────────────────────────────────────────────────────────

def test_max_combo_length_is_longest_sequence():
    # Two combos: one of length 3, one of length 2
    punches = [
        _punch(0), _punch(400), _punch(800),          # combo of 3
        _punch(10_000), _punch(10_400),               # combo of 2
    ]
    result = aggregate_metrics(punches, duration_ms=20_000)
    assert result["max_combo_length"] == 3


# ── AC5 ──────────────────────────────────────────────────────────────────────

def test_active_ratio_capped_at_one():
    # Every 10ms punch → active windows fully overlap → ratio should not exceed 1.0
    punches = [_punch(i * 10) for i in range(500)]
    result = aggregate_metrics(punches, duration_ms=5_000)
    assert result["active_ratio"] <= 1.0


# ── AC6 ──────────────────────────────────────────────────────────────────────

def test_punch_breakdown_counts_each_type():
    punches = [
        _punch(0,    "JAB",         "LEFT"),
        _punch(500,  "JAB",         "LEFT"),
        _punch(1000, "CROSS",       "RIGHT"),
        _punch(1500, "LEFT_HOOK",   "LEFT"),
    ]
    result = aggregate_metrics(punches, duration_ms=10_000)
    assert result["punch_breakdown"]["JAB"] == 2
    assert result["punch_breakdown"]["CROSS"] == 1
    assert result["punch_breakdown"]["LEFT_HOOK"] == 1


