package com.example.smartlifedashboard.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import com.example.smartlifedashboard.ui.components.ProfileImagePlaceholder
import com.example.smartlifedashboard.util.convertIndexToAvatarId
import com.example.smartlifedashboard.ui.components.QuoteCard
import com.example.smartlifedashboard.ui.components.QuickNotesCard
import com.example.smartlifedashboard.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    onSignOut: () -> Unit,
    refreshProfile: () -> Unit = {},
    userViewModel: UserViewModel = viewModel()
) {
    val uiState by userViewModel.uiState.collectAsState()
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var refreshKey by remember { mutableStateOf(0) }
    
    // Listen for auth state changes to update the UI when profile is updated
    val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        currentUser = auth.currentUser
        Log.d("DashboardScreen", "Auth state changed, current user: ${currentUser?.uid}")
        // Refresh user data from ViewModel
        userViewModel.refreshUserData()
    }
    
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(authStateListener)
        // Refresh profile data when screen becomes active
        refreshProfile()
        userViewModel.refreshUserData()
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }
    

    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Smart Life Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    OverflowMenu(navController = navController, onSignOut = onSignOut)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileImagePlaceholder(uiState.profileImageId, size = 64.dp)

                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = uiState.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Text(
                                    text = "Welcome back!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                            }
                        }
                    }
                }
                
                // Widgets grid
                item {
                    Text(
                        text = "Daily Essentials",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Weather Snapshot Widget
                item {
                    WidgetCard(
                        title = "Weather Snapshot",
                        icon = "🌤️",
                        description = "Current weather in your location",
                        onOpen = { navController.navigate("weather") }
                    )
                }
                
                // Quote of the Day Widget
                item {
                    WidgetCard(
                        title = "Quote of the Day",
                        icon = "💡",
                        description = "Get your daily dose of motivation",
                        onOpen = { navController.navigate("quote") }
                    )
                }
                
                // Task Reminder Widget
                item {
                    WidgetCard(
                        title = "Task Reminders",
                        icon = "⏰",
                        description = "Top tasks for today",
                        onOpen = { navController.navigate("task_reminder") }
                    )
                }
                
                // Habit Tracker Widget
                item {
                    WidgetCard(
                        title = "Habit Tracker",
                        icon = "🔥",
                        description = "Track your daily habits and build streaks",
                        onOpen = { navController.navigate("habitTrackerScreen") }
                    )
                }
                
                // Quick Notes Widget
                item {
                    WidgetCard(
                        title = "Quick Notes",
                        icon = "📝",
                        description = "Jot down quick thoughts and ideas",
                        onOpen = { navController.navigate("quick_notes") }
                    )
                }
                
                // Event Countdown Widget
                item {
                    WidgetCard(
                        title = "Event Countdown",
                        icon = "📅",
                        description = "Count down to important events",
                        onOpen = { navController.navigate("event_countdown") }
                    )
                }
            }
        }
    }
}

@Composable
fun OverflowMenu(
    navController: NavController,
    onSignOut: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit Profile") },
                onClick = { 
                    expanded = false
                    navController.navigate("edit_profile")
                }
            )
            DropdownMenuItem(
                text = { Text("About App") },
                onClick = { 
                    expanded = false
                    navController.navigate("about_app")
                }
            )

        }
    }
}

@Composable
fun WidgetCard(
    title: String,
    icon: String,
    description: String,
    onClick: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onOpen ?: onClick ?: {},
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Open")
            }
        }
    }
}