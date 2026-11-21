package com.example.lauzhack26_multiplatform.map

import kotlinx.browser.document
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.Element

/**
 * Manages OSM tile images using HTML <img> elements positioned in a DOM container.
 * This is the WasmJs implementation that avoids Skia image decoding issues.
 *
 * Uses the same Mercator projection as the Canvas to ensure tiles are positioned correctly.
 */
class HtmlTileLayer(
    containerId: String = "TileLayer"
) : TileLayerInterface {
    private val container: Element? = document.getElementById(containerId)
    private val tiles = mutableMapOf<TileKey, HTMLImageElement>()

    data class TileKey(val row: Int, val col: Int, val zoom: Int)

    private fun tileUrl(row: Int, col: Int, zoom: Int): String {
        return "https://tile.openstreetmap.org/$zoom/$col/$row.png"
    }

    override fun updateTiles(
        offsetX: Float,
        offsetY: Float,
        zoom: Int,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        container ?: return

        val tileSize = 256
        val maxTiles = MercatorProjection.tilesAtZoom(zoom)

        // Use the exact same logic as the Canvas in OsmMap.kt:
        // - startCol/Row from the offset
        // - position = col * tileSize + offset
        val startCol = kotlin.math.floor((-offsetX) / tileSize).toInt()
        val endCol = startCol + canvasWidth / tileSize + 1
        val startRow = kotlin.math.floor((-offsetY) / tileSize).toInt()
        val endRow = startRow + canvasHeight / tileSize + 1

        val neededKeys = mutableSetOf<TileKey>()
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                if (row < 0 || row >= maxTiles || col < 0 || col >= maxTiles) continue
                val key = TileKey(row, col, zoom)
                neededKeys.add(key)

                // Position: exactly matches the Canvas drawRect topLeft
                val x = col * tileSize + offsetX
                val y = row * tileSize + offsetY

                if (!tiles.containsKey(key)) {
                    val img = document.createElement("img") as HTMLImageElement
                    img.src = tileUrl(row, col, zoom)
                    img.style.position = "absolute"
                    img.style.width = "${tileSize}px"
                    img.style.height = "${tileSize}px"
                    img.style.left = "${x}px"
                    img.style.top = "${y}px"
                    img.style.setProperty("pointer-events", "none")
                    img.style.imageRendering = "auto"
                    img.style.border = "none"
                    img.style.outline = "none"
                    container.appendChild(img)
                    tiles[key] = img
                } else {
                    val img = tiles[key]!!
                    img.style.left = "${x}px"
                    img.style.top = "${y}px"
                    img.style.display = "block"
                }
            }
        }

        // Hide tiles that are no longer visible
        for ((key, img) in tiles) {
            if (!neededKeys.contains(key)) {
                img.style.display = "none"
            }
        }
    }

    override fun cleanup() {
        tiles.values.forEach { it.remove() }
        tiles.clear()
    }
}

actual fun createTileLayer(): TileLayerInterface? = HtmlTileLayer()
