package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
enum class MobilityProfile(
    val orsProfile: String,
    val incompatibleReportTypes: Set<ReportType>
) {
    WHEELCHAIR(
        orsProfile = "wheelchair-push",
        incompatibleReportTypes = setOf(
            ReportType.STAIRS,
            ReportType.STEPS,
            ReportType.CURB,
            ReportType.CONSTRUCTION,
            ReportType.NARROW_PASSAGE
        )
    ),
    CRUTCHES(
        orsProfile = "foot-walking",
        incompatibleReportTypes = setOf(
            ReportType.STAIRS,
            ReportType.CONSTRUCTION,
            ReportType.FLOORS
        )
    ),
    VISUAL_IMPAIRMENT(
        orsProfile = "foot-walking",
        incompatibleReportTypes = setOf(
            ReportType.UNPAVED_SURFACE,
            ReportType.POOR_LIGHTING
        )
    ),
    STANDARD(
        orsProfile = "foot-walking",
        incompatibleReportTypes = emptySet()
    )
}
