package com.example.smartlifedashboard.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

data class GeocodingResult(
    val formatted_address: String,
    val geometry: Geometry,
    val place_id: String,
    val types: List<String>
)

data class Geometry(
    val location: Location,
    val location_type: String,
    val viewport: Viewport
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class Viewport(
    val northeast: Location,
    val southwest: Location
)

data class PlacesAutocompleteResponse(
    val predictions: List<Prediction>,
    val status: String
)

data class Prediction(
    val description: String,
    val place_id: String,
    val types: List<String>,
    val matched_substrings: List<MatchedSubstring>,
    val terms: List<Term>,
    val structured_formatting: StructuredFormatting
)

data class MatchedSubstring(
    val length: Int,
    val offset: Int
)

data class Term(
    val value: String,
    val offset: Int
)

data class StructuredFormatting(
    val main_text: String,
    val secondary_text: String,
    val main_text_matched_substrings: List<MatchedSubstring>
)

interface GeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun geocodeAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Response<GeocodingResponse>

    @GET("maps/api/place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("types") types: String = "(cities)"
    ): Response<PlacesAutocompleteResponse>

    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String
    ): Response<GeocodingResponse>
}