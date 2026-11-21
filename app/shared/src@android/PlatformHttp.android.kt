package com.example.lauzhack26_multiplatform

import io.ktor.client.engine.*
import io.ktor.client.engine.android.Android

actual fun findEngine(): HttpClientEngineFactory<*> = Android
