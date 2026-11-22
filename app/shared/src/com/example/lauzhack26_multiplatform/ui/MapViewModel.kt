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
import com.example.lauzhack26_multiplatform.model.NominatimSearchResult
import com.example.lauzhack26_multiplatform.model.ObstacleType
import com.example.lauzhack26_multiplatform.model.OsmFeature
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportType
import com.example.lauzhack26_multiplatform.model.RouteRequest
import com.example.lauzhack26_multiplatform.model.RouteResponse
import com.example.lauzhack26_multiplatform.resources.Res
import io.ktor.client.*
import io.ktor.client.call.body
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

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

    // Avoid polygon mode state
    private val _avoidMode = MutableStateFlow(false)
    val avoidMode: StateFlow<Boolean> = _avoidMode.asStateFlow()

    // List of avoid polygon centers (each polygon is a square around a center point)
    private val _avoidPolygonCenters = MutableStateFlow<List<Location>>(emptyList())
    val avoidPolygonCenters: StateFlow<List<Location>> = _avoidPolygonCenters.asStateFlow()

    // Debounce job for Overpass API calls
    private var overpassDebounceJob: Job? = null

    // Debounce jobs for Nominatim search
    private var searchStartDebounceJob: Job? = null
    private var searchDestinationDebounceJob: Job? = null

    // Store start/end locations for route recalculation when avoid polygons change
    private var _routeStart: Location? = null
    private var _routeEnd: Location? = null

    // Start location search state
    private val _searchResultsStart = MutableStateFlow<List<NominatimSearchResult>>(emptyList())
    val searchResultsStart: StateFlow<List<NominatimSearchResult>> = _searchResultsStart.asStateFlow()

    private val _isSearchingStart = MutableStateFlow(false)
    val isSearchingStart: StateFlow<Boolean> = _isSearchingStart.asStateFlow()

    // Destination search state
    private val _searchResultsDestination = MutableStateFlow<List<NominatimSearchResult>>(emptyList())
    val searchResultsDestination: StateFlow<List<NominatimSearchResult>> = _searchResultsDestination.asStateFlow()

    private val _isSearchingDestination = MutableStateFlow(false)
    val isSearchingDestination: StateFlow<Boolean> = _isSearchingDestination.asStateFlow()

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
        _routeStart = start
        _routeEnd = end
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val avoidPolygons = buildAvoidPolygons()
                val request = RouteRequest(start, end, _selectedProfile.value, avoidPolygons)
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

    fun toggleAvoidMode() {
        _avoidMode.value = !_avoidMode.value
        // Clear avoid polygons when disabling avoid mode
        if (!_avoidMode.value) {
            _avoidPolygonCenters.value = emptyList()
        }
    }

    fun addAvoidPolygonAt(location: Location) {
        val updated = _avoidPolygonCenters.value + location
        _avoidPolygonCenters.value = updated
        // Recalculate route if start and end are set
        if (_routeStart != null && _routeEnd != null) {
            calculateRoute(_routeStart!!, _routeEnd!!)
        }
    }

    /**
     * Build the list of avoid polygons (as GeoJSON-style coordinate rings) from the stored centers.
     * Each polygon is a small circle (approximated by 16 points) around the center point.
     */
    private fun buildAvoidPolygons(): List<List<Location>> {
        val radiusDegrees = 0.0002 // ~20 meters
        val points = 16
        return _avoidPolygonCenters.value.map { center ->
            // Scale longitude radius by cos(lat) so the shape appears circular on the map
            val lonRadius = radiusDegrees * cos(center.lat * PI / 180.0)
            val ring = (0 until points).map { i ->
                val angle = 2.0 * PI * i / points
                Location(
                    center.lat + radiusDegrees * cos(angle),
                    center.lon + lonRadius * sin(angle)
                )
            }
            ring + ring.first() // close the ring
        }
    }

    /**
     * Search for the starting point using the Nominatim geocoding API via the backend proxy.
     * Uses debouncing so suggestions appear as the user types.
     */
    fun searchAddressStart(query: String) {
        if (query.isBlank()) {
            _searchResultsStart.value = emptyList()
            searchStartDebounceJob?.cancel()
            return
        }

        // Cancel any pending debounced request
        searchStartDebounceJob?.cancel()

        searchStartDebounceJob = viewModelScope.launch {
            // Wait 500ms after the user stops typing
            delay(500.milliseconds)

            _isSearchingStart.value = true
            _errorMessage.value = null
            try {
                val response = httpClient.post("$serverUrl/api/nominatim/search") {
                    contentType(ContentType.Application.Json)
                    setBody(NominatimSearchRequest(query))
                }

                if (response.status == HttpStatusCode.OK) {
                    val result = response.body<NominatimSearchResponse>()
                    val results = nominatimJson.decodeFromString<List<NominatimSearchResult>>(result.result)
                    _searchResultsStart.value = results
                } else {
                    _searchResultsStart.value = emptyList()
                    _errorMessage.value = "Erreur de recherche (${response.status})"
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _searchResultsStart.value = emptyList()
                    _errorMessage.value = "Erreur: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isSearchingStart.value = false
            }
        }
    }

    /**
     * Search for the destination using the Nominatim geocoding API via the backend proxy.
     * Uses debouncing so suggestions appear as the user types.
     */
    fun searchAddressDestination(query: String) {
        if (query.isBlank()) {
            _searchResultsDestination.value = emptyList()
            searchDestinationDebounceJob?.cancel()
            return
        }

        // Cancel any pending debounced request
        searchDestinationDebounceJob?.cancel()

        searchDestinationDebounceJob = viewModelScope.launch {
            // Wait 500ms after the user stops typing
            delay(500.milliseconds)

            _isSearchingDestination.value = true
            _errorMessage.value = null
            try {
                val response = httpClient.post("$serverUrl/api/nominatim/search") {
                    contentType(ContentType.Application.Json)
                    setBody(NominatimSearchRequest(query))
                }

                if (response.status == HttpStatusCode.OK) {
                    val result = response.body<NominatimSearchResponse>()
                    val results = nominatimJson.decodeFromString<List<NominatimSearchResult>>(result.result)
                    _searchResultsDestination.value = results
                } else {
                    _searchResultsDestination.value = emptyList()
                    _errorMessage.value = "Erreur de recherche (${response.status})"
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _searchResultsDestination.value = emptyList()
                    _errorMessage.value = "Erreur: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isSearchingDestination.value = false
            }
        }
    }

    fun clearSearchResultsStart() {
        _searchResultsStart.value = emptyList()
    }

    fun clearSearchResultsDestination() {
        _searchResultsDestination.value = emptyList()
    }

    /**
     * Fetch staircase features from OpenStreetMap using the Overpass API via the backend proxy.
     * Uses debouncing to avoid spamming requests when zooming/panning.
     * @param centerLat center latitude of the map viewport
     * @param centerLon center longitude of the map viewport
     * @param zoom current zoom level
     */
    fun loadStepsFromOverpass(centerLat: Double, centerLon: Double, zoom: Int) {
        // Cancel any pending debounced request
        overpassDebounceJob?.cancel()

        overpassDebounceJob = viewModelScope.launch {
            // Wait 1 second after the last zoom/pan before making the request
            delay(1000.milliseconds)

            try {
                // Calculate bounding box from center and zoom
                val (minLat, minLon, maxLat, maxLon) = calculateBoundingBox(centerLat, centerLon, zoom)

                // Build Overpass API query
                val query = """
                    [out:json];
                    node["highway"="steps"]($minLat,$minLon,$maxLat,$maxLon);
                    out;
                """.trimIndent()

                // Send query through the backend proxy to avoid CORS issues
                val response = httpClient.post("$serverUrl/api/overpass") {
                    contentType(ContentType.Application.Json)
                    setBody(OverpassQueryRequest(query))
                }
                if (response.status == HttpStatusCode.OK) {
                    val result = response.body<OverpassQueryResponse>()
                    val features = parseOverpassResponse(result.result)
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


    /**
     * Parse Overpass API JSON response into a list of OsmFeature.
     */
    private fun parseOverpassResponse(jsonText: String): List<OsmFeature> {
        val element = overpassJson.parseToJsonElement(jsonText)

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

}

@Serializable
data class OverpassQueryRequest(val data: String)

@Serializable
data class OverpassQueryResponse(val result: String)

@Serializable
data class NominatimSearchRequest(val query: String)

@Serializable
data class NominatimSearchResponse(val result: String)

data class BoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double
)

@Serializable
data class ReportRequest(
    val type: ReportType,
    val description: String,
    val location: Location
)

// Shared Json instances to avoid redundant creation on each parse
val nominatimJson = Json { ignoreUnknownKeys = true }
val overpassJson = Json { ignoreUnknownKeys = true }

