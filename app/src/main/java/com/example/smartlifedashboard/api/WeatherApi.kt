package com.example.smartlifedashboard.api

import com.example.smartlifedashboard.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true,
        @Query("hourly") hourly: String = "temperature_2m,relativehumidity_2m,apparent_temperature,precipitation_probability,weathercode,windspeed_10m,winddirection_10m,uv_index",
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
    
    @GET("v1/forecast")
    suspend fun getWeatherByCity(
        @Query("name") city: String,
        @Query("current_weather") current: Boolean = true,
        @Query("hourly") hourly: String = "temperature_2m,relativehumidity_2m,apparent_temperature,precipitation_probability,weathercode,windspeed_10m,winddirection_10m,uv_index",
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}