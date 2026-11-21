package com.example.lauzhack26_multiplatform.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lauzhack26_multiplatform.model.Location
import com.example.lauzhack26_multiplatform.model.MobilityProfile
import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportType
import com.example.lauzhack26_multiplatform.model.RouteRequest
import com.example.lauzhack26_multiplatform.model.RouteResponse
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val httpClient: HttpClient,
    private val serverUrl: String = "http://localhost:8081"
) : ViewModel() {

    private val _selectedProfile = MutableStateFlow(MobilityProfile.WHEELCHAIR)
    val selectedProfile: StateFlow<MobilityProfile> = _selectedProfile.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _route = MutableStateFlow<List<Location>>(emptyList())
    val route: StateFlow<List<Location>> = _route.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showReportForm = MutableStateFlow(false)
    val showReportForm: StateFlow<Boolean> = _showReportForm.asStateFlow()

    private val _reportLocation = MutableStateFlow<Location?>(null)
    val reportLocation: StateFlow<Location?> = _reportLocation.asStateFlow()

    fun setProfile(profile: MobilityProfile) {
        _selectedProfile.value = profile
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = httpClient.get("$serverUrl/api/reports")
                if (response.status == HttpStatusCode.OK) {
                    val reportsList = response.body<List<Report>>()
                    _reports.value = reportsList
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur chargement signalements: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addReport(type: ReportType, description: String, location: Location) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = ReportRequest(type, description, location)
                val response = httpClient.post("$serverUrl/api/reports") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.Created) {
                    loadReports()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur ajout signalement: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
                _showReportForm.value = false
            }
        }
    }

    fun calculateRoute(start: Location, end: Location) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = RouteRequest(start, end, _selectedProfile.value)
                val response = httpClient.post("$serverUrl/api/route") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.OK) {
                    val routeResponse = response.body<RouteResponse>()
                    _route.value = routeResponse.coordinates
                } else {
                    // Try to read the error message from the server response
                    val errorBody = try {
                        response.body<String>()
                    } catch (_: Exception) {
                        "<unable to read error>"
                    }
                    _errorMessage.value = "Erreur calcul itinéraire (${response.status}): $errorBody"
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = "Erreur: ${e.message}"
                } else {
                    throw e
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearRoute() {
        _route.value = emptyList()
    }

    fun openReportForm(location: Location) {
        _reportLocation.value = location
        _showReportForm.value = true
    }

    fun closeReportForm() {
        _showReportForm.value = false
        _reportLocation.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    data class ReportRequest(
        val type: ReportType,
        val description: String,
        val location: Location
    )
}
