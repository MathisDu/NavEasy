package com.example.lauzhack26_multiplatform.map

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun decodePngToImageBitmap(bytes: ByteArray): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        println("[TileDecoder] Error decoding tile: ${e.message}")
        null
    }
}
