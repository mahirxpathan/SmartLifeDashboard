package com.example.smartlifedashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlifedashboard.model.Habit
import com.example.smartlifedashboard.model.calculateCurrentStreak
import com.example.smartlifedashboard.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HabitSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST, 
    TITLE_A_TO_Z,
    TITLE_Z_TO_A,
    STREAK_HIGHEST,
    FREQUENCY_DAILY_FIRST
}

class HabitTrackerViewModel : ViewModel() {
    private val habitRepository = HabitRepository()
    
    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(HabitSortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<HabitSortOrder> = _sortOrder.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredHabits = MutableStateFlow<List<Habit>>(emptyList())
    val filteredHabits: StateFlow<List<Habit>> = _filteredHabits.asStateFlow()
    
    init {
        observeHabits()
    }
    
    private fun observeHabits() {
        viewModelScope.launch {
            habitRepository.getHabitsFlow().collect { result ->
                result.fold(
                    onSuccess = { habitList ->
                        _habits.value = habitList
                        filterAndSortHabits(habitList)
                        _isLoading.value = false
                        _error.value = null
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                )
            }
        }
    }
    
    private fun filterAndSortHabits(habitList: List<Habit>) {
        val query = _searchQuery.value.lowercase()
        val filtered = if (query.isEmpty()) {
            habitList
        } else {
            habitList.filter { habit ->
                habit.title.lowercase().contains(query) ||
                habit.details.lowercase().contains(query)
            }
        }
        val sortedHabits = sortHabits(filtered)
        _filteredHabits.value = sortedHabits
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterAndSortHabits(_habits.value)
    }
    
    fun addHabit(title: String, frequency: String, icon: String = "", color: String = "", details: String = "", customInterval: Int = 1) {
        viewModelScope.launch {
            try {
                val newHabit = Habit(
                    title = title,
                    frequency = frequency,
                    customInterval = customInterval,
                    streak = 0,
                    completedToday = false,
                    createdAt = java.util.Date(),
                    history = mapOf(),
                    icon = icon,
                    color = color,
                    details = details
                )
                val habitId = habitRepository.addHabit(newHabit)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    // Simplified version for basic habit creation
    fun addHabit(title: String, frequency: String) {
        addHabit(title, frequency, "", "", "")
    }
    
    fun updateHabit(habitId: String, title: String, frequency: String, icon: String, color: String, details: String, customInterval: Int) {
        viewModelScope.launch {
            try {
                val existingHabit = _habits.value.find { it.id == habitId }
                if (existingHabit != null) {
                    val updatedHabit = existingHabit.copy(
                        title = title,
                        frequency = frequency,
                        icon = icon,
                        color = color,
                        details = details,
                        customInterval = customInterval
                    )
                    habitRepository.updateHabit(habitId, updatedHabit)
                    // Real-time updates are handled by the observer
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                habitRepository.updateHabit(habit.id, habit)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            try {
                habitRepository.deleteHabit(habitId)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun markHabitAsCompleted(habitId: String) {
        viewModelScope.launch {
            try {
                val habit = _habits.value.find { it.id == habitId }
                if (habit != null) {
                    // Get current date in YYYY-MM-DD format
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val updatedHistory = habit.history.toMutableMap().apply {
                        this[today] = true
                    }
                    // Calculate the correct streak based on consecutive days
                    val newStreak = calculateCurrentStreak(updatedHistory)
                    val updatedHabit = habit.copy(
                        completedToday = true,
                        streak = newStreak,
                        history = updatedHistory
                    )
                    habitRepository.updateHabit(habitId, updatedHabit)
                    // Real-time updates are handled by the observer
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun markHabitAsIncomplete(habitId: String) {
        viewModelScope.launch {
            try {
                val habit = _habits.value.find { it.id == habitId }
                if (habit != null) {
                    // Get current date in YYYY-MM-DD format
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val updatedHistory = habit.history.toMutableMap().apply {
                        this[today] = false
                    }
                    // Calculate the correct streak based on consecutive days
                    val newStreak = calculateCurrentStreak(updatedHistory)
                    val updatedHabit = habit.copy(
                        completedToday = false,
                        streak = newStreak,
                        history = updatedHistory
                    )
                    habitRepository.updateHabit(habitId, updatedHabit)
                    // Real-time updates are handled by the observer
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    private fun sortHabits(habits: List<Habit>): List<Habit> {
        return when (_sortOrder.value) {
            HabitSortOrder.NEWEST_FIRST -> habits.sortedByDescending { it.createdAt }
            HabitSortOrder.OLDEST_FIRST -> habits.sortedBy { it.createdAt }
            HabitSortOrder.TITLE_A_TO_Z -> habits.sortedBy { it.title.lowercase() }
            HabitSortOrder.TITLE_Z_TO_A -> habits.sortedByDescending { it.title.lowercase() }
            HabitSortOrder.STREAK_HIGHEST -> habits.sortedByDescending { it.streak }
            HabitSortOrder.FREQUENCY_DAILY_FIRST -> habits.sortedWith(
                compareBy<Habit> { habit ->
                    when (habit.frequency) {
                        "Daily" -> 0
                        "Weekly" -> 1
                        "Monthly" -> 2
                        else -> 3 // Custom
                    }
                }.thenBy { it.createdAt }
            )
        }
    }
    
    fun setSortOrder(sortOrder: HabitSortOrder) {
        _sortOrder.value = sortOrder
        // Re-filter and sort the current habits
        filterAndSortHabits(_habits.value)
    }
    
    fun getSortOrder(): HabitSortOrder = _sortOrder.value
}