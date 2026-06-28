#!/usr/bin/env bash
# Downloads the MediaPipe Pose Landmarker model for local development.
# The worker Dockerfile runs this automatically at build time.

set -euo pipefail

MODEL_DIR="$(cd "$(dirname "$0")/../ml/pose" && pwd)"
MODEL_FILE="$MODEL_DIR/pose_landmarker_full.task"
MODEL_URL="https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task"

if [ -f "$MODEL_FILE" ]; then
  echo "Model already present: $MODEL_FILE"
  exit 0
fi

echo "Downloading MediaPipe Pose Landmarker model..."
mkdir -p "$MODEL_DIR"
curl -L --progress-bar -o "$MODEL_FILE" "$MODEL_URL"
echo "Saved to $MODEL_FILE"
