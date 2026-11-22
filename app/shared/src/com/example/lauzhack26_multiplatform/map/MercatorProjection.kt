package com.example.lauzhack26_multiplatform.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * Web Mercator projection utilities for converting between geographic coordinates (lat/lon)
 * and pixel coordinates on the map canvas.
 */
object MercatorProjection {

    private const val TILE_SIZE = 256
    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    private fun toRadians(degrees: Double): Double = degrees * DEG_TO_RAD
    private fun toDegrees(radians: Double): Double = radians * RAD_TO_DEG

    /**
     * Convert latitude to Y tile coordinate at a given zoom level.
     */
    fun latToY(lat: Double, zoom: Int): Double {
        val latRad = toRadians(lat)
        val sinLat = sin(latRad)
        val y = 0.5 * ln((1.0 + sinLat) / (1.0 - sinLat))
        val scale = TILE_SIZE * (1 shl zoom) / (2.0 * PI)
        return scale * (PI - y)
    }

    /**
     * Convert longitude to X tile coordinate at a given zoom level.
     */
    fun lonToX(lon: Double, zoom: Int): Double {
        return (lon + 180.0) / 360.0 * TILE_SIZE * (1 shl zoom)
    }

    /**
     * Convert latitude/longitude to pixel coordinates on the canvas.
     */
    fun locationToPixel(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
        val x = lonToX(lon, zoom)
        val y = latToY(lat, zoom)
        return x to y
    }

    /**
     * Convert pixel coordinates back to latitude/longitude.
     */
    fun pixelToLocation(x: Double, y: Double, zoom: Int): Pair<Double, Double> {
        val scale = TILE_SIZE * (1 shl zoom)
        val lon = (x / scale) * 360.0 - 180.0
        val n = PI - 2.0 * PI * y / scale
        val lat = toDegrees(atan(0.5 * (exp(n) - exp(-n))))
        return lat to lon
    }

    /**
     * Get the tile row and column for a given lat/lon at a zoom level.
     */
    @Suppress("unused")
    fun getTileCoordinates(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val x = lonToX(lon, zoom)
        val y = latToY(lat, zoom)
        val col = (x / TILE_SIZE).toInt()
        val row = (y / TILE_SIZE).toInt()
        return row to col
    }

    /**
     * Number of tiles per axis at a given zoom level.
     */
    fun tilesAtZoom(zoom: Int): Int {
        return 1 shl zoom // 2^zoom
    }
}
