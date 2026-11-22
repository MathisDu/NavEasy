package com.example.lauzhack26_multiplatform

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.Darwin

actual fun findEngine(): HttpClientEngineFactory<*> = Darwin
