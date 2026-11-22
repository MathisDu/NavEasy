package com.example.lauzhack26_multiplatform

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val serverUrl: String = "http://localhost:8081"
}

actual fun getPlatform(): Platform = IOSPlatform()
