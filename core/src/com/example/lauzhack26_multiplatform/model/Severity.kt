package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class Severity(
    val label: String
) {
    @SerialName("low")
    LOW("low"),
    @SerialName("medium")
    MEDIUM("medium"),
    @SerialName("high")
    HIGH("high")
}
