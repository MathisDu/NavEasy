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
import com.example.lauzhack26_multiplatform.model.Severity
import io.ktor.client.*
import io.ktor.client.call.body
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.lauzhack26_multiplatform.resources.Res
import io.ktor.client.request.get
import io.ktor.client.request.patch
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

    // Avoid polygon mode state
    private val _avoidMode = MutableStateFlow(false)
    val avoidMode: StateFlow<Boolean> = _avoidMode.asStateFlow()

    // List of avoid polygon centers (each polygon is a square around a center point)
    private val _avoidPolygonCenters = MutableStateFlow<List<Location>>(emptyList())
    val avoidPolygonCenters: StateFlow<List<Location>> = _avoidPolygonCenters.asStateFlow()

    // Debounce jobs for Nominatim search
    private var searchStartDebounceJob: Job? = null
    private var searchDestinationDebounceJob: Job? = null

    // Store start/end locations for route recalculation when avoid polygons change
    private var _routeStart: Location? = null
    private var _routeEnd: Location? = null

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
                val bytes = Res.readBytes("files/obstacles1.geojson")
                val jsonText = bytes.decodeToString()
                val features = parseGeoJsonObstacles(jsonText)
                _allOsmFeatures.value = features
                println("[Obstacles] ${features.size} obstacles chargés depuis le fichier hors-ligne")
            } catch (e: CancellationException) {
                throw e
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
     * Returns the appropriate ObstacleType based on tag values, or null if not a recognized obstacle.
     */
    private fun categorizeByTags(properties: JsonObject): ObstacleType? {
        val highway = properties["highway"]?.jsonPrimitive?.content
        val wheelchair = properties["wheelchair"]?.jsonPrimitive?.content
        val surface = properties["surface"]?.jsonPrimitive?.content
        val smoothness = properties["smoothness"]?.jsonPrimitive?.content
        val barrier = properties["barrier"]?.jsonPrimitive?.content
        val kerb = properties["kerb"]?.jsonPrimitive?.content
        val door = properties["door"]?.jsonPrimitive?.content
        val tactilePaving = properties["tactile_paving"]?.jsonPrimitive?.content
        val crossingSignalsSound = properties["crossing:signals:sound"]?.jsonPrimitive?.content

        return when {
            // Stairs
            highway == "steps" -> ObstacleType.STAIRS

            // Revolving door
            door == "revolving" -> ObstacleType.REVOLVING_DOOR

            // Kerb barrier (high kerb) - barrier is "kerb" and kerb is not flush/lowered
            barrier == "kerb" && kerb != null && kerb != "flush" && kerb != "lowered" -> ObstacleType.HIGH_KERB

            // Blocking barriers
            barrier == "stile" || barrier == "turnstile" || barrier == "motorcycle_barrier" -> ObstacleType.BARRIER

            // Wheelchair access restriction
            wheelchair == "no" || wheelchair == "limited" -> ObstacleType.WHEELCHAIR_RESTRICTED

            // Bad surfaces
            surface == "cobblestone" || surface == "sett" || surface == "sand" || surface == "gravel" || surface == "dirt" -> ObstacleType.BAD_SURFACE

            // Smoothness indicates bad surface
            smoothness == "bad" || smoothness == "very_bad" || smoothness == "horrible" || smoothness == "impassable" -> ObstacleType.BAD_SURFACE

            // Missing tactile paving at crossings
            highway == "crossing" && tactilePaving == "no" -> ObstacleType.MISSING_TACTILE

            // Silent crossing (no sound signals)
            highway == "crossing" && crossingSignalsSound == "no" -> ObstacleType.SILENT_CROSSING

            // Legacy: plain kerb without specific kerb tag
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
            ObstacleType.WHEELCHAIR_RESTRICTED -> {
                val wheelchair = properties["wheelchair"]?.jsonPrimitive?.content
                "Accès fauteuil : $wheelchair"
            }
            ObstacleType.BAD_SURFACE -> {
                val surface = properties["surface"]?.jsonPrimitive?.content
                val smoothness = properties["smoothness"]?.jsonPrimitive?.content
                val parts = mutableListOf<String>()
                if (surface != null) parts.add("Surface: $surface")
                if (smoothness != null) parts.add("Rugosité: $smoothness")
                if (parts.isEmpty()) "Surface impraticable" else parts.joinToString(" • ")
            }
            ObstacleType.BARRIER -> {
                val barrier = properties["barrier"]?.jsonPrimitive?.content
                "Barrière: $barrier"
            }
            ObstacleType.HIGH_KERB -> {
                val kerb = properties["kerb"]?.jsonPrimitive?.content
                "Bordure haute${if (kerb != null) " ($kerb)" else ""}"
            }
            ObstacleType.REVOLVING_DOOR -> "Porte à tambour"
            ObstacleType.MISSING_TACTILE -> "Passe piéton sans bande podotactile"
            ObstacleType.SILENT_CROSSING -> "Passe piéton sans signal sonore"
            ObstacleType.COBBLESTONE -> {
                val surface = properties["surface"]?.jsonPrimitive?.content
                "Surface $surface"
            }
            ObstacleType.KERB -> "Bordure de trottoir"
        }
    }

    /**
     * Filter obstacles for display on the Canvas based on the selected mobility profile.
     * Returns both blockers (physically impassable) and warnings (informational) so the user
     * can see all relevant obstacles, even if only blockers are sent to the routing service.
     */
    private fun filterFeaturesByProfile(
        allFeatures: List<OsmFeature>,
        profile: MobilityProfile
    ): List<OsmFeature> {
        val displayTypes = when (profile) {
            MobilityProfile.WHEELCHAIR -> setOf(
                // Blockers
                ObstacleType.STAIRS,
                ObstacleType.WHEELCHAIR_RESTRICTED,
                ObstacleType.BARRIER,
                ObstacleType.HIGH_KERB,
                ObstacleType.REVOLVING_DOOR,
                // Warnings
                ObstacleType.BAD_SURFACE,
                ObstacleType.COBBLESTONE,
                ObstacleType.KERB
            )
            MobilityProfile.CRUTCHES -> setOf(
                // Blockers
                ObstacleType.STAIRS,
                ObstacleType.REVOLVING_DOOR,
                // Warnings
                ObstacleType.BAD_SURFACE,
                ObstacleType.HIGH_KERB
            )
            MobilityProfile.VISUAL_IMPAIRMENT -> setOf(
                // Warnings only — no blockers sent to ORS
                ObstacleType.MISSING_TACTILE,
                ObstacleType.SILENT_CROSSING,
                ObstacleType.REVOLVING_DOOR
            )
            MobilityProfile.STANDARD -> emptySet()
        }
        return allFeatures.filter { it.obstacleType in displayTypes }
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
    private val _showReportMode = MutableStateFlow(false)
    val showReportMode: StateFlow<Boolean> = _showReportMode.asStateFlow()

    private val _selectedReport = MutableStateFlow<Report?>(null)
    val selectedReport: StateFlow<Report?> = _selectedReport.asStateFlow()

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
                    _errorMessage.value = "Error loading reports: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addReport(category: String, description: String, location: Location, disabilityTypes: List<String>, severity: Severity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = ReportRequest(category, description, location, disabilityTypes, severity)
                val response = httpClient.post("$serverUrl/api/reports") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.Created) {
                    loadReports()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Error adding report: ${e.message}"
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
                val profile = _selectedProfile.value

                // Generate avoidance polygons only for physically impassable OSM blockers
                val osmPolygons = getBlockerOsmPolygons(profile, _allOsmFeatures.value)

                // Generate manual avoidance polygons from user-placed centers
                val manualPolygons = buildManualAvoidPolygons()

                // Combine both lists of polygons
                val combinedPolygons = osmPolygons + manualPolygons

                val request = RouteRequest(start, end, profile, combinedPolygons)
                val response = httpClient.post("$serverUrl/api/route") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.OK) {
                    val routeResponse = response.body<RouteResponse>()
                    _route.value = routeResponse.coordinates
                } else {
                    _errorMessage.value = "No easily accessible route available for this type of disability."
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Error: ${e.message}"
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

    fun enterReportMode() {
        _showReportMode.value = true
    }

    fun exitReportMode() {
        _showReportMode.value = false
    }

    fun openReportForm(location: Location) {
        _reportLocation.value = location
        _showReportForm.value = true
        _showReportMode.value = false
    }

    fun closeReportForm() {
        _showReportForm.value = false
        _reportLocation.value = null
        _showReportMode.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun selectReport(report: Report) {
        _selectedReport.value = report
    }

    fun closeReportDetails() {
        _selectedReport.value = null
    }

    fun voteOnReport(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = httpClient.patch("$serverUrl/api/reports/$reportId") {
                    contentType(ContentType.Application.Json)
                }
                if (response.status == HttpStatusCode.OK) {
                    loadReports()
                    // Update the selected report if it's still open
                    _selectedReport.value?.let { current ->
                        if (current.id == reportId) {
                            _selectedReport.value = response.body<Report>()
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Error voting: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
            }
        }
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
     * Generate avoidance polygons (bounding boxes) only for physically impassable obstacles
     * (Blockers) that must be sent to OpenRouteService.
     * Warnings like BAD_SURFACE or MISSING_TACTILE are excluded to avoid absurd detours,
     * because ORS treats avoid_polygons as strict walls.
     *
     * Returns a list of closed polygons (5 points each: top-left, top-right, bottom-right, bottom-left, top-left).
     */
    private fun getBlockerOsmPolygons(
        profile: MobilityProfile,
        features: List<OsmFeature>
    ): List<List<Location>> {
        // Blockers: physically impassable obstacles that ORS must route around
        val blockerTypes = when (profile) {
            MobilityProfile.WHEELCHAIR -> setOf(
                ObstacleType.STAIRS,
                ObstacleType.BARRIER,
                ObstacleType.REVOLVING_DOOR,
                ObstacleType.HIGH_KERB,
                ObstacleType.WHEELCHAIR_RESTRICTED
            )
            MobilityProfile.CRUTCHES -> setOf(
                ObstacleType.STAIRS,
                ObstacleType.REVOLVING_DOOR
            )
            MobilityProfile.VISUAL_IMPAIRMENT -> emptySet()
            MobilityProfile.STANDARD -> emptySet()
        }

        // Filter features that are physically impassable blockers
        val blockerFeatures = features.filter { it.obstacleType in blockerTypes }

        // Generate bounding-box polygons around each blocker obstacle
        val radiusDegrees = 0.0005 // ~50 meters
        return blockerFeatures.map { feature ->
            val lat = feature.location.lat
            val lon = feature.location.lon
            listOf(
                Location(lat + radiusDegrees, lon - radiusDegrees),  // top-left
                Location(lat + radiusDegrees, lon + radiusDegrees),  // top-right
                Location(lat - radiusDegrees, lon + radiusDegrees),  // bottom-right
                Location(lat - radiusDegrees, lon - radiusDegrees),  // bottom-left
                Location(lat + radiusDegrees, lon - radiusDegrees)   // close: top-left
            )
        }
    }

    /**
     * Build the list of avoid polygons from manually placed polygon centers.
     * Each polygon is a small circle (approximated by 16 points) around the center point.
     */
    private fun buildManualAvoidPolygons(): List<List<Location>> {
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

    @Serializable
    data class ReportRequest(
        val category: String,
        val description: String,
        val location: Location,
        val disabilityTypes: List<String>,
        val severity: Severity = Severity.MEDIUM
    )

}


@Serializable
data class NominatimSearchRequest(val query: String)

@Serializable
data class NominatimSearchResponse(val result: String)

// Shared Json instances to avoid redundant creation on each parse
val nominatimJson = Json { ignoreUnknownKeys = true }

