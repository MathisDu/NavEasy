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

        val response = httpClient.post("https://api.openrouteservice.org/v2/directions/$orsProfile") {
            contentType(ContentType.Application.Json)
            header("Authorization", apiKey)
            setBody(body)
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
        val routes = json.jsonObject["routes"]?.jsonArray ?: return emptyList()
        if (routes.isEmpty()) return emptyList()

        val route = routes[0].jsonObject

        // Check if geometry is a GeoJSON object or an encoded polyline string
        val geometry = route["geometry"]
        if (geometry is JsonObject) {
            // GeoJSON format: geometry.coordinates is an array of [lon, lat]
            val coordinates = geometry["coordinates"]?.jsonArray ?: return emptyList()
            return coordinates.mapNotNull { coord ->
                val arr = coord.jsonArray
                if (arr.size >= 2) {
                    val lon = arr[0].jsonPrimitive.content.toDouble()
                    val lat = arr[1].jsonPrimitive.content.toDouble()
                    Location(lat = lat, lon = lon)
                } else null
            }
        } else if (geometry is kotlinx.serialization.json.JsonPrimitive && geometry.isString) {
            // Encoded polyline format (Polyline6 encoding used by ORS)
            val encoded = geometry.content
            return decodePolyline6(encoded)
        }

        return emptyList()
    }

    /**
     * Decode a Polyline6 encoded string (used by OpenRouteService) into a list of coordinates.
     * Based on the openrouteservice-py reference implementation.
     */
    private fun decodePolyline6(encoded: String): List<Location> {
        val coordinates = mutableListOf<Location>()
        var lat = 0
        var lng = 0
        val len = encoded.length

        var i = 0
        while (i < len) {
            // Decode latitude
            var result = 1
            var shift = 0
            var b: Int
            do {
                if (i >= len) break
                b = encoded[i].code - 63 - 1
                i++
                result += b shl shift
                shift += 5
            } while (b >= 0x1F)
            // Python: lat += (~result >> 1) if (result & 1) != 0 else (result >> 1)
            // ~result in Python = result.inv() in Kotlin (both are two's complement NOT)
            // >> in Python is arithmetic shift = shr in Kotlin
            lat += if ((result and 1) != 0) (result.inv()).shr(1) else (result shr 1)

            // Decode longitude
            result = 1
            shift = 0
            do {
                if (i >= len) break
                b = encoded[i].code - 63 - 1
                i++
                result += b shl shift
                shift += 5
            } while (b >= 0x1F)
            // Python: lng += ~(result >> 1) if (result & 1) != 0 else (result >> 1)
            lng += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

            coordinates.add(Location(lat = lat * 1e-5, lon = lng * 1e-5))
        }

        return coordinates
    }

    private fun parseDistance(json: JsonElement): Double {
        return try {
            val routes = json.jsonObject["routes"]?.jsonArray
            val summary = routes?.get(0)?.jsonObject["summary"]
            summary?.jsonObject["distance"]?.jsonPrimitive?.content?.toDouble() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    private fun parseDuration(json: JsonElement): Double {
        return try {
            val routes = json.jsonObject["routes"]?.jsonArray
            val summary = routes?.get(0)?.jsonObject["summary"]
            summary?.jsonObject["duration"]?.jsonPrimitive?.content?.toDouble() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }
}
