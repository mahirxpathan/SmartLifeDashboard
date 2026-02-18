package com.example.smartlifedashboard.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.smartlifedashboard.util.convertIndexToAvatarId

data class UserUiState(
    val displayName: String = "",
    val profilePictureIndex: Int = 0,
    val profileImageId: String = "avatar1"
)

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    init {
        loadUserData()
    }
    
    fun loadUserData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val snapshot = db.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                    
                    val name = snapshot.getString("displayName") ?: "User"
                    val pfpIndex = snapshot.getLong("pfpIndex")?.toInt() ?: 0
                    val profileImageId = snapshot.getString("profileImage") ?: "avatar1"
                    
                    _uiState.value = UserUiState(
                        displayName = name,
                        profilePictureIndex = pfpIndex,
                        profileImageId = profileImageId
                    )
                    
                    Log.d("UserViewModel", "Loaded user data - Name: $name, Index: $pfpIndex")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error loading user data", e)
            }
        }
    }
    
    fun refreshUserData() {
        loadUserData()
    }
}