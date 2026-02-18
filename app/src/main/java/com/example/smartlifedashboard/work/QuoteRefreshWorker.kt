package com.example.smartlifedashboard.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartlifedashboard.util.QuoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuoteRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    init {
        Log.d(TAG, "QuoteRefreshWorker initialized")
    }
    
    companion object {
        const val TAG = "QuoteRefreshWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork called with inputData: ${inputData.keyValueMap}")
        return try {
            Log.d(TAG, "Refreshing quote...")
            
            // Check if there's a pinned favorite quote
            val prefs = applicationContext.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
            val pinnedText = prefs.getString("pinned_quote_text", null)
            val pinnedAuthor = prefs.getString("pinned_quote_author", null)
            Log.d(TAG, "Pinned quote check - text: $pinnedText, author: $pinnedAuthor")
            
            // If there's a pinned quote, don't refresh
            if (pinnedText != null && pinnedAuthor != null) {
                Log.d(TAG, "Skipping quote refresh - pinned favorite detected")
                return Result.success()
            }
            
            // Fetch a new quote from the API
            Log.d(TAG, "Attempting to fetch new quote from API")
            val newQuote = QuoteRepository.getRandomQuoteFromZenAPI()
            Log.d(TAG, "API call result: ${newQuote?.text ?: "null"}")
            
            if (newQuote != null) {
                // Save the new quote to cache
                Log.d(TAG, "Saving new quote to cache")
                QuoteRepository.saveLastQuote(applicationContext, newQuote)
                Log.d(TAG, "New quote saved to cache successfully")
                Log.d(TAG, "Quote refreshed successfully: ${newQuote.text}")
                Result.success()
            } else {
                // If API fails, try to use cached quote
                Log.w(TAG, "Failed to refresh quote from API, trying cached quote")
                val cachedQuote = QuoteRepository.getLastQuote(applicationContext)
                if (cachedQuote != null) {
                    Log.d(TAG, "Using cached quote for auto-refresh: ${cachedQuote.text}")
                    // Still save the cached quote to refresh the timestamp
                    QuoteRepository.saveLastQuote(applicationContext, cachedQuote)
                    Result.success()
                } else {
                    // If both API and cache fail, try local quote
                    Log.w(TAG, "Failed to refresh quote from cache, trying local quote")
                    val localQuote = QuoteRepository.getRandomQuoteForToday()
                    if (localQuote != null) {
                        Log.d(TAG, "Using local quote for auto-refresh: ${localQuote.text}")
                        QuoteRepository.saveLastQuote(applicationContext, localQuote)
                        Result.success()
                    } else {
                        Log.w(TAG, "All quote sources failed, retrying")
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing quote", e)
            Result.retry()
        }
    }
}