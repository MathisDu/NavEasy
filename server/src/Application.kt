package com.example.lauzhack26_multiplatform.server

import com.example.lauzhack26_multiplatform.server.repository.InMemoryReportRepository
import com.example.lauzhack26_multiplatform.server.routes.reportsRoutes
import com.example.lauzhack26_multiplatform.server.routes.routeRoutes
import com.example.lauzhack26_multiplatform.server.service.OpenRouteServiceService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(CORS) {
        allowHost("localhost:8080")
        allowHost("127.0.0.1:8080")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.Accept)
        allowNonSimpleContentTypes = true
    }

    install(ContentNegotiation) {
        json()
    }

    val reportRepository = InMemoryReportRepository()
    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json()
        }
    }
    val orsService = OpenRouteServiceService(reportRepository, httpClient)

    routing {
        get("/") {
            call.respondText("Navigation Accessibility API")
        }

        reportsRoutes(reportRepository)
        routeRoutes(orsService)
    }
}
