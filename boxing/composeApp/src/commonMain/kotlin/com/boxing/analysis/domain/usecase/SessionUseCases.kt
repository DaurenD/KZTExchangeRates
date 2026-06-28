package com.boxing.analysis.domain.usecase

import com.boxing.analysis.data.remote.BoxingApi
import com.boxing.analysis.data.remote.CreateSessionRequest
import com.boxing.analysis.domain.model.AnalysisState
import com.boxing.analysis.domain.model.SessionResults
import com.boxing.analysis.domain.model.SessionSummary
import kotlinx.coroutines.delay

class GetSessionHistoryUseCase(private val api: BoxingApi) {
    suspend operator fun invoke(page: Int = 1): List<SessionSummary> =
        api.listSessions(page = page).sessions
}

class GetSessionResultsUseCase(private val api: BoxingApi) {
    suspend operator fun invoke(sessionId: String): SessionResults =
        api.getResults(sessionId)
}

class SubmitSessionUseCase(private val api: BoxingApi) {
    /**
     * 1. Create session record → get GCS upload URL
     * 2. Upload video bytes directly to GCS
     * 3. Trigger analysis
     * Returns the session ID.
     */
    suspend operator fun invoke(
        sessionType: String,
        startedAt: String,
        endedAt: String,
        durationMs: Long,
        videoBytes: ByteArray,
    ): String {
        val response = api.createSession(
            CreateSessionRequest(sessionType, startedAt, endedAt, durationMs)
        )
        api.uploadVideo(response.uploadUrl, videoBytes)
        api.triggerAnalysis(response.sessionId)
        return response.sessionId
    }
}

class PollAnalysisStatusUseCase(private val api: BoxingApi) {
    /**
     * Polls every [intervalMs] until state is COMPLETE or FAILED.
     * Emits each intermediate state via [onStatus].
     */
    suspend operator fun invoke(
        sessionId: String,
        intervalMs: Long = 5_000L,
        onStatus: (String) -> Unit,
    ): AnalysisState {
        while (true) {
            val state = api.getStatus(sessionId).analysisState
            onStatus(state)
            when (state) {
                "COMPLETE" -> return AnalysisState.COMPLETE
                "FAILED"   -> return AnalysisState.FAILED
                else       -> delay(intervalMs)
            }
        }
    }
}

class GetProgressUseCase(private val api: BoxingApi) {
    suspend operator fun invoke() = api.getProgress().dataPoints
}
