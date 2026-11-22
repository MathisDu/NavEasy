package com.example.lauzhack26_multiplatform.map

import androidx.compose.ui.graphics.ImageBitmap

actual suspend fun decodePngToImageBitmap(bytes: ByteArray): ImageBitmap? {
    // WasmJs uses HTML img elements, not ImageBitmap
    return null
}
