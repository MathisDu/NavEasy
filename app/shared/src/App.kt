package com.example.lauzhack26_multiplatform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lauzhack26_multiplatform.map.MapState
import com.example.lauzhack26_multiplatform.ui.MapViewModel
import com.example.lauzhack26_multiplatform.ui.NavigationApp
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import kotlin.Exception

// Find the available HTTP client engine for the current platform
expect fun findEngine(): HttpClientEngineFactory<*>

@Composable
@DevelopmentEntryPoint
fun App() {
    var initializationError by remember { mutableStateOf<String?>(null) }
    
    val httpClient = remember {
        try {
            HttpClient(findEngine()) {
                install(ContentNegotiation) {
                    json()
                }
            }
        } catch (e: Exception) {
            initializationError = "Erreur HttpClient: ${e.message}"
            null
        }
    }

    val mapState = remember {
        try {
            MapState()
        } catch (e: Exception) {
            initializationError = "Erreur MapState: ${e.message}"
            null
        }
    }

    val viewModel = remember {
        try {
            if (httpClient != null) MapViewModel(httpClient) else null
        } catch (e: Exception) {
            initializationError = "Erreur ViewModel: ${e.message}"
            null
        }
    }

    MaterialTheme {
        if (initializationError != null || httpClient == null || mapState == null || viewModel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF44336)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initializationError ?: "Erreur d'initialisation",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            NavigationApp(
                viewModel = viewModel,
                mapState = mapState
            )
        }
    }
}
