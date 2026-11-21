package com.example.lauzhack26_multiplatform.map

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple in-memory cache for OSM tile images.
 */
class TileCache {

    private val cache = mutableMapOf<TileKey, ImageBitmap>()
    private val mutex = Mutex()

    data class TileKey(val row: Int, val col: Int, val zoom: Int)

    suspend fun get(row: Int, col: Int, zoom: Int): ImageBitmap? = mutex.withLock {
        cache[TileKey(row, col, zoom)]
    }

    @Suppress("unused")
    suspend fun put(row: Int, col: Int, zoom: Int, bitmap: ImageBitmap) = mutex.withLock {
        cache[TileKey(row, col, zoom)] = bitmap
    }

    @Suppress("unused")
    suspend fun clear() = mutex.withLock {
        cache.clear()
    }
}
