package com.example.lauzhack26_multiplatform.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import com.example.lauzhack26_multiplatform.model.Location
import kotlin.math.floor

/**
 * State for the OSM map component.
 */
class MapState {
    @Suppress("unused")
    var centerLat by mutableStateOf(48.8566) // Paris par défaut
    @Suppress("unused")
    var centerLon by mutableStateOf(2.3522)
    var zoom by mutableStateOf(15)
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
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    mapState: MapState,
    onLocationClick: ((Location) -> Unit)? = null,
    routeCoordinates: List<Location> = emptyList(),
    reportLocations: List<Location> = emptyList(),
) {

    Canvas(
        modifier = modifier
            .pointerInput(mapState.zoom) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (zoomChange != 1.0f) {
                        val newZoom = kotlin.math.max(1, kotlin.math.min(19, (mapState.zoom * zoomChange).toInt()))
                        if (newZoom != mapState.zoom) {
                            mapState.zoom = newZoom
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    mapState.offset = Offset(
                        mapState.offset.x + dragAmount.x,
                        mapState.offset.y + dragAmount.y
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val worldX = (tapOffset.x - mapState.offset.x).toDouble()
                    val worldY = (tapOffset.y - mapState.offset.y).toDouble()
                    val (lat, lon) = MercatorProjection.pixelToLocation(worldX, worldY, mapState.zoom)
                    onLocationClick?.invoke(Location(lat = lat, lon = lon))
                }
            }
    ) {
        // Use this.size directly instead of storing in mutableState to avoid layout conflicts
        val canvasSize = IntSize(this.size.width.toInt(), this.size.height.toInt())

        val tileSize = 256f
        val startCol = floor((-mapState.offset.x) / tileSize).toInt()
        val endCol = startCol + (canvasSize.width / tileSize).toInt() + 1
        val startRow = floor((-mapState.offset.y) / tileSize).toInt()
        val endRow = startRow + (canvasSize.height / tileSize).toInt() + 1
        val maxTiles = MercatorProjection.tilesAtZoom(mapState.zoom)

        // Draw tiles
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                if (row < 0 || row >= maxTiles || col < 0 || col >= maxTiles) continue

                val x = col * tileSize + mapState.offset.x
                val y = row * tileSize + mapState.offset.y

                // Draw placeholder for missing tiles
                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(x, y),
                    size = Size(tileSize, tileSize)
                )
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
