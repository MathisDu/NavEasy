package com.example.lauzhack26_multiplatform.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lauzhack26_multiplatform.location.LocationTracker
import com.example.lauzhack26_multiplatform.location.createLocationTracker
import com.example.lauzhack26_multiplatform.map.MapState
import com.example.lauzhack26_multiplatform.map.MercatorProjection
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.MobilityProfile
import com.example.lauzhack26_multiplatform.model.ObstacleType
import com.example.lauzhack26_multiplatform.model.OsmFeature
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportType
import com.example.lauzhack26_multiplatform.model.RouteRequest
import com.example.lauzhack26_multiplatform.model.RouteResponse
import com.example.lauzhack26_multiplatform.resources.Res
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    // All obstacles loaded from the offline GeoJSON resource
    private val _allOsmFeatures = MutableStateFlow<List<OsmFeature>>(emptyList())

    // Filtered obstacles derived from all features + selected profile
    private val _filteredOsmFeatures = MutableStateFlow<List<OsmFeature>>(emptyList())
    val filteredOsmFeatures: StateFlow<List<OsmFeature>> = _filteredOsmFeatures.asStateFlow()

    private val _isCentering = MutableStateFlow(false)
    val isCentering: StateFlow<Boolean> = _isCentering.asStateFlow()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private var locationTracker: LocationTracker? = null

    init {
        loadOfflineObstacles()
        // Re-filter whenever the profile or the full feature list changes
        viewModelScope.launch {
            combine(_allOsmFeatures, _selectedProfile) { features, profile ->
                Pair(features, profile)
            }.collect { (features, profile) ->
                _filteredOsmFeatures.value = filterFeaturesByProfile(features, profile)
            }
        }
    }

    /**
     * Load obstacles from the offline GeoJSON resource file.
     */
    private fun loadOfflineObstacles() {
        viewModelScope.launch {
            try {
                val bytes = Res.readBytes("files/obstacles.geojson")
                val jsonText = bytes.decodeToString()
                val features = parseGeoJsonObstacles(jsonText)
                _allOsmFeatures.value = features
                println("[Obstacles] ${features.size} obstacles chargés depuis le fichier hors-ligne")
            } catch (e: Exception) {
                println("[Obstacles] Erreur lors du chargement du fichier hors-ligne: ${e.message}")
                _allOsmFeatures.value = emptyList()
            }
        }
    }

    /**
     * Parse GeoJSON FeatureCollection into OsmFeature list, categorizing by OSM tags.
     */
    private fun parseGeoJsonObstacles(jsonText: String): List<OsmFeature> {
        val json = Json { ignoreUnknownKeys = true }
        val element = json.parseToJsonElement(jsonText)

        val rootObj = element.jsonObject
        val features = (rootObj["features"] as? JsonArray) ?: return emptyList()

        return features.mapNotNull { feature: JsonElement ->
            val obj = feature.jsonObject
            val properties = obj["properties"]?.jsonObject ?: return@mapNotNull null
            val geometry = obj["geometry"]?.jsonObject ?: return@mapNotNull null

            // Extract coordinates [lon, lat] from geometry
            val coordinates = geometry["coordinates"]?.jsonArray ?: return@mapNotNull null
            if (coordinates.size < 2) return@mapNotNull null
            val lon = coordinates[0].jsonPrimitive.doubleOrNull ?: return@mapNotNull null
            val lat = coordinates[1].jsonPrimitive.doubleOrNull ?: return@mapNotNull null

            // Categorize by OSM tags
            val obstacleType = categorizeByTags(properties) ?: return@mapNotNull null

            // Build description from tags
            val description = buildDescription(properties, obstacleType)

            OsmFeature(
                location = Location(lat = lat, lon = lon),
                obstacleType = obstacleType,
                description = description
            )
        }
    }

    /**
     * Categorize a GeoJSON feature by its OSM tags.
     */
    private fun categorizeByTags(properties: JsonObject): ObstacleType? {
        val highway = properties["highway"]?.jsonPrimitive?.content
        val surface = properties["surface"]?.jsonPrimitive?.content
        val barrier = properties["barrier"]?.jsonPrimitive?.content

        return when {
            highway == "steps" -> ObstacleType.STAIRS
            surface == "cobblestone" || surface == "sett" -> ObstacleType.COBBLESTONE
            barrier == "kerb" -> ObstacleType.KERB
            else -> null
        }
    }

    /**
     * Build a human-readable description from OSM tags.
     */
    private fun buildDescription(properties: JsonObject, type: ObstacleType): String {
        val name = properties["name"]?.jsonPrimitive?.content
        if (name != null && name.isNotEmpty()) return name

        return when (type) {
            ObstacleType.STAIRS -> {
                val stepCount = properties["step_count"]?.jsonPrimitive?.content
                val incline = properties["incline"]?.jsonPrimitive?.content
                val handrail = properties["handrail"]?.jsonPrimitive?.content
                val parts = mutableListOf<String>()
                parts.add("Escalier")
                if (stepCount != null) parts.add("$stepCount marches")
                if (incline != null) parts.add(if (incline == "up") "montant" else "descendant")
                if (handrail == "yes") parts.add("main courante")
                parts.joinToString(" • ")
            }
            ObstacleType.COBBLESTONE -> {
                val surface = properties["surface"]?.jsonPrimitive?.content
                "Surface $surface"
            }
            ObstacleType.KERB -> "Bordure de trottoir"
        }
    }

    /**
     * Filter obstacles based on the selected mobility profile.
     */
    private fun filterFeaturesByProfile(
        allFeatures: List<OsmFeature>,
        profile: MobilityProfile
    ): List<OsmFeature> {
        return when (profile) {
            MobilityProfile.WHEELCHAIR -> allFeatures
            MobilityProfile.CRUTCHES -> allFeatures
            MobilityProfile.VISUAL_IMPAIRMENT -> allFeatures.filter {
                it.obstacleType == ObstacleType.KERB
            }
            MobilityProfile.STANDARD -> emptyList()
        }
    }

    private fun getLocationTracker(): LocationTracker? {
        var tracker = locationTracker
        if (tracker == null) {
            tracker = runCatching { createLocationTracker() }.getOrNull()
            locationTracker = tracker
        }
        return tracker
    }

    /**
     * Centers the map on the user's current location.
     * Updates [mapState] center coordinates and recalculates the offset
     * so that the camera is exactly centered on the new location.
     */
    fun centerOnUser(mapState: MapState, canvasSize: IntSize) {
        val tracker = getLocationTracker() ?: run {
            _errorMessage.value = "Localisation non disponible sur cette plateforme"
            return
        }
        viewModelScope.launch {
            _isCentering.value = true
            try {
                val location = tracker.getCurrentLocation()
                if (location != null) {
                    // Store user location for the blue marker
                    _userLocation.value = location

                    // Update center coordinates
                    mapState.centerLat = location.lat
                    mapState.centerLon = location.lon

                    // Recalculate offset so the map centers on the new location
                    val (pixelX, pixelY) = MercatorProjection.locationToPixel(
                        location.lat, location.lon, mapState.zoom
                    )
                    mapState.offset = Offset(
                        -(pixelX - canvasSize.width / 2).toFloat(),
                        -(pixelY - canvasSize.height / 2).toFloat()
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur localisation: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isCentering.value = false
            }
        }
    }

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


    data class ReportRequest(
        val type: ReportType,
        val description: String,
        val location: Location
    )
}


private fun Int.toHexString(): String {
    val hexVal = this and 0xFF
    return buildString(2) {
        append("0123456789ABCDEF".getOrNull(hexVal / 16) ?: '?')
        append("0123456789ABCDEF".getOrNull(hexVal % 16) ?: '?')
    }
}
