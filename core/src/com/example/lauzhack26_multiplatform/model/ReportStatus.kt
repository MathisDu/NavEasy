package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class ReportStatus(
    val label: String
) {
    @SerialName("active")
    ACTIVE("active"),
    @SerialName("resolved")
    RESOLVED("resolved")
}
