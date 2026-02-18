package com.example.smartlifedashboard.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import android.util.Log

import kotlinx.coroutines.launch

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val category: String = ""
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    TITLE_A_TO_Z,
    TITLE_Z_TO_A
}

class QuickNotesViewModel : ViewModel() {
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> = _notes
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    // Expose the value directly for easier access
    val searchQueryValue: String get() = _searchQuery.value
    
    private val _filteredNotes = mutableStateListOf<Note>()
    val filteredNotes: List<Note> = _filteredNotes
    
    private var _sortOrder = SortOrder.NEWEST_FIRST
    
    var errorMessage: String? = null
        private set
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var snapshotListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Enable Firestore persistence
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            // Ignore if settings already set
        }
    }
    
    private fun filterNotes(query: String) {
        val unfilteredNotes = if (query.isEmpty()) {
            _notes.toList()
        } else {
            _notes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
            }
        }
        
        // Apply sorting
        val sortedNotes = when (_sortOrder) {
            SortOrder.NEWEST_FIRST -> unfilteredNotes.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> unfilteredNotes.sortedBy { it.timestamp }
            SortOrder.TITLE_A_TO_Z -> unfilteredNotes.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_Z_TO_A -> unfilteredNotes.sortedByDescending { it.title.lowercase() }
        }
        
        _filteredNotes.clear()
        _filteredNotes.addAll(sortedNotes)
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterNotes(query)
    }
    
    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder = sortOrder
        // Re-filter with the new sort order
        filterNotes(searchQueryValue)
    }
    
    fun getSortOrder(): SortOrder {
        return _sortOrder
    }

    /**
     * Load notes from Firestore
     * This method should be called every time the screen is opened
     * to ensure we get the latest data from the cloud
     */
    fun loadNotesFromFirestore() {
        val user = auth.currentUser
        if (user == null) {
            // Clear notes if no user
            _notes.clear()
            _filteredNotes.clear()
            return
        }
        
        // Set loading state
        _isLoading.value = true
        
        // Set up snapshot listener for real-time updates (do this first to ensure we get updates)
        setupSnapshotListener(user.uid)
        
        // Load notes from Firestore
        firestore.collection("users")
            .document(user.uid)
            .collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                _notes.clear()
                for (document in snapshot) {
                    val text = document.getString("text") ?: ""
                    val lines = text.split("\n", limit = 2)
                    val title = if (lines.isNotEmpty()) lines[0] else ""
                    val content = if (lines.size > 1) lines[1] else ""
                    val category = document.getString("category") ?: ""
                    
                    _notes.add(
                        Note(
                            id = document.id,
                            title = title,
                            content = content,
                            timestamp = document.getLong("timestamp") ?: 0L,
                            category = category
                        )
                    )
                }
                
                // Update filtered notes after loading
                filterNotes(searchQueryValue)
                
                // Finished loading
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                Log.e("QuickNotesViewModel", "Error loading notes from Firestore", exception)
                
                // Finished loading (even with error)
                _isLoading.value = false
            }
    }
    
    private fun setupSnapshotListener(uid: String) {
        // Remove any existing listener to avoid duplicates
        snapshotListenerRegistration?.remove()
        
        snapshotListenerRegistration = firestore.collection("users")
            .document(uid)
            .collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("QuickNotesViewModel", "Error in snapshot listener", e)
                    // Still update UI with available data even if there's an error
                    // The error is logged but we continue processing any available snapshot data
                }

                if (snapshot != null) {
                    // Always clear and repopulate to avoid duplicates
                    _notes.clear()
                    
                    // Convert Firestore documents to Note objects
                    for (document in snapshot) {
                        val text = document.getString("text") ?: ""
                        val lines = text.split("\n", limit = 2)
                        val title = if (lines.isNotEmpty()) lines[0] else ""
                        val content = if (lines.size > 1) lines[1] else ""
                        val category = document.getString("category") ?: ""
                        
                        _notes.add(
                            Note(
                                id = document.id,
                                title = title,
                                content = content,
                                timestamp = document.getLong("timestamp") ?: 0L,
                                category = category
                            )
                        )
                    }
                    
                    // Update filtered notes after snapshot update
                    filterNotes(searchQueryValue)
                }
            }
    }

    fun addNote(title: String, content: String = "", category: String = "") {
        val user = auth.currentUser ?: return
        
        // Validate character limits
        if (title.length > 25) {
            errorMessage = "Title too long. Maximum 25 characters."
            return
        }
        if (content.length > 9999) {
            errorMessage = "Content too long. Maximum 9999 characters."
            return
        }
        
        // Combine title and content into single text field for storage
        val text = if (content.isNotEmpty()) {
            "$title\n$content"
        } else {
            title
        }
        
        // Create note data matching the specified format
        val noteData = hashMapOf(
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "category" to category
        )

        // Add to Firestore using the exact path specified
        firestore.collection("users")
            .document(user.uid)
            .collection("notes")
            .add(noteData)
            .addOnSuccessListener { documentReference ->
                // Let the snapshot listener handle updating the UI
                errorMessage = null // Clear any previous error
            }
            .addOnFailureListener { exception ->
                Log.e("QuickNotesViewModel", "Error adding note to Firestore", exception)
                errorMessage = "Failed to add note. Please try again."
            }
    }

    fun deleteNote(noteId: String) {
        val user = auth.currentUser ?: return

        // Delete note from Firestore using the exact path specified
        firestore.collection("users")
            .document(user.uid)
            .collection("notes")
            .document(noteId)
            .delete()
            .addOnSuccessListener {
                // The snapshot listener will handle updating the UI
                errorMessage = null // Clear any previous error
            }
            .addOnFailureListener { exception ->
                Log.e("QuickNotesViewModel", "Error deleting note from Firestore", exception)
                errorMessage = "Failed to delete note. Please try again."
            }
    }
    
    fun editNote(noteId: String, title: String, content: String = "", category: String = "") {
        val user = auth.currentUser ?: return
        
        // Validate character limits
        if (title.length > 25) {
            errorMessage = "Title too long. Maximum 25 characters."
            return
        }
        if (content.length > 9999) {
            errorMessage = "Content too long. Maximum 9999 characters."
            return
        }
        
        // Combine title and content into single text field for storage
        val text = if (content.isNotEmpty()) {
            "$title\n$content"
        } else {
            title
        }
        
        // Update note in Firestore using the exact path specified
        firestore.collection("users")
            .document(user.uid)
            .collection("notes")
            .document(noteId)
            .update(
                mapOf(
                    "text" to text,
                    "timestamp" to System.currentTimeMillis(),
                    "category" to category
                )
            )
            .addOnSuccessListener {
                errorMessage = null // Clear any previous error
            }
            .addOnFailureListener { exception ->
                Log.e("QuickNotesViewModel", "Error updating note in Firestore", exception)
                errorMessage = "Failed to update note. Please try again."
            }
    }
    
    /**
     * Reset the ViewModel state when a new user logs in
     * This ensures data reload from Firestore on fresh sessions
     */
    fun resetState() {
        _notes.clear()
        _filteredNotes.clear()
        _isLoading.value = false
        _searchQuery.value = ""
        errorMessage = null
    }
    
    fun clearErrorMessage() {
        errorMessage = null
    }
    
    /**
     * Force refresh notes from server
     * This should be called when the screen is opened to ensure
     * we get the latest data from the cloud
     */
    fun forceRefresh() {
        loadNotesFromFirestore()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up the snapshot listener to prevent memory leaks
        snapshotListenerRegistration?.remove()
    }
}