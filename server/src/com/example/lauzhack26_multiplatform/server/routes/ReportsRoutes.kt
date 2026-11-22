package com.example.lauzhack26_multiplatform.server.routes

import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportStatus
import com.example.lauzhack26_multiplatform.model.Severity
import com.example.lauzhack26_multiplatform.server.repository.ReportRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.time.Instant
import java.time.ZoneOffset

// Threshold: when resolved_votes reaches this number, the report is marked as resolved
private const val VOTE_THRESHOLD = 5

fun Route.reportsRoutes(repository: ReportRepository) {

    route("/api/reports") {

        get {
            val reports = repository.getAll()
            call.respond(reports)
        }

        post {
            call.response.headers.append(HttpHeaders.AccessControlAllowOrigin, "*")
            call.response.headers.append(HttpHeaders.AccessControlAllowMethods, "GET, POST, PATCH, OPTIONS")
            call.response.headers.append(HttpHeaders.AccessControlAllowHeaders, "Content-Type")
            val reportRequest = call.receive<ReportRequest>()
            // Supabase generates the UUID automatically via gen_random_uuid()
            val report = Report(
                id = "",
                userId = null,
                title = reportRequest.title,
                description = reportRequest.description,
                category = reportRequest.category,
                location = reportRequest.location,
                disabilityTypes = reportRequest.disabilityTypes,
                severity = reportRequest.severity,
                status = ReportStatus.ACTIVE,
                createdAt = Instant.now().atOffset(ZoneOffset.UTC).toString()
            )
            val saved = repository.add(report)
            call.respond(HttpStatusCode.Created, saved)
        }
    }

    // Get a single report by ID
    get("/api/reports/{id}") {
        call.response.headers.append(HttpHeaders.AccessControlAllowOrigin, "*")
        call.response.headers.append(HttpHeaders.AccessControlAllowMethods, "GET, POST, PATCH, OPTIONS")
        call.response.headers.append(HttpHeaders.AccessControlAllowHeaders, "Content-Type")
        val id = call.parameters["id"] ?: throw Exception("Report ID is required")
        val report = repository.getById(id)
        if (report != null) {
            call.respond(report)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Report not found"))
        }
    }

    // Vote to resolve a report
    patch("/api/reports/{id}") {
        call.response.headers.append(HttpHeaders.AccessControlAllowOrigin, "*")
        call.response.headers.append(HttpHeaders.AccessControlAllowMethods, "GET, POST, PATCH, OPTIONS")
        call.response.headers.append(HttpHeaders.AccessControlAllowHeaders, "Content-Type")
        val id = call.parameters["id"] ?: throw Exception("Report ID is required")
        
        // Get current report
        val currentReport = repository.getById(id)
            ?: throw Exception("Report not found")
        
        // Increment the resolved vote count
        val afterVote = repository.incrementVote(id, "resolve")
            ?: throw Exception("Failed to vote on report")
        
        // Check if threshold is reached — if so, mark as resolved
        if (afterVote.resolvedVotes >= VOTE_THRESHOLD && afterVote.status != ReportStatus.RESOLVED) {
            val updated = repository.updateStatus(id, ReportStatus.RESOLVED) ?: afterVote
            call.respond(updated)
        } else {
            call.respond(afterVote)
        }
    }
}


// Separate request class to avoid sending the id from the client
@kotlinx.serialization.Serializable
data class ReportRequest(
    val title: String,
    val description: String,
    val category: String,
    val location: Location,
    val disabilityTypes: List<String>,
    val severity: Severity = Severity.MEDIUM
)
