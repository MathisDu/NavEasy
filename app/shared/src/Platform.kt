package com.example.lauzhack26_multiplatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
