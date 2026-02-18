package com.example.smartlifedashboard.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartlifedashboard.ui.components.ProfileImagePlaceholder
import com.example.smartlifedashboard.ui.theme.ThemeType
import com.example.smartlifedashboard.util.convertAvatarIdToIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onProfileUpdated: () -> Unit,
    onThemeChanged: (ThemeType) -> Unit,
    onShowToast: (String) -> Unit,
    onLogout: () -> Unit,
    currentTheme: ThemeType
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var selectedProfileImage by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Load user profile data when screen is initialized
    LaunchedEffect(Unit) {
        Log.d("EditProfileScreen", "Loading user profile data")
        currentUser?.uid?.let { uid ->
            try {
                val document = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                if (document.exists()) {
                    displayName = document.getString("displayName") ?: ""
                    // Load profile image directly from Firestore
                    selectedProfileImage = document.getString("profileImage")
                    Log.d("EditProfileScreen", "Loaded user profile data - Display name: $displayName, Profile image: $selectedProfileImage")
                } else {
                    Log.d("EditProfileScreen", "No user document found")
                }
            } catch (e: Exception) {
                Log.e("EditProfileScreen", "Error loading user profile data", e)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Profile")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Profile Picture Section
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        selectedProfileImage?.let { image ->
                            // Display the selected inbuilt profile image
                            ProfileImagePlaceholder(image, size = 100.dp)
                        } ?: run {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Default Profile Image",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Name Field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Profile Image Selection
                Text(
                    text = "Select Profile Picture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Inbuilt profile image options
                ProfileImageSelector(
                    selectedImage = selectedProfileImage,
                    onImageSelected = { imageId: String ->
                        selectedProfileImage = imageId
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Theme Switcher Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeOption(
                        themeName = "Light",
                        isSelected = currentTheme == ThemeType.LIGHT,
                        onSelected = { onThemeChanged(ThemeType.LIGHT) }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ThemeOption(
                        themeName = "Dark",
                        isSelected = currentTheme == ThemeType.DARK,
                        onSelected = { onThemeChanged(ThemeType.DARK) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save Button
                Button(
                    onClick = {
                        Log.d("EditProfileScreen", "Save button clicked")
                        Log.d("EditProfileScreen", "Display name: $displayName")
                        Log.d("EditProfileScreen", "Selected profile image: $selectedProfileImage")
                        selectedProfileImage?.let { image ->
                            Log.d("EditProfileScreen", "Converted pfpIndex: ${convertAvatarIdToIndex(image)}")
                        }
                        
                        // Update profile
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            val profileBuilder = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                                
                            val profileUpdates = profileBuilder.build()
                                            
                            user.updateProfile(profileUpdates)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("EditProfileScreen", "Profile updated successfully")
                                        // Convert selectedProfileImage to pfpIndex
                                        val pfpIndex = selectedProfileImage?.let { convertAvatarIdToIndex(it) } ?: 0
                                        // Save to Firestore only if selectedProfileImage is not null
                                        selectedProfileImage?.let { image ->
                                            saveUserDataToFirestore(user.uid, displayName, pfpIndex, image)
                                        }
                                        // Show toast message
                                        onShowToast("Saved successfully")
                                    } else {
                                        Log.e("EditProfileScreen", "Failed to update profile", task.exception)
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Logout Button
                Button(
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
                
                // Logout Confirmation Dialog
                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = {
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to logout?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    showLogoutDialog = false
                                    FirebaseAuth.getInstance().signOut()
                                    onLogout()
                                }
                            ) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showLogoutDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
            }
        }
    }
}


}

@Composable
fun ThemeOption(
    themeName: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (themeName) {
                            "Light" -> Color(0xFFE0E0E0)
                            "Dark" -> Color(0xFF424242)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = themeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun saveUserDataToFirestore(userId: String, displayName: String, pfpIndex: Int, selectedProfileImage: String?, onComplete: ((Boolean) -> Unit)? = null) {
    val db = FirebaseFirestore.getInstance()
    val userData = hashMapOf<String, Any?>()
    userData["displayName"] = displayName
    userData["pfpIndex"] = pfpIndex
    userData["profileImage"] = selectedProfileImage ?: "avatar1"
    userData["lastUpdated"] = System.currentTimeMillis() // Add timestamp for debugging
    
    db.collection("users").document(userId)
        .set(userData)
        .addOnSuccessListener {
            Log.d("EditProfileScreen", "User data saved to Firestore with pfpIndex: $pfpIndex")
            onComplete?.invoke(true)
        }
        .addOnFailureListener { exception ->
            Log.w("EditProfileScreen", "Error saving user data to Firestore", exception)
            onComplete?.invoke(false)
        }
}

@Composable
fun ProfileImageSelector(
    selectedImage: String?,
    onImageSelected: (String) -> Unit
) {
    val profileImages = listOf("avatar1", "avatar2", "avatar3", "avatar4", "avatar5")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.width(8.dp)) // Add left padding
        profileImages.forEach { imageId ->
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { onImageSelected(imageId) },
                contentAlignment = Alignment.Center
            ) {
                ProfileImagePlaceholder(imageId, isSelected = (selectedImage == imageId), size = 60.dp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp)) // Add right padding
    }
}
