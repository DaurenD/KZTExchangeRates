"""Main analysis orchestrator called by the worker endpoint.

Flow:
  1. Download video from GCS to a temp file
  2. Extract frames via OpenCV at 30fps (every frame)
  3. Run MediaPipe Pose on each frame
  4. Detect punches from pose trajectories
  5. Score footwork and technique
  6. Aggregate metrics
  7. Generate coaching tips via Claude
  8. Return structured result dict
"""
from __future__ import annotations

import asyncio
import os
import tempfile
from pathlib import Path

import cv2

from app.services.storage import download_to_file
from ml.analysis.footwork import score_footwork
from ml.analysis.metrics import aggregate_metrics
from ml.analysis.technique import score_technique
from ml.coaching.tips import generate_tips
from ml.detection.punch_detector import detect_punches
from ml.pose.estimator import PoseEstimator


async def analyse_video(gcs_path: str) -> dict:
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
        tmp_path = tmp.name

    try:
        # Download is blocking I/O — run in executor to avoid blocking event loop
        await asyncio.get_event_loop().run_in_executor(None, download_to_file, gcs_path, tmp_path)
        return await asyncio.get_event_loop().run_in_executor(None, _run_pipeline, tmp_path)
    finally:
        os.unlink(tmp_path)


def _run_pipeline(video_path: str) -> dict:
    frames = _extract_frames(video_path)
    duration_ms = frames[-1][0] if frames else 0

    estimator = PoseEstimator()
    try:
        pose_frames = estimator.estimate_frames(frames)
    finally:
        estimator.close()

    punches = detect_punches(pose_frames)
    footwork = score_footwork(pose_frames)
    technique = score_technique(pose_frames)
    punch_metrics = aggregate_metrics(punches, duration_ms)

    metrics = {
        **punch_metrics,
        "guard_score": technique["guard_score"],
        "footwork_score": footwork["footwork_score"],
        "balance_score": technique["balance_score"],
    }

    # Minimal profile — coaching tips use whatever is stored with the session
    profile = {"stance": "ORTHODOX", "experience_level": "BEGINNER"}
    coaching_tips = generate_tips(metrics, profile)

    return {
        "metrics": metrics,
        "punches": [
            {
                "timestamp_ms": p.timestamp_ms,
                "punch_type": p.punch_type,
                "hand": p.hand,
                "speed_estimate": p.speed_estimate,
                "confidence": p.confidence,
            }
            for p in punches
        ],
        "coaching_tips": coaching_tips,
    }


def _extract_frames(video_path: str) -> list[tuple[int, object]]:
    """Returns [(timestamp_ms, bgr_frame), ...] sampled at every frame."""
    cap = cv2.VideoCapture(video_path)
    frames = []
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            ts_ms = int(cap.get(cv2.CAP_PROP_POS_MSEC))
            frames.append((ts_ms, frame))
    finally:
        cap.release()
    return frames
