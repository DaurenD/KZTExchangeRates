package com.boxing.analysis.domain.usecase

import com.boxing.analysis.data.remote.BoxingApi
import com.boxing.analysis.data.remote.CreateSessionRequest
import com.boxing.analysis.data.remote.CreateSessionResponse
import com.boxing.analysis.data.remote.ProgressResponse
import com.boxing.analysis.data.remote.SessionListResponse
import com.boxing.analysis.data.remote.SessionStatusResponse
import com.boxing.analysis.domain.model.AnalysisState
import com.boxing.analysis.domain.model.SessionResults
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Task 7 — PollAnalysisStatusUseCase
 * Acceptance criteria:
 *   AC1: returns COMPLETE when API returns COMPLETE
 *   AC2: returns FAILED when API returns FAILED
 *   AC3: onStatus callback is called for each poll
 *   AC4: stops polling immediately on first terminal state
 */
class PollAnalysisStatusUseCaseTest {

    // AC1
    @Test
    fun returnsCompleteWhenApiReturnsComplete() = runTest {
        val api = FakeBoxingApi(statusSequence = listOf("COMPLETE"))
        val result = PollAnalysisStatusUseCase(api).invoke("s1", intervalMs = 0L, onStatus = {})
        assertEquals(AnalysisState.COMPLETE, result)
    }

    // AC2
    @Test
    fun returnsFailedWhenApiReturnsFailed() = runTest {
        val api = FakeBoxingApi(statusSequence = listOf("FAILED"))
        val result = PollAnalysisStatusUseCase(api).invoke("s1", intervalMs = 0L, onStatus = {})
        assertEquals(AnalysisState.FAILED, result)
    }

    // AC3
    @Test
    fun onStatusCalledForEveryPollResponse() = runTest {
        val sequence = listOf("PENDING", "PROCESSING", "COMPLETE")
        val api = FakeBoxingApi(statusSequence = sequence)
        val observed = mutableListOf<String>()

        PollAnalysisStatusUseCase(api).invoke("s1", intervalMs = 0L, onStatus = { observed += it })

        assertEquals(sequence, observed)
    }

    // AC4
    @Test
    fun stopsPollingImmediatelyAfterTerminalState() = runTest {
        // COMPLETE first; anything after should never be fetched
        val api = FakeBoxingApi(statusSequence = listOf("COMPLETE", "PROCESSING", "PROCESSING"))
        PollAnalysisStatusUseCase(api).invoke("s1", intervalMs = 0L, onStatus = {})
        assertEquals(1, api.callCount)
    }
}

// ── Minimal fake — only implements what the tests need ─────────────────────

private class FakeBoxingApi(private val statusSequence: List<String>) : BoxingApi {
    var callCount = 0

    override suspend fun getStatus(sessionId: String): SessionStatusResponse {
        val state = statusSequence[callCount.coerceAtMost(statusSequence.lastIndex)]
        callCount++
        return SessionStatusResponse(sessionId = sessionId, analysisState = state)
    }

    override suspend fun createSession(request: CreateSessionRequest): CreateSessionResponse = error("unused")
    override suspend fun listSessions(page: Int, pageSize: Int): SessionListResponse = error("unused")
    override suspend fun triggerAnalysis(sessionId: String) = Unit
    override suspend fun getResults(sessionId: String): SessionResults = error("unused")
    override suspend fun getProgress(): ProgressResponse = error("unused")
    override suspend fun uploadVideo(uploadUrl: String, videoBytes: ByteArray) = Unit
}
