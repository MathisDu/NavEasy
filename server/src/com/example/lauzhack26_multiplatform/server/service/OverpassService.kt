package com.example.lauzhack26_multiplatform.server.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.URLEncoder

class OverpassService(
    private val httpClient: HttpClient
) {

    /**
     * Proxy an Overpass API query to avoid CORS issues when calling from the browser.
     * @param data the Overpass QL query string
     * @return the raw JSON response from Overpass API
     */
    suspend fun queryOverpass(data: String): String {
        val encodedData = URLEncoder.encode(data, Charsets.UTF_8)
        val url = "https://overpass-api.de/api/interpreter?data=$encodedData"

        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, "Lauzhack26-Multiplatform/1.0 (https://github.com/Lauzhack26-Multiplatform)")
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body<String>()
        } else {
            val errorBody = response.body<String>()
            throw Exception("Overpass API error ${response.status}: $errorBody")
        }
    }
}
