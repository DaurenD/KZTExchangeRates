"""
Task 2 — Punch Detector
Acceptance criteria:
  AC1: wrist velocity below threshold → no punch detected
  AC2: lead hand, arm extended, fast → JAB
  AC3: rear hand, arm extended, fast → CROSS
  AC4: lateral wrist arc → HOOK
  AC5: wrist moving upward from below shoulder → UPPERCUT
  AC6: two punches within refractory window → only 1 counted
  AC7: landmark with visibility < 0.3 → not used
"""
import pytest

from ml.detection.punch_detector import SPEED_THRESHOLD, detect_punches
from ml.pose.estimator import Landmark, PoseFrame

# MediaPipe landmark indices used by the detector
LEFT_WRIST, RIGHT_WRIST = 15, 16
LEFT_ELBOW, RIGHT_ELBOW = 13, 14
LEFT_SHOULDER, RIGHT_SHOULDER = 11, 12
NOSE = 0

_VISIBLE = 0.9
_HIDDEN = 0.1


def _make_landmarks(**overrides) -> list[Landmark]:
    """Return a full 33-landmark list with sensible defaults; override by index."""
    defaults = {
        LEFT_SHOULDER:  Landmark(LEFT_SHOULDER,  x=0.35, y=0.40, z=0.0, visibility=_VISIBLE),
        RIGHT_SHOULDER: Landmark(RIGHT_SHOULDER, x=0.65, y=0.40, z=0.0, visibility=_VISIBLE),
        LEFT_ELBOW:     Landmark(LEFT_ELBOW,     x=0.30, y=0.55, z=0.0, visibility=_VISIBLE),
        RIGHT_ELBOW:    Landmark(RIGHT_ELBOW,    x=0.70, y=0.55, z=0.0, visibility=_VISIBLE),
        LEFT_WRIST:     Landmark(LEFT_WRIST,     x=0.28, y=0.65, z=0.0, visibility=_VISIBLE),
        RIGHT_WRIST:    Landmark(RIGHT_WRIST,    x=0.72, y=0.65, z=0.0, visibility=_VISIBLE),
        NOSE:           Landmark(NOSE,           x=0.50, y=0.25, z=0.0, visibility=_VISIBLE),
    }
    defaults.update(overrides)
    # Fill remaining indices with invisible placeholders
    result = [Landmark(i, 0.5, 0.5, 0.0, 0.0) for i in range(33)]
    for idx, lm in defaults.items():
        result[idx] = lm
    return result


def _frame(ts, **landmark_overrides) -> PoseFrame:
    return PoseFrame(timestamp_ms=ts, landmarks=_make_landmarks(**landmark_overrides))


def _fast_delta(base_x: float, base_y: float, dt_ms: int = 33) -> tuple[float, float]:
    """Return a wrist displacement that clears SPEED_THRESHOLD within dt_ms."""
    required = SPEED_THRESHOLD * dt_ms * 1.5
    return base_x - required, base_y  # move left (toward target)


# ── AC1 ──────────────────────────────────────────────────────────────────────

def test_no_punch_when_wrist_stationary():
    frames = [
        _frame(0),
        _frame(33),
        _frame(66),
    ]
    assert detect_punches(frames) == []


# ── AC2 ──────────────────────────────────────────────────────────────────────

def test_jab_detected_for_lead_hand_orthodox():
    # Left wrist moves forward fast with arm extended (wrist far from shoulder)
    dx, _ = _fast_delta(0.28, 0.65)
    frames = [
        _frame(0),
        _frame(33, **{
            LEFT_WRIST: Landmark(LEFT_WRIST, x=dx, y=0.40, z=0.0, visibility=_VISIBLE),
        }),
    ]
    punches = detect_punches(frames, stance="ORTHODOX")
    jabs = [p for p in punches if p.punch_type == "JAB"]
    assert len(jabs) >= 1


# ── AC3 ──────────────────────────────────────────────────────────────────────

def test_cross_detected_for_rear_hand_orthodox():
    dx, _ = _fast_delta(0.72, 0.65)
    frames = [
        _frame(0),
        _frame(33, **{
            RIGHT_WRIST: Landmark(RIGHT_WRIST, x=dx, y=0.40, z=0.0, visibility=_VISIBLE),
        }),
    ]
    punches = detect_punches(frames, stance="ORTHODOX")
    crosses = [p for p in punches if p.punch_type == "CROSS"]
    assert len(crosses) >= 1


# ── AC4 ──────────────────────────────────────────────────────────────────────

def test_hook_detected_when_wrist_lateral_arc():
    # Wrist moves fast but stays close to shoulder (low extension ratio) → hook
    dt = 33
    speed = SPEED_THRESHOLD * dt * 2
    frames = [
        _frame(0),
        _frame(dt, **{
            # Wrist near elbow level, far lateral — low extension ratio
            LEFT_WRIST: Landmark(LEFT_WRIST, x=0.28 - speed, y=0.55, z=0.0, visibility=_VISIBLE),
            LEFT_ELBOW: Landmark(LEFT_ELBOW, x=0.20, y=0.55, z=0.0, visibility=_VISIBLE),
        }),
    ]
    punches = detect_punches(frames, stance="ORTHODOX")
    hooks = [p for p in punches if "HOOK" in p.punch_type]
    assert len(hooks) >= 1


# ── AC5 ──────────────────────────────────────────────────────────────────────

def test_uppercut_detected_when_wrist_rises_from_below_shoulder():
    dt = 33
    speed = SPEED_THRESHOLD * dt * 2
    frames = [
        # Wrist starts below shoulder level (higher y value = lower in frame)
        _frame(0, **{LEFT_WRIST: Landmark(LEFT_WRIST, x=0.35, y=0.70, z=0.0, visibility=_VISIBLE)}),
        # Wrist moves upward fast (smaller y = higher in frame)
        _frame(dt, **{LEFT_WRIST: Landmark(LEFT_WRIST, x=0.35, y=0.70 - speed, z=0.0, visibility=_VISIBLE)}),
    ]
    punches = detect_punches(frames, stance="ORTHODOX")
    uppercuts = [p for p in punches if "UPPERCUT" in p.punch_type]
    assert len(uppercuts) >= 1


# ── AC6 ──────────────────────────────────────────────────────────────────────

def test_refractory_period_prevents_double_count():
    # Two rapid left-wrist movements 100ms apart → only 1 should register
    dt = 33
    speed = SPEED_THRESHOLD * dt * 2
    frames = [
        _frame(0),
        _frame(dt, **{LEFT_WRIST: Landmark(LEFT_WRIST, x=0.28 - speed, y=0.40, z=0.0, visibility=_VISIBLE)}),
        _frame(dt + 100, **{LEFT_WRIST: Landmark(LEFT_WRIST, x=0.28 - speed * 2, y=0.40, z=0.0, visibility=_VISIBLE)}),
    ]
    left_punches = [p for p in detect_punches(frames) if p.hand == "LEFT"]
    assert len(left_punches) == 1


# ── AC7 ──────────────────────────────────────────────────────────────────────

def test_low_visibility_landmark_ignored():
    dt = 33
    speed = SPEED_THRESHOLD * dt * 2
    frames = [
        _frame(0),
        _frame(dt, **{
            LEFT_WRIST: Landmark(LEFT_WRIST, x=0.28 - speed, y=0.40, z=0.0, visibility=_HIDDEN),
        }),
    ]
    assert detect_punches(frames) == []
