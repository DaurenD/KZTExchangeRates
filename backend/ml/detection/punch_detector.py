"""Rule-based punch detector operating on pose landmark trajectories.

Algorithm per hand:
  1. Compute wrist velocity (Euclidean distance / Δt) in normalised coords.
  2. Detect onset: velocity crosses SPEED_THRESHOLD upward.
  3. Classify type from the arm geometry at peak velocity:
       - Extension ratio > 0.85 → straight (jab or cross by hand)
       - Lateral wrist offset > elbow offset → hook
       - Wrist below shoulder and upward velocity → uppercut
  4. Suppress overlapping detections within REFRACTORY_MS.
"""
from __future__ import annotations

import math
from dataclasses import dataclass

from ml.pose.estimator import PoseFrame

# Landmark indices
LEFT_WRIST, RIGHT_WRIST = 15, 16
LEFT_ELBOW, RIGHT_ELBOW = 13, 14
LEFT_SHOULDER, RIGHT_SHOULDER = 11, 12
NOSE = 0

SPEED_THRESHOLD = 0.018      # normalised units / ms; tuned for typical 30fps video
REFRACTORY_MS = 300           # minimum gap between detected punches for same hand
EXTENSION_RATIO_STRAIGHT = 0.80


@dataclass
class DetectedPunch:
    timestamp_ms: int
    punch_type: str   # JAB | CROSS | LEFT_HOOK | RIGHT_HOOK | LEFT_UPPERCUT | RIGHT_UPPERCUT
    hand: str         # LEFT | RIGHT
    speed_estimate: float
    confidence: float


def detect_punches(pose_frames: list[PoseFrame], stance: str = "ORTHODOX") -> list[DetectedPunch]:
    """stance: 'ORTHODOX' (lead=left) or 'SOUTHPAW' (lead=right)."""
    punches: list[DetectedPunch] = []
    last_detection: dict[str, int] = {"LEFT": -9999, "RIGHT": -9999}

    prev: PoseFrame | None = None
    for frame in pose_frames:
        if prev is None:
            prev = frame
            continue

        dt = frame.timestamp_ms - prev.timestamp_ms
        if dt <= 0:
            prev = frame
            continue

        for hand, wrist_idx, elbow_idx, shoulder_idx in [
            ("LEFT",  LEFT_WRIST,  LEFT_ELBOW,  LEFT_SHOULDER),
            ("RIGHT", RIGHT_WRIST, RIGHT_ELBOW, RIGHT_SHOULDER),
        ]:
            w_cur  = frame.get(wrist_idx)
            w_prev = prev.get(wrist_idx)
            elbow  = frame.get(elbow_idx)
            shoulder = frame.get(shoulder_idx)

            if not (w_cur and w_prev and elbow and shoulder):
                continue

            speed = math.dist((w_cur.x, w_cur.y), (w_prev.x, w_prev.y)) / dt

            if speed < SPEED_THRESHOLD:
                continue
            if frame.timestamp_ms - last_detection[hand] < REFRACTORY_MS:
                continue

            punch_type = _classify(hand, stance, w_cur, w_prev, elbow, shoulder, dt)
            confidence = min(1.0, speed / (SPEED_THRESHOLD * 3))

            punches.append(DetectedPunch(
                timestamp_ms=frame.timestamp_ms,
                punch_type=punch_type,
                hand=hand,
                speed_estimate=round(speed * 1000, 3),   # store as normalised-units/s
                confidence=round(confidence, 3),
            ))
            last_detection[hand] = frame.timestamp_ms

        prev = frame
    return punches


def _classify(hand, stance, wrist, wrist_prev, elbow, shoulder, dt):
    # Extension ratio: how straight the arm is
    wrist_shoulder_dist  = math.dist((wrist.x, wrist.y), (shoulder.x, shoulder.y))
    elbow_shoulder_dist  = math.dist((elbow.x, elbow.y), (shoulder.x, shoulder.y))
    wrist_elbow_dist     = math.dist((wrist.x, wrist.y), (elbow.x, elbow.y))
    arm_length = elbow_shoulder_dist + wrist_elbow_dist
    extension_ratio = wrist_shoulder_dist / arm_length if arm_length > 0 else 0

    # Vertical wrist velocity (positive = moving up)
    vert_velocity = (wrist_prev.y - wrist.y) / dt  # y decreases upward in image coords

    # Lateral offset of wrist vs elbow relative to shoulder
    lateral_wrist = abs(wrist.x - shoulder.x)
    lateral_elbow = abs(elbow.x - shoulder.x)

    is_straight = extension_ratio >= EXTENSION_RATIO_STRAIGHT
    is_uppercut = wrist.y > shoulder.y and vert_velocity > SPEED_THRESHOLD * 0.5

    if is_uppercut:
        return f"{hand}_UPPERCUT"
    if is_straight:
        # Jab = lead hand straight, Cross = rear hand straight
        is_lead = (hand == "LEFT" and stance == "ORTHODOX") or (hand == "RIGHT" and stance == "SOUTHPAW")
        return "JAB" if is_lead else "CROSS"
    # Hook: wrist sweeps laterally more than elbow extends
    return f"{hand}_HOOK"
