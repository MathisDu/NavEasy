package com.example.lauzhack26_multiplatform.map

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes PNG bytes into an ImageBitmap. Platform-specific implementation.
 */
expect suspend fun decodePngToImageBitmap(bytes: ByteArray): ImageBitmap?
