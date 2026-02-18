package com.example.smartlifedashboard.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

data class FavoriteQuote(
    val quote: Quote,
    val documentId: String,
    val timestamp: Long = System.currentTimeMillis()
)

object FirebaseUtil {
    private const val TAG = "FirebaseUtil"
    
    /**
     * Extracts the web client ID from google-services.json file
     * This is needed for Google Sign-In configuration
     */
    fun getWebClientId(context: Context): String? {
        return try {
            // Try to get from assets first
            val inputStream = context.assets.open("google-services.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            reader.close()
            inputStream.close()
            
            val json = stringBuilder.toString()
            val jsonObject = JSONObject(json)
            
            // Navigate to the OAuth client information
            val clientArray = jsonObject.getJSONArray("client")
            if (clientArray.length() > 0) {
                val clientObject = clientArray.getJSONObject(0)
                val oauthClients = clientObject.getJSONArray("oauth_client")
                
                // Find the web client
                for (i in 0 until oauthClients.length()) {
                    val oauthClient = oauthClients.getJSONObject(i)
                    val clientType = oauthClient.getInt("client_type")
                    
                    // Type 3 is for web clients
                    if (clientType == 3) {
                        return oauthClient.getString("client_id")
                    }
                }
            }
            
            Log.w(TAG, "Web client ID not found in google-services.json")
            null
        } catch (e: IOException) {
            Log.e(TAG, "Error reading google-services.json", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing google-services.json", e)
            null
        }
    }
    
    /**
     * Gets a fallback web client ID by constructing it from the project information
     * This is a workaround when the OAuth client is not present in google-services.json
     */
    fun getFallbackWebClientId(context: Context): String? {
        return try {
            // Try to get from assets first
            val inputStream = context.assets.open("google-services.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            reader.close()
            inputStream.close()
            
            val json = stringBuilder.toString()
            val jsonObject = JSONObject(json)
            
            // Get project info
            val projectInfo = jsonObject.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            val mobilesdkAppId = jsonObject.getJSONArray("client")
                .getJSONObject(0)
                .getJSONObject("client_info")
                .getString("mobilesdk_app_id")
            
            // Construct the web client ID
            // Format: mobilesdk_app_id.apps.googleusercontent.com
            return "$mobilesdkAppId.apps.googleusercontent.com"
        } catch (e: Exception) {
            Log.e(TAG, "Error constructing fallback web client ID", e)
            null
        }
    }
    
    /**
     * Saves a favorite quote to Firestore under the user's profile
     */
    suspend fun saveFavoriteQuote(quote: Quote): Boolean {
        Log.d(TAG, "saveFavoriteQuote called with quote: ${quote.text} - ${quote.author}")
        val currentUser = FirebaseAuth.getInstance().currentUser
        return try {
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found")
                return false
            }
            
            Log.d(TAG, "Saving favorite quote: ${quote.text} - ${quote.author}")
            val db = FirebaseFirestore.getInstance()
            val favoriteData = mapOf(
                "text" to quote.text,
                "author" to quote.author,
                "timestamp" to System.currentTimeMillis()
            )
            
            Log.d(TAG, "Attempting to add document to Firestore")
            val documentReference = db.collection("users")
                .document(currentUser.uid)
                .collection("favorites")
                .add(favoriteData)
                .await()
                
            Log.d(TAG, "Favorite quote saved successfully with document ID: ${documentReference.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite quote: ${e.message}", e)
            return false
        } finally {
            Log.d(TAG, "Finished saveFavoriteQuote")
        }
    }
    
    /**
     * Retrieves all favorite quotes for the current user
     */
    suspend fun getFavoriteQuotes(): List<FavoriteQuote> {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found")
                return emptyList()
            }
            
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("users")
                .document(currentUser.uid)
                .collection("favorites")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val favoriteQuotes = mutableListOf<FavoriteQuote>()
            for (document in snapshot.documents) {
                val text = document.getString("text") ?: continue
                val author = document.getString("author") ?: continue
                val timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                val quote = Quote(text, author)
                favoriteQuotes.add(FavoriteQuote(quote, document.id, timestamp))
            }
            
            Log.d(TAG, "Retrieved ${favoriteQuotes.size} favorite quotes")
            favoriteQuotes
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving favorite quotes", e)
            emptyList()
        }
    }
    
    /**
     * Deletes a favorite quote by its document ID
     */
    suspend fun deleteFavoriteQuote(documentId: String): Boolean {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found")
                return false
            }
            
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(currentUser.uid)
                .collection("favorites")
                .document(documentId)
                .delete()
                .await()
                
            Log.d(TAG, "Favorite quote deleted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting favorite quote", e)
            false
        }
    }
    
    /**
     * Checks if a quote is already favorited by the current user
     */
    suspend fun isQuoteFavorited(quote: Quote): Boolean {
        Log.d(TAG, "isQuoteFavorited called with quote: ${quote.text} - ${quote.author}")
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found")
                return false
            }
            
            Log.d(TAG, "Checking if quote is favorited: ${quote.text} - ${quote.author}")
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("users")
                .document(currentUser.uid)
                .collection("favorites")
                .whereEqualTo("text", quote.text)
                .whereEqualTo("author", quote.author)
                .get()
                .await()
                
            val isFavorited = !snapshot.isEmpty
            Log.d(TAG, "Quote favorited status: $isFavorited, found ${snapshot.size()} matching documents")
            // Log the documents found
            for (document in snapshot.documents) {
                Log.d(TAG, "Found matching document: ${document.id} with data: ${document.data}")
            }
            // Check if any documents match the criteria
            isFavorited
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if quote is favorited", e)
            false
        } finally {
            Log.d(TAG, "Finished isQuoteFavorited")
        }
    }
}