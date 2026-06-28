package com.boxing.analysis.data.remote

import com.boxing.analysis.domain.model.ProgressPoint
import com.boxing.analysis.domain.model.SessionResults
import com.boxing.analysis.domain.model.SessionSummary

/** Minimal interface the use-cases depend on — keeps them testable without a real HTTP client. */
interface BoxingApi {
    suspend fun createSession(request: CreateSessionRequest): CreateSessionResponse
    suspend fun listSessions(page: Int = 1, pageSize: Int = 20): SessionListResponse
    suspend fun getStatus(sessionId: String): SessionStatusResponse
    suspend fun triggerAnalysis(sessionId: String)
    suspend fun getResults(sessionId: String): SessionResults
    suspend fun getProgress(): ProgressResponse
    suspend fun uploadVideo(uploadUrl: String, videoBytes: ByteArray)
}
