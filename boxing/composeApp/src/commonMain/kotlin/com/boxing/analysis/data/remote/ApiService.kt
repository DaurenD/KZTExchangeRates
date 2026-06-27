package com.boxing.analysis.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable data class RegisterRequest(val email: String, val password: String, val name: String, val stance: String = "ORTHODOX")
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class TokenResponse(val accessToken: String, val tokenType: String)
@Serializable data class UserResponse(val id: String, val email: String, val name: String, val stance: String, val experienceLevel: String)
@Serializable data class CreateSessionRequest(val sessionType: String, val startedAt: String, val endedAt: String, val durationMs: Long)
@Serializable data class CreateSessionResponse(val sessionId: String, val uploadUrl: String, val uploadExpiresAt: String)
@Serializable data class SessionStatusResponse(val sessionId: String, val analysisState: String)
@Serializable data class SessionListResponse(val sessions: List<com.boxing.analysis.domain.model.SessionSummary>, val total: Int, val page: Int, val pageSize: Int)
@Serializable data class ProgressResponse(val dataPoints: List<com.boxing.analysis.domain.model.ProgressPoint>, val sessionsAnalysed: Int)

class ApiService(private val client: HttpClient) {

    suspend fun register(email: String, password: String, name: String): TokenResponse =
        client.post("$BASE_URL/auth/register") { setBody(RegisterRequest(email, password, name)) }.body()

    suspend fun login(email: String, password: String): TokenResponse =
        client.post("$BASE_URL/auth/login") { setBody(LoginRequest(email, password)) }.body()

    suspend fun me(): UserResponse =
        client.get("$BASE_URL/auth/me").body()

    suspend fun createSession(request: CreateSessionRequest): CreateSessionResponse =
        client.post("$BASE_URL/sessions") { setBody(request) }.body()

    suspend fun listSessions(page: Int = 1, pageSize: Int = 20): SessionListResponse =
        client.get("$BASE_URL/sessions") {
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

    suspend fun getStatus(sessionId: String): SessionStatusResponse =
        client.get("$BASE_URL/sessions/$sessionId/status").body()

    suspend fun triggerAnalysis(sessionId: String) =
        client.post("$BASE_URL/sessions/$sessionId/analyze")

    suspend fun getResults(sessionId: String): com.boxing.analysis.domain.model.SessionResults =
        client.get("$BASE_URL/sessions/$sessionId/results").body()

    suspend fun getProgress(): ProgressResponse =
        client.get("$BASE_URL/progress").body()

    suspend fun uploadVideo(uploadUrl: String, videoBytes: ByteArray) =
        client.put(uploadUrl) {
            setBody(videoBytes)
            headers.append("Content-Type", "video/mp4")
        }
}
