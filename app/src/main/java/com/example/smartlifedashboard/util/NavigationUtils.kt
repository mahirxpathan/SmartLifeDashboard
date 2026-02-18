package com.example.smartlifedashboard.util

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

/**
 * Utility class to handle debounced navigation to prevent rapid back gestures
 * from causing white screens or navigation conflicts
 */
class NavigationDebouncer {
    private var debounceJob: Job? = null
    private val debounceDelay = 300L // 300ms delay
    
    /**
     * Perform debounced back navigation with provided coroutine scope
     */
    fun debouncePopBackStack(navController: NavController, coroutineScope: CoroutineScope) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch(Dispatchers.Main) {
            delay(debounceDelay)
            navController.popBackStack()
        }
    }
    
    /**
     * Perform debounced back navigation using internal coroutine scope
     * Use this for callback-based navigation where scope isn't available
     */
    fun debouncePopBackStack(navController: NavController) {
        debounceJob?.cancel()
        val scope = MainScope()
        debounceJob = scope.launch(Dispatchers.Main) {
            delay(debounceDelay)
            navController.popBackStack()
        }
    }
    
    /**
     * Cancel any pending navigation
     */
    fun cancelPendingNavigation() {
        debounceJob?.cancel()
        debounceJob = null
    }
}