package com.example.smartlifedashboard.util

import android.content.Context
import android.content.SharedPreferences

object BackendConfig {
    // Constants for backend configuration
    const val BACKEND_BASE_URL = "https://your-weather-backend.com/api/"
    const val API_KEY_PREFS = "backend_api_config"
    
    // Function to securely store API configuration (in a real app, this would be encrypted)
    fun storeApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(API_KEY_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("api_key", apiKey).apply()
    }
    
    // Function to retrieve API key
    fun getApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(API_KEY_PREFS, Context.MODE_PRIVATE)
        return prefs.getString("api_key", null)
    }
    
    // Function to check if API key is configured
    fun isApiKeyConfigured(context: Context): Boolean {
        return !getApiKey(context).isNullOrEmpty()
    }
    
    // Function to clear API key
    fun clearApiKey(context: Context) {
        val prefs = context.getSharedPreferences(API_KEY_PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove("api_key").apply()
    }
}