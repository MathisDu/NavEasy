package com.example.lauzhack26_multiplatform.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.OsmFeature
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * State for the OSM map component.
 */
class MapState(
    initialLat: Double = 48.8566, // Paris par défaut
    initialLon: Double = 2.3522,
    initialZoom: Int = 15
) {
    var centerLat by mutableStateOf(initialLat)
    var centerLon by mutableStateOf(initialLon)
    var zoom by mutableStateOf(initialZoom)
    var offset by mutableStateOf(Offset.Zero)
    var isDarkMode by mutableStateOf(false)

    /**
     * Convert a Location to canvas pixel coordinates.
     */
    @Suppress("unused")
    fun locationToCanvas(location: Location): Offset {
        val (worldX, worldY) = MercatorProjection.locationToPixel(location.lat, location.lon, zoom)
        val canvasX = (worldX + offset.x.toDouble()).toFloat()
        val canvasY = (worldY + offset.y.toDouble()).toFloat()
        return Offset(canvasX, canvasY)
    }

    /**
     * Get the location at the center of the canvas.
     */
    @Suppress("unused")
    fun getCenterLocation(size: IntSize): Location {
        val centerX = (size.width / 2f + offset.x).toDouble()
        val centerY = (size.height / 2f + offset.y).toDouble()
        val (lat, lon) = MercatorProjection.pixelToLocation(centerX, centerY, zoom)
        return Location(lat = lat, lon = lon)
    }

    /**
     * Adjust the map's center, zoom, and offset so that all the given route coordinates
     * are visible within the canvas of the specified size.
     * All calculations use local variables to avoid triggering Compose recomposition mid-calculation.
     */
    fun fitRouteToViewport(coordinates: List<Location>, canvasSize: IntSize) {
        if (coordinates.size < 2) return

        val canvasWidth = canvasSize.width.toDouble()
        val canvasHeight = canvasSize.height.toDouble()
        val padding = 80.0

        // Find the geographic bounding box
        var minLat = Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var maxLon = Double.MIN_VALUE

        for (loc in coordinates) {
            if (loc.lat < minLat) minLat = loc.lat
            if (loc.lon < minLon) minLon = loc.lon
            if (loc.lat > maxLat) maxLat = loc.lat
            if (loc.lon > maxLon) maxLon = loc.lon
        }

        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        if (latRange <= 0.0001 && lonRange <= 0.0001) return

        // Try each zoom level from 19 down to 1 and pick the highest zoom where the route fits
        val TILE_SIZE = 256.0
        var bestZoom = 1
        for (testZoom in 19 downTo 1) {
            val yMin = MercatorProjection.latToY(maxLat, testZoom)
            val yMax = MercatorProjection.latToY(minLat, testZoom)
            val yRange = yMax - yMin
            val xRange = lonRange / 360.0 * TILE_SIZE * (1 shl testZoom).toDouble()

            if (xRange <= canvasWidth - 2 * padding && yRange <= canvasHeight - 2 * padding) {
                bestZoom = testZoom
                break
            }
        }

        val newZoom = bestZoom

        // Calculate pixel bounds at the new zoom
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        for (loc in coordinates) {
            val (x, y) = MercatorProjection.locationToPixel(loc.lat, loc.lon, newZoom)
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }

        // Center of the route in pixel space
        val centerX = (minX + maxX) / 2.0
        val centerY = (minY + maxY) / 2.0

        // Update all state at once to avoid mid-calculation recomposition
        zoom = newZoom
        offset = Offset(
            -(centerX - canvasWidth / 2).toFloat(),
            -(centerY - canvasHeight / 2).toFloat()
        )
        val (newLat, newLon) = MercatorProjection.pixelToLocation(centerX, centerY, newZoom)
        centerLat = newLat
        centerLon = newLon
    }

}

/**
 * Composable that renders an OpenStreetMap using Canvas.
 * On WasmJs, tiles are rendered via HTML <img> elements behind the Canvas.
 * On other platforms, tiles are drawn as gray placeholders in the Canvas.
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    mapState: MapState,
    onLocationClick: ((Location) -> Unit)? = null,
    onFeatureClick: ((OsmFeature) -> Unit)? = null,
    routeCoordinates: List<Location> = emptyList(),
    reportLocations: List<Location> = emptyList(),
    osmFeatures: List<OsmFeature> = emptyList(),
    avoidPolygonCenters: List<Location> = emptyList(),
    onCanvasSizeReady: ((IntSize) -> Unit)? = null,
) {
    val tileLayer = remember { createTileLayer() }
    val isWasmJs = tileLayer != null

    // Canvas dimensions obtained via onGloballyPositioned before first draw
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Initialize offset once when canvas size becomes known
    var initialized by remember { mutableStateOf(false) }

    // Initialize offset and notify parent when canvas size becomes known
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            // Initialize offset only once on first canvas size
            if (!initialized) {
                val (centerPixelX, centerPixelY) = MercatorProjection.locationToPixel(
                    mapState.centerLat, mapState.centerLon, mapState.zoom
                )
                mapState.offset = Offset(
                    -(centerPixelX - canvasSize.width / 2).toFloat(),
                    -(centerPixelY - canvasSize.height / 2).toFloat()
                )
                initialized = true
            }
            // Notify parent whenever canvas size is available
            onCanvasSizeReady?.invoke(canvasSize)
        }
    }

    // Extraire la lecture de l'état ici, au niveau racine du composant
    val currentOffset = mapState.offset
    val currentZoom = mapState.zoom
    val currentDarkMode = mapState.isDarkMode

    // Update tile layer after every recomposition
    if (isWasmJs && initialized && canvasSize.width > 0 && canvasSize.height > 0) {
        SideEffect {
            tileLayer.updateTiles(
                offsetX = currentOffset.x,
                offsetY = currentOffset.y,
                zoom = currentZoom,
                canvasWidth = canvasSize.width,
                canvasHeight = canvasSize.height,
                isDarkMode = currentDarkMode
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                    val newSize = IntSize(layoutCoordinates.size.width, layoutCoordinates.size.height)
                    if (newSize != canvasSize) {
                        canvasSize = newSize
                    }
                }
                .background(Color.Transparent) // Hitbox forcée pour WasmJs
                .pointerInput(Unit) {
                    // 1. Gère le zoom tactile (Pinch-to-zoom)
                    detectTransformGestures { centroid, _, zoomChange, _ ->
                        if (zoomChange != 1.0f) {
                            val oldZoom = mapState.zoom
                            val newZoom = kotlin.math.max(1, kotlin.math.min(19, (oldZoom * zoomChange).toInt()))

                            if (newZoom != oldZoom) {
                                // Try to keep the centroid point under the same location
                                val (centroidLat, centroidLon) = MercatorProjection.pixelToLocation(
                                    (centroid.x - mapState.offset.x).toDouble(),
                                    (centroid.y - mapState.offset.y).toDouble(),
                                    oldZoom
                                )

                                mapState.zoom = newZoom

                                // Recalculate offset to keep the centroid stable
                                val (newCentroidPixelX, newCentroidPixelY) = MercatorProjection.locationToPixel(
                                    centroidLat, centroidLon, newZoom
                                )

                                mapState.offset = Offset(
                                    (centroid.x - newCentroidPixelX).toFloat(),
                                    (centroid.y - newCentroidPixelY).toFloat()
                                )
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 2. Gère le glissement (Drag) avec le clic-gauche de la souris ou un doigt
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        mapState.offset = Offset(
                            mapState.offset.x + dragAmount.x,
                            mapState.offset.y + dragAmount.y
                        )
                    }
                }
                .pointerInput(Unit) {
                    // 3. Gère le Zoom spécifiquement avec la molette de la souris (et cible le curseur)
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                                val change = event.changes.firstOrNull()
                                val delta = change?.scrollDelta?.y ?: 0f

                                if (delta != 0f) {
                                    val zoomDirection = if (delta > 0) -1 else 1
                                    val newZoom = kotlin.math.max(1, kotlin.math.min(19, mapState.zoom + zoomDirection))

                                    if (newZoom != mapState.zoom) {
                                        // a) Trouver la position actuelle du curseur de la souris
                                        val pointerPos = change?.position ?: Offset.Zero

                                        // b) Convertir la position de la souris en coordonnée géographique (lat/lon) avec l'ANCIEN zoom
                                        val worldXBefore = (pointerPos.x - mapState.offset.x).toDouble()
                                        val worldYBefore = (pointerPos.y - mapState.offset.y).toDouble()
                                        val (lat, lon) = MercatorProjection.pixelToLocation(worldXBefore, worldYBefore, mapState.zoom)

                                        // c) Appliquer le nouveau zoom
                                        mapState.zoom = newZoom

                                        // d) Recalculer les pixels mondiaux de cette même coordonnée géographique avec le NOUVEAU zoom
                                        val (worldXAfter, worldYAfter) = MercatorProjection.locationToPixel(lat, lon, newZoom)

                                        // e) Recalculer le décalage (offset) de la carte pour que le point sous la souris ne bouge pas
                                        mapState.offset = Offset(
                                            (pointerPos.x - worldXAfter).toFloat(),
                                            (pointerPos.y - worldYAfter).toFloat()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 4. Gère les clics courts pour le signalement d'obstacles ou les points
                    detectTapGestures { tapOffset ->
                        // Check if tap hits an OSM feature (orange circle)
                        val hitRadius = 20f
                        var featureHit = false

                        for (feature in osmFeatures) {
                            val (worldX, worldY) = MercatorProjection.locationToPixel(
                                feature.location.lat, feature.location.lon, mapState.zoom
                            )
                            val canvasX = (worldX + mapState.offset.x.toDouble()).toFloat()
                            val canvasY = (worldY + mapState.offset.y.toDouble()).toFloat()

                            val dx = tapOffset.x - canvasX
                            val dy = tapOffset.y - canvasY
                            val distance = sqrt(dx * dx + dy * dy)

                            if (distance <= hitRadius) {
                                onFeatureClick?.invoke(feature)
                                featureHit = true
                                break
                            }
                        }

                        // If no feature was hit, trigger the default location click
                        if (!featureHit) {
                            val worldX = (tapOffset.x - mapState.offset.x).toDouble()
                            val worldY = (tapOffset.y - mapState.offset.y).toDouble()
                            val (lat, lon) = MercatorProjection.pixelToLocation(worldX, worldY, mapState.zoom)
                            onLocationClick?.invoke(Location(lat = lat, lon = lon))
                        }
                    }
                }
        ) {
            val size = IntSize(this.size.width.toInt(), this.size.height.toInt())

            // Percer le fond du canvas WebGL pour révéler #TileLayer en dessous
            if (isWasmJs) {
                drawRect(
                    color = Color.Transparent,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
            }

            val tileSize = 256f
            val startCol = floor((-mapState.offset.x) / tileSize).toInt()
            val endCol = startCol + (size.width / tileSize).toInt() + 1
            val startRow = floor((-mapState.offset.y) / tileSize).toInt()
            val endRow = startRow + (size.height / tileSize).toInt() + 1
            val maxTiles = MercatorProjection.tilesAtZoom(mapState.zoom)

            // Draw placeholder tiles for non-WasmJs platforms
            if (!isWasmJs) {
                for (row in startRow..endRow) {
                    for (col in startCol..endCol) {
                        if (row < 0 || row >= maxTiles || col < 0 || col >= maxTiles) continue

                        val x = col * tileSize + mapState.offset.x
                        val y = row * tileSize + mapState.offset.y

                        drawRect(
                            color = Color(0xFFE0E0E0),
                            topLeft = Offset(x, y),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }
            }

        // Draw route polyline
        if (routeCoordinates.size > 1) {
            drawRoute(routeCoordinates, mapState, Color(0xFF1565C0), 5f)
        }

        // Draw start and end markers
        if (routeCoordinates.isNotEmpty()) {
            val startLoc = routeCoordinates.first()
            val (sx, sy) = MercatorProjection.locationToPixel(startLoc.lat, startLoc.lon, mapState.zoom)
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = 8f,
                center = Offset((sx + mapState.offset.x.toDouble()).toFloat(), (sy + mapState.offset.y.toDouble()).toFloat())
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset((sx + mapState.offset.x.toDouble()).toFloat(), (sy + mapState.offset.y.toDouble()).toFloat())
            )
        }
        if (routeCoordinates.size > 1) {
            val endLoc = routeCoordinates.last()
            val (ex, ey) = MercatorProjection.locationToPixel(endLoc.lat, endLoc.lon, mapState.zoom)
            drawCircle(
                color = Color(0xFFF44336),
                radius = 8f,
                center = Offset((ex + mapState.offset.x.toDouble()).toFloat(), (ey + mapState.offset.y.toDouble()).toFloat())
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset((ex + mapState.offset.x.toDouble()).toFloat(), (ey + mapState.offset.y.toDouble()).toFloat())
            )
        }

            // Draw report markers
            for (location in reportLocations) {
                val (worldX, worldY) = MercatorProjection.locationToPixel(
                    location.lat, location.lon, mapState.zoom
                )
                val canvasX = (worldX + mapState.offset.x.toDouble()).toFloat()
                val canvasY = (worldY + mapState.offset.y.toDouble()).toFloat()
                drawCircle(
                    color = Color.Red,
                    radius = 8f,
                    center = Offset(canvasX, canvasY)
                )
            }

            // Draw OSM features (stairs) as orange circles
            for (feature in osmFeatures) {
                val (worldX, worldY) = MercatorProjection.locationToPixel(
                    feature.location.lat, feature.location.lon, mapState.zoom
                )
                val canvasX = (worldX + mapState.offset.x.toDouble()).toFloat()
                val canvasY = (worldY + mapState.offset.y.toDouble()).toFloat()
                drawCircle(
                    color = Color(0xFFFFA500),
                    radius = 12f,
                    center = Offset(canvasX, canvasY)
                )
            }

            // Draw avoid polygons as semi-transparent red circles
            val radiusDegrees = 0.0002
            val circlePoints = 16
            for (center in avoidPolygonCenters) {
                // Scale longitude radius by cos(lat) so the shape appears circular on the map
                val lonRadius = radiusDegrees * cos(center.lat * PI / 180.0)
                val path = Path()
                var first = true
                for (i in 0 until circlePoints) {
                    val angle = 2.0 * PI * i / circlePoints
                    val corner = Location(
                        center.lat + radiusDegrees * cos(angle),
                        center.lon + lonRadius * sin(angle)
                    )
                    val (wx, wy) = MercatorProjection.locationToPixel(corner.lat, corner.lon, mapState.zoom)
                    val cx = (wx + mapState.offset.x.toDouble()).toFloat()
                    val cy = (wy + mapState.offset.y.toDouble()).toFloat()
                    if (first) {
                        path.moveTo(cx, cy)
                        first = false
                    } else {
                        path.lineTo(cx, cy)
                    }
                }
                path.close()
                drawPath(path, color = Color.Red.copy(alpha = 0.3f))
                drawPath(path, color = Color.Red, style = Stroke(width = 2f))
            }
        }

        // Legal attribution
        Text(
            text = "© OpenStreetMap contributors",
            style = TextStyle(
                fontSize = 10.sp,
                color = Color.Gray.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
        )
    }
}

private fun DrawScope.drawRoute(
    coordinates: List<Location>,
    mapState: MapState,
    color: Color,
    strokeWidth: Float
) {
    if (coordinates.size < 2) return

    val path = Path()
    var first = true

    for (location in coordinates) {
        val (worldX, worldY) = MercatorProjection.locationToPixel(
            location.lat, location.lon, mapState.zoom
        )
        val canvasX = (worldX + mapState.offset.x.toDouble()).toFloat()
        val canvasY = (worldY + mapState.offset.y.toDouble()).toFloat()

        if (first) {
            path.moveTo(canvasX, canvasY)
            first = false
        } else {
            path.lineTo(canvasX, canvasY)
        }
    }

    drawPath(path, color = color, style = Stroke(width = strokeWidth))
}
