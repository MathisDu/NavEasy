package com.example.lauzhack26_multiplatform.map

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fetches OSM map tiles over HTTP and caches them as ImageBitmap.
 */
class TileLoader(
    private val httpClient: HttpClient
) {
    private val cache = mutableMapOf<TileKey, ImageBitmap>()
    private val loading = mutableSetOf<TileKey>()
    private val mutex = Mutex()

    data class TileKey(val row: Int, val col: Int, val zoom: Int)

    private fun tileUrl(row: Int, col: Int, zoom: Int): String {
        return "https://tile.openstreetmap.org/$zoom/$col/$row.png"
    }

    suspend fun getTile(row: Int, col: Int, zoom: Int): ImageBitmap? {
        val key = TileKey(row, col, zoom)

        mutex.withLock {
            // Return from cache if available
            cache[key]?.let { return it }

            // Avoid duplicate requests for the same tile
            if (key in loading) {
                return null
            }

            loading.add(key)
        }

        return try {
            val response = httpClient.get(tileUrl(row, col, zoom))
            if (response.status.isSuccess()) {
                val bytes = response.body<ByteArray>()
                decodePngToImageBitmap(bytes)?.also { bitmap ->
                    mutex.withLock {
                        cache[key] = bitmap
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("[TileLoader] Error loading tile $zoom/$col/$row: ${e.message}")
            null
        } finally {
            mutex.withLock {
                loading.remove(key)
            }
        }
    }

    fun getTileFromCache(row: Int, col: Int, zoom: Int): ImageBitmap? {
        return cache[TileKey(row, col, zoom)]
    }
}
