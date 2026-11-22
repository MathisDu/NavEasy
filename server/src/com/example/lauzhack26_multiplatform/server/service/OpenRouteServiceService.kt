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

        // 1. Récupération des obstacles signalés incompatibles avec le profil
        val allReports = repository.getAll()
        val incompatibleReports = allReports.filter { report ->
            val reportType = try { ReportType.valueOf(report.category) } catch (e: IllegalArgumentException) { null }
            reportType != null && request.profile.incompatibleReportTypes.contains(reportType)
        }

        // 2. Génération des polygones d'évitement automatiques (carrés autour des obstacles)
        val autoAvoidPolygons = buildObstaclePolygons(incompatibleReports)

        // 3. Fusion avec les polygones dessinés manuellement par l'utilisateur dans la requête
        val allAvoidPolygonCoordinates = request.avoidPolygons + autoAvoidPolygons

        val orsProfile = request.profile.orsProfile

        val body = buildJsonObject {
            put("coordinates", buildJsonArray {
                add(buildJsonArray { add(request.start.lon); add(request.start.lat) })
                add(buildJsonArray { add(request.end.lon); add(request.end.lat) })
            })
            put("profile", orsProfile)
            put("format", "geojson")

            // Construction correcte du GeoJSON pour ORS (Polygon ou MultiPolygon)
            if (allAvoidPolygonCoordinates.isNotEmpty()) {
                put("options", buildJsonObject {
                    if (allAvoidPolygonCoordinates.size == 1) {
                        put("avoid_polygons", buildJsonObject {
                            put("type", "Polygon")
                            put("coordinates", buildJsonArray {
                                add(buildJsonArray {
                                    for (coord in allAvoidPolygonCoordinates[0]) {
                                        add(buildJsonArray { add(coord.lon); add(coord.lat) })
                                    }
                                })
                            })
                        })
                    } else {
                        put("avoid_polygons", buildJsonObject {
                            put("type", "MultiPolygon")
                            put("coordinates", buildJsonArray {
                                for (polygon in allAvoidPolygonCoordinates) {
                                    add(buildJsonArray {
                                        add(buildJsonArray {
                                            for (coord in polygon) {
                                                add(buildJsonArray { add(coord.lon); add(coord.lat) })
                                            }
                                        })
                                    })
                                }
                            })
                        })
                    }
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

            val coordinates = parseCoordinates(json)
            val distance = parseDistance(json)
            val duration = parseDuration(json)

            return RouteResponse(
                coordinates = coordinates,
                distanceMeters = distance,
                durationSeconds = duration
            )
        } else {
            val errorBody = try {
                response.body<String>()
            } catch (_: Exception) {
                "<unable to read error body>"
            }
            throw Exception("ORS API error ${response.status}: $errorBody")
        }
    }

    /**
     * Convertit les signalements en listes de coordonnées (polygones d'évitement).
     */
    private fun buildObstaclePolygons(reports: List<Report>): List<List<Location>> {
        val radiusDegrees = 0.0005 // ~50 meters buffer around each obstacle
        return reports.map { report ->
            val lat = report.location.lat
            val lon = report.location.lon
            listOf(
                Location(lat = lat - radiusDegrees, lon = lon - radiusDegrees),
                Location(lat = lat - radiusDegrees, lon = lon + radiusDegrees),
                Location(lat = lat + radiusDegrees, lon = lon + radiusDegrees),
                Location(lat = lat + radiusDegrees, lon = lon - radiusDegrees),
                Location(lat = lat - radiusDegrees, lon = lon - radiusDegrees) // Fermeture du polygone
            )
        }
    }

    private fun parseCoordinates(json: JsonElement): List<Location> {
        val routes = json.jsonObject["routes"]?.jsonArray ?: return emptyList()
        if (routes.isEmpty()) return emptyList()

        val route = routes[0].jsonObject

        val geometry = route["geometry"]
        if (geometry is JsonObject) {
            val coordinates = geometry["coordinates"]?.jsonArray ?: return emptyList()
            return coordinates.mapNotNull { coord ->
                val arr = coord.jsonArray
                if (arr.size >= 2) {
                    val lon = arr[0].jsonPrimitive.content.toDouble()
                    val lat = arr[1].jsonPrimitive.content.toDouble()
                    Location(lat = lat, lon = lon)
                } else null
            }
        } else if (geometry is JsonPrimitive && geometry.isString) {
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
            lat += if ((result and 1) != 0) (result.inv()).shr(1) else (result shr 1)

            result = 1
            shift = 0
            do {
                if (i >= len) break
                b = encoded[i].code - 63 - 1
                i++
                result += b shl shift
                shift += 5
            } while (b >= 0x1F)
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