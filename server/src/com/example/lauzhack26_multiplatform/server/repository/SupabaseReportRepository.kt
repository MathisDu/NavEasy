package com.example.lauzhack26_multiplatform.server.repository

import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportStatus
import com.example.lauzhack26_multiplatform.model.Severity
import com.example.lauzhack26_multiplatform.server.config.SupabaseConfig
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


// DTOs for the flat JSON structure returned by PostgREST
@Serializable
data class ReportDto(
    val id: String,
    @SerialName("user_id")
    val userId: String?,
    val title: String,
    val description: String,
    val category: String,
    @SerialName("latitude")
    val lat: Double,
    @SerialName("longitude")
    val lon: Double,
    @SerialName("disability_types")
    val disabilityTypes: List<String>,
    val severity: String,
    val status: String,
    @SerialName("resolved_votes")
    val resolvedVotes: Int = 0,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class ReportInsertDto(
    val title: String,
    val description: String,
    val category: String,
    @SerialName("latitude")
    val lat: Double,
    @SerialName("longitude")
    val lon: Double,
    @SerialName("disability_types")
    val disabilityTypes: List<String>,
    val severity: String,
    val status: String
)

class SupabaseReportRepository(
    private val supabaseConfig: SupabaseConfig
) : ReportRepository {

    private var httpClient: HttpClient? = null

    private fun getClient(): HttpClient {
        return httpClient ?: supabaseConfig.createHttpClient().also { httpClient = it }
    }

    private fun HttpRequestBuilder.supabaseHeaders() {
        headers {
            append("apikey", supabaseConfig.apiKey)
            append("Authorization", "Bearer ${supabaseConfig.apiKey}")
            append("Prefer", "return=representation")
        }
    }

    override suspend fun getAll(): List<Report> {
        val baseUrl = supabaseConfig.restBaseUrl()
        val reports: List<ReportDto> = getClient().get("$baseUrl/reports") {
            supabaseHeaders()
        }.body()
        return reports.map { it.toReport() }
    }

    override suspend fun getById(id: String): Report? {
        return try {
            val baseUrl = supabaseConfig.restBaseUrl()
            val reports: List<ReportDto> = getClient().get("$baseUrl/reports?id=eq.$id") {
                supabaseHeaders()
            }.body()
            reports.firstOrNull()?.toReport()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun add(report: Report): Report {
        val insertDto = ReportInsertDto(
            title = report.title,
            description = report.description,
            category = report.category,
            lat = report.location.lat,
            lon = report.location.lon,
            disabilityTypes = report.disabilityTypes,
            severity = report.severity.label,
            status = report.status.label
        )
        val baseUrl = supabaseConfig.restBaseUrl()
        val created: List<ReportDto> = getClient().post("$baseUrl/reports") {
            supabaseHeaders()
            contentType(ContentType.Application.Json)
            setBody(insertDto)
        }.body()
        return created.firstOrNull()?.toReport()
            ?: throw Exception("Failed to create report: empty response from Supabase")
    }

    override suspend fun remove(id: String): Boolean {
        return try {
            val baseUrl = supabaseConfig.restBaseUrl()
            getClient().delete("$baseUrl/reports?id=eq.$id") {
                supabaseHeaders()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateStatus(id: String, status: ReportStatus): Report? {
        return try {
            val baseUrl = supabaseConfig.restBaseUrl()
            val json = Json { ignoreUnknownKeys = true }
            val obj = buildJsonObject {
                put("status", status.label)
            }
            val body = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), obj)
            val updated: List<ReportDto> = getClient().patch("$baseUrl/reports?id=eq.$id") {
                supabaseHeaders()
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
            updated.firstOrNull()?.toReport()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun incrementVote(id: String, voteType: String): Report? {
        return try {
            val baseUrl = supabaseConfig.restBaseUrl()
            val json = Json { ignoreUnknownKeys = true }
            
            // Get current report to read vote counts
            val current: List<ReportDto> = getClient().get("$baseUrl/reports?id=eq.$id") {
                supabaseHeaders()
            }.body()
            val reportDto = current.firstOrNull() ?: return null
            
            val newResolvedVotes = reportDto.resolvedVotes + 1
            
            val obj = buildJsonObject {
                put("resolved_votes", newResolvedVotes)
            }
            val updateBody = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), obj)
            
            val updated: List<ReportDto> = getClient().patch("$baseUrl/reports?id=eq.$id") {
                supabaseHeaders()
                contentType(ContentType.Application.Json)
                setBody(updateBody)
            }.body()
            updated.firstOrNull()?.toReport()
        } catch (e: Exception) {
            null
        }
    }

    private fun ReportDto.toReport(): Report {
        val severity = try {
            Severity.valueOf(severity.uppercase())
        } catch (e: IllegalArgumentException) {
            Severity.MEDIUM
        }
        val status = try {
            ReportStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            ReportStatus.ACTIVE
        }
        return Report(
            id = id,
            userId = userId,
            title = title,
            description = description,
            category = category,
            location = Location(lat = lat, lon = lon),
            disabilityTypes = disabilityTypes,
            severity = severity,
            status = status,
            resolvedVotes = resolvedVotes,
            createdAt = createdAt
        )
    }

    fun close() {
        httpClient?.close()
    }
}
