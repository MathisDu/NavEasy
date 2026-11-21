package com.example.lauzhack26_multiplatform.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.example.lauzhack26_multiplatform.model.Location
import kotlin.math.floor

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
    routeCoordinates: List<Location> = emptyList(),
    reportLocations: List<Location> = emptyList(),
) {
    val tileLayer = remember { createTileLayer() }
    val isWasmJs = tileLayer != null

    // Canvas dimensions obtained via onGloballyPositioned before first draw
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Initialize offset once when canvas size becomes known
    var initialized by remember { mutableStateOf(false) }

    // Initialize offset as soon as we know the canvas size
    LaunchedEffect(canvasSize) {
        if (!initialized && canvasSize.width > 0 && canvasSize.height > 0) {
            val (centerPixelX, centerPixelY) = MercatorProjection.locationToPixel(
                mapState.centerLat, mapState.centerLon, mapState.zoom
            )
            mapState.offset = Offset(
                -(centerPixelX - canvasSize.width / 2).toFloat(),
                -(centerPixelY - canvasSize.height / 2).toFloat()
            )
            initialized = true
        }
    }

    // 1. CORRECTION : Extraire la lecture de l'état ici, au niveau racine du composant
    // Cela force Compose à rappeler ce bloc de code à chaque mouvement de la souris
    val currentOffset = mapState.offset
    val currentZoom = mapState.zoom

    // 2. Le SideEffect doit utiliser ces nouvelles variables
    if (isWasmJs && initialized && canvasSize.width > 0 && canvasSize.height > 0) {
        SideEffect {
            tileLayer.updateTiles(
                offsetX = currentOffset.x,
                offsetY = currentOffset.y,
                zoom = currentZoom,
                canvasWidth = canvasSize.width,
                canvasHeight = canvasSize.height
            )
        }
    }

    Canvas(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates ->
                val newSize = IntSize(layoutCoordinates.size.width, layoutCoordinates.size.height)
                if (newSize != canvasSize) {
                    canvasSize = newSize
                }
            }
            .background(Color.Transparent) // Hitbox forcée pour WasmJs
            .pointerInput(Unit) {
                // 1. Gère le glissement (Drag) avec le clic-gauche de la souris ou un doigt
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    mapState.offset = Offset(
                        mapState.offset.x + dragAmount.x,
                        mapState.offset.y + dragAmount.y
                    )
                }
            }
            .pointerInput(Unit) {
                // 2. Gère le Zoom spécifiquement avec la molette de la souris (et cible le curseur)
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
                // 3. Gère les clics courts pour le signalement d'obstacles
                detectTapGestures { tapOffset ->
                    val worldX = (tapOffset.x - mapState.offset.x).toDouble()
                    val worldY = (tapOffset.y - mapState.offset.y).toDouble()
                    val (lat, lon) = MercatorProjection.pixelToLocation(worldX, worldY, mapState.zoom)
                    onLocationClick?.invoke(Location(lat = lat, lon = lon))
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

        // Draw placeholder tiles (gray background) for non-WasmJs platforms
        // On WasmJs, tiles are rendered by HtmlTileLayer behind the Canvas
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
            drawRoute(routeCoordinates, mapState, Color.Blue, 4f)
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
