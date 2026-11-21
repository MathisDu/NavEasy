package com.example.lauzhack26_multiplatform.server.routes

import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.server.repository.InMemoryReportRepository
import com.example.lauzhack26_multiplatform.server.repository.ReportRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.reportsRoutes(repository: ReportRepository) {

    route("/api/reports") {

        get {
            val reports = repository.getAll()
            call.respond(reports)
        }

        post {
            val reportRequest = call.receive<ReportRequest>()
            val id = InMemoryReportRepository.generateId()
            val report = Report(
                id = id,
                type = reportRequest.type,
                description = reportRequest.description,
                location = reportRequest.location
            )
            val saved = repository.add(report)
            call.respond(HttpStatusCode.Created, saved)
        }
    }
}

// Separate request class to avoid sending the id from the client
@kotlinx.serialization.Serializable
data class ReportRequest(
    val type: com.example.lauzhack26_multiplatform.model.ReportType,
    val description: String,
    val location: com.example.lauzhack26_multiplatform.model.Location
)
