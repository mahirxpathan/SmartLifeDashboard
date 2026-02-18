package com.example.smartlifedashboard.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class CitySuggestion(
    val name: String,
    val country: String,
    val state: String? = null,
    val latitude: Double,
    val longitude: Double,
    val placeId: String? = null
)

interface WeatherBackendService {
    @GET("api/weather/cities/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<CitySuggestion>
}