"""Footwork analyser operating on per-frame ankle landmarks."""
from __future__ import annotations

import math

from ml.pose.estimator import PoseFrame

LEFT_ANKLE, RIGHT_ANKLE = 27, 28
LEFT_HIP, RIGHT_HIP = 23, 24

PIVOT_ANGLE_THRESHOLD = 25.0   # degrees; foot-line rotation that counts as a pivot
STEP_DISTANCE_THRESHOLD = 0.04  # normalised units between consecutive frames


def score_footwork(pose_frames: list[PoseFrame]) -> dict:
    """Returns footwork_score (0-100) and supporting stats."""
    total_distance = 0.0
    pivot_count = 0
    stance_widths: list[float] = []
    valid_frames = 0

    prev_ankle_midpoint = None
    prev_foot_angle = None

    for frame in pose_frames:
        la = frame.get(LEFT_ANKLE)
        ra = frame.get(RIGHT_ANKLE)
        if not (la and ra):
            continue

        valid_frames += 1
        midpoint = ((la.x + ra.x) / 2, (la.y + ra.y) / 2)
        stance_width = math.dist((la.x, la.y), (ra.x, ra.y))
        stance_widths.append(stance_width)

        foot_angle = math.degrees(math.atan2(ra.y - la.y, ra.x - la.x))

        if prev_ankle_midpoint:
            step = math.dist(midpoint, prev_ankle_midpoint)
            total_distance += step

        if prev_foot_angle is not None:
            angle_change = abs(foot_angle - prev_foot_angle)
            if angle_change > 180:
                angle_change = 360 - angle_change
            if angle_change > PIVOT_ANGLE_THRESHOLD:
                pivot_count += 1

        prev_ankle_midpoint = midpoint
        prev_foot_angle = foot_angle

    if valid_frames == 0:
        return {"footwork_score": 0.0, "total_distance": 0.0, "pivot_count": 0}

    avg_stance = sum(stance_widths) / len(stance_widths)
    stance_variance = sum((w - avg_stance) ** 2 for w in stance_widths) / len(stance_widths)

    # Good footwork = active movement + consistent stance width + pivots
    movement_score = min(50.0, total_distance * 500)
    pivot_score    = min(30.0, pivot_count * 3.0)
    stability_score = max(0.0, 20.0 - stance_variance * 1000)

    footwork_score = round(movement_score + pivot_score + stability_score, 1)

    return {
        "footwork_score": min(100.0, footwork_score),
        "total_distance": round(total_distance, 4),
        "pivot_count": pivot_count,
    }
