package com.example.lauzhack26_multiplatform.server.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.URLEncoder

class NominatimService(
    private val httpClient: HttpClient
) {

    /**
     * Proxy a Nominatim search query to avoid CORS issues when calling from the browser.
     * @param query the search query string (address, place name, etc.)
     * @return the raw JSON response from Nominatim API
     */
    suspend fun search(query: String): String {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8)
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=jsonv2&limit=5&addressdetails=1"

        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, "Lauzhack26-Multiplatform/1.0 (https://github.com/Lauzhack26-Multiplatform)")
            header(HttpHeaders.AcceptLanguage, "fr,en")
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body<String>()
        } else {
            val errorBody = response.body<String>()
            throw Exception("Nominatim API error ${response.status}: $errorBody")
        }
    }
}
