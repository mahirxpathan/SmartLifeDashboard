package com.example.smartlifedashboard.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlifedashboard.R

import com.example.smartlifedashboard.api.WeatherApi
import com.example.smartlifedashboard.api.CitySuggestion
import com.example.smartlifedashboard.api.WeatherBackendService
import com.example.smartlifedashboard.model.WeatherResponse
import com.example.smartlifedashboard.repository.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.Builder
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

data class WeatherState(
    val temperature: String = "",
    val condition: String = "",
    val icon: Int = R.drawable.ic_weather_sunny, // fallback icon
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val locationName: String = "",
    val humidity: String = "",
    val windSpeed: String = "",
    val precipitation: String = "",
    val uvIndex: String = "",
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList()
)

data class HourlyForecast(
    val time: String,
    val temperature: String,
    val icon: Int
)

data class DailyForecast(
    val date: String,
    val condition: String,
    val maxTemp: String,
    val minTemp: String,
    val icon: Int
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private val api: WeatherApi
    private val weatherRepository: WeatherRepository
    
    init {
        // Clear cache if version mismatch to avoid ID conflicts from old builds
        val prefs = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        if (prefs.getInt("cache_version", 0) < 2) {
            prefs.edit().clear().putInt("cache_version", 2).apply()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(WeatherApi::class.java)
        
        val backendRetrofit = Retrofit.Builder()
            .baseUrl("https://your-backend-service.com/") // Replace with actual backend URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val backendServiceForRepo = backendRetrofit.create(WeatherBackendService::class.java)
        // Initialize repository
        weatherRepository = WeatherRepository(backendServiceForRepo, getApplication())
        
        // Load last saved weather data if available
        loadLastWeatherData()
    }
    
    private fun loadLastWeatherData() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val lastLat = sharedPreferences.getFloat("last_latitude", 0f).toDouble()
        val lastLon = sharedPreferences.getFloat("last_longitude", 0f).toDouble()
        val lastLocationName = sharedPreferences.getString("last_location_name", "") ?: ""
        
        // Try to load cached weather data
        val cachedTemperature = sharedPreferences.getString("cached_temperature", "") ?: ""
        val cachedCondition = sharedPreferences.getString("cached_condition", "") ?: ""
        val cachedIcon = sharedPreferences.getInt("cached_icon", R.drawable.ic_weather_sunny)
        val cachedHumidity = sharedPreferences.getString("cached_humidity", "") ?: ""
        val cachedWindSpeed = sharedPreferences.getString("cached_wind_speed", "") ?: ""
        val cachedPrecipitation = sharedPreferences.getString("cached_precipitation", "") ?: ""
        val cachedUvIndex = sharedPreferences.getString("cached_uv_index", "") ?: ""
        
        // Load cached forecasts
        val cachedHourlyJson = sharedPreferences.getString("cached_hourly_forecast", "") ?: ""
        val cachedDailyJson = sharedPreferences.getString("cached_daily_forecast", "") ?: ""
        
        if (cachedTemperature.isNotEmpty() && lastLocationName.isNotEmpty()) {
            // Parse cached forecasts if they exist
            val hourlyForecast = parseHourlyForecast(cachedHourlyJson)
            val dailyForecast = parseDailyForecast(cachedDailyJson)
            
            // We have cached weather data, display it without auto-refreshing
            _weatherState.value = WeatherState(
                temperature = cachedTemperature, 
                condition = cachedCondition,
                icon = cachedIcon,
                locationName = lastLocationName,
                humidity = cachedHumidity,
                windSpeed = cachedWindSpeed,
                precipitation = cachedPrecipitation,
                uvIndex = if (cachedUvIndex.isNotEmpty()) cachedUvIndex else "3",
                hourlyForecast = hourlyForecast,
                dailyForecast = dailyForecast,
                isLoading = false
            )
        } else if (lastLat != 0.0 && lastLon != 0.0 && lastLocationName.isNotEmpty()) {
            // We have saved location data, set a placeholder
            _weatherState.value = WeatherState(
                temperature = "Offline", 
                condition = "Last saved: $lastLocationName",
                icon = R.drawable.ic_weather_sunny,
                locationName = lastLocationName,
                isLoading = false
            )
        }
    }
    
    fun fetchWeatherData(context: Context) {
        _weatherState.value = WeatherState(isLoading = true)
        
        // Check for location permissions
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle permission not granted
            _weatherState.value = WeatherState(
                temperature = "Permission Required",
                condition = "Location permission needed",
                icon = R.drawable.ic_weather_sunny,
                isLoading = false
            )
            return
        }
        
        // First try to get the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // We have a location, fetch weather
                fetchWeatherForLocation(location)
            } else {
                // No location available, request location updates
                requestLocationUpdates(context)
            }
        }.addOnFailureListener {
            // Handle location request failure
            _weatherState.value = WeatherState(
                temperature = "Location Error",
                condition = "Failed to get location",
                icon = R.drawable.ic_weather_sunny,
                isLoading = false
            )
        }
    }
    
    private fun fetchWeatherForLocation(location: Location) {
        viewModelScope.launch {
            try {
                val response = api.getCurrentWeather(location.latitude, location.longitude)
                
                // Get location name using Repository
                val reverseGeocodeResult = weatherRepository.reverseGeocode(location.latitude, location.longitude)
                val locationName = if (reverseGeocodeResult.isSuccess) {
                    val citySuggestion = reverseGeocodeResult.getOrNull()
                    if (citySuggestion != null) {
                        "${citySuggestion.name}, ${citySuggestion.state?.let { "$it, " } ?: ""}${citySuggestion.country}"
                    } else {
                        "Current Location"
                    }
                } else {
                    "Current Location"
                }
                
                // Extract humidity, precipitation, and UV index from hourly data if available
                val humidity = response.hourly?.relativehumidity_2m?.firstOrNull()?.toString() ?: "45"
                val precipitation = response.hourly?.precipitation_probability?.firstOrNull()?.toString() ?: "10"
                val uvIndex = response.hourly?.uv_index?.firstOrNull()?.let { "${it.toInt()}" } ?: "3"
                
                val newState = WeatherState(
                    temperature = "${response.currentWeather.temperature.toInt()}°C",
                    condition = mapWeatherCode(response.currentWeather.weathercode),
                    icon = mapIcon(response.currentWeather.weathercode),
                    locationName = locationName,
                    humidity = "${humidity}%",
                    windSpeed = "${response.currentWeather.windspeed.toInt()} km/h",
                    precipitation = "${precipitation}%",
                    uvIndex = uvIndex,
                    isLoading = false,
                    // Add hourly and daily forecast if available
                    hourlyForecast = response.hourly?.let { hourly ->
                        hourly.time.take(24).mapIndexed { index, time ->
                            HourlyForecast(
                                time = time.substring(11, 16), // Extract HH:MM from ISO time
                                temperature = "${hourly.temperature_2m[index].toInt()}°",
                                icon = mapIcon(hourly.weathercode[index])
                            )
                        }
                    } ?: emptyList(),
                    dailyForecast = response.daily?.let { daily ->
                        daily.time.take(7).mapIndexed { index, date ->
                            DailyForecast(
                                date = date.substring(5, 10).replace("-", "/"), // Format as MM/DD
                                condition = mapWeatherCode(daily.weathercode[index]),
                                maxTemp = "${daily.temperature_2m_max[index].toInt()}°",
                                minTemp = "${daily.temperature_2m_min[index].toInt()}°",
                                icon = mapIcon(daily.weathercode[index])
                            )
                        }
                    } ?: emptyList()
                )
                
                _weatherState.value = newState.copy(isRefreshing = false)
                
                // Save location coordinates for offline access
                saveLocationCoordinates(location.latitude, location.longitude, locationName)
            } catch (e: Exception) {
                // Check if the error is related to network/internet issues
                val errorMessage = e.message ?: "Unknown error"
                val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                   errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                   errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                   errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                   errorMessage.contains("Connection refused", ignoreCase = true) ||
                                   errorMessage.contains("Failed to connect", ignoreCase = true)
                
                val friendlyMessage = if (isNetworkError) {
                    "No internet connection"
                } else {
                    "Weather data unavailable"
                }
                
                _weatherState.value = WeatherState(
                    temperature = if (isNetworkError) "Offline" else "Error",
                    condition = friendlyMessage,
                    icon = R.drawable.ic_weather_sunny,
                    isLoading = false,
                    isRefreshing = false
                )
                // Still save the location coordinates even if weather fetch failed
                // This allows for offline access to the location
                if (!isNetworkError) {
                    saveLocationCoordinates(0.0, 0.0, "Last Attempted: Current Location")
                }
            }
        }
    }
    
    private fun requestLocationUpdates(context: Context) {
        // Try to get location updates if last known location is not available
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Got a location, fetch weather
                    fetchWeatherForLocation(location)
                } ?: run {
                    // Still no location, use default
                    viewModelScope.launch {
                        try {
                            // Default coordinates for New York City
                            val defaultLat = 40.7128
                            val defaultLon = -74.0060
                            
                            val response = api.getCurrentWeather(defaultLat, defaultLon)
                            
                            // Get location name using Repository
                            val reverseGeocodeResult = weatherRepository.reverseGeocode(defaultLat, defaultLon)
                            val locationName = if (reverseGeocodeResult.isSuccess) {
                                val citySuggestion = reverseGeocodeResult.getOrNull()
                                if (citySuggestion != null) {
                                    "${citySuggestion.name}, ${citySuggestion.state?.let { "$it, " } ?: ""}${citySuggestion.country}"
                                } else {
                                    "New York, NY"
                                }
                            } else {
                                "New York, NY"
                            }
                            
                            // Extract humidity, precipitation, and UV index from hourly data if available
                            val humidity = response.hourly?.relativehumidity_2m?.firstOrNull()?.toString() ?: "45"
                            val precipitation = response.hourly?.precipitation_probability?.firstOrNull()?.toString() ?: "10"
                            val uvIndex = response.hourly?.uv_index?.firstOrNull()?.toString() ?: "3"
                            
                            val newState = WeatherState(
                                temperature = "${response.currentWeather.temperature.toInt()}°C",
                                condition = mapWeatherCode(response.currentWeather.weathercode) + " (Default)",
                                icon = mapIcon(response.currentWeather.weathercode),
                                locationName = locationName,
                                humidity = "${humidity}%",
                                windSpeed = "${response.currentWeather.windspeed.toInt()} km/h",
                                precipitation = "${precipitation}%",
                                uvIndex = uvIndex,
                                isLoading = false,
                                // Add hourly and daily forecast if available
                                hourlyForecast = response.hourly?.let { hourly ->
                                    hourly.time.take(24).mapIndexed { index, time ->
                                        HourlyForecast(
                                            time = time.substring(11, 16), // Extract HH:MM from ISO time
                                            temperature = "${hourly.temperature_2m[index].toInt()}°",
                                            icon = mapIcon(hourly.weathercode[index])
                                        )
                                    }
                                } ?: emptyList(),
                                dailyForecast = response.daily?.let { daily ->
                                    daily.time.take(7).mapIndexed { index, date ->
                                        DailyForecast(
                                            date = date.substring(5, 10).replace("-", "/"), // Format as MM/DD
                                            condition = mapWeatherCode(daily.weathercode[index]),
                                            maxTemp = "${daily.temperature_2m_max[index].toInt()}°",
                                            minTemp = "${daily.temperature_2m_min[index].toInt()}°",
                                            icon = mapIcon(daily.weathercode[index])
                                        )
                                    }
                                } ?: emptyList()
                            )
                            
                            _weatherState.value = newState.copy(isRefreshing = false)
                        } catch (e: Exception) {
                            // Handle API error
                            // Check if the error is related to network/internet issues
                            val errorMessage = e.message ?: "Unknown error"
                            val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                               errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                               errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                               errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                               errorMessage.contains("Connection refused", ignoreCase = true) ||
                                               errorMessage.contains("Failed to connect", ignoreCase = true)
                            
                            val friendlyMessage = if (isNetworkError) {
                                "No internet connection"
                            } else {
                                "Weather data unavailable"
                            }
                            
                            _weatherState.value = WeatherState(
                                temperature = if (isNetworkError) "Offline" else "Error",
                                condition = friendlyMessage,
                                icon = R.drawable.ic_weather_sunny,
                                isLoading = false
                            )
                            // Still save the location coordinates even if weather fetch failed
                            // This allows for offline access to the location
                            if (!isNetworkError) {
                                saveLocationCoordinates(0.0, 0.0, "Last Attempted: Default Location")
                            }
                        }
                    }
                }
            }
        }
        
        // Request location updates
        val locationRequest = LocationRequest.Builder(10000).apply {
            setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            setWaitForAccurateLocation(false)
            setMaxUpdates(1) // Only one update needed
        }.build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission issue
            _weatherState.value = WeatherState(
                temperature = "Permission Error",
                condition = "Location permission denied",
                icon = R.drawable.ic_weather_sunny,
                isLoading = false
            )
        }
    }
    
    fun refreshWeatherData(context: Context) {
        _weatherState.value = _weatherState.value.copy(isRefreshing = true)
        fetchWeatherData(context)
    }
    
    fun fetchWeatherForCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                _weatherState.value = WeatherState(isLoading = true)
                val response = api.getCurrentWeather(lat, lon)
                
                // Get location name using Repository
                val reverseGeocodeResult = weatherRepository.reverseGeocode(lat, lon)
                val locationName = if (reverseGeocodeResult.isSuccess) {
                    val citySuggestion = reverseGeocodeResult.getOrNull()
                    if (citySuggestion != null) {
                        "${citySuggestion.name}, ${citySuggestion.state?.let { "$it, " } ?: ""}${citySuggestion.country}"
                    } else {
                        "Selected Location"
                    }
                } else {
                    "Selected Location"
                }
                
                // Extract humidity, precipitation, and UV index from hourly data if available
                val humidity = response.hourly?.relativehumidity_2m?.firstOrNull()?.toString() ?: "45"
                val precipitation = response.hourly?.precipitation_probability?.firstOrNull()?.toString() ?: "10"
                val uvIndex = response.hourly?.uv_index?.firstOrNull()?.let { "${it.toInt()}" } ?: "3"
                
                val newState = WeatherState(
                    temperature = "${response.currentWeather.temperature.toInt()}°C",
                    condition = mapWeatherCode(response.currentWeather.weathercode),
                    icon = mapIcon(response.currentWeather.weathercode),
                    locationName = locationName,
                    humidity = "${humidity}%",
                    windSpeed = "${response.currentWeather.windspeed.toInt()} km/h",
                    precipitation = "${precipitation}%",
                    uvIndex = uvIndex,
                    isLoading = false,
                    // Add hourly and daily forecast if available
                    hourlyForecast = response.hourly?.let { hourly ->
                        hourly.time.take(24).mapIndexed { index, time ->
                            HourlyForecast(
                                time = time.substring(11, 16), // Extract HH:MM from ISO time
                                temperature = "${hourly.temperature_2m[index].toInt()}°",
                                icon = mapIcon(hourly.weathercode[index])
                            )
                        }
                    } ?: emptyList(),
                    dailyForecast = response.daily?.let { daily ->
                        daily.time.take(7).mapIndexed { index, date ->
                            DailyForecast(
                                date = date.substring(5, 10).replace("-", "/"), // Format as MM/DD
                                condition = mapWeatherCode(daily.weathercode[index]),
                                maxTemp = "${daily.temperature_2m_max[index].toInt()}°",
                                minTemp = "${daily.temperature_2m_min[index].toInt()}°",
                                icon = mapIcon(daily.weathercode[index])
                            )
                        }
                    } ?: emptyList()
                )
                
                _weatherState.value = newState.copy(isRefreshing = false)
            } catch (e: Exception) {
                // Check if the error is related to network/internet issues
                val errorMessage = e.message ?: "Unknown error"
                val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                   errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                   errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                   errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                   errorMessage.contains("Connection refused", ignoreCase = true) ||
                                   errorMessage.contains("Failed to connect", ignoreCase = true)
                
                val friendlyMessage = if (isNetworkError) {
                    "No internet connection"
                } else {
                    "Weather data unavailable"
                }
                
                _weatherState.value = WeatherState(
                    temperature = if (isNetworkError) "Offline" else "Error",
                    condition = friendlyMessage,
                    icon = R.drawable.ic_weather_sunny,
                    isLoading = false,
                    isRefreshing = false
                )
                // Still save the location coordinates even if weather fetch failed
                // This allows for offline access to the location
                if (!isNetworkError) {
                    saveLocationCoordinates(0.0, 0.0, "Last Attempted: Coordinates")
                }
            }
        }
    }
    

    private fun mapWeatherCode(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Clear"
        2 -> "Partly Cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing Rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow Grains"
        80, 81, 82 -> "Rain Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm"
        else -> "Weather"
    }
    
    private fun mapIcon(code: Int): Int = when (code) {
        0, 1 -> R.drawable.ic_weather_sunny
        2, 3 -> R.drawable.ic_weather_cloudy
        45, 48 -> R.drawable.ic_weather_cloudy
        51, 53, 55, 61, 63, 65, 66, 67 -> R.drawable.ic_weather_rainy
        71, 73, 75, 77, 80, 81, 82, 85, 86 -> R.drawable.ic_weather_rainy
        95, 96, 99 -> R.drawable.ic_weather_rainy
        else -> R.drawable.ic_weather_sunny
    }
    
    // Function to save location coordinates and weather data
    private fun saveLocationCoordinates(lat: Double, lon: Double, locationName: String) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        
        // Convert forecasts to JSON strings for storage
        val hourlyJson = serializeHourlyForecast(_weatherState.value.hourlyForecast)
        val dailyJson = serializeDailyForecast(_weatherState.value.dailyForecast)
        
        sharedPreferences.edit()
            .putFloat("last_latitude", lat.toFloat())
            .putFloat("last_longitude", lon.toFloat())
            .putString("last_location_name", locationName)
            .putString("cached_temperature", _weatherState.value.temperature)
            .putString("cached_condition", _weatherState.value.condition)
            .putInt("cached_icon", _weatherState.value.icon)
            .putString("cached_humidity", _weatherState.value.humidity)
            .putString("cached_wind_speed", _weatherState.value.windSpeed)
            .putString("cached_precipitation", _weatherState.value.precipitation)
            .putString("cached_uv_index", _weatherState.value.uvIndex)
            .putString("cached_hourly_forecast", hourlyJson)
            .putString("cached_daily_forecast", dailyJson)
            .apply()
    }
    
    // Function to get saved location
    fun getSavedLocation(): Pair<Double, Double>? {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val lastLat = sharedPreferences.getFloat("last_latitude", 0f).toDouble()
        val lastLon = sharedPreferences.getFloat("last_longitude", 0f).toDouble()
        
        return if (lastLat != 0.0 && lastLon != 0.0) {
            Pair(lastLat, lastLon)
        } else {
            null
        }
    }
    
    // Function to get saved location name
    fun getSavedLocationName(): String {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("last_location_name", "") ?: ""
    }
    
        // Function to fetch weather for saved location (for location persistence)
    fun fetchWeatherForSavedLocation() {
        val savedLocation = getSavedLocation()
        if (savedLocation != null) {
            fetchWeatherForCoordinates(savedLocation.first, savedLocation.second)
        }
    }
    
// Helper functions for serializing forecasts
    private fun serializeHourlyForecast(forecast: List<HourlyForecast>): String {
        return try {
            val gson = com.google.gson.Gson()
            gson.toJson(forecast)
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun serializeDailyForecast(forecast: List<DailyForecast>): String {
        return try {
            val gson = com.google.gson.Gson()
            gson.toJson(forecast)
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun parseHourlyForecast(json: String): List<HourlyForecast> {
        return try {
            if (json.isEmpty()) return emptyList()
            val gson = com.google.gson.Gson()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, HourlyForecast::class.java).type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseDailyForecast(json: String): List<DailyForecast> {
        return try {
            if (json.isEmpty()) return emptyList()
            val gson = com.google.gson.Gson()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, DailyForecast::class.java).type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    

    

    

}