package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Represents a single result from the Nominatim geocoding search API.
 */
@Serializable
data class NominatimSearchResult(
    @SerialName("place_id")
    val placeId: Long,
    val licence: String? = null,
    @SerialName("osm_type")
    val osmType: String? = null,
    @SerialName("osm_id")
    val osmId: Long? = null,
    @SerialName("boundingbox")
    val boundingBox: List<Double>? = null,
    val lat: String,
    val lon: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("class")
    val osmClass: String? = null,
    val type: String? = null,
    val importance: Double? = null
) {
    /**
     * Returns the latitude as a Double.
     */
    fun getLatitude(): Double = lat.toDouble()

    /**
     * Returns the longitude as a Double.
     */
    fun getLongitude(): Double = lon.toDouble()

    /**
     * Converts this Nominatim result to a [Location] instance.
     */
    fun toLocation(): Location = Location(lat = getLatitude(), lon = getLongitude())
}
