package com.example.lauzhack26_multiplatform.server

import com.example.lauzhack26_multiplatform.server.repository.InMemoryReportRepository
import com.example.lauzhack26_multiplatform.server.routes.reportsRoutes
import com.example.lauzhack26_multiplatform.server.routes.routeRoutes
import com.example.lauzhack26_multiplatform.server.service.OpenRouteServiceService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    val reportRepository = InMemoryReportRepository()
    val httpClient = HttpClient(CIO)
    val orsService = OpenRouteServiceService(reportRepository, httpClient)

    routing {
        get("/") {
            call.respondText("Navigation Accessibility API")
        }

        reportsRoutes(reportRepository)
        routeRoutes(orsService)
    }
}
