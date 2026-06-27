"""MediaPipe Pose Landmarker wrapper.

Returns 33 normalised landmarks per frame. Indices follow the MediaPipe
BlazePose topology: 15=left wrist, 16=right wrist, 13=left elbow,
14=right elbow, 11=left shoulder, 12=right shoulder, 23=left hip,
24=right hip, 27=left ankle, 28=right ankle, 0=nose.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import mediapipe as mp
import numpy as np

_MODEL_PATH = Path(__file__).parent / "pose_landmarker_full.task"

BaseOptions = mp.tasks.BaseOptions
PoseLandmarker = mp.tasks.vision.PoseLandmarker
PoseLandmarkerOptions = mp.tasks.vision.PoseLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode


@dataclass
class Landmark:
    index: int
    x: float
    y: float
    z: float
    visibility: float


@dataclass
class PoseFrame:
    timestamp_ms: int
    landmarks: list[Landmark]  # 33 entries; empty list if no person detected

    def get(self, index: int) -> Landmark | None:
        if index < len(self.landmarks):
            lm = self.landmarks[index]
            return lm if lm.visibility > 0.3 else None
        return None


class PoseEstimator:
    """Stateless wrapper — call estimate_frames() with a list of (timestamp_ms, bgr_frame)."""

    def __init__(self) -> None:
        if not _MODEL_PATH.exists():
            raise FileNotFoundError(
                f"MediaPipe model not found at {_MODEL_PATH}. "
                "Download pose_landmarker_full.task from "
                "https://developers.google.com/mediapipe/solutions/vision/pose_landmarker"
            )
        options = PoseLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=str(_MODEL_PATH)),
            running_mode=VisionRunningMode.VIDEO,
            num_poses=1,
            min_pose_detection_confidence=0.5,
            min_pose_presence_confidence=0.5,
            min_tracking_confidence=0.5,
        )
        self._landmarker = PoseLandmarker.create_from_options(options)

    def estimate_frames(self, frames: list[tuple[int, np.ndarray]]) -> list[PoseFrame]:
        results = []
        for ts_ms, bgr in frames:
            rgb = bgr[:, :, ::-1]
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            detection = self._landmarker.detect_for_video(mp_image, ts_ms)

            if not detection.pose_landmarks:
                results.append(PoseFrame(timestamp_ms=ts_ms, landmarks=[]))
                continue

            landmarks = [
                Landmark(
                    index=i,
                    x=lm.x,
                    y=lm.y,
                    z=lm.z,
                    visibility=lm.visibility,
                )
                for i, lm in enumerate(detection.pose_landmarks[0])
            ]
            results.append(PoseFrame(timestamp_ms=ts_ms, landmarks=landmarks))

        return results

    def close(self) -> None:
        self._landmarker.close()
