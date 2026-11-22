package com.example.lauzhack26_multiplatform.map

import androidx.compose.ui.graphics.ImageBitmap

actual suspend fun decodePngToImageBitmap(bytes: ByteArray): ImageBitmap? {
    // iOS tile loading not implemented yet
    return null
}
