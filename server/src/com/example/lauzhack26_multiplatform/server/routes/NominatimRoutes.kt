package com.example.lauzhack26_multiplatform.server.routes

import com.example.lauzhack26_multiplatform.server.service.NominatimService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.nominatimRoutes(nominatimService: NominatimService) {

    post("/api/nominatim/search") {
        val request = call.receive<NominatimSearchRequest>()
        try {
            val result = nominatimService.search(request.query)
            call.respond(NominatimSearchResponse(result))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Nominatim API error: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }
}

@Serializable
data class NominatimSearchRequest(val query: String)

@Serializable
data class NominatimSearchResponse(val result: String)
