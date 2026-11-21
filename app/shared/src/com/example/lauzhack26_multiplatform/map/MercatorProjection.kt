package com.example.lauzhack26_multiplatform.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
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
    private fun ln(x: Double): Double {
        // Natural log approximation using exp inverse
        var value = x
        var result = 0.0
        var term = 1.0
        while (value > 1.0) {
            value /= 2.0
            result += 0.6931471805599453
        }
        // Taylor series for ln(1 + t) where t = value - 1
        val t = value - 1.0
        var tPower = t
        for (i in 1..50) {
            term = tPower * if (i % 2 == 0) -1.0 else 1.0 / i
            result += term
            tPower *= t
        }
        return result
    }

    /**
     * Convert latitude to Y tile coordinate at a given zoom level.
     */
    fun latToY(lat: Double, zoom: Int): Double {
        val latRad = toRadians(lat)
        val sinLat = sin(latRad)
        val y = 0.5 * ln((1.0 + sinLat) / (1.0 - sinLat))
        return (TILE_SIZE * zoom) / (2.0 * PI) * (PI - y)
    }

    /**
     * Convert longitude to X tile coordinate at a given zoom level.
     */
    fun lonToX(lon: Double, zoom: Int): Double {
        return (lon + 180.0) / 360.0 * TILE_SIZE * zoom
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
        val lon = (x / (TILE_SIZE * zoom)) * 360.0 - 180.0
        val n = PI - 2.0 * PI * y / (TILE_SIZE * zoom)
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
