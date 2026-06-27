package com.boxing.analysis.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val BASE_URL = "https://api.boxing-analysis.com"

fun buildHttpClient(tokenProvider: () -> String?): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    install(Auth) {
        bearer {
            loadTokens {
                tokenProvider()?.let { BearerTokens(it, "") }
            }
        }
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}
