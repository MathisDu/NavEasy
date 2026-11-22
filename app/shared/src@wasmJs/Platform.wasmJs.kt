package com.example.lauzhack26_multiplatform

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val serverUrl: String = "http://localhost:8081"
}

actual fun getPlatform(): Platform = WasmPlatform()
