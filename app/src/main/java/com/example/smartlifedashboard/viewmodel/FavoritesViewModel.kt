package com.example.smartlifedashboard.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlifedashboard.util.FavoriteQuote
import com.example.smartlifedashboard.util.FirebaseUtil
import com.example.smartlifedashboard.util.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteQuotes: List<FavoriteQuote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "FavoritesViewModel"
    }
    
    init {
        Log.d(TAG, "Initializing FavoritesViewModel")
        loadFavorites()
    }
    
    fun loadFavorites() {
        Log.d(TAG, "loadFavorites called")
        viewModelScope.launch {
            Log.d(TAG, "Loading favorites in coroutine")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                Log.d(TAG, "Fetching favorite quotes from Firebase")
                val favoriteQuotes = FirebaseUtil.getFavoriteQuotes()
                Log.d(TAG, "Successfully fetched ${'$'}{favoriteQuotes.size} favorite quotes")
                // Save to local storage for offline access
                saveFavoritesToLocal(favoriteQuotes)
                _uiState.value = _uiState.value.copy(
                    favoriteQuotes = favoriteQuotes,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading favorites from Firebase", e)
                                        
                // Check if the error is related to network/internet issues
                val errorMessage = e.message ?: "Unknown error"
                val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                   errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                   errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                   errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                   errorMessage.contains("Connection refused", ignoreCase = true) ||
                                   errorMessage.contains("Failed to connect", ignoreCase = true)
                                        
                if (isNetworkError) {
                    // If network error, try loading from local storage
                    Log.d(TAG, "Network error detected, trying to load from local storage")
                    val localFavorites = loadFavoritesFromLocal()
                    Log.d(TAG, "Loaded ${'$'}{localFavorites.size} favorites from local storage")
                    // Combine any existing UI favorites with local favorites, removing duplicates
                    val combinedFavorites = (_uiState.value.favoriteQuotes + localFavorites).distinctBy { 
                        it.quote.text + it.quote.author 
                    }
                    Log.d(TAG, "Combined favorites count: ${'$'}{combinedFavorites.size}")
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = combinedFavorites,
                        isLoading = false,
                        error = "No internet connection"
                    )
                } else {
                    // Even if not a network error, try to load local favorites as backup
                    val localFavorites = loadFavoritesFromLocal()
                    Log.d(TAG, "Loaded ${'$'}{localFavorites.size} favorites from local storage as fallback")
                    // Combine any existing UI favorites with local favorites, removing duplicates
                    val combinedFavorites = (_uiState.value.favoriteQuotes + localFavorites).distinctBy { 
                        it.quote.text + it.quote.author 
                    }
                    Log.d(TAG, "Combined favorites count: ${'$'}{combinedFavorites.size}")
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = combinedFavorites,
                        isLoading = false,
                        error = "No internet connection"
                    )
                }
            }
        }
    }
    
    fun removeFavorite(favoriteQuote: FavoriteQuote) {
        Log.d(TAG, "removeFavorite called for quote: ${'$'}{favoriteQuote.quote.text}")
        viewModelScope.launch {
            try {
                // Try to delete from Firestore using document ID
                val success = FirebaseUtil.deleteFavoriteQuote(favoriteQuote.documentId)
                if (success) {
                    // Remove from UI
                    val currentFavorites = _uiState.value.favoriteQuotes.toMutableList()
                    currentFavorites.removeAll { it.quote.text == favoriteQuote.quote.text && it.quote.author == favoriteQuote.quote.author }
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = currentFavorites,
                        error = null
                    )
                    // Update local storage
                    saveFavoritesToLocal(currentFavorites)
                    Log.d(TAG, "Successfully removed favorite quote from Firestore and UI")
                } else {
                    // If Firestore fails, try to remove from local storage
                    val currentFavorites = _uiState.value.favoriteQuotes.toMutableList()
                    currentFavorites.removeAll { it.quote.text == favoriteQuote.quote.text && it.quote.author == favoriteQuote.quote.author }
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = currentFavorites,
                        error = null
                    )
                    saveFavoritesToLocal(currentFavorites)
                    Log.d(TAG, "Removed favorite quote from local storage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing favorite", e)
                                
                // Check if the error is related to network/internet issues
                val errorMessage = e.message ?: "Unknown error"
                val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                   errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                   errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                   errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                   errorMessage.contains("Connection refused", ignoreCase = true) ||
                                   errorMessage.contains("Failed to connect", ignoreCase = true)
                                
                if (isNetworkError) {
                    // Remove from UI and local storage when offline
                    val currentFavorites = _uiState.value.favoriteQuotes.toMutableList()
                    currentFavorites.removeAll { it.quote.text == favoriteQuote.quote.text && it.quote.author == favoriteQuote.quote.author }
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = currentFavorites,
                        error = "No internet connection"
                    )
                    saveFavoritesToLocal(currentFavorites)
                } else {
                    val friendlyMessage = "No internet connection"
                    _uiState.value = _uiState.value.copy(
                        error = friendlyMessage
                    )
                }
            }
        }
    }
    
    fun addFavoriteLocally(quote: Quote) {
        Log.d(TAG, "addFavoriteLocally called for quote: ${'$'}{quote.text}")
        viewModelScope.launch {
            try {
                // Load current local favorites
                val localFavorites = loadFavoritesFromLocal().toMutableList()
                
                // Check if quote is already in the local list
                val existingQuote = localFavorites.find { 
                    it.quote.text == quote.text && it.quote.author == quote.author 
                }
                
                if (existingQuote == null) {
                    val localFavorite = FavoriteQuote(quote, "local_${'$'}{System.currentTimeMillis()}", System.currentTimeMillis())
                    localFavorites.add(localFavorite)
                    saveFavoritesToLocal(localFavorites)
                    
                    // Update UI state - combine with any existing UI favorites but ensure no duplicates
                    val uiFavorites = _uiState.value.favoriteQuotes.toMutableList()
                    val updatedFavorites = (uiFavorites + localFavorites).distinctBy { 
                        it.quote.text + it.quote.author 
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = updatedFavorites,
                        error = "No internet connection"
                    )
                    
                    Log.d(TAG, "Added quote to local favorites")
                } else {
                    Log.d(TAG, "Quote already exists in local favorites")
                    // Still update UI to ensure it's visible
                    val uiFavorites = _uiState.value.favoriteQuotes.toMutableList()
                    val allFavorites = (uiFavorites + localFavorites).distinctBy { 
                        it.quote.text + it.quote.author 
                    }
                    _uiState.value = _uiState.value.copy(
                        favoriteQuotes = allFavorites,
                        error = "No internet connection"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding favorite locally", e)
            }
        }
    }
    
    fun clearError() {
        Log.d(TAG, "clearError called")
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun saveFavoritesToLocal(favorites: List<FavoriteQuote>) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("favorites_prefs", Application.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Clear existing favorites
            val existingKeys = prefs.all.keys.filter { it.startsWith("favorite_") }
            existingKeys.forEach { key ->
                editor.remove(key)
            }
            
            // Save new favorites
            favorites.forEachIndexed { index, favorite ->
                val key = "favorite_${'$'}index"
                val value = "${'$'}{favorite.quote.text}|${'$'}{favorite.quote.author}|${'$'}{favorite.timestamp}"
                editor.putString(key, value)
            }
            
            editor.putInt("favorite_count", favorites.size)
            editor.apply()
            
            Log.d(TAG, "Saved ${'$'}{favorites.size} favorites to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorites to local storage", e)
        }
    }
    
    private fun loadFavoritesFromLocal(): List<FavoriteQuote> {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("favorites_prefs", Application.MODE_PRIVATE)
            val count = prefs.getInt("favorite_count", 0)
            
            val favorites = mutableListOf<FavoriteQuote>()
            for (i in 0 until count) {
                val key = "favorite_${'$'}i"
                val value = prefs.getString(key, null)
                if (value != null) {
                    val parts = value.split("|")
                    if (parts.size >= 3) {
                        val quote = Quote(parts[0], parts[1])
                        val timestamp = parts[2].toLongOrNull() ?: System.currentTimeMillis()
                        favorites.add(FavoriteQuote(quote, "local_$key", timestamp))
                    }
                }
            }
            
            Log.d(TAG, "Loaded ${'$'}{favorites.size} favorites from local storage")
            return favorites
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorites from local storage", e)
            return emptyList()
        }
    }
}