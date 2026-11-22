package com.example.lauzhack26_multiplatform.server.routes

import com.example.lauzhack26_multiplatform.server.service.OverpassService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.overpassRoutes(overpassService: OverpassService) {

    post("/api/overpass") {
        val request = call.receive<OverpassQueryRequest>()
        try {
            val result = overpassService.queryOverpass(request.data)
            call.respond(OverpassQueryResponse(result))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Overpass API error: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }
}

@Serializable
data class OverpassQueryRequest(val data: String)

@Serializable
data class OverpassQueryResponse(val result: String)
