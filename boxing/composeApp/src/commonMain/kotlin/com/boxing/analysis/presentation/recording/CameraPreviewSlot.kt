package com.boxing.analysis.presentation.recording

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Camera preview with recording capability.
 *
 * Implemented per-platform:
 *   - androidMain: CameraX VideoCapture
 *   - iosMain:     AVFoundation AVCaptureSession
 *
 * [onVideoReady] is called with (mp4Bytes, durationMs) after recording stops.
 */
@Composable
expect fun CameraPreviewSlot(
    isRecording: Boolean,
    modifier: Modifier,
    onVideoReady: (ByteArray, Long) -> Unit,
)
