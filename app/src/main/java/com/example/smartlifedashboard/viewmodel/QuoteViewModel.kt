package com.example.smartlifedashboard.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.smartlifedashboard.util.FirebaseUtil
import com.example.smartlifedashboard.util.Quote
import com.example.smartlifedashboard.util.QuoteRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class QuoteUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class QuoteViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(QuoteUiState())
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()
    
    private val prefs: SharedPreferences = application.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    
    init {
        Log.d("QuoteViewModel", "Initializing QuoteViewModel")
        Log.d("QuoteViewModel", "Calling loadQuote from init")
        loadQuote()
        Log.d("QuoteViewModel", "Finished calling loadQuote from init")
    }
    
    private fun checkIfFavorite(quote: Quote) {
        Log.d("QuoteViewModel", "checkIfFavorite called with quote: ${quote.text}")
        viewModelScope.launch {
            Log.d("QuoteViewModel", "Checking if quote is favorited: ${quote.text}")
            val isFavorited = FirebaseUtil.isQuoteFavorited(quote)
            Log.d("QuoteViewModel", "Quote favorited status: $isFavorited")
            _uiState.value = _uiState.value.copy(isFavorite = isFavorited)
        }
        Log.d("QuoteViewModel", "Finished checkIfFavorite")
    }
    
    fun loadQuote(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            Log.d("QuoteViewModel", "loadQuote called with forceRefresh: $forceRefresh")
            val currentQuote = _uiState.value.quote
            Log.d("QuoteViewModel", "Current quote: ${currentQuote?.text}")
            
            // Show loading state if this is a forced refresh (from refresh button)
            if (forceRefresh) {
                Log.d("QuoteViewModel", "Setting isRefreshing to true")
                _uiState.value = _uiState.value.copy(
                    isRefreshing = true,
                    error = null
                )
            } else {
                Log.d("QuoteViewModel", "Setting isLoading to true")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // Only check for pinned favorite quote if not forcing refresh
                Log.d("QuoteViewModel", "Checking for pinned quote")
                val pinnedQuote = getPinnedFavoriteQuote()
                if (pinnedQuote != null) {
                    Log.d("QuoteViewModel", "Using pinned quote: ${pinnedQuote.text}")
                    _uiState.value = _uiState.value.copy(
                        quote = pinnedQuote,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    checkIfFavorite(pinnedQuote)
                    return@launch
                } else {
                    Log.d("QuoteViewModel", "No pinned quote found")
                }
                
                // Check for cached quote
                Log.d("QuoteViewModel", "Auto refresh: Checking for cached quote")
                val cachedQuote = QuoteRepository.getLastQuote(getApplication())
                if (cachedQuote != null) {
                    Log.d("QuoteViewModel", "Using cached quote: ${cachedQuote.text}")
                    _uiState.value = _uiState.value.copy(
                        quote = cachedQuote,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    checkIfFavorite(cachedQuote)
                    return@launch
                } else {
                    Log.d("QuoteViewModel", "No cached quote found")
                }
            }
            
            // For manual refresh, skip all checks and go straight to API
            if (forceRefresh) {
                Log.d("QuoteViewModel", "Manual refresh: Skipping all checks, going straight to API")
                val apiQuote = QuoteRepository.getRandomQuoteFromAPI()
                if (apiQuote != null) {
                    Log.d("QuoteViewModel", "Successfully fetched API quote: ${apiQuote.text}")
                    QuoteRepository.saveLastQuote(getApplication(), apiQuote)
                    _uiState.value = _uiState.value.copy(
                        quote = apiQuote,
                        isRefreshing = false,
                        error = null
                    )
                    checkIfFavorite(apiQuote)
                } else {
                    Log.d("QuoteViewModel", "API call failed, using fallback")
                    val fallback = QuoteRepository.getRandomQuoteForToday()
                                    
                    // Check for network errors and show user-friendly message
                    val errorMessage = "No internet connection"
                                    
                    _uiState.value = _uiState.value.copy(
                        quote = fallback,
                        isRefreshing = false,
                        error = errorMessage
                    )
                    checkIfFavorite(fallback)
                }
                return@launch
            }
            
            // Try to fetch from API (for auto refresh)
            Log.d("QuoteViewModel", "Attempting to fetch from API")
            val apiQuote = QuoteRepository.getRandomQuoteFromAPI()
            Log.d("QuoteViewModel", "API quote result: ${apiQuote?.text}")
            if (apiQuote != null) {
                Log.d("QuoteViewModel", "Successfully fetched API quote: ${apiQuote.text}")
                // Save to cache
                QuoteRepository.saveLastQuote(getApplication(), apiQuote)
                _uiState.value = _uiState.value.copy(
                    quote = apiQuote,
                    isLoading = false,
                    isRefreshing = false,
                    error = null
                )
                checkIfFavorite(apiQuote)
            } else {
                Log.d("QuoteViewModel", "API call failed, using fallback")
                // For auto refresh, try cached quote first, then fallback to local
                val cachedQuote = QuoteRepository.getLastQuote(getApplication())
                if (cachedQuote != null) {
                    Log.d("QuoteViewModel", "Using cached quote as fallback: ${'$'}{cachedQuote.text}")
                    _uiState.value = _uiState.value.copy(
                        quote = cachedQuote,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    checkIfFavorite(cachedQuote)
                } else {
                    // Fallback to local quote if API and cache both fail
                    val localQuote = QuoteRepository.getRandomQuoteForToday()
                    Log.d("QuoteViewModel", "Using local quote: ${'$'}{localQuote.text}")
                                    
                    // Show user-friendly message for offline situation
                    val errorMessage = "No internet connection"
                                    
                    _uiState.value = _uiState.value.copy(
                        quote = localQuote,
                        isLoading = false,
                        isRefreshing = false,
                        error = errorMessage
                    )
                    checkIfFavorite(localQuote)
                }
            }
        }
    }
    
    fun refreshQuote() {
        Log.d("QuoteViewModel", "refreshQuote called - forcing refresh")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            val apiQuote = QuoteRepository.getRandomQuoteFromZenAPI()
            if (apiQuote != null) {
                QuoteRepository.saveLastQuote(getApplication(), apiQuote)
                _uiState.value = _uiState.value.copy(
                    quote = apiQuote,
                    isRefreshing = false,
                    error = null
                )
                checkIfFavorite(apiQuote)
            } else {
                val fallback = QuoteRepository.getRandomQuoteForToday()
                
                // Check for network errors and show user-friendly message
                val errorMessage = "No internet connection"
                
                _uiState.value = _uiState.value.copy(
                    quote = fallback,
                    isRefreshing = false,
                    error = errorMessage
                )
                checkIfFavorite(fallback)
            }
        }
    }
    
    fun toggleFavorite() {
        Log.d("QuoteViewModel", "toggleFavorite called")
        viewModelScope.launch {
            val currentQuote = _uiState.value.quote ?: return@launch
            val isCurrentlyFavorite = _uiState.value.isFavorite
            Log.d("QuoteViewModel", "Toggling favorite for quote: ${'$'}{currentQuote.text}, current status: $isCurrentlyFavorite")
                
            if (isCurrentlyFavorite) {
                // Remove from favorites
                Log.d("QuoteViewModel", "Unfavoriting quote")
                try {
                    val favoriteQuotes = FirebaseUtil.getFavoriteQuotes()
                    val quoteToDelete = favoriteQuotes.find { 
                        it.quote.text == currentQuote.text && it.quote.author == currentQuote.author 
                    }
                        
                    if (quoteToDelete != null) {
                        val success = FirebaseUtil.deleteFavoriteQuote(quoteToDelete.documentId)
                        if (success) {
                            Log.d("QuoteViewModel", "Successfully unfavorited quote")
                            _uiState.value = _uiState.value.copy(isFavorite = false)
                        } else {
                            Log.d("QuoteViewModel", "Failed to un favorite quote")
                            _uiState.value = _uiState.value.copy(isFavorite = false) // Still update UI
                        }
                    } else {
                        Log.d("QuoteViewModel", "Quote not found in favorites, updating UI only")
                        _uiState.value = _uiState.value.copy(isFavorite = false)
                    }
                } catch (e: Exception) {
                    Log.e("QuoteViewModel", "Exception while unfavoriting quote", e)
                                            
                    // Check if the error is related to network/internet issues
                    val errorMessage = e.message ?: "Unknown error"
                    val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                       errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                       errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                       errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                       errorMessage.contains("Connection refused", ignoreCase = true) ||
                                       errorMessage.contains("Failed to connect", ignoreCase = true)
                                            
                    if (isNetworkError) {
                        // Update UI state anyway when offline
                        _uiState.value = _uiState.value.copy(isFavorite = false)
                        _uiState.value = _uiState.value.copy(error = "No internet connection")
                    } else {
                        _uiState.value = _uiState.value.copy(error = "No internet connection")
                    }
                }
            } else {
                // Save to favorites
                Log.d("QuoteViewModel", "Favoriting quote")
                try {
                    val success = FirebaseUtil.saveFavoriteQuote(currentQuote)
                    if (success) {
                        Log.d("QuoteViewModel", "Successfully favorited quote")
                        _uiState.value = _uiState.value.copy(isFavorite = true)
                    } else {
                        // Update UI anyway
                        Log.d("QuoteViewModel", "Marking as favorited in UI")
                        _uiState.value = _uiState.value.copy(isFavorite = true)
                    }
                } catch (e: Exception) {
                    Log.e("QuoteViewModel", "Exception while favoriting quote", e)
                                                    
                    // Check if the error is related to network/internet issues
                    val errorMessage = e.message ?: "Unknown error"
                    val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                                       errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                       errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                       errorMessage.contains("Network is unreachable", ignoreCase = true) ||
                                       errorMessage.contains("Connection refused", ignoreCase = true) ||
                                       errorMessage.contains("Failed to connect", ignoreCase = true)
                                                    
                    if (isNetworkError) {
                        // Update UI state anyway when offline
                        _uiState.value = _uiState.value.copy(isFavorite = true)
                        _uiState.value = _uiState.value.copy(error = "No internet connection")
                    } else {
                        _uiState.value = _uiState.value.copy(error = "No internet connection")
                    }
                }
                // Update favorites list in case we have access to FavoritesViewModel
                // Since we don't have direct access, we'll just update our local state
                // The favorites screen will sync when opened
            }
        }
        Log.d("QuoteViewModel", "Finished toggleFavorite")
    }
        
    // Helper function to handle offline favorite operations
    fun toggleFavoriteOffline() {
        Log.d("QuoteViewModel", "toggleFavoriteOffline called")
        viewModelScope.launch {
            val currentQuote = _uiState.value.quote ?: return@launch
            val isCurrentlyFavorite = _uiState.value.isFavorite
                
            if (!isCurrentlyFavorite) {
                // Add to offline favorites
                _uiState.value = _uiState.value.copy(isFavorite = true, error = "No internet connection")
            } else {
                // Remove from offline favorites
                _uiState.value = _uiState.value.copy(isFavorite = false, error = "No internet connection")
            }
        }
    }
    
    fun pinFavoriteQuote() {
        Log.d("QuoteViewModel", "pinFavoriteQuote called")
        viewModelScope.launch {
            val currentQuote = _uiState.value.quote ?: return@launch
            Log.d("QuoteViewModel", "Pinning favorite quote: ${currentQuote.text}")
            prefs.edit()
                .putString("pinned_quote_text", currentQuote.text)
                .putString("pinned_quote_author", currentQuote.author)
                .apply()
            Log.d("QuoteViewModel", "Quote pinned successfully")
        }
        Log.d("QuoteViewModel", "Finished pinFavoriteQuote")
    }
    
    fun unpinFavoriteQuote() {
        Log.d("QuoteViewModel", "unpinFavoriteQuote called")
        Log.d("QuoteViewModel", "Unpinning favorite quote")
        prefs.edit()
            .remove("pinned_quote_text")
            .remove("pinned_quote_author")
            .apply()
        Log.d("QuoteViewModel", "Quote unpinned successfully")
        Log.d("QuoteViewModel", "Finished unpinFavoriteQuote")
    }
    
    private suspend fun getPinnedFavoriteQuote(): Quote? {
        val text = prefs.getString("pinned_quote_text", null)
        val author = prefs.getString("pinned_quote_author", null)
        Log.d("QuoteViewModel", "Pinned quote check - text: $text, author: $author")
        
        return if (text != null && author != null) {
            val quote = Quote(text, author)
            Log.d("QuoteViewModel", "Returning pinned quote: ${quote.text}")
            quote
        } else {
            Log.d("QuoteViewModel", "No pinned quote found")
            null
        }
    }
    
    private fun getCachedQuoteWithoutFreshnessCheck(): Quote? {
        Log.d("QuoteViewModel", "getCachedQuoteWithoutFreshnessCheck called")
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
            val text = prefs.getString("last_quote_text", null)
            val author = prefs.getString("last_quote_author", null)
            Log.d("QuoteViewModel", "Cached quote data without freshness check - text: $text, author: $author")
            
            if (text != null && author != null) {
                val quote = Quote(text, author)
                Log.d("QuoteViewModel", "Returning cached quote without freshness check: ${quote.text}")
                quote
            } else {
                Log.d("QuoteViewModel", "No cached quote found without freshness check")
                null
            }
        } catch (e: Exception) {
            Log.e("QuoteViewModel", "Error retrieving cached quote without freshness check", e)
            null
        }
    }
    
    private fun isNetworkError(): Boolean {
        // This function is used when we want to determine if an error is network-related
        // It's called when we catch exceptions, so the fact that we're in this function
        // suggests there was an exception, which may or may not be network-related
        // The actual determination happens by checking the error message content
        return false
    }
    
    fun clearError() {
        Log.d("QuoteViewModel", "Clearing error message")
        _uiState.value = _uiState.value.copy(error = null)
    }
}