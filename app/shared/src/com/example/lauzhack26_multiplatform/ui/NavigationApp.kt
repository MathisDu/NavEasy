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

    var showProfileSelector by remember { mutableStateOf(true) }
    var routeStart by remember { mutableStateOf<Location?>(null) }
    var routeEnd by remember { mutableStateOf<Location?>(null) }
    var showRouteInput by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
            routeCoordinates = route,
            reportLocations = reports.map { it.location },
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
                    viewModel.openReportForm(location)
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
