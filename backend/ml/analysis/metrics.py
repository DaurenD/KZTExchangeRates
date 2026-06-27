"""Aggregate punch list into session-level metrics."""
from __future__ import annotations

from collections import defaultdict

from ml.detection.punch_detector import DetectedPunch

COMBO_GAP_MS = 1500  # punches within this window = a combination


def aggregate_metrics(punches: list[DetectedPunch], duration_ms: int) -> dict:
    if not punches:
        return _empty_metrics()

    duration_min = duration_ms / 60_000
    ppm = len(punches) / duration_min if duration_min > 0 else 0.0

    breakdown: dict[str, int] = defaultdict(int)
    for p in punches:
        breakdown[p.punch_type] += 1

    # Combination detection
    sorted_punches = sorted(punches, key=lambda p: p.timestamp_ms)
    combos: list[list[DetectedPunch]] = []
    current_combo: list[DetectedPunch] = [sorted_punches[0]]

    for prev, cur in zip(sorted_punches, sorted_punches[1:]):
        if cur.timestamp_ms - prev.timestamp_ms <= COMBO_GAP_MS:
            current_combo.append(cur)
        else:
            if len(current_combo) >= 2:
                combos.append(current_combo)
            current_combo = [cur]
    if len(current_combo) >= 2:
        combos.append(current_combo)

    max_combo = max((len(c) for c in combos), default=0)
    avg_combo = sum(len(c) for c in combos) / len(combos) if combos else 0.0

    # Active time = time windows around each punch (±150ms)
    active_windows: list[tuple[int, int]] = [(p.timestamp_ms - 150, p.timestamp_ms + 150) for p in punches]
    merged = _merge_intervals(active_windows)
    active_ms = sum(end - start for start, end in merged)
    active_ratio = active_ms / duration_ms if duration_ms > 0 else 0.0

    speeds = [p.speed_estimate for p in punches]

    return {
        "total_punches": len(punches),
        "punches_per_minute": round(ppm, 2),
        "punch_breakdown": dict(breakdown),
        "total_combinations": len(combos),
        "max_combo_length": max_combo,
        "avg_combo_length": round(avg_combo, 2),
        "active_ratio": round(min(1.0, active_ratio), 3),
        "peak_speed": round(max(speeds), 3),
        "avg_speed": round(sum(speeds) / len(speeds), 3),
    }


def _merge_intervals(intervals: list[tuple[int, int]]) -> list[tuple[int, int]]:
    if not intervals:
        return []
    sorted_iv = sorted(intervals)
    merged = [sorted_iv[0]]
    for start, end in sorted_iv[1:]:
        if start <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(merged[-1][1], end))
        else:
            merged.append((start, end))
    return merged


def _empty_metrics() -> dict:
    return {
        "total_punches": 0,
        "punches_per_minute": 0.0,
        "punch_breakdown": {},
        "total_combinations": 0,
        "max_combo_length": 0,
        "avg_combo_length": 0.0,
        "active_ratio": 0.0,
        "peak_speed": 0.0,
        "avg_speed": 0.0,
    }
