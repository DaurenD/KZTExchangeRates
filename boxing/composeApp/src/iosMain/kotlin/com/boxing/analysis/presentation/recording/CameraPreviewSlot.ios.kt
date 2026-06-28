package com.boxing.analysis.presentation.recording

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.UIKit.UIView

/**
 * iOS camera preview backed by AVFoundation.
 * AVCaptureMovieFileOutput writes to a temp file; on stop we read bytes and call [onVideoReady].
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreviewSlot(
    isRecording: Boolean,
    modifier: Modifier,
    onVideoReady: (ByteArray, Long) -> Unit,
) {
    val controller = remember { AVFoundationCameraController(onVideoReady) }

    LaunchedEffect(isRecording) {
        if (isRecording) controller.startRecording() else controller.stopRecording()
    }

    UIKitView(
        factory  = { controller.previewView },
        modifier = modifier,
        update   = {},
    )
}

class AVFoundationCameraController(private val onVideoReady: (ByteArray, Long) -> Unit) {
    val previewView: UIView = UIView()
    private val session = AVCaptureSession()
    private val fileOutput = AVCaptureMovieFileOutput()
    private var startMs = 0L

    init { setupSession() }

    private fun setupSession() {
        session.beginConfiguration()
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input  = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return
        if (session.canAddInput(input)) session.addInput(input)
        if (session.canAddOutput(fileOutput)) session.addOutput(fileOutput)
        session.commitConfiguration()

        val layer = AVCaptureVideoPreviewLayer(session = session)
        layer.frame = previewView.bounds
        previewView.layer.addSublayer(layer)

        session.startRunning()
    }

    fun startRecording() {
        val url = platform.Foundation.NSURL.fileURLWithPath(
            platform.Foundation.NSTemporaryDirectory() + "boxing_${platform.Foundation.NSDate().timeIntervalSince1970}.mp4"
        )
        startMs = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
        fileOutput.startRecordingToOutputFileURL(url, recordingDelegate = RecordingDelegate(startMs, onVideoReady))
    }

    fun stopRecording() = fileOutput.stopRecording()
}

class RecordingDelegate(
    private val startMs: Long,
    private val onVideoReady: (ByteArray, Long) -> Unit,
) : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureFileOutput,
        didFinishRecordingToOutputFileAtURL: platform.Foundation.NSURL,
        fromConnections: List<*>,
        error: platform.Foundation.NSError?,
    ) {
        if (error != null) return
        val path  = didFinishRecordingToOutputFileAtURL.path ?: return
        val bytes = platform.Foundation.NSData.dataWithContentsOfFile(path) ?: return
        val duration = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong() - startMs
        // Convert NSData to ByteArray
        val byteArray = ByteArray(bytes.length.toInt()).also {
            platform.Foundation.NSInputStream(data = bytes).run { open(); read(it, bytes.length.toInt()); close() }
        }
        onVideoReady(byteArray, duration)
        platform.Foundation.NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}
