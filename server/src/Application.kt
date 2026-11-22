package com.example.lauzhack26_multiplatform.server

import com.example.lauzhack26_multiplatform.server.config.SupabaseConfig
import com.example.lauzhack26_multiplatform.server.repository.SupabaseReportRepository
import com.example.lauzhack26_multiplatform.server.repository.InMemoryReportRepository
import com.example.lauzhack26_multiplatform.server.routes.nominatimRoutes
import com.example.lauzhack26_multiplatform.server.routes.overpassRoutes
import com.example.lauzhack26_multiplatform.server.routes.reportsRoutes
import com.example.lauzhack26_multiplatform.server.routes.routeRoutes
import com.example.lauzhack26_multiplatform.server.service.NominatimService
import com.example.lauzhack26_multiplatform.server.service.OverpassService
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

    // Read Supabase REST API credentials from environment variables
    val supabaseUrl = System.getenv("SUPABASE_URL")
        ?: throw IllegalStateException("SUPABASE_URL environment variable is not set. " +
            "Set it to your Supabase project URL, e.g.: https://your-ref.supabase.co")
    val supabaseKey = System.getenv("SUPABASE_ANON_KEY")
        ?: throw IllegalStateException("SUPABASE_ANON_KEY environment variable is not set. " +
            "Set it to your Supabase publishable (anon) API key, e.g.: sb_publishable_...")

    val supabaseConfig = SupabaseConfig(
        projectUrl = supabaseUrl,
        apiKey = supabaseKey
    )

    val reportRepository = SupabaseReportRepository(supabaseConfig)
    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json()
        }
    }
    val orsService = OpenRouteServiceService(httpClient)
    val overpassService = OverpassService(httpClient)
    val nominatimService = NominatimService(httpClient)

    routing {
        get("/") {
            call.respondText("Navigation Accessibility API")
        }

        reportsRoutes(reportRepository)
        routeRoutes(orsService)
        overpassRoutes(overpassService)
        nominatimRoutes(nominatimService)
    }
}
