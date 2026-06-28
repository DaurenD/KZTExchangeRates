package com.boxing.analysis.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxing.analysis.domain.usecase.SubmitSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface RecordingState {
    data class Idle(val sessionType: String = "SHADOWBOXING") : RecordingState
    data class Recording(val sessionType: String, val startMs: Long, val elapsedMs: Long = 0L) : RecordingState
    data object Uploading : RecordingState
    data class Submitted(val sessionId: String) : RecordingState
}

class RecordingViewModel(
    private val submitSession: SubmitSessionUseCase = TODO("inject"),
) : ViewModel() {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null

    fun setSessionType(type: String) {
        val current = _state.value
        if (current is RecordingState.Idle) _state.value = current.copy(sessionType = type)
    }

    fun startRecording() {
        val idle = _state.value as? RecordingState.Idle ?: return
        val startMs = Clock.System.now().toEpochMilliseconds()
        _state.value = RecordingState.Recording(idle.sessionType, startMs)
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val s = _state.value as? RecordingState.Recording ?: break
                _state.value = s.copy(elapsedMs = Clock.System.now().toEpochMilliseconds() - s.startMs)
            }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        // Signal to CameraPreviewSlot to stop — it will call onVideoReady with the file bytes
        val s = _state.value as? RecordingState.Recording ?: return
        _state.value = RecordingState.Recording(s.sessionType, s.startMs, s.elapsedMs) // UI shows stop triggered
    }

    fun onVideoReady(videoBytes: ByteArray, durationMs: Long) {
        val s = _state.value as? RecordingState.Recording ?: return
        val startMs = s.startMs
        val endMs   = startMs + durationMs
        _state.value = RecordingState.Uploading

        viewModelScope.launch {
            runCatching {
                submitSession(
                    sessionType = s.sessionType,
                    startedAt   = startMs.toIsoString(),
                    endedAt     = endMs.toIsoString(),
                    durationMs  = durationMs,
                    videoBytes  = videoBytes,
                )
            }.onSuccess { id ->
                _state.value = RecordingState.Submitted(id)
            }.onFailure {
                _state.value = RecordingState.Idle(s.sessionType)
            }
        }
    }

    private fun Long.toIsoString(): String {
        // kotlinx-datetime ISO-8601 instant string
        return kotlinx.datetime.Instant.fromEpochMilliseconds(this).toString()
    }
}
