package com.example.lauzhack26_multiplatform.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.MobilityProfile
import com.example.lauzhack26_multiplatform.model.OsmFeature
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportType
import com.example.lauzhack26_multiplatform.model.RouteRequest
import com.example.lauzhack26_multiplatform.model.RouteResponse
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

class MapViewModel(
    private val httpClient: HttpClient,
    private val serverUrl: String = "http://localhost:8081"
) : ViewModel() {

    private val _selectedProfile = MutableStateFlow(MobilityProfile.WHEELCHAIR)
    val selectedProfile: StateFlow<MobilityProfile> = _selectedProfile.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _route = MutableStateFlow<List<Location>>(emptyList())
    val route: StateFlow<List<Location>> = _route.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showReportForm = MutableStateFlow(false)
    val showReportForm: StateFlow<Boolean> = _showReportForm.asStateFlow()

    private val _reportLocation = MutableStateFlow<Location?>(null)
    val reportLocation: StateFlow<Location?> = _reportLocation.asStateFlow()

    private val _osmFeatures = MutableStateFlow<List<OsmFeature>>(emptyList())
    val osmFeatures: StateFlow<List<OsmFeature>> = _osmFeatures.asStateFlow()

    fun setProfile(profile: MobilityProfile) {
        _selectedProfile.value = profile
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = httpClient.get("$serverUrl/api/reports")
                if (response.status == HttpStatusCode.OK) {
                    val reportsList = response.body<List<Report>>()
                    _reports.value = reportsList
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur chargement signalements: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addReport(type: ReportType, description: String, location: Location) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = ReportRequest(type, description, location)
                val response = httpClient.post("$serverUrl/api/reports") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.Created) {
                    loadReports()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur ajout signalement: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
                _showReportForm.value = false
            }
        }
    }

    fun calculateRoute(start: Location, end: Location) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = RouteRequest(start, end, _selectedProfile.value)
                val response = httpClient.post("$serverUrl/api/route") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.OK) {
                    val routeResponse = response.body<RouteResponse>()
                    _route.value = routeResponse.coordinates
                } else {
                    // Try to read the error message from the server response
                    val errorBody = try {
                        response.body<String>()
                    } catch (_: Exception) {
                        "<unable to read error>"
                    }
                    _errorMessage.value = "Erreur calcul itinéraire (${response.status}): $errorBody"
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearRoute() {
        _route.value = emptyList()
    }

    fun openReportForm(location: Location) {
        _reportLocation.value = location
        _showReportForm.value = true
    }

    fun closeReportForm() {
        _showReportForm.value = false
        _reportLocation.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    /**
     * Fetch staircase features from OpenStreetMap using the Overpass API.
     * @param centerLat center latitude of the map viewport
     * @param centerLon center longitude of the map viewport
     * @param zoom current zoom level
     */
    fun loadStepsFromOverpass(centerLat: Double, centerLon: Double, zoom: Int) {
        viewModelScope.launch {
            try {
                // Calculate bounding box from center and zoom
                val (minLat, minLon, maxLat, maxLon) = calculateBoundingBox(centerLat, centerLon, zoom)

                // Build Overpass API query
                val query = """
                    [out:json];
                    node["highway"="steps"]($minLat,$minLon,$maxLat,$maxLon);
                    out;
                """.trimIndent()

                val encodedQuery = encodeOverpassQuery(query)
                val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

                val response = httpClient.get(url)
                if (response.status == HttpStatusCode.OK) {
                    val jsonText = response.body<String>()
                    val features = parseOverpassResponse(jsonText)
                    _osmFeatures.value = features
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    // Silently fail - OSM features are optional
                    e.printStackTrace()
                } else {
                    throw e
                }
            }
        }
    }

    /**
     * Calculate bounding box from map center and zoom level.
     * Returns Pair of (minLat, minLon, maxLat, maxLon).
     */
    private fun calculateBoundingBox(
        centerLat: Double,
        centerLon: Double,
        zoom: Int
    ): BoundingBox {
        // Approximate degrees per pixel at this zoom level
        val degreesPerPixel = 360.0 / (256.0 * 2.0.pow(zoom.toDouble()))
        // Assume a viewport of ~1000x1000 pixels
        val viewportPixels = 1000.0
        val latDelta = (viewportPixels * degreesPerPixel) * 0.6
        val lonDelta = (viewportPixels * degreesPerPixel) * 0.6 / cos(centerLat * PI / 180.0)

        return BoundingBox(
            centerLat - latDelta,
            centerLon - lonDelta,
            centerLat + latDelta,
            centerLon + lonDelta
        )
    }

    data class BoundingBox(
        val minLat: Double,
        val minLon: Double,
        val maxLat: Double,
        val maxLon: Double
    )

    /**
     * Parse Overpass API JSON response into a list of OsmFeature.
     */
    private fun parseOverpassResponse(jsonText: String): List<OsmFeature> {
        val json = Json { ignoreUnknownKeys = true }
        val element = json.parseToJsonElement(jsonText)

        // Overpass response structure: { "elements": [ { "lat": ..., "lon": ..., "tags": { ... } } ] }
        val rootObj = element.jsonObject
        val elements = (rootObj["elements"] as? JsonArray) ?: return emptyList()

        return elements.mapNotNull { elem: JsonElement ->
            val nodeObj = elem.jsonObject
            val lat = nodeObj["lat"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val lon = nodeObj["lon"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val tags = nodeObj["tags"]?.jsonObject ?: return@mapNotNull null

            val ref = tags["ref"]?.jsonPrimitive?.content ?: ""
            val name = tags["name"]?.jsonPrimitive?.content ?: ""
            val description = if (name.isNotEmpty()) name else if (ref.isNotEmpty()) "Escalier (ref: $ref)" else "Escalier"

            OsmFeature(
                location = Location(lat = lat, lon = lon),
                type = "highway=steps",
                description = description
            )
        }
    }

    data class ReportRequest(
        val type: ReportType,
        val description: String,
        val location: Location
    )
}

/**
 * Simple URL encoder for Overpass API queries that works across all platforms.
 */
private fun encodeOverpassQuery(query: String): String {
    val safeChars = setOf('.', '_', '-', '~')
    return buildString {
        for (ch in query) {
            if (ch.isLetterOrDigit() || ch in safeChars) {
                append(ch)
            } else {
                val code = ch.code
                if (code < 128) {
                    // ASCII character
                    append('%')
                    append(code.toHexString())
                } else {
                    // UTF-8 encode
                    if (code < 0x800) {
                        append('%')
                        append((0xC0 or (code ushr 6)).toHexString())
                        append('%')
                        append((0x80 or (code and 0x3F)).toHexString())
                    } else if (code < 0x10000) {
                        append('%')
                        append((0xE0 or (code ushr 12)).toHexString())
                        append('%')
                        append((0x80 or ((code ushr 6) and 0x3F)).toHexString())
                        append('%')
                        append((0x80 or (code and 0x3F)).toHexString())
                    } else {
                        append('%')
                        append((0xF0 or (code ushr 18)).toHexString())
                        append('%')
                        append((0x80 or ((code ushr 12) and 0x3F)).toHexString())
                        append('%')
                        append((0x80 or ((code ushr 6) and 0x3F)).toHexString())
                        append('%')
                        append((0x80 or (code and 0x3F)).toHexString())
                    }
                }
            }
        }
    }
}

private fun Int.toHexString(): String {
    val hexVal = this and 0xFF
    return buildString(2) {
        append("0123456789ABCDEF".getOrNull(hexVal / 16) ?: '?')
        append("0123456789ABCDEF".getOrNull(hexVal % 16) ?: '?')
    }
}
