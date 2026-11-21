package com.example.lauzhack26_multiplatform

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.lauzhack26_multiplatform.map.MapState
import com.example.lauzhack26_multiplatform.ui.MapViewModel
import com.example.lauzhack26_multiplatform.ui.NavigationApp
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.jetbrains.compose.reload.DevelopmentEntryPoint

// Find the available HTTP client engine for the current platform
expect fun findEngine(): HttpClientEngineFactory<*>

@Composable
@DevelopmentEntryPoint
fun App() {
    val httpClient = remember {
        HttpClient(findEngine()) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    val mapState = remember { MapState() }
    val viewModel = remember { MapViewModel(httpClient) }

    MaterialTheme {
        NavigationApp(
            viewModel = viewModel,
            mapState = mapState
        )
    }
}
