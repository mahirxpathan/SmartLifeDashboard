package com.example.smartlifedashboard.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather,
    @SerializedName("hourly") val hourly: HourlyData? = null,
    @SerializedName("daily") val daily: DailyData? = null,
    @SerializedName("timezone") val timezone: String? = null
)

data class CurrentWeather(
    val temperature: Double,
    val weathercode: Int,
    val windspeed: Double,
    val winddirection: Double? = null,
    val is_day: Int? = null,
    val time: String? = null
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val relativehumidity_2m: List<Int>,
    val apparent_temperature: List<Double>,
    val precipitation_probability: List<Int>,
    val weathercode: List<Int>,
    val windspeed_10m: List<Double>,
    val winddirection_10m: List<Int>,
    val uv_index: List<Double>? = null
)

data class DailyData(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_sum: List<Double>
)