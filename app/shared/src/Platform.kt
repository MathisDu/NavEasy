package com.example.lauzhack26_multiplatform

interface Platform {
    val name: String
    val serverUrl: String
}

expect fun getPlatform(): Platform

fun getServerUrl(): String = getPlatform().serverUrl
