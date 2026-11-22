package com.example.lauzhack26_multiplatform.server.routes

import com.example.lauzhack26_multiplatform.model.RouteRequest
import com.example.lauzhack26_multiplatform.server.service.OpenRouteServiceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.routeRoutes(orsService: OpenRouteServiceService) {

    post("/api/route") {
        val request = call.receive<RouteRequest>()
        try {
            val response = orsService.calculateRoute(request)
            call.respond(response)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }
}
