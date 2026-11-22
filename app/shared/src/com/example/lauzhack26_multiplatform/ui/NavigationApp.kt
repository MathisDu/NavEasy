package com.example.lauzhack26_multiplatform.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.lauzhack26_multiplatform.map.MapState
import com.example.lauzhack26_multiplatform.map.OsmMap
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.MobilityProfile
import com.example.lauzhack26_multiplatform.model.OsmFeature
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportStatus
import com.example.lauzhack26_multiplatform.model.VOTE_THRESHOLD
import com.example.lauzhack26_multiplatform.model.ReportType
import com.example.lauzhack26_multiplatform.model.Severity

// Adaptive Light & Dark Design System Palette
private data class AppPalette(
    val isDark: Boolean,
    val emeraldPrimary: Color,
    val emeraldIcon: Color,
    val emeraldTintSurface: Color,
    val emeraldBadgeBg: Color,
    val emeraldPillBorder: Color,
    val emeraldDisabled: Color,
    val canvasBg: Color,
    val cardBg: Color,
    val inputBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textBody: Color,
    val borderSubtle: Color,
    val borderInput: Color,
    val dividerColor: Color,
    val glowStart: Color,
    val glowEnd: Color,
    val scrimColor: Color
)

private val LightPalette = AppPalette(
    isDark = false,
    emeraldPrimary = Color(0xFF188644),
    emeraldIcon = Color(0xFF16803C),
    emeraldTintSurface = Color(0xFFEDFBF2),
    emeraldBadgeBg = Color(0xFFD6F7E2),
    emeraldPillBorder = Color(0xFFBBEAD0),
    emeraldDisabled = Color(0xFFA7E3BE),
    canvasBg = Color(0xFFF8FAF9),
    cardBg = Color.White,
    inputBg = Color(0xFFF8FAF9),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF4B5563),
    textBody = Color(0xFF1F2937),
    borderSubtle = Color(0xFFE5E7EB),
    borderInput = Color(0xFFD1D5DB),
    dividerColor = Color(0xFFF3F4F6),
    glowStart = Color(0x66D8F8E5),
    glowEnd = Color(0x00F8FAF9),
    scrimColor = Color(0x660F172A)
)

private val DarkPalette = AppPalette(
    isDark = true,
    emeraldPrimary = Color(0xFF22C55E),
    emeraldIcon = Color(0xFF4ADE80),
    emeraldTintSurface = Color(0xFF172B23),
    emeraldBadgeBg = Color(0xFF1D3B2F),
    emeraldPillBorder = Color(0xFF2A5443),
    emeraldDisabled = Color(0xFF1B4332),
    canvasBg = Color(0xFF0B1110),
    cardBg = Color(0xFF131D1A),
    inputBg = Color(0xFF0E1614),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textBody = Color(0xFFCBD5E1),
    borderSubtle = Color(0xFF243530),
    borderInput = Color(0xFF2E433D),
    dividerColor = Color(0xFF1E2D28),
    glowStart = Color(0x33188644),
    glowEnd = Color(0x000B1110),
    scrimColor = Color(0x99020617)
)

private val LocalAppPalette = staticCompositionLocalOf { LightPalette }

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
    val searchResultsDestination by viewModel.searchResultsDestination.collectAsState()
    val avoidMode by viewModel.avoidMode.collectAsState()
    val avoidPolygonCenters by viewModel.avoidPolygonCenters.collectAsState()
    val isCentering by viewModel.isCentering.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val showReportMode by viewModel.showReportMode.collectAsState()
    val selectedReport by viewModel.selectedReport.collectAsState()
    val osmFeatures by viewModel.filteredOsmFeatures.collectAsState()

    var isDarkMode by remember { mutableStateOf(mapState.isDarkMode) }
    val palette = if (isDarkMode) DarkPalette else LightPalette
    val votedReports by viewModel.votedReports.collectAsState()

    var showProfileSelector by remember { mutableStateOf(true) }
    var showJourneyPlanner by remember { mutableStateOf(false) }
    var routeStart by remember { mutableStateOf<Location?>(null) }
    var routeEnd by remember { mutableStateOf<Location?>(null) }
    var showRouteInput by remember { mutableStateOf(false) }
    var selectedFeature by remember { mutableStateOf<OsmFeature?>(null) }

    // Start search state
    var searchQueryStart by remember { mutableStateOf("") }
    var isSearchBarFocusedStart by remember { mutableStateOf(false) }
    var showDropdownStart by remember { mutableStateOf(false) }
    var selectedStartDisplayName by remember { mutableStateOf<String?>(null) }

    // Destination search state
    var searchQueryDestination by remember { mutableStateOf("") }
    var isSearchBarFocusedDestination by remember { mutableStateOf(false) }
    var showDropdownDestination by remember { mutableStateOf(false) }
    var selectedDestinationDisplayName by remember { mutableStateOf<String?>(null) }

    // Automatically open dropdowns when search results arrive
    LaunchedEffect(searchResultsStart, searchQueryStart) {
        if (searchResultsStart.isNotEmpty() && searchQueryStart.isNotBlank() && searchQueryStart != selectedStartDisplayName) {
            showDropdownStart = true
        }
    }

    LaunchedEffect(searchResultsDestination, searchQueryDestination) {
        if (searchResultsDestination.isNotEmpty() && searchQueryDestination.isNotBlank() && searchQueryDestination != selectedDestinationDisplayName) {
            showDropdownDestination = true
        }
    }

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

    val toggleTheme = {
        isDarkMode = !isDarkMode
        mapState.isDarkMode = isDarkMode
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
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
                    if (showProfileSelector) {
                        // Do nothing while profile selector is shown
                    } else if (showReportMode) {
                        // User is in report mode: click picks the location and opens the form
                        viewModel.openReportForm(location)
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
                        } else if (avoidMode) {
                            viewModel.addAvoidPolygonAt(location)
                        }
                        // Otherwise do nothing — clicking on the map no longer opens the report form
                    }
                }
            )

            // Profile Selector Overlay (Matches design mockup + dark mode)
            if (showProfileSelector) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.canvasBg)
                ) {
                    val centerX = constraints.maxWidth / 2f
                    val centerY = constraints.maxHeight * 0.35f
                    val glowRadius = maxOf(constraints.maxWidth, constraints.maxHeight) * 0.55f

                    // Subtle radial mint glow behind header & cards
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.glowStart,
                                        palette.glowEnd
                                    ),
                                    center = Offset(centerX, centerY),
                                    radius = glowRadius.coerceAtLeast(1f)
                                )
                            )
                    )

                    // Light / Dark Mode Toggle button (top-right)
                    ThemeToggleButton(
                        isDarkMode = isDarkMode,
                        onToggle = toggleTheme,
                        showLabel = true,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val interFont = InterFont

                        Text(
                            text = "Hello!",
                            fontFamily = interFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 54.sp,
                            letterSpacing = (-1.0).sp,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "How can we adapt your journey?",
                            fontFamily = interFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 20.sp,
                            color = palette.textBody
                        )
                        Spacer(modifier = Modifier.height(44.dp))

                        // Constrained container so cards and Continue button align cleanly
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 820.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                ProfileCardFigma(
                                    label = "Wheelchair",
                                    iconType = 0,
                                    isSelected = MobilityProfile.WHEELCHAIR == selectedProfile,
                                    onClick = {
                                        viewModel.setProfile(MobilityProfile.WHEELCHAIR)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileCardFigma(
                                    label = "Reduced mobility",
                                    iconType = 1,
                                    isSelected = MobilityProfile.CRUTCHES == selectedProfile,
                                    onClick = {
                                        viewModel.setProfile(MobilityProfile.CRUTCHES)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                ProfileCardFigma(
                                    label = "Visually impaired",
                                    iconType = 2,
                                    isSelected = MobilityProfile.VISUAL_IMPAIRMENT == selectedProfile,
                                    onClick = {
                                        viewModel.setProfile(MobilityProfile.VISUAL_IMPAIRMENT)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileCardFigma(
                                    label = "I just want to report obstacles",
                                    iconType = 3,
                                    isSelected = MobilityProfile.STANDARD == selectedProfile,
                                    onClick = {
                                        viewModel.setProfile(MobilityProfile.STANDARD)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    showProfileSelector = false
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(48.dp)
                                    .widthIn(min = 148.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.emeraldPrimary,
                                    contentColor = if (palette.isDark) Color(0xFF052E16) else Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 1.dp
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Continue",
                                    fontFamily = interFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Main Map Overlays
            if (!showProfileSelector) {
                if (showRouteInput) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, palette.emeraldPrimary),
                        color = palette.emeraldTintSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(palette.emeraldBadgeBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                    drawLocationPinIcon(palette.emeraldPrimary)
                                }
                            }
                            Text(
                                text = if (routeStart == null) "Tap the map for your starting point" else "Tap the map for your destination",
                                fontFamily = InterFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                // Floating "Your journey" card - top center, matching homepage design system
                if (!showJourneyPlanner && !showReportMode) {
                    val density = LocalDensity.current
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .fillMaxWidth()
                            .widthIn(max = 820.dp),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, palette.borderSubtle),
                        color = palette.cardBg
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            // Header row: Back button + Title + Theme toggle + Active Mobility Profile pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { showProfileSelector = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.emeraldTintSurface,
                                        border = BorderStroke(1.dp, palette.emeraldPillBorder)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                                drawBackArrowIcon(palette.emeraldPrimary)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Your journey",
                                        fontFamily = InterFont,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Light / Dark Mode Toggle button
                                    ThemeToggleButton(
                                        isDarkMode = isDarkMode,
                                        onToggle = toggleTheme,
                                        showLabel = false
                                    )

                                    val (profileLabel, profileIconType) = when (selectedProfile) {
                                        MobilityProfile.WHEELCHAIR -> "Wheelchair" to 0
                                        MobilityProfile.CRUTCHES -> "Reduced mobility" to 1
                                        MobilityProfile.VISUAL_IMPAIRMENT -> "Visually impaired" to 2
                                        MobilityProfile.STANDARD -> "Report obstacles" to 3
                                    }

                                    Surface(
                                        modifier = Modifier.clickable { showProfileSelector = true },
                                        shape = CircleShape,
                                        color = palette.emeraldTintSurface,
                                        border = BorderStroke(1.dp, palette.emeraldPillBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .background(palette.emeraldBadgeBg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                                    when (profileIconType) {
                                                        0 -> drawIconWheelchair(palette.emeraldIcon)
                                                        1 -> drawIconMobility(palette.emeraldIcon)
                                                        2 -> drawIconVisual(palette.emeraldIcon)
                                                        3 -> drawIconFlag(palette.emeraldIcon)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = profileLabel,
                                                fontFamily = InterFont,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = palette.emeraldPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // From and To fields row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // From field
                                Box(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "From",
                                            fontFamily = InterFont,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSearchBarFocusedStart) palette.emeraldTintSurface.copy(alpha = 0.55f) else palette.inputBg,
                                            border = BorderStroke(
                                                width = if (isSearchBarFocusedStart) 1.5.dp else 1.dp,
                                                color = if (isSearchBarFocusedStart) palette.emeraldPrimary else palette.borderInput
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(palette.emeraldBadgeBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                                                        drawLocationPinIcon(palette.emeraldPrimary)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                BasicTextField(
                                                    value = searchQueryStart,
                                                    onValueChange = {
                                                        searchQueryStart = it
                                                        selectedStartDisplayName = null
                                                        routeStart = null
                                                        if (it.isNotBlank()) {
                                                            viewModel.searchAddressStart(it)
                                                        } else {
                                                            showDropdownStart = false
                                                            viewModel.clearSearchResultsStart()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onFocusChanged { focusState ->
                                                            isSearchBarFocusedStart = focusState.isFocused
                                                            if (focusState.isFocused && searchResultsStart.isNotEmpty() && searchQueryStart.isNotBlank()) {
                                                                showDropdownStart = true
                                                            }
                                                            if (!focusState.isFocused && searchQueryStart.isBlank()) {
                                                                viewModel.clearSearchResultsStart()
                                                                showDropdownStart = false
                                                            }
                                                        },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = InterFont,
                                                        fontSize = 13.sp,
                                                        color = palette.textPrimary
                                                    ),
                                                    decorationBox = { innerTextField ->
                                                        if (searchQueryStart.isBlank()) {
                                                            Text(
                                                                text = "Choose a starting point",
                                                                fontFamily = InterFont,
                                                                fontSize = 13.sp,
                                                                color = palette.textSecondary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        innerTextField()
                                                    },
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (searchQueryStart.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clickable {
                                                                searchQueryStart = ""
                                                                selectedStartDisplayName = null
                                                                routeStart = null
                                                                showDropdownStart = false
                                                                viewModel.clearSearchResultsStart()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.dp)) {
                                                            drawCloseIcon(palette.textSecondary)
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier.size(20.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                                                            drawSearchIcon(palette.emeraldPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Search results dropdown for From
                                    if (showDropdownStart && searchResultsStart.isNotEmpty() && searchQueryStart.isNotBlank()) {
                                        Popup(
                                            alignment = Alignment.TopStart,
                                            offset = IntOffset(0, with(density) { 68.dp.roundToPx() }),
                                            onDismissRequest = { showDropdownStart = false },
                                            properties = PopupProperties(focusable = false)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .width(360.dp)
                                                    .heightIn(max = 240.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                shadowElevation = 8.dp,
                                                border = BorderStroke(1.dp, palette.borderSubtle),
                                                color = palette.cardBg
                                            ) {
                                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                    searchResultsStart.forEachIndexed { index, result ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    val location = result.toLocation()
                                                                    routeStart = location
                                                                    selectedStartDisplayName = result.displayName
                                                                    searchQueryStart = result.displayName
                                                                    showDropdownStart = false
                                                                    viewModel.clearSearchResultsStart()
                                                                    if (routeEnd != null) {
                                                                        viewModel.calculateRoute(location, routeEnd!!)
                                                                    }
                                                                }
                                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .background(palette.emeraldBadgeBg, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                                                                    drawLocationPinIcon(palette.emeraldPrimary)
                                                                }
                                                            }
                                                            Text(
                                                                text = result.displayName,
                                                                modifier = Modifier.weight(1f),
                                                                fontFamily = InterFont,
                                                                fontSize = 13.sp,
                                                                color = palette.textPrimary,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (index < searchResultsStart.lastIndex) {
                                                            HorizontalDivider(color = palette.dividerColor)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Direction arrow badge
                                Box(
                                    modifier = Modifier
                                        .padding(top = 20.dp)
                                        .size(32.dp)
                                        .background(palette.emeraldTintSurface, CircleShape)
                                        .border(1.dp, palette.emeraldPillBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(15.dp)) {
                                        drawRightArrowIcon(palette.emeraldPrimary)
                                    }
                                }

                                // To field
                                Box(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "To",
                                            fontFamily = InterFont,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSearchBarFocusedDestination) palette.emeraldTintSurface.copy(alpha = 0.55f) else palette.inputBg,
                                            border = BorderStroke(
                                                width = if (isSearchBarFocusedDestination) 1.5.dp else 1.dp,
                                                color = if (isSearchBarFocusedDestination) palette.emeraldPrimary else palette.borderInput
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(palette.emeraldBadgeBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                                                        drawDestinationIcon(palette.emeraldPrimary)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                BasicTextField(
                                                    value = searchQueryDestination,
                                                    onValueChange = {
                                                        searchQueryDestination = it
                                                        selectedDestinationDisplayName = null
                                                        routeEnd = null
                                                        if (it.isNotBlank()) {
                                                            viewModel.searchAddressDestination(it)
                                                        } else {
                                                            showDropdownDestination = false
                                                            viewModel.clearSearchResultsDestination()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onFocusChanged { focusState ->
                                                            isSearchBarFocusedDestination = focusState.isFocused
                                                            if (focusState.isFocused && searchResultsDestination.isNotEmpty() && searchQueryDestination.isNotBlank()) {
                                                                showDropdownDestination = true
                                                            }
                                                            if (!focusState.isFocused && searchQueryDestination.isBlank()) {
                                                                viewModel.clearSearchResultsDestination()
                                                                showDropdownDestination = false
                                                            }
                                                        },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = InterFont,
                                                        fontSize = 13.sp,
                                                        color = palette.textPrimary
                                                    ),
                                                    decorationBox = { innerTextField ->
                                                        if (searchQueryDestination.isBlank()) {
                                                            Text(
                                                                text = "Enter a destination",
                                                                fontFamily = InterFont,
                                                                fontSize = 13.sp,
                                                                color = palette.textSecondary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        innerTextField()
                                                    },
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (searchQueryDestination.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clickable {
                                                                searchQueryDestination = ""
                                                                selectedDestinationDisplayName = null
                                                                routeEnd = null
                                                                showDropdownDestination = false
                                                                viewModel.clearSearchResultsDestination()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.dp)) {
                                                            drawCloseIcon(palette.textSecondary)
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier.size(20.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                                                            drawSearchIcon(palette.emeraldPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Search results dropdown for To
                                    if (showDropdownDestination && searchResultsDestination.isNotEmpty() && searchQueryDestination.isNotBlank()) {
                                        Popup(
                                            alignment = Alignment.TopStart,
                                            offset = IntOffset(0, with(density) { 68.dp.roundToPx() }),
                                            onDismissRequest = { showDropdownDestination = false },
                                            properties = PopupProperties(focusable = false)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .width(360.dp)
                                                    .heightIn(max = 240.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                shadowElevation = 8.dp,
                                                border = BorderStroke(1.dp, palette.borderSubtle),
                                                color = palette.cardBg
                                            ) {
                                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                    searchResultsDestination.forEachIndexed { index, result ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    val location = result.toLocation()
                                                                    routeEnd = location
                                                                    selectedDestinationDisplayName = result.displayName
                                                                    searchQueryDestination = result.displayName
                                                                    showDropdownDestination = false
                                                                    viewModel.clearSearchResultsDestination()
                                                                    if (routeStart != null) {
                                                                        viewModel.calculateRoute(routeStart!!, location)
                                                                    }
                                                                }
                                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .background(palette.emeraldBadgeBg, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                                                                    drawDestinationIcon(palette.emeraldPrimary)
                                                                }
                                                            }
                                                            Text(
                                                                text = result.displayName,
                                                                modifier = Modifier.weight(1f),
                                                                fontFamily = InterFont,
                                                                fontSize = 13.sp,
                                                                color = palette.textPrimary,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (index < searchResultsDestination.lastIndex) {
                                                            HorizontalDivider(color = palette.dividerColor)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Find accessible routes button
                            Button(
                                onClick = {
                                    if (routeStart != null && routeEnd != null) {
                                        viewModel.calculateRoute(routeStart!!, routeEnd!!)
                                    }
                                },
                                enabled = routeStart != null && routeEnd != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.emeraldPrimary,
                                    contentColor = if (palette.isDark) Color(0xFF052E16) else Color.White,
                                    disabledContainerColor = palette.emeraldDisabled,
                                    disabledContentColor = if (palette.isDark) Color(0xFF6EE7B7).copy(alpha = 0.6f) else Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 1.dp,
                                    disabledElevation = 0.dp
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Find accessible routes",
                                    fontFamily = InterFont,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Styled Report Mode Instruction Banner
                if (showReportMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.5.dp, palette.emeraldPrimary),
                        color = palette.cardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(palette.emeraldBadgeBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                                    drawLocationPinIcon(palette.emeraldPrimary)
                                }
                            }
                            Text(
                                text = "Tap on the map to place the obstacle",
                                fontFamily = InterFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Surface(
                                modifier = Modifier.clickable { viewModel.exitReportMode() },
                                shape = RoundedCornerShape(8.dp),
                                color = palette.emeraldTintSurface,
                                border = BorderStroke(1.dp, palette.emeraldPillBorder)
                            ) {
                                Text(
                                    text = "Cancel",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontFamily = InterFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.emeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Journey Planner Overlay - matches Figma design + dark mode
            if (showJourneyPlanner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.canvasBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top bar with back arrow + theme toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(onClick = { showJourneyPlanner = false }),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                                    drawBackArrowIcon(palette.emeraldPrimary)
                                }
                            }
                            ThemeToggleButton(
                                isDarkMode = isDarkMode,
                                onToggle = toggleTheme,
                                showLabel = false
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        // Centered content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 896.dp)
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title section with radial gradient background
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val boxWidth = maxWidth.value
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.radialGradient(
                                                center = Offset(
                                                    boxWidth / 2,
                                                    100f
                                                ),
                                                radius = 400f,
                                                colors = listOf(
                                                    palette.glowStart,
                                                    palette.glowEnd
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Plan your journey",
                                            fontFamily = InterFont,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 44.sp,
                                            color = palette.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Where would you like to go?",
                                            fontFamily = InterFont,
                                            fontSize = 16.sp,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Journey form card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp,
                                color = palette.cardBg,
                                border = BorderStroke(1.dp, palette.borderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // From location
                                    Text(
                                        text = "From",
                                        fontFamily = InterFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = palette.inputBg,
                                        border = BorderStroke(1.dp, palette.borderInput)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                                    drawLocationPinIcon(palette.emeraldPrimary)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                BasicTextField(
                                                    value = searchQueryStart,
                                                    onValueChange = {
                                                        searchQueryStart = it
                                                        selectedStartDisplayName = null
                                                        routeStart = null
                                                        if (it.isNotBlank()) {
                                                            viewModel.searchAddressStart(it)
                                                        } else {
                                                            viewModel.clearSearchResultsStart()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onFocusChanged { focusState ->
                                                            isSearchBarFocusedStart = focusState.isFocused
                                                            if (!focusState.isFocused && searchQueryStart.isBlank()) {
                                                                viewModel.clearSearchResultsStart()
                                                            }
                                                        },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = InterFont,
                                                        fontSize = 14.sp,
                                                        color = palette.textPrimary
                                                    ),
                                                    decorationBox = { innerTextField ->
                                                        if (searchQueryStart.isBlank()) {
                                                            Text(
                                                                text = "Choose a starting point",
                                                                fontFamily = InterFont,
                                                                fontSize = 14.sp,
                                                                color = palette.textSecondary
                                                            )
                                                        }
                                                        innerTextField()
                                                    },
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // To location
                                    Text(
                                        text = "To",
                                        fontFamily = InterFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = palette.inputBg,
                                        border = BorderStroke(1.dp, palette.borderInput)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                                    drawDestinationIcon(palette.emeraldPrimary)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                BasicTextField(
                                                    value = searchQueryDestination,
                                                    onValueChange = {
                                                        searchQueryDestination = it
                                                        selectedDestinationDisplayName = null
                                                        routeEnd = null
                                                        if (it.isNotBlank()) {
                                                            viewModel.searchAddressDestination(it)
                                                        } else {
                                                            viewModel.clearSearchResultsDestination()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onFocusChanged { focusState ->
                                                            isSearchBarFocusedDestination = focusState.isFocused
                                                            if (!focusState.isFocused && searchQueryDestination.isBlank()) {
                                                                viewModel.clearSearchResultsDestination()
                                                            }
                                                        },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = InterFont,
                                                        fontSize = 14.sp,
                                                        color = palette.textPrimary
                                                    ),
                                                    decorationBox = { innerTextField ->
                                                        if (searchQueryDestination.isBlank()) {
                                                            Text(
                                                                text = "Enter a destination",
                                                                fontFamily = InterFont,
                                                                fontSize = 14.sp,
                                                                color = palette.textSecondary
                                                            )
                                                        }
                                                        innerTextField()
                                                    },
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Back button
                                Row(
                                    modifier = Modifier
                                        .clickable(onClick = { showJourneyPlanner = false })
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                                        drawBackArrowIcon(palette.emeraldPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Back",
                                        fontFamily = InterFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.emeraldPrimary
                                    )
                                }

                                // Find accessible routes button
                                Button(
                                    onClick = {
                                        if (routeStart != null && routeEnd != null) {
                                            viewModel.calculateRoute(routeStart!!, routeEnd!!)
                                            showJourneyPlanner = false
                                        }
                                    },
                                    enabled = routeStart != null && routeEnd != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.emeraldPrimary,
                                        contentColor = if (palette.isDark) Color(0xFF052E16) else Color.White,
                                        disabledContainerColor = palette.emeraldDisabled
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text(
                                        text = "Find accessible routes",
                                        fontFamily = InterFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Report Form
            if (showReportForm && reportLocation != null) {
                ReportFormDialog(
                    location = reportLocation!!,
                    onDismiss = { viewModel.closeReportForm() },
                    onSubmit = { category, description, disabilityTypes, severity ->
                        viewModel.addReport(category, description, reportLocation!!, disabilityTypes, severity)
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
                },
                isVoted = votedReports.contains(report.id)
            )
        }

            // Error message toast
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 88.dp)
                        .widthIn(max = 560.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, if (palette.isDark) Color(0xFFEF4444) else Color(0xFFFCA5A5)),
                    color = palette.cardBg
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            fontFamily = InterFont,
                            fontSize = 13.sp,
                            color = palette.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            maxLines = 4
                        )
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text(
                                text = "Close",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.emeraldPrimary
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, palette.borderSubtle),
                    color = palette.cardBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = palette.emeraldPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Calculating accessible route…",
                            fontFamily = InterFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                    }
                }
            }

            // ETA / Confidence bottom panel
            if (route.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 450.dp) // Évite que le bloc prenne 100% de l'écran sur la version Web/Desktop
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logique du temps
                        val timeMultiplier = when (selectedProfile) {
                            MobilityProfile.WHEELCHAIR -> 1.3
                            MobilityProfile.CRUTCHES -> 1.5
                            MobilityProfile.VISUAL_IMPAIRMENT -> 1.2
                            MobilityProfile.STANDARD -> 1.0
                        }
                        val simulatedDurationSec = (route.size * 10) * timeMultiplier
                        val minutes = (simulatedDurationSec / 60).toInt().coerceAtLeast(1)

                        // Bloc de gauche : Temps
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⏱️ Temps estimé", color = Color.Gray, fontSize = 12.sp)
                            Text("~ $minutes min", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }

            // Bottom Map Controls (Center on User + Report Obstacle)
            if (!showProfileSelector) {
                // Center on User pill button - bottom left
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                        .height(48.dp)
                        .clickable {
                            canvasSizeForFit?.let { size ->
                                viewModel.centerOnUser(mapState, size)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, palette.borderSubtle),
                    color = palette.cardBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(palette.emeraldBadgeBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCentering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = palette.emeraldPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                    drawCrosshairLocationIcon(palette.emeraldPrimary)
                                }
                            }
                        }
                        Text(
                            text = "My location",
                            fontFamily = InterFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                }

                // Report obstacle pill button - bottom right, matching homepage emerald button
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .height(48.dp)
                        .clickable {
                            if (showReportMode) {
                                viewModel.exitReportMode()
                            } else {
                                viewModel.enterReportMode()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    border = if (showReportMode) BorderStroke(1.dp, palette.borderSubtle) else null,
                    color = if (showReportMode) palette.cardBg else palette.emeraldPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val buttonContentColor = when {
                            showReportMode -> palette.textPrimary
                            palette.isDark -> Color(0xFF052E16)
                            else -> Color.White
                        }
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                            drawFlagButtonIcon(buttonContentColor)
                        }
                        Text(
                            text = if (showReportMode) "Cancel report" else "Report obstacle",
                            fontFamily = InterFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = buttonContentColor
                        )
                    }
                }
            }

            // OSM Feature Detail Modal
            selectedFeature?.let { feature ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.scrimColor)
                        .clickable(onClick = { selectedFeature = null })
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, palette.borderSubtle),
                        color = palette.cardBg
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(palette.emeraldBadgeBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = feature.obstacleType.icon,
                                        fontSize = 20.sp
                                    )
                                }
                                Text(
                                    text = feature.obstacleType.label,
                                    fontFamily = InterFont,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = feature.description,
                                fontFamily = InterFont,
                                fontSize = 15.sp,
                                color = palette.textBody
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lat: ${feature.location.lat}, Lng: ${feature.location.lon}",
                                fontFamily = InterFont,
                                fontSize = 12.sp,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { selectedFeature = null },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.emeraldPrimary,
                                    contentColor = if (palette.isDark) Color(0xFF052E16) else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Close",
                                    fontFamily = InterFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    Surface(
        modifier = modifier.clickable(onClick = onToggle),
        shape = CircleShape,
        color = palette.emeraldTintSurface,
        border = BorderStroke(1.dp, palette.emeraldPillBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (showLabel) 14.dp else 8.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(palette.emeraldBadgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    if (isDarkMode) {
                        drawSunIcon(palette.emeraldPrimary)
                    } else {
                        drawMoonIcon(palette.emeraldPrimary)
                    }
                }
            }
            if (showLabel) {
                Text(
                    text = if (isDarkMode) "Light mode" else "Dark mode",
                    fontFamily = InterFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.emeraldPrimary
                )
            }
        }
    }
}

private fun DrawScope.drawMoonIcon(color: Color) {
    val w = size.width
    val h = size.height
    drawPath(
        path = Path().apply {
            moveTo(w * 0.72f, h * 0.62f)
            cubicTo(w * 0.48f, h * 0.62f, w * 0.34f, h * 0.44f, w * 0.42f, h * 0.16f)
            cubicTo(w * 0.22f, h * 0.22f, w * 0.12f, h * 0.44f, w * 0.18f, h * 0.65f)
            cubicTo(w * 0.25f, h * 0.85f, w * 0.49f, h * 0.94f, w * 0.70f, h * 0.82f)
            cubicTo(w * 0.77f, h * 0.78f, w * 0.82f, h * 0.70f, w * 0.85f, h * 0.62f)
            cubicTo(w * 0.80f, h * 0.62f, w * 0.76f, h * 0.62f, w * 0.72f, h * 0.62f)
            close()
        },
        color = color,
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSunIcon(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = w * 0.22f, center = Offset(cx, cy), style = stroke)
    val rays = listOf(
        Offset(0.5f, 0.08f) to Offset(0.5f, 0.18f),
        Offset(0.5f, 0.82f) to Offset(0.5f, 0.92f),
        Offset(0.08f, 0.5f) to Offset(0.18f, 0.5f),
        Offset(0.82f, 0.5f) to Offset(0.92f, 0.5f),
        Offset(0.20f, 0.20f) to Offset(0.27f, 0.27f),
        Offset(0.73f, 0.73f) to Offset(0.80f, 0.80f),
        Offset(0.80f, 0.20f) to Offset(0.73f, 0.27f),
        Offset(0.27f, 0.73f) to Offset(0.20f, 0.80f)
    )
    for ((start, end) in rays) {
        drawLine(
            color = color,
            start = Offset(w * start.x, h * start.y),
            end = Offset(w * end.x, h * end.y),
            strokeWidth = stroke.width,
            cap = stroke.cap
        )
    }
}

private fun DrawScope.drawIconWheelchair(color: Color) {
    val s = size.width / 96f
    val stroke = Stroke(width = 3.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        path = Path().apply {
            moveTo(40f * s, 29.3333f * s)
            cubicTo(40f * s, 30.7478f * s, 40.5619f * s, 32.1044f * s, 41.5621f * s, 33.1046f * s)
            cubicTo(42.5623f * s, 34.1048f * s, 43.9188f * s, 34.6667f * s, 45.3333f * s, 34.6667f * s)
            cubicTo(46.7478f * s, 34.6667f * s, 48.1044f * s, 34.1048f * s, 49.1046f * s, 33.1046f * s)
            cubicTo(50.1048f * s, 32.1044f * s, 50.6667f * s, 30.7478f * s, 50.6667f * s, 29.3333f * s)
            cubicTo(50.6667f * s, 27.9188f * s, 50.1048f * s, 26.5623f * s, 49.1046f * s, 25.5621f * s)
            cubicTo(48.1044f * s, 24.5619f * s, 46.7478f * s, 24f * s, 45.3333f * s, 24f * s)
            cubicTo(43.9188f * s, 24f * s, 42.5623f * s, 24.5619f * s, 41.5621f * s, 25.5621f * s)
            cubicTo(40.5619f * s, 26.5623f * s, 40f * s, 27.9188f * s, 40f * s, 29.3333f * s)
            close()
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(45.3333f * s, 34.6666f * s)
            lineTo(45.3333f * s, 56f * s)
            lineTo(56f * s, 56f * s)
            lineTo(66.6667f * s, 69.3333f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(45.3333f * s, 45.3334f * s)
            lineTo(58.6667f * s, 45.3334f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(34.6667f * s, 46.6666f * s)
            cubicTo(32.756f * s, 47.4579f * s, 31.0556f * s, 48.6828f * s, 29.6998f * s, 50.2444f * s)
            cubicTo(28.344f * s, 51.806f * s, 27.3701f * s, 53.6617f * s, 26.8551f * s, 55.6645f * s)
            cubicTo(26.34f * s, 57.6674f * s, 26.2979f * s, 59.7626f * s, 26.732f * s, 61.7846f * s)
            cubicTo(27.1662f * s, 63.8065f * s, 28.0647f * s, 65.6998f * s, 29.3566f * s, 67.3147f * s)
            cubicTo(30.6485f * s, 68.9295f * s, 32.2984f * s, 70.2218f * s, 34.1757f * s, 71.0892f * s)
            cubicTo(36.053f * s, 71.9566f * s, 38.1064f * s, 72.3754f * s, 40.1735f * s, 72.3126f * s)
            cubicTo(42.2406f * s, 72.2497f * s, 44.2647f * s, 71.7069f * s, 46.0859f * s, 70.727f * s)
            cubicTo(47.907f * s, 69.7471f * s, 49.4753f * s, 68.357f * s, 50.6667f * s, 66.6666f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawIconMobility(color: Color) {
    val s = size.width / 96f
    val stroke = Stroke(width = 3.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        path = Path().apply {
            moveTo(37.3333f * s, 29.3333f * s)
            cubicTo(37.3333f * s, 27.9188f * s, 37.8952f * s, 26.5623f * s, 38.8954f * s, 25.5621f * s)
            cubicTo(39.8956f * s, 24.5619f * s, 41.2522f * s, 24f * s, 42.6666f * s, 24f * s)
            lineTo(53.3333f * s, 24f * s)
            cubicTo(54.7478f * s, 24f * s, 56.1044f * s, 24.5619f * s, 57.1045f * s, 25.5621f * s)
            cubicTo(58.1047f * s, 26.5623f * s, 58.6666f * s, 27.9188f * s, 58.6666f * s, 29.3333f * s)
            cubicTo(58.6666f * s, 30.7478f * s, 58.1047f * s, 32.1044f * s, 57.1045f * s, 33.1046f * s)
            cubicTo(56.1044f * s, 34.1048f * s, 54.7478f * s, 34.6667f * s, 53.3333f * s, 34.6667f * s)
            lineTo(42.6666f * s, 34.6667f * s)
            cubicTo(41.2522f * s, 34.6667f * s, 39.8956f * s, 34.1048f * s, 38.8954f * s, 33.1046f * s)
            cubicTo(37.8952f * s, 32.1044f * s, 37.3333f * s, 30.7478f * s, 37.3333f * s, 29.3333f * s)
            close()
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(45.3333f * s, 72f * s)
            lineTo(50.6666f * s, 72f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(48f * s, 72f * s)
            lineTo(48f * s, 61.088f * s)
            cubicTo(48.0002f * s, 59.5086f * s, 48.4678f * s, 57.9646f * s, 49.344f * s, 56.6506f * s)
            lineTo(51.9893f * s, 52.6826f * s)
            cubicTo(52.8655f * s, 51.3686f * s, 53.3332f * s, 49.8246f * s, 53.3333f * s, 48.2453f * s)
            lineTo(53.3333f * s, 34.6666f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(48f * s, 72f * s)
            lineTo(48f * s, 61.088f * s)
            cubicTo(47.9998f * s, 59.5086f * s, 47.5322f * s, 57.9646f * s, 46.656f * s, 56.6506f * s)
            lineTo(44.0107f * s, 52.6826f * s)
            cubicTo(43.1345f * s, 51.3686f * s, 42.6669f * s, 49.8246f * s, 42.6667f * s, 48.2453f * s)
            lineTo(42.6667f * s, 34.6666f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(42.6667f * s, 45.3334f * s)
            lineTo(53.3334f * s, 45.3334f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawIconVisual(color: Color) {
    val s = size.width / 96f
    val stroke = Stroke(width = 3.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        path = Path().apply {
            moveTo(44.2267f * s, 44.2322f * s)
            cubicTo(43.2266f * s, 45.2326f * s, 42.6649f * s, 46.5893f * s, 42.6652f * s, 48.0038f * s)
            cubicTo(42.6654f * s, 49.4183f * s, 43.2276f * s, 50.7748f * s, 44.228f * s, 51.7748f * s)
            cubicTo(45.2284f * s, 52.7749f * s, 46.5851f * s, 53.3366f * s, 47.9996f * s, 53.3363f * s)
            cubicTo(49.4141f * s, 53.3361f * s, 50.7706f * s, 52.7739f * s, 51.7707f * s, 51.7735f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(60.4827f * s, 60.4614f * s)
            cubicTo(56.742f * s, 62.8018f * s, 52.4124f * s, 64.0292f * s, 48f * s, 64.0001f * s)
            cubicTo(38.4f * s, 64.0001f * s, 30.4f * s, 58.6667f * s, 24f * s, 48.0001f * s)
            cubicTo(27.392f * s, 42.3467f * s, 31.232f * s, 38.1921f * s, 35.52f * s, 35.5361f * s)
            moveTo(43.1467f * s, 32.4801f * s)
            cubicTo(44.7441f * s, 32.1567f * s, 46.3702f * s, 31.9959f * s, 48f * s, 32.0001f * s)
            cubicTo(57.6f * s, 32.0001f * s, 65.6f * s, 37.3334f * s, 72f * s, 48.0001f * s)
            cubicTo(70.224f * s, 50.9601f * s, 68.3227f * s, 53.5121f * s, 66.2987f * s, 55.6534f * s)
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(24f * s, 24f * s)
            lineTo(72f * s, 72f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawIconFlag(color: Color) {
    val s = size.width / 96f
    val stroke = Stroke(width = 3.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        path = Path().apply {
            moveTo(29.3333f * s, 29.3332f * s)
            cubicTo(31.8257f * s, 26.8901f * s, 35.1766f * s, 25.5217f * s, 38.6666f * s, 25.5217f * s)
            cubicTo(42.1567f * s, 25.5217f * s, 45.5076f * s, 26.8901f * s, 48f * s, 29.3332f * s)
            cubicTo(50.4924f * s, 31.7762f * s, 53.8433f * s, 33.1446f * s, 57.3333f * s, 33.1446f * s)
            cubicTo(60.8233f * s, 33.1446f * s, 64.1743f * s, 31.7762f * s, 66.6666f * s, 29.3332f * s)
            lineTo(66.6666f * s, 53.3332f * s)
            cubicTo(64.1743f * s, 55.7762f * s, 60.8233f * s, 57.1446f * s, 57.3333f * s, 57.1446f * s)
            cubicTo(53.8433f * s, 57.1446f * s, 50.4924f * s, 55.7762f * s, 48f * s, 53.3332f * s)
            cubicTo(45.5076f * s, 50.8901f * s, 42.1567f * s, 49.5217f * s, 38.6666f * s, 49.5217f * s)
            cubicTo(35.1766f * s, 49.5217f * s, 31.8257f * s, 50.8901f * s, 29.3333f * s, 53.3332f * s)
            lineTo(29.3333f * s, 29.3332f * s)
            close()
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(29.3333f * s, 71.9999f * s)
            lineTo(29.3333f * s, 53.3333f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawFlagButtonIcon(color: Color) {
    val s = size.width / 96f
    val stroke = Stroke(width = 6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        path = Path().apply {
            moveTo(24f * s, 24f * s)
            cubicTo(27.5f * s, 20.5f * s, 32f * s, 18.5f * s, 37f * s, 18.5f * s)
            cubicTo(42f * s, 18.5f * s, 46.5f * s, 20.5f * s, 50f * s, 24f * s)
            cubicTo(53.5f * s, 27.5f * s, 58f * s, 29.5f * s, 63f * s, 29.5f * s)
            cubicTo(68f * s, 29.5f * s, 72.5f * s, 27.5f * s, 76f * s, 24f * s)
            lineTo(76f * s, 56f * s)
            cubicTo(72.5f * s, 59.5f * s, 68f * s, 61.5f * s, 63f * s, 61.5f * s)
            cubicTo(58f * s, 61.5f * s, 53.5f * s, 59.5f * s, 50f * s, 56f * s)
            cubicTo(46.5f * s, 52.5f * s, 42f * s, 50.5f * s, 37f * s, 50.5f * s)
            cubicTo(32f * s, 50.5f * s, 27.5f * s, 52.5f * s, 24f * s, 56f * s)
            close()
        },
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(24f * s, 80f * s)
            lineTo(24f * s, 56f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawCheckmarkIcon(color: Color) {
    val w = size.width
    val h = size.height
    drawPath(
        path = Path().apply {
            moveTo(w * 0.2f, h * 0.53f)
            lineTo(w * 0.42f, h * 0.75f)
            lineTo(w * 0.82f, h * 0.28f)
        },
        color = color,
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawCloseIcon(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(w * 0.15f, h * 0.15f), end = Offset(w * 0.85f, h * 0.85f), strokeWidth = stroke.width, cap = stroke.cap)
    drawLine(color = color, start = Offset(w * 0.85f, h * 0.15f), end = Offset(w * 0.15f, h * 0.85f), strokeWidth = stroke.width, cap = stroke.cap)
}

private fun DrawScope.drawRightArrowIcon(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(color = color, start = Offset(w * 0.15f, h * 0.5f), end = Offset(w * 0.85f, h * 0.5f), strokeWidth = stroke.width, cap = stroke.cap)
    drawPath(
        path = Path().apply {
            moveTo(w * 0.55f, h * 0.22f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.55f, h * 0.78f)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawCrosshairLocationIcon(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = w * 0.30f
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = r, center = Offset(cx, cy), style = stroke)
    drawCircle(color = color, radius = w * 0.09f, center = Offset(cx, cy))
    drawLine(color = color, start = Offset(cx, h * 0.05f), end = Offset(cx, h * 0.22f), strokeWidth = stroke.width, cap = stroke.cap)
    drawLine(color = color, start = Offset(cx, h * 0.78f), end = Offset(cx, h * 0.95f), strokeWidth = stroke.width, cap = stroke.cap)
    drawLine(color = color, start = Offset(w * 0.05f, cy), end = Offset(w * 0.22f, cy), strokeWidth = stroke.width, cap = stroke.cap)
    drawLine(color = color, start = Offset(w * 0.78f, cy), end = Offset(w * 0.95f, cy), strokeWidth = stroke.width, cap = stroke.cap)
}

// Location pin icon for Journey Planner From field
private fun DrawScope.drawLocationPinIcon(color: Color) {
    val s = size.width / 16f
    val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        path = Path().apply {
            moveTo(8f * s, 1f * s)
            cubicTo(4.2f * s, 1f * s, 1.5f * s, 3.8f * s, 1.5f * s, 7.2f * s)
            cubicTo(1.5f * s, 11.4f * s, 8f * s, 15.5f * s, 8f * s, 15.5f * s)
            cubicTo(8f * s, 15.5f * s, 14.5f * s, 11.4f * s, 14.5f * s, 7.2f * s)
            cubicTo(14.5f * s, 3.8f * s, 11.8f * s, 1f * s, 8f * s, 1f * s)
            close()
        },
        color = color,
        style = stroke
    )
    drawCircle(
        color = color,
        radius = 2.2f * s,
        center = Offset(8f * s, 7.2f * s),
        style = stroke
    )
}

// Destination icon for Journey Planner To field
private fun DrawScope.drawDestinationIcon(color: Color) {
    val s = size.width / 16f
    val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawCircle(
        color = color,
        radius = 6f * s,
        center = Offset(8f * s, 8f * s),
        style = stroke
    )
    drawCircle(
        color = color,
        radius = 2f * s,
        center = Offset(8f * s, 8f * s)
    )
}

// Back arrow icon for Journey Planner & Top Card
private fun DrawScope.drawBackArrowIcon(color: Color) {
    val s = size.width / 24f
    drawPath(
        path = Path().apply {
            moveTo(16f * s, 4f * s)
            lineTo(8f * s, 12f * s)
            lineTo(16f * s, 20f * s)
        },
        color = color,
        style = Stroke(width = 2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// Search icon (magnifying glass) for search fields
private fun DrawScope.drawSearchIcon(color: Color) {
    val s = size.width / 18f
    val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round)
    drawCircle(
        center = Offset(7.5f * s, 7.5f * s),
        radius = 5f * s,
        color = color,
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(11.5f * s, 11.5f * s)
            lineTo(16f * s, 16f * s)
        },
        color = color,
        style = stroke
    )
}

private val InterFont = FontFamily.SansSerif

@Composable
fun ProfileCardFigma(
    label: String,
    iconType: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interFont = InterFont
    val palette = LocalAppPalette.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) palette.emeraldTintSurface else palette.cardBg,
        border = if (isSelected) {
            BorderStroke(1.5.dp, palette.emeraldPrimary)
        } else {
            BorderStroke(1.dp, palette.borderSubtle)
        },
        shape = RoundedCornerShape(10.dp),
        shadowElevation = if (isSelected) 3.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(palette.emeraldBadgeBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(30.dp)) {
                        when (iconType) {
                            0 -> drawIconWheelchair(palette.emeraldIcon)
                            1 -> drawIconMobility(palette.emeraldIcon)
                            2 -> drawIconVisual(palette.emeraldIcon)
                            3 -> drawIconFlag(palette.emeraldIcon)
                        }
                    }
                }
                Text(
                    text = label,
                    fontFamily = interFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                    drawCheckmarkIcon(palette.emeraldPrimary)
                }
            }
        }
    }
}


@Composable
fun ReportFormDialog(
    location: Location,
    onDismiss: () -> Unit,
    onSubmit: (String, String, List<String>, Severity) -> Unit
) {
    val palette = LocalAppPalette.current
    var selectedType by remember { mutableStateOf(ReportType.STAIRS) }
    var description by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(Severity.MEDIUM) }
    val allDisabilityTypes = listOf("wheelchair", "blind", "crutches", "elderly", "parent-child")
    var selectedDisabilityTypes by remember { mutableStateOf(listOf<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrimColor)
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, palette.borderSubtle),
            color = palette.cardBg
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Report an obstacle",
                    fontFamily = InterFont,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${location.lat}, Lng: ${location.lon}",
                    fontFamily = InterFont,
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable form fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Category",
                        fontFamily = InterFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        val reportTypes = ReportType.entries.filter { it != ReportType.RAMP }
                        for (i in reportTypes.indices step 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                reportTypes.getOrNull(i)?.let { type ->
                                    val active = type == selectedType
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedType = type },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) palette.emeraldTintSurface else palette.inputBg,
                                        border = BorderStroke(
                                            if (active) 1.5.dp else 1.dp,
                                            if (active) palette.emeraldPrimary else palette.borderSubtle
                                        )
                                    ) {
                                        Text(
                                            text = type.label,
                                            modifier = Modifier.padding(12.dp),
                                            fontFamily = InterFont,
                                            fontSize = 13.sp,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (active) palette.emeraldPrimary else palette.textPrimary
                                        )
                                    }
                                }
                                reportTypes.getOrNull(i + 1)?.let { type ->
                                    val active = type == selectedType
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedType = type },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) palette.emeraldTintSurface else palette.inputBg,
                                        border = BorderStroke(
                                            if (active) 1.5.dp else 1.dp,
                                            if (active) palette.emeraldPrimary else palette.borderSubtle
                                        )
                                    ) {
                                        Text(
                                            text = type.label,
                                            modifier = Modifier.padding(12.dp),
                                            fontFamily = InterFont,
                                            fontSize = 13.sp,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (active) palette.emeraldPrimary else palette.textPrimary
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
                    Text(
                        text = "Affected disability types",
                        fontFamily = InterFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column {
                        for (dt in allDisabilityTypes) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDisabilityTypes = if (selectedDisabilityTypes.contains(dt)) {
                                            selectedDisabilityTypes - dt
                                        } else {
                                            selectedDisabilityTypes + dt
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedDisabilityTypes.contains(dt),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = palette.emeraldPrimary,
                                        uncheckedColor = palette.textSecondary
                                    ),
                                    onCheckedChange = {
                                        selectedDisabilityTypes = if (it) {
                                            selectedDisabilityTypes + dt
                                        } else {
                                            selectedDisabilityTypes - dt
                                        }
                                    }
                                )
                                Text(
                                    text = dt,
                                    modifier = Modifier.padding(start = 6.dp),
                                    fontFamily = InterFont,
                                    fontSize = 13.sp,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Severity",
                        fontFamily = InterFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (sev in Severity.entries) {
                            val label = when (sev) {
                                Severity.LOW -> "Low"
                                Severity.MEDIUM -> "Medium"
                                Severity.HIGH -> "High"
                            }
                            val active = sev == selectedSeverity
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSeverity = sev },
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) palette.emeraldTintSurface else palette.inputBg,
                                border = BorderStroke(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) palette.emeraldPrimary else palette.borderSubtle
                                )
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(10.dp),
                                    fontFamily = InterFont,
                                    fontSize = 13.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active) palette.emeraldPrimary else palette.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Description (optional)",
                        fontFamily = InterFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFont,
                            fontSize = 13.sp,
                            color = palette.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(palette.inputBg, RoundedCornerShape(10.dp))
                            .border(1.dp, palette.borderInput, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Fixed action buttons at the bottom
                if (selectedDisabilityTypes.isEmpty()) {
                    Text(
                        text = "Select at least one affected disability type to submit.",
                        fontFamily = InterFont,
                        fontSize = 12.sp,
                        color = if (palette.isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        border = BorderStroke(1.dp, palette.borderInput),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = InterFont,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                    Button(
                        onClick = {
                            if (selectedDisabilityTypes.isEmpty()) {
                                return@Button
                            }
                            onSubmit(selectedType.name, description, selectedDisabilityTypes, selectedSeverity)
                        },
                        enabled = selectedDisabilityTypes.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.emeraldPrimary,
                            contentColor = if (palette.isDark) Color(0xFF052E16) else Color.White,
                            disabledContainerColor = palette.emeraldDisabled,
                            disabledContentColor = if (palette.isDark) Color(0xFF6EE7B7).copy(alpha = 0.6f) else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Send Report",
                            fontFamily = InterFont,
                            fontWeight = FontWeight.SemiBold
                        )
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
    onResolve: () -> Unit,
    isVoted: Boolean = false
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrimColor)
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, palette.borderSubtle),
            color = palette.cardBg
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
                        fontFamily = InterFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    val statusColor = when (report.status) {
                        ReportStatus.ACTIVE -> if (palette.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        ReportStatus.RESOLVED -> palette.emeraldPrimary
                    }
                    val statusBg = when (report.status) {
                        ReportStatus.ACTIVE -> if (palette.isDark) Color(0xFF422006) else Color(0xFFFEF3C7)
                        ReportStatus.RESOLVED -> palette.emeraldTintSurface
                    }
                    val statusLabel = when (report.status) {
                        ReportStatus.ACTIVE -> "Active"
                        ReportStatus.RESOLVED -> "Resolved"
                    }
                    Surface(
                        shape = CircleShape,
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            fontFamily = InterFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Category: ${report.category}",
                    fontFamily = InterFont,
                    fontSize = 14.sp,
                    color = palette.textPrimary
                )

                val severityLabel = when (report.severity) {
                    Severity.LOW -> "Low"
                    Severity.MEDIUM -> "Medium"
                    Severity.HIGH -> "High"
                }
                val severityColor = when (report.severity) {
                    Severity.LOW -> palette.emeraldPrimary
                    Severity.MEDIUM -> if (palette.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                    Severity.HIGH -> if (palette.isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                }
                Text(
                    text = "Severity: $severityLabel",
                    fontFamily = InterFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = severityColor
                )

                report.description.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Description",
                        fontFamily = InterFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = desc,
                        fontFamily = InterFont,
                        fontSize = 14.sp,
                        color = palette.textBody
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Affected: ${report.disabilityTypes.joinToString(", ")}",
                    fontFamily = InterFont,
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${report.location.lat}, Lng: ${report.location.lon} · Created ${report.createdAt.take(10)}",
                    fontFamily = InterFont,
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = palette.borderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${report.resolvedVotes} / ${VOTE_THRESHOLD}",
                        fontFamily = InterFont,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.emeraldPrimary
                    )
                    Text(
                        text = "Votes confirming this obstacle is resolved",
                        fontFamily = InterFont,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (report.status == ReportStatus.ACTIVE) {
                    val hasVoted = isVoted
                    Button(
                        onClick = onResolve,
                        enabled = !hasVoted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasVoted)
                                Color(0xFF9E9E9E)
                            else
                                palette.emeraldPrimary,

                            contentColor = Color.White,

                            disabledContainerColor = if (palette.isDark)
                                Color(0xFF1B4332)
                            else
                                Color(0xFF9E9E9E),

                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (hasVoted) "✓ Voted" else "Confirm Resolved",
                            fontFamily = InterFont,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "This report is marked as resolved.",
                        fontFamily = InterFont,
                        fontSize = 13.sp,
                        color = palette.emeraldPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    border = BorderStroke(1.dp, palette.borderInput),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Close",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}