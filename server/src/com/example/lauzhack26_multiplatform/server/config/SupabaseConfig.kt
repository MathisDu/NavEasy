package com.example.lauzhack26_multiplatform.server.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*

data class SupabaseConfig(
    val projectUrl: String,
    val apiKey: String
) {
    fun restBaseUrl(): String {
        return "$projectUrl/rest/v1"
    }

    fun createHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}
