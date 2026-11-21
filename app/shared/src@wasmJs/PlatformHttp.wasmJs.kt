package com.example.lauzhack26_multiplatform

import io.ktor.client.engine.*
import io.ktor.client.engine.js.Js

actual fun findEngine(): HttpClientEngineFactory<*> = Js
