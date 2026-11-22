package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

// Threshold: when resolved_votes reaches this number, the report is considered resolved and removed
const val VOTE_THRESHOLD = 3

@Serializable
data class Report(
    val id: String,
    val userId: String?,
    val title: String,
    val description: String,
    val category: String,
    val location: Location,
    val disabilityTypes: List<String>,
    val severity: Severity,
    val status: ReportStatus,
    val resolvedVotes: Int = 0,
    val createdAt: String
)
