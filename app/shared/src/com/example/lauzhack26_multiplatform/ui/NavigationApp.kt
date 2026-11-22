package com.example.lauzhack26_multiplatform.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lauzhack26_multiplatform.map.MapState
import com.example.lauzhack26_multiplatform.map.OsmMap
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.MobilityProfile
import com.example.lauzhack26_multiplatform.model.OsmFeature
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportStatus
import com.example.lauzhack26_multiplatform.model.ReportType
import com.example.lauzhack26_multiplatform.model.Severity

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
    val selectedReport by viewModel.selectedReport.collectAsState()
    val osmFeatures by viewModel.osmFeatures.collectAsState()

    var showProfileSelector by remember { mutableStateOf(true) }
    var routeStart by remember { mutableStateOf<Location?>(null) }
    var routeEnd by remember { mutableStateOf<Location?>(null) }
    var showRouteInput by remember { mutableStateOf(false) }
    var selectedFeature by remember { mutableStateOf<OsmFeature?>(null) }

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

    // Reload OSM features when map center or zoom changes
    LaunchedEffect(mapState.centerLat, mapState.centerLon, mapState.zoom) {
        if (!showProfileSelector) {
            viewModel.loadStepsFromOverpass(mapState.centerLat, mapState.centerLon, mapState.zoom)
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
            onFeatureClick = { feature ->
                selectedFeature = feature
            },
            onCanvasSizeReady = { size ->
                canvasSizeForFit = size
            },
            onLocationClick = { location ->
                if (showProfileSelector) {
                    // Do nothing while profile selector is shown
                } else if (showRouteInput) {
                    if (routeStart == null) {
                        routeStart = location
                    } else if (routeEnd == null) {
                        routeEnd = location
                        routeStart?.let { start ->
                            viewModel.calculateRoute(start, routeEnd!!)
                        }
                        showRouteInput = false
                        routeStart = null
                        routeEnd = null
                    }
                } else {
                    // Check if the tap is near an existing report marker
                    val tappedReport = reports.find { report ->
                        val dx = report.location.lat - location.lat
                        val dy = report.location.lon - location.lon
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                        distance < 0.001 // ~100m threshold
                    }
                    if (tappedReport != null) {
                        viewModel.selectReport(tappedReport)
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
                        onClick = {
                            showRouteInput = !showRouteInput
                            viewModel.clearRoute()
                            routeStart = null
                            routeEnd = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showRouteInput) Color(0xFF1565C0) else Color.LightGray
                        )
                    ) {
                        Text(if (showRouteInput) "Annuler" else "Itinéraire")
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

            if (showRouteInput) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = if (routeStart == null)
                            "Cliquez pour le point de départ"
                        else
                            "Cliquez pour la destination",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Report Form
        if (showReportForm && reportLocation != null) {
            ReportFormDialog(
                location = reportLocation!!,
                onDismiss = { viewModel.closeReportForm() },
                onSubmit = { title, category, description, disabilityTypes, severity ->
                    viewModel.addReport(title, category, description, reportLocation!!, disabilityTypes, severity)
                }
            )
        }

        // Report Details Dialog
        selectedReport?.let { report ->
            ReportDetailsDialog(
                report = report,
                onDismiss = { viewModel.closeReportDetails() },
                onResolve = {
                    viewModel.voteOnReport(report.id)
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
                            text = "🪜 Escalier",
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
    onSubmit: (String, String, String, List<String>, Severity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ReportType.STAIRS) }
    var description by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(Severity.MEDIUM) }
    val allDisabilityTypes = listOf("wheelchair", "blind", "crutches", "elderly", "parent-child")
    var selectedDisabilityTypes by remember { mutableStateOf(listOf<String>()) }

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

                Text("Titre:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Catégorie:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Column(
                    modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState())
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
                Text("Types de handicap affectés:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Column(
                    modifier = Modifier.height(100.dp).verticalScroll(rememberScrollState())
                ) {
                    for (dt in allDisabilityTypes) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedDisabilityTypes.contains(dt)) {
                                        selectedDisabilityTypes = selectedDisabilityTypes - dt
                                    } else {
                                        selectedDisabilityTypes = selectedDisabilityTypes + dt
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedDisabilityTypes.contains(dt),
                                onCheckedChange = {
                                    if (it) {
                                        selectedDisabilityTypes = selectedDisabilityTypes + dt
                                    } else {
                                        selectedDisabilityTypes = selectedDisabilityTypes - dt
                                    }
                                }
                            )
                            Text(
                                text = dt,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Gravité:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (sev in Severity.entries) {
                        val label = when (sev) {
                            Severity.LOW -> "Basse"
                            Severity.MEDIUM -> "Moyenne"
                            Severity.HIGH -> "Haute"
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSeverity = sev },
                            colors = CardDefaults.cardColors(
                                containerColor = if (sev == selectedSeverity) Color(0xFFFFEBEE) else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (sev == selectedSeverity) Color.Red else Color.LightGray
                            )
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
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
                        onClick = {
                            if (selectedDisabilityTypes.isEmpty()) {
                                return@Button
                            }
                            onSubmit(title.ifBlank { selectedType.label }, selectedType.name, description, selectedDisabilityTypes, selectedSeverity)
                        },
                        enabled = selectedDisabilityTypes.isNotEmpty() && title.isNotBlank(),
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

@Composable
fun ReportDetailsDialog(
    report: Report,
    onDismiss: () -> Unit,
    onResolve: () -> Unit
) {
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
                .wrapContentWidth()
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title and status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    val statusColor = when (report.status) {
                        ReportStatus.ACTIVE -> Color(0xFFFF9800)
                        ReportStatus.RESOLVED -> Color(0xFF4CAF50)
                    }
                    val statusLabel = when (report.status) {
                        ReportStatus.ACTIVE -> "Actif"
                        ReportStatus.RESOLVED -> "Résolu"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category
                Text(
                    text = "Catégorie: ${report.category}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Severity
                val severityLabel = when (report.severity) {
                    Severity.LOW -> "Basse"
                    Severity.MEDIUM -> "Moyenne"
                    Severity.HIGH -> "Haute"
                }
                val severityColor = when (report.severity) {
                    Severity.LOW -> Color(0xFF4CAF50)
                    Severity.MEDIUM -> Color(0xFFFF9800)
                    Severity.HIGH -> Color.Red
                }
                Text(
                    text = "Gravité: $severityLabel",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = severityColor,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Description
                report.description.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Description:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Disability types
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Handicaps affectés: ${report.disabilityTypes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Location
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${report.location.lat}, Lng: ${report.location.lon}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Created at
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Créé: ${report.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Vote section
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Vote pour ce signalement:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Vote counts
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${report.resolvedVotes}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    )
                    Text(
                        text = "Résolu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "5 votes nécessaires pour marquer comme résolu",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                if (report.status == ReportStatus.ACTIVE) {
                    Button(
                        onClick = onResolve,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("✓ Résolu")
                    }
                } else {
                    Text(
                        text = "Ce signalement est résolu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}
