package com.example.lauzhack26_multiplatform.server.service

import com.example.lauzhack26_multiplatform.model.*
import com.example.lauzhack26_multiplatform.server.repository.ReportRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*

class OpenRouteServiceService(
    private val repository: ReportRepository,
    private val httpClient: HttpClient
) {

    private val apiKey: String? = System.getenv("ORS_API_KEY")

    suspend fun calculateRoute(request: RouteRequest): RouteResponse {
        if (apiKey.isNullOrEmpty()) {
            throw IllegalStateException("ORS_API_KEY environment variable is not set. Set it to enable routing via OpenRouteService.")
        }

        val allReports = repository.getAll()
        val incompatibleReports = allReports.filter { report ->
            request.profile.incompatibleReportTypes.contains(report.type)
        }

        val avoidPolygons = buildAvoidPolygons(incompatibleReports)

        val orsProfile = request.profile.orsProfile

        val body = buildJsonObject {
            put("coordinates", buildJsonArray {
                add(buildJsonArray { add(request.start.lon); add(request.start.lat) })
                add(buildJsonArray { add(request.end.lon); add(request.end.lat) })
            })
            put("profile", orsProfile)
            put("format", "geojson")

            if (avoidPolygons.isNotEmpty()) {
                put("options", buildJsonObject {
                    put("avoid_polygons", avoidPolygons)
                })
            }
        }

        val response = httpClient.post("https://api.openrouteservice.org/v2/directions/{profile}".replace("{profile}", orsProfile)) {
            contentType(ContentType.Application.Json)
            header("Authorization", apiKey)
            setBody(body.toString())
        }

        if (response.status == HttpStatusCode.OK) {
            val json = response.body<JsonElement>()

            // Extract coordinates from GeoJSON LineString
            val coordinates = parseCoordinates(json)
            val distance = parseDistance(json)
            val duration = parseDuration(json)

            return RouteResponse(
                coordinates = coordinates,
                distanceMeters = distance,
                durationSeconds = duration
            )
        } else {
            // Read the error body to include the actual ORS error message
            val errorBody = try {
                response.body<String>()
            } catch (_: Exception) {
                "<unable to read error body>"
            }
            throw Exception("ORS API error ${response.status}: $errorBody")
        }
    }

    private fun buildAvoidPolygons(reports: List<Report>): JsonArray {
        val radiusDegrees = 0.0005 // ~50 meters buffer around each obstacle
        return buildJsonArray {
            for (report in reports) {
                val lat = report.location.lat
                val lon = report.location.lon
                // Build a small polygon (square) around the obstacle
                add(buildJsonArray {
                    add(buildJsonArray {
                        add(buildJsonArray { add(lon - radiusDegrees); add(lat - radiusDegrees) })
                        add(buildJsonArray { add(lon + radiusDegrees); add(lat - radiusDegrees) })
                        add(buildJsonArray { add(lon + radiusDegrees); add(lat + radiusDegrees) })
                        add(buildJsonArray { add(lon - radiusDegrees); add(lat + radiusDegrees) })
                        add(buildJsonArray { add(lon - radiusDegrees); add(lat - radiusDegrees) })
                    })
                })
            }
        }
    }

    private fun parseCoordinates(json: JsonElement): List<Location> {
        val features = json.jsonObject["features"]?.jsonArray ?: return emptyList()
        if (features.isEmpty()) return emptyList()

        val coordinates = features[0]
            .jsonObject["geometry"]
            ?.jsonObject["coordinates"]
            ?.jsonArray ?: return emptyList()

        return coordinates.mapNotNull { coord ->
            val arr = coord.jsonArray
            if (arr.size >= 2) {
                val lon = arr[0].jsonPrimitive.content.toDouble()
                val lat = arr[1].jsonPrimitive.content.toDouble()
                Location(lat = lat, lon = lon)
            } else null
        }
    }

    private fun parseDistance(json: JsonElement): Double {
        return try {
            val features = json.jsonObject["features"]?.jsonArray
            val summary = features?.get(0)?.jsonObject["properties"]
                ?.jsonObject["summary"]
            summary?.jsonObject["distance"]?.jsonPrimitive?.content?.toDouble() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    private fun parseDuration(json: JsonElement): Double {
        return try {
            val features = json.jsonObject["features"]?.jsonArray
            val summary = features?.get(0)?.jsonObject["properties"]
                ?.jsonObject["summary"]
            summary?.jsonObject["duration"]?.jsonPrimitive?.content?.toDouble() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }
}
