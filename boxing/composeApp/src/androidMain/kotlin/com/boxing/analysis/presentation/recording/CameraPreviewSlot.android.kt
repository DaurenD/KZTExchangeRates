package com.boxing.analysis.presentation.recording

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
actual fun CameraPreviewSlot(
    isRecording: Boolean,
    modifier: Modifier,
    onVideoReady: (ByteArray, Long) -> Unit,
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView   = remember { PreviewView(context) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recordingStartMs by remember { mutableLongStateOf(0L) }

    // Bind camera when composable enters composition
    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview  = androidx.camera.core.Preview.Builder().build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        val vc = VideoCapture.withOutput(recorder)
        videoCapture = vc

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, vc)
    }

    // Start / stop recording when [isRecording] changes
    LaunchedEffect(isRecording) {
        val vc = videoCapture ?: return@LaunchedEffect
        if (isRecording) {
            val tmpFile = File(context.cacheDir, "boxing_${System.currentTimeMillis()}.mp4")
            val output  = FileOutputOptions.Builder(tmpFile).build()
            recordingStartMs = System.currentTimeMillis()
            activeRecording = vc.output
                .prepareRecording(context, output)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                        val durationMs = System.currentTimeMillis() - recordingStartMs
                        onVideoReady(tmpFile.readBytes(), durationMs)
                        tmpFile.delete()
                    }
                }
        } else {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
