package com.example.smartlifedashboard

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartlifedashboard.screens.AboutAppScreen
import com.example.smartlifedashboard.screens.DashboardScreen
import com.example.smartlifedashboard.screens.EditProfileScreen
import com.example.smartlifedashboard.screens.LoginScreen
import com.example.smartlifedashboard.screens.QuoteScreen
import com.example.smartlifedashboard.screens.SplashScreen
import com.example.smartlifedashboard.viewmodel.QuoteViewModel
import com.example.smartlifedashboard.viewmodel.FavoritesViewModel
import com.example.smartlifedashboard.screens.QuickNotesScreen
import com.example.smartlifedashboard.screens.FavoritesScreen
import com.example.smartlifedashboard.screens.HabitTrackerScreen
import com.example.smartlifedashboard.screens.HabitDetailScreen
import com.example.smartlifedashboard.screens.WeatherScreen
import com.example.smartlifedashboard.screens.TaskReminderScreen
import com.example.smartlifedashboard.screens.EventCountdownScreen
import com.example.smartlifedashboard.screens.NoteDetailScreen
import com.example.smartlifedashboard.viewmodel.WeatherViewModel
import com.example.smartlifedashboard.util.WorkManagerUtil
import com.example.smartlifedashboard.util.NavigationDebouncer
import java.util.concurrent.TimeUnit
import com.example.smartlifedashboard.ui.theme.SmartLifeDashboardTheme
import com.example.smartlifedashboard.ui.theme.ThemeType
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize shared preferences for theme persistence
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        
        // Schedule default quote refresh (every 1 hour)
        // Only schedule if not already scheduled to avoid duplication
        WorkManagerUtil.scheduleQuoteRefresh(this, 1, TimeUnit.HOURS)
        
        // Schedule daily habit reset
        WorkManagerUtil.scheduleHabitReset(this)
        
        setContent {
            AppNavigation(sharedPreferences, this)
        }
    }
}

@Composable
fun AppNavigation(sharedPreferences: SharedPreferences, context: android.content.Context) {
    val navController = rememberNavController()
    val navDebouncer = remember { NavigationDebouncer() }
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    // Load saved theme preference or default to LIGHT
    var currentTheme by remember { 
        val savedTheme = sharedPreferences.getString("selected_theme", ThemeType.LIGHT.name)
        val themeType = try {
            val theme = ThemeType.valueOf(savedTheme ?: ThemeType.LIGHT.name)
            // Ensure only LIGHT and DARK themes are used
            when (theme) {
                ThemeType.LIGHT, ThemeType.DARK -> theme
                else -> ThemeType.LIGHT // fallback to default if saved theme is invalid
            }
        } catch (e: IllegalArgumentException) {
            ThemeType.LIGHT // fallback to default if saved theme is invalid
        }
        mutableStateOf(themeType)
    }
    
    // Update current user when auth state changes
    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        currentUser = firebaseAuth.currentUser
    }
    
    // Register and unregister the listener
    DisposableEffect(auth) {
        auth.addAuthStateListener(authStateListener)
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }
    
    SmartLifeDashboardTheme(themeType = currentTheme) {
        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") {
                SplashScreen(
                    onNavigateNext = {
                        val destination = if (currentUser != null) "dashboard" else "login"
                        navController.navigate(destination) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable("login") {
                LoginScreen(
                    onNavigateToDashboard = {
                        currentUser = auth.currentUser
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            
            composable("dashboard") {
                DashboardScreen(
                    navController = navController,
                    onSignOut = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    refreshProfile = {
                        // This will be called when the screen becomes active
                    }
                )
            }
            
            composable("edit_profile") {
                EditProfileScreen(
                    onBack = {
                        navDebouncer.debouncePopBackStack(navController)
                    },
                    onProfileUpdated = {
                        // Navigate back to dashboard and trigger a refresh
                        navDebouncer.debouncePopBackStack(navController)
                    },
                    onThemeChanged = { theme ->
                        currentTheme = theme
                        // Save theme preference
                        sharedPreferences.edit()
                            .putString("selected_theme", theme.name)
                            .apply()
                    },
                    onShowToast = { message ->
                        // Show toast message
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onLogout = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    currentTheme = currentTheme
                )
            }
            
            composable("about_app") {
                AboutAppScreen(
                    onBack = {
                        navDebouncer.debouncePopBackStack(navController)
                    }
                )
            }
            
            composable("quick_notes") {
                QuickNotesScreen(
                    navController = navController
                )
            }
            
            composable("quote") {
                QuoteScreen(
                    navController = navController
                )
            }
            
            composable("favorites") {
                FavoritesScreen(
                    navController = navController
                )
            }
            
            composable("weather") {
                WeatherScreen(
                    viewModel = viewModel(),
                    onBack = { navDebouncer.debouncePopBackStack(navController) }
                )
            }
            
            composable("habitTrackerScreen") {
                HabitTrackerScreen(navController = navController)
            }
            
            composable("habitDetail/{habitId}") { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString("habitId") ?: ""
                HabitDetailScreen(
                    navController = navController,
                    habitId = habitId
                )
            }
            
            composable("task_reminder") {
                TaskReminderScreen(
                    navController = navController
                )
            }
            
            composable("event_countdown") {
                EventCountdownScreen(
                    navController = navController
                )
            }
            
            composable("note_detail") {
                NoteDetailScreen(
                    navController = navController,
                    viewModel = viewModel()
                )
            }
            
            composable("note_detail/{noteId}") { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                NoteDetailScreen(
                    navController = navController,
                    viewModel = viewModel(),
                    noteId = noteId
                )
            }
        }
    }
}