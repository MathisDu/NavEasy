package com.example.lauzhack26_multiplatform.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lauzhack26_multiplatform.map.MapState
import com.example.lauzhack26_multiplatform.map.OsmMap
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.MobilityProfile
import com.example.lauzhack26_multiplatform.model.NominatimSearchResult
import com.example.lauzhack26_multiplatform.model.OsmFeature
import com.example.lauzhack26_multiplatform.model.ReportType

@Composable
fun NavigationApp(
    viewModel: MapViewModel,
    mapState: MapState
) {
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val route by viewModel.route.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showReportForm by viewModel.showReportForm.collectAsState()
    val reportLocation by viewModel.reportLocation.collectAsState()
    val searchResultsStart by viewModel.searchResultsStart.collectAsState()
    val isSearchingStart by viewModel.isSearchingStart.collectAsState()
    val searchResultsDestination by viewModel.searchResultsDestination.collectAsState()
    val isSearchingDestination by viewModel.isSearchingDestination.collectAsState()
    val avoidMode by viewModel.avoidMode.collectAsState()
    val avoidPolygonCenters by viewModel.avoidPolygonCenters.collectAsState()
    val osmFeatures by viewModel.filteredOsmFeatures.collectAsState()
    val isCentering by viewModel.isCentering.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    var showProfileSelector by remember { mutableStateOf(true) }
    var routeStart by remember { mutableStateOf<Location?>(null) }
    var routeEnd by remember { mutableStateOf<Location?>(null) }
    var selectedFeature by remember { mutableStateOf<OsmFeature?>(null) }

    // Start search state
    var searchQueryStart by remember { mutableStateOf("") }
    var isSearchBarFocusedStart by remember { mutableStateOf(false) }
    var selectedStartDisplayName by remember { mutableStateOf<String?>(null) }

    // Destination search state
    var searchQueryDestination by remember { mutableStateOf("") }
    var isSearchBarFocusedDestination by remember { mutableStateOf(false) }
    var selectedDestinationDisplayName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    // Fit the map viewport when a new route is displayed
    var canvasSizeForFit by remember { mutableStateOf<IntSize?>(null) }
    LaunchedEffect(route) {
        if (route.isNotEmpty() && canvasSizeForFit != null) {
            mapState.fitRouteToViewport(route, canvasSizeForFit!!)
        }
    }

    // Auto-center on user location when the canvas size becomes available
    var hasAutoCentered by remember { mutableStateOf(false) }
    LaunchedEffect(canvasSizeForFit) {
        if (canvasSizeForFit != null && !hasAutoCentered) {
            hasAutoCentered = true
            viewModel.centerOnUser(mapState, canvasSizeForFit!!)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
            routeCoordinates = route,
            reportLocations = reports.map { it.location },
            osmFeatures = osmFeatures,
            avoidPolygonCenters = avoidPolygonCenters,
            userLocation = userLocation,
            onFeatureClick = { feature ->
                selectedFeature = feature
            },
            onCanvasSizeReady = { size ->
                canvasSizeForFit = size
            },
            onLocationClick = { location ->
                if (!showProfileSelector) {
                    if (avoidMode) {
                        viewModel.addAvoidPolygonAt(location)
                    } else {
                        viewModel.openReportForm(location)
                    }
                }
            }
        )

        // Profile Selector Overlay
        if (showProfileSelector) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6FFFFFF))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Navigation Accessible",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF1565C0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choisissez votre profil de mobilité",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileCard(
                            profile = MobilityProfile.WHEELCHAIR,
                            isSelected = MobilityProfile.WHEELCHAIR == selectedProfile,
                            onClick = {
                                viewModel.setProfile(MobilityProfile.WHEELCHAIR)
                                showProfileSelector = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileCard(
                            profile = MobilityProfile.CRUTCHES,
                            isSelected = MobilityProfile.CRUTCHES == selectedProfile,
                            onClick = {
                                viewModel.setProfile(MobilityProfile.CRUTCHES)
                                showProfileSelector = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileCard(
                            profile = MobilityProfile.VISUAL_IMPAIRMENT,
                            isSelected = MobilityProfile.VISUAL_IMPAIRMENT == selectedProfile,
                            onClick = {
                                viewModel.setProfile(MobilityProfile.VISUAL_IMPAIRMENT)
                                showProfileSelector = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileCard(
                            profile = MobilityProfile.STANDARD,
                            isSelected = MobilityProfile.STANDARD == selectedProfile,
                            onClick = {
                                viewModel.setProfile(MobilityProfile.STANDARD)
                                showProfileSelector = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Appuyez sur la carte pour signaler un obstacle",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Top bar
        if (!showProfileSelector) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedProfile.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable { showProfileSelector = true }
                        .padding(4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.toggleAvoidMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (avoidMode) Color(0xFFD32F2F) else Color(0xFF9C27B0)
                        )
                    ) {
                        Text(if (avoidMode) "⛔ Éviter" else "⭕ Éviter")
                    }
                    Button(
                        onClick = { mapState.isDarkMode = !mapState.isDarkMode },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Text(if (mapState.isDarkMode) "☀️" else "🌙")
                    }
                    Button(
                        onClick = { showProfileSelector = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Profil")
                    }
                }
            }

            // Search panel with two fields (always visible)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 8.dp, end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Start location search
                    SearchBar(
                        query = searchQueryStart,
                        onQueryChange = {
                            searchQueryStart = it
                            viewModel.searchAddressStart(it)
                        },
                        onClear = {
                            searchQueryStart = ""
                            selectedStartDisplayName = null
                            routeStart = null
                            viewModel.clearSearchResultsStart()
                        },
                        searchResults = searchResultsStart,
                        isSearching = isSearchingStart,
                        isFocused = isSearchBarFocusedStart,
                        onFocusChange = { isSearchBarFocusedStart = it },
                        placeholder = "Point de départ",
                        onResultSelected = { result ->
                            val location = result.toLocation()
                            routeStart = location
                            selectedStartDisplayName = result.displayName
                            searchQueryStart = result.displayName
                            viewModel.clearSearchResultsStart()
                            isSearchBarFocusedStart = false
                            // If destination is also set, calculate route
                            if (routeEnd != null) {
                                viewModel.calculateRoute(location, routeEnd!!)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Destination search
                    SearchBar(
                        query = searchQueryDestination,
                        onQueryChange = {
                            searchQueryDestination = it
                            viewModel.searchAddressDestination(it)
                        },
                        onClear = {
                            searchQueryDestination = ""
                            selectedDestinationDisplayName = null
                            routeEnd = null
                            viewModel.clearSearchResultsDestination()
                        },
                        searchResults = searchResultsDestination,
                        isSearching = isSearchingDestination,
                        isFocused = isSearchBarFocusedDestination,
                        onFocusChange = { isSearchBarFocusedDestination = it },
                        placeholder = "Destination",
                        onResultSelected = { result ->
                            val location = result.toLocation()
                            routeEnd = location
                            selectedDestinationDisplayName = result.displayName
                            searchQueryDestination = result.displayName
                            viewModel.clearSearchResultsDestination()
                            isSearchBarFocusedDestination = false
                            // If start is also set, calculate route
                            if (routeStart != null) {
                                viewModel.calculateRoute(routeStart!!, location)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clear button to reset both fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                searchQueryStart = ""
                                searchQueryDestination = ""
                                selectedStartDisplayName = null
                                selectedDestinationDisplayName = null
                                routeStart = null
                                routeEnd = null
                                viewModel.clearSearchResultsStart()
                                viewModel.clearSearchResultsDestination()
                            },
                            modifier = Modifier.wrapContentWidth(),
                            enabled = searchQueryStart.isNotBlank() || searchQueryDestination.isNotBlank()
                        ) {
                            Text("Effacer")
                        }
                    }
                }
            }
        }

        // Report Form
        if (showReportForm && reportLocation != null) {
            ReportFormDialog(
                location = reportLocation!!,
                onDismiss = { viewModel.closeReportForm() },
                onSubmit = { type, description ->
                    viewModel.addReport(type, description, reportLocation!!)
                }
            )
        }

        // Error message
        errorMessage?.let { error ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 5
                    )
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text("Fermer")
                    }
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        // Center on User FAB
        if (!showProfileSelector) {
            FloatingActionButton(
                onClick = {
                    canvasSizeForFit?.let { size ->
                        viewModel.centerOnUser(mapState, size)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF1565C0)
            ) {
                if (isCentering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("📍")
                }
            }
        }

        // OSM Feature Detail Modal
        selectedFeature?.let { feature ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(onClick = { selectedFeature = null })
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .wrapContentHeight()
                        .wrapContentWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "${feature.obstacleType.icon} ${feature.obstacleType.label}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = feature.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lat: ${feature.location.lat}, Lng: ${feature.location.lon}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { selectedFeature = null }) {
                                Text("✕ Fermer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: MobilityProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF1565C0))
        } else {
            BorderStroke(1.dp, Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (profile) {
                    MobilityProfile.WHEELCHAIR -> "♿"
                    MobilityProfile.CRUTCHES -> "🦯"
                    MobilityProfile.VISUAL_IMPAIRMENT -> "👁️"
                    MobilityProfile.STANDARD -> "🚶"
                },
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (profile) {
                    MobilityProfile.WHEELCHAIR -> "Fauteuil"
                    MobilityProfile.CRUTCHES -> "Béquilles"
                    MobilityProfile.VISUAL_IMPAIRMENT -> "Visuel"
                    MobilityProfile.STANDARD -> "Standard"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun ReportFormDialog(
    location: Location,
    onDismiss: () -> Unit,
    onSubmit: (ReportType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReportType.STAIRS) }
    var description by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(onClick = onDismiss)
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .wrapContentHeight()
                .wrapContentWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Nouveau signalement",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lat: ${location.lat}, Lng: ${location.lon}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Type d'obstacle:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Column(
                    modifier = Modifier.height(200.dp).verticalScroll(rememberScrollState())
                ) {
                    val reportTypes = ReportType.entries.filter { it != ReportType.RAMP }
                    for (i in reportTypes.indices step 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            reportTypes.getOrNull(i)?.let { type ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedType = type },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (type == selectedType) Color(0xFFFFEBEE) else Color.White
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (type == selectedType) Color.Red else Color.LightGray
                                    )
                                ) {
                                    Text(
                                        text = type.label,
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            reportTypes.getOrNull(i + 1)?.let { type ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedType = type },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (type == selectedType) Color(0xFFFFEBEE) else Color.White
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (type == selectedType) Color.Red else Color.LightGray
                                    )
                                ) {
                                    Text(
                                        text = type.label,
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        if (i + 2 < reportTypes.size) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Description (optionnel):", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSubmit(selectedType, description) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Signaler")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }
                }
            }
        }
    }
}

/**
 * Search bar composable for address/place search using Nominatim.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    searchResults: List<NominatimSearchResult>,
    isSearching: Boolean,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    placeholder: String,
    onResultSelected: (NominatimSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val showResults = searchResults.isNotEmpty() || (isFocused && (isSearching || query.isNotBlank()))

    Column(modifier = modifier.fillMaxWidth()) {
        // Search input row
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = if (isFocused) 4.dp else 2.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search icon
                Text(
                    text = "🔍",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                )

                // Text field
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black
                    ),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty() && !isFocused) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    },
                    singleLine = true,
                    maxLines = 1
                )

                // Clear button
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Search results dropdown
        if (showResults) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                tonalElevation = 4.dp,
                color = Color.White
            ) {
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1565C0)
                        )
                    }
                } else if (searchResults.isEmpty() && query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun résultat trouvé",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(searchResults, key = { result -> "${result.lat}-${result.lon}" }) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = {
                                    onResultSelected(result)
                                    onFocusChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual search result item.
 */
@Composable
fun SearchResultItem(
    result: NominatimSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📍",
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (result.osmClass != null || result.type != null) {
                Text(
                    text = buildString {
                        result.osmClass?.let { append(it) }
                        if (result.osmClass != null && result.type != null) append(" · ")
                        result.type?.let { append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
