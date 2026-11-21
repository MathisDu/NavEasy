package com.example.lauzhack26_multiplatform.map

/**
 * Interface for tile layer rendering - abstracts the platform-specific implementation.
 * Uses the same offset-based positioning as the Canvas to ensure perfect alignment.
 */
interface TileLayerInterface {
    fun updateTiles(offsetX: Float, offsetY: Float, zoom: Int, canvasWidth: Int, canvasHeight: Int, isDarkMode: Boolean = false)
    fun cleanup()
}

/**
 * Creates a tile layer instance for the current platform.
 */
expect fun createTileLayer(): TileLayerInterface?
