"""Technique scorer: guard, balance, hip rotation."""
from __future__ import annotations

import math
import statistics

from ml.pose.estimator import PoseFrame

LEFT_WRIST, RIGHT_WRIST = 15, 16
LEFT_ELBOW, RIGHT_ELBOW = 13, 14
LEFT_SHOULDER, RIGHT_SHOULDER = 11, 12
LEFT_HIP, RIGHT_HIP = 23, 24
NOSE, LEFT_EAR, RIGHT_EAR = 0, 7, 8


def score_technique(pose_frames: list[PoseFrame]) -> dict:
    guard_frames = 0
    valid_frames = 0
    hip_rotations: list[float] = []
    com_x_positions: list[float] = []

    for frame in pose_frames:
        lw = frame.get(LEFT_WRIST)
        rw = frame.get(RIGHT_WRIST)
        ls = frame.get(LEFT_SHOULDER)
        rs = frame.get(RIGHT_SHOULDER)
        lh = frame.get(LEFT_HIP)
        rh = frame.get(RIGHT_HIP)
        nose = frame.get(NOSE)

        if not (lw and rw and ls and rs):
            continue

        valid_frames += 1

        # Guard: both wrists above the mid-point between shoulder and nose
        if nose:
            chin_y = (ls.y + rs.y) / 2 + (nose.y - (ls.y + rs.y) / 2) * 0.3
            if lw.y < chin_y and rw.y < chin_y:
                guard_frames += 1

        # Hip rotation (yaw) estimated from shoulder/hip width ratio projected to 2D
        if lh and rh:
            shoulder_width = abs(rs.x - ls.x)
            hip_width      = abs(rh.x - lh.x)
            if shoulder_width > 0:
                rotation_proxy = abs(1.0 - hip_width / shoulder_width) * 90
                hip_rotations.append(rotation_proxy)

            # Centre of mass x (approximate)
            com_x = (ls.x + rs.x + lh.x + rh.x) / 4
            com_x_positions.append(com_x)

    if valid_frames == 0:
        return {"guard_score": 0.0, "balance_score": 0.0}

    guard_score = round((guard_frames / valid_frames) * 100, 1)

    # Balance: low variance in centre-of-mass x → stable base
    if len(com_x_positions) > 1:
        com_variance = statistics.variance(com_x_positions)
        balance_score = round(max(0.0, 100.0 - com_variance * 5000), 1)
    else:
        balance_score = 50.0

    return {
        "guard_score": min(100.0, guard_score),
        "balance_score": min(100.0, balance_score),
    }
