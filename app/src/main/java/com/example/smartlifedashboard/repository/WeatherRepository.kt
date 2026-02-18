package com.example.smartlifedashboard.repository

import android.content.Context
import android.location.Geocoder
import com.example.smartlifedashboard.api.CitySuggestion
import com.example.smartlifedashboard.api.WeatherBackendService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class WeatherRepository(
    private val backendService: WeatherBackendService,
    private val context: Context
) {
    companion object {
        private const val BASE_URL = "https://your-weather-backend.com/api/"
        
        fun create(context: Context): WeatherRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val backendService = retrofit.create(WeatherBackendService::class.java)
            return WeatherRepository(backendService, context)
        }
    }
    
    suspend fun reverseGeocode(lat: Double, lng: Double): Result<com.example.smartlifedashboard.api.CitySuggestion> {
        // Try backend service first
        try {
            val response = backendService.reverseGeocode(lat, lng)
            if (response.isSuccessful && response.body() != null) {
                return Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            // If backend fails, fall back to local Geocoder
        }
        
        // Fallback to Android Geocoder
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val cityName = address.locality ?: address.adminArea ?: address.countryName
                val countryName = address.countryName ?: "Unknown"
                val stateName = address.adminArea
                
                val citySuggestion = CitySuggestion(
                    name = cityName,
                    country = countryName,
                    state = stateName,
                    latitude = lat,
                    longitude = lng,
                    placeId = null
                )
                Result.success(citySuggestion)
            } else {
                // If Geocoder fails, return a basic location
                Result.success(
                    CitySuggestion(
                        name = "Location",
                        country = "Unknown",
                        state = null,
                        latitude = lat,
                        longitude = lng,
                        placeId = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}