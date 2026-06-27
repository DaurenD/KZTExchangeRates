"""
Task 4 — Technique & Footwork Scoring
Acceptance criteria:
  Technique:
    AC1: guard_score = 0 when wrists always below chin level
    AC2: guard_score = 100 when wrists always above chin level
    AC3: stable centre-of-mass → balance_score near 100
    AC4: wandering centre-of-mass → balance_score near 0
  Footwork:
    AC5: no ankle movement → footwork_score stays low (movement component = 0)
    AC6: foot angle change > 25° between frames → pivot counted
"""

import pytest

from ml.analysis.footwork import score_footwork
from ml.analysis.technique import score_technique
from ml.pose.estimator import Landmark, PoseFrame

_V = 0.9  # visible

# Landmark indices
NOSE = 0
LEFT_SHOULDER, RIGHT_SHOULDER = 11, 12
LEFT_HIP, RIGHT_HIP = 23, 24
LEFT_WRIST, RIGHT_WRIST = 15, 16
LEFT_ANKLE, RIGHT_ANKLE = 27, 28


def _full_frame(ts: int, **overrides) -> PoseFrame:
    base = {
        NOSE:           Landmark(NOSE,           0.50, 0.20, 0.0, _V),
        LEFT_SHOULDER:  Landmark(LEFT_SHOULDER,  0.35, 0.40, 0.0, _V),
        RIGHT_SHOULDER: Landmark(RIGHT_SHOULDER, 0.65, 0.40, 0.0, _V),
        LEFT_HIP:       Landmark(LEFT_HIP,       0.38, 0.60, 0.0, _V),
        RIGHT_HIP:      Landmark(RIGHT_HIP,      0.62, 0.60, 0.0, _V),
        LEFT_WRIST:     Landmark(LEFT_WRIST,     0.30, 0.65, 0.0, _V),
        RIGHT_WRIST:    Landmark(RIGHT_WRIST,    0.70, 0.65, 0.0, _V),
        LEFT_ANKLE:     Landmark(LEFT_ANKLE,     0.38, 0.90, 0.0, _V),
        RIGHT_ANKLE:    Landmark(RIGHT_ANKLE,    0.62, 0.90, 0.0, _V),
    }
    base.update(overrides)
    landmarks = [Landmark(i, 0.5, 0.5, 0.0, 0.0) for i in range(33)]
    for idx, lm in base.items():
        landmarks[idx] = lm
    return PoseFrame(timestamp_ms=ts, landmarks=landmarks)


# ── AC1: wrists always below chin ────────────────────────────────────────────

def test_guard_score_zero_when_wrists_always_low():
    # Chin is ~y=0.35 (between nose y=0.20 and shoulder y=0.40)
    # Wrists at y=0.80 = far below chin
    frames = [
        _full_frame(i * 33, **{
            LEFT_WRIST:  Landmark(LEFT_WRIST,  0.30, 0.80, 0.0, _V),
            RIGHT_WRIST: Landmark(RIGHT_WRIST, 0.70, 0.80, 0.0, _V),
        })
        for i in range(30)
    ]
    result = score_technique(frames)
    assert result["guard_score"] == 0.0


# ── AC2: wrists always above chin ────────────────────────────────────────────

def test_guard_score_100_when_wrists_always_high():
    # Wrists at y=0.10 = above nose (well above chin)
    frames = [
        _full_frame(i * 33, **{
            LEFT_WRIST:  Landmark(LEFT_WRIST,  0.30, 0.10, 0.0, _V),
            RIGHT_WRIST: Landmark(RIGHT_WRIST, 0.70, 0.10, 0.0, _V),
        })
        for i in range(30)
    ]
    result = score_technique(frames)
    assert result["guard_score"] == 100.0


# ── AC3: stable COM → high balance ───────────────────────────────────────────

def test_balance_score_high_when_com_stable():
    # Identical frames → zero COM variance → score near 100
    frames = [_full_frame(i * 33) for i in range(30)]
    result = score_technique(frames)
    assert result["balance_score"] >= 90.0


# ── AC4: wandering COM → low balance ─────────────────────────────────────────

def test_balance_score_low_when_com_wanders():
    frames = []
    for i in range(30):
        # Hips alternate between far left and far right every frame
        offset = 0.4 if i % 2 == 0 else -0.4
        frames.append(_full_frame(i * 33, **{
            LEFT_HIP:  Landmark(LEFT_HIP,  0.38 + offset, 0.60, 0.0, _V),
            RIGHT_HIP: Landmark(RIGHT_HIP, 0.62 + offset, 0.60, 0.0, _V),
        }))
    result = score_technique(frames)
    assert result["balance_score"] < 50.0


# ── AC5: no ankle movement → low movement component ─────────────────────────

def test_footwork_score_low_when_feet_stationary():
    frames = [_full_frame(i * 33) for i in range(60)]
    result = score_footwork(frames)
    # Movement component = 0, only stability contributes → below 25
    assert result["footwork_score"] < 25.0
    assert result["total_distance"] == pytest.approx(0.0, abs=1e-6)


# ── AC6: large foot-angle change → pivot counted ─────────────────────────────

def test_pivot_counted_when_foot_angle_changes():
    frames = [
        # Frame 0: feet horizontal (angle ≈ 0°)
        _full_frame(0, **{
            LEFT_ANKLE:  Landmark(LEFT_ANKLE,  0.30, 0.90, 0.0, _V),
            RIGHT_ANKLE: Landmark(RIGHT_ANKLE, 0.70, 0.90, 0.0, _V),
        }),
        # Frame 1: feet rotated ~45° (right ankle moves up-right)
        _full_frame(33, **{
            LEFT_ANKLE:  Landmark(LEFT_ANKLE,  0.30, 0.90, 0.0, _V),
            RIGHT_ANKLE: Landmark(RIGHT_ANKLE, 0.70, 0.60, 0.0, _V),
        }),
    ]
    result = score_footwork(frames)
    assert result["pivot_count"] >= 1


