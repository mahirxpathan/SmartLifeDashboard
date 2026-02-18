package com.example.smartlifedashboard.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class QuoteRepository {
    companion object {
        private const val PREFS_NAME = "quote_prefs"
        private const val LAST_QUOTE_TEXT = "last_quote_text"
        private const val LAST_QUOTE_AUTHOR = "last_quote_author"
        
        private val quotes = listOf(
            Quote("The only way to do great work is to love what you do.", "Steve Jobs"),
            Quote("Innovation distinguishes between a leader and a follower.", "Steve Jobs"),
            Quote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs"),
            Quote("Stay hungry, stay foolish.", "Steve Jobs"),
            Quote("The future belongs to those who believe in the beauty of their dreams.", "Eleanor Roosevelt"),
            Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
            Quote("Everything you've ever wanted is on the other side of fear.", "George Addair"),
            Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill"),
            Quote("The only impossible journey is the one you never begin.", "Tony Robbins"),
            Quote("The purpose of our lives is to be happy.", "Dalai Lama")
        )

        fun getRandomQuoteForToday(): Quote {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val year = calendar.get(Calendar.YEAR)
            val seed = dayOfYear + year * 1000
            val randomIndex = Math.abs(seed) % quotes.size
            val quote = quotes[randomIndex]
            Log.d("QuoteRepository", "Returning local quote: ${quote.text} - ${quote.author} (index: $randomIndex, seed: $seed)")
            return quote
        }
        
        // Removed Quotable API - using only ZenQuotes API as per project requirements
        // For backward compatibility, we'll redirect this function to use ZenQuotes API
        suspend fun getRandomQuoteFromAPI(): Quote? {
            Log.d("QuoteRepository", "getRandomQuoteFromAPI called - redirecting to ZenQuotes API")
            return getRandomQuoteFromZenAPI()
        }
        
        // Removed tryHTTPSConnection function as it was only used by the Quotable API
        
        suspend fun getRandomQuoteFromZenAPI(): Quote? {
            return withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://zenquotes.io/api/random")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.connect()

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = org.json.JSONArray(response)
                        val jsonObject = jsonArray.getJSONObject(0)
                        val content = jsonObject.getString("q")
                        val author = jsonObject.getString("a")
                        Quote(content, author)
                    } else null
                } catch (e: Exception) {
                    Log.e("QuoteRepository", "Error fetching ZenQuote: ${e.message}", e)
                    null
                }
            }
        }
        
        fun saveLastQuote(context: Context, quote: Quote) {
            Log.d("QuoteRepository", "Saving quote to cache: ${quote.text} - ${quote.author}")
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestamp = System.currentTimeMillis()
            Log.d("QuoteRepository", "Saving quote with timestamp: $timestamp")
            with(prefs.edit()) {
                putString(LAST_QUOTE_TEXT, quote.text)
                putString(LAST_QUOTE_AUTHOR, quote.author)
                putLong("last_quote_timestamp", timestamp)
                apply()
            }
            Log.d("QuoteRepository", "Quote saved to cache successfully")
        }
        
        fun isNetworkAvailable(): Boolean {
            Log.d("QuoteRepository", "isNetworkAvailable called")
            // Try HTTPS first
            var result = tryHTTPSNetworkCheck()
            if (result) {
                Log.d("QuoteRepository", "Network is available")
                return true
            }
            
            // If HTTPS fails, return false
            Log.d("QuoteRepository", "HTTPS network check failed")
            return false
        }
        
        private fun tryHTTPSNetworkCheck(): Boolean {
            Log.d("QuoteRepository", "tryHTTPSNetworkCheck called")
            var connection: HttpURLConnection? = null
            try {
                Log.d("QuoteRepository", "Checking network availability")
                val url = URL("https://www.google.com")
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "SmartLifeDashboard/1.0")
                // Add cache-busting headers
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                connection.setRequestProperty("Pragma", "no-cache")
                connection.setRequestProperty("Expires", "0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                Log.d("QuoteRepository", "Attempting to connect to Google for network check")
                connection.connect()
                Log.d("QuoteRepository", "Connected successfully to Google for network check")
                val responseCode = connection.responseCode
                Log.d("QuoteRepository", "Google response code: $responseCode")
                Log.d("QuoteRepository", "Google response message: ${connection.responseMessage}")
                if (responseCode == 200) {
                    connection.getInputStream().close()
                    Log.d("QuoteRepository", "Basic network check passed")
                    return true
                } else {
                    Log.e("QuoteRepository", "Google returned non-success response code: $responseCode")
                    return false
                }
            } catch (e: javax.net.ssl.SSLHandshakeException) {
                Log.e("QuoteRepository", "SSL error during network check: ${e.message}", e)
                return false
            } catch (e: java.net.UnknownHostException) {
                Log.e("QuoteRepository", "Unknown host during network check: ${e.message}", e)
                return false
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("QuoteRepository", "Timeout during network check: ${e.message}", e)
                return false
            } catch (e: java.io.IOException) {
                Log.e("QuoteRepository", "IO error during network check: ${e.message}", e)
                return false
            } catch (e: Exception) {
                Log.e("QuoteRepository", "Network check failed: ${e.message}", e)
                return false
            } finally {
                Log.d("QuoteRepository", "Disconnecting network check HTTP connection")
                connection?.disconnect()
            }
        }
        
        fun getLastQuote(context: Context): Quote? {
            Log.d("QuoteRepository", "getLastQuote called")
            return try {
                val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val text = prefs.getString(LAST_QUOTE_TEXT, null)
                val author = prefs.getString(LAST_QUOTE_AUTHOR, null)
                val timestamp = prefs.getLong("last_quote_timestamp", 0)
                Log.d("QuoteRepository", "Cached quote data - text: $text, author: $author, timestamp: $timestamp")
                
                if (text != null && author != null) {
                    Log.d("QuoteRepository", "Returning cached quote: $text")
                    Quote(text, author)
                } else {
                    Log.d("QuoteRepository", "No cached quote found")
                    null
                }
            } catch (e: Exception) {
                Log.e("QuoteRepository", "Error retrieving cached quote", e)
                null
            }
        }
    }
}