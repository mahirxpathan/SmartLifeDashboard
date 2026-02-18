package com.example.smartlifedashboard.model

import java.util.Date

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


fun calculateCurrentStreak(history: Map<String, Boolean>): Int {
    if (history.isEmpty()) {
        return 0
    }
    
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    
    // Get all completed dates
    val completedDates = history.filter { it.value }.keys.mapNotNull { dateString ->
        try {
            LocalDate.parse(dateString, formatter)
        } catch (e: Exception) {
            null
        }
    }.sorted()
    
    if (completedDates.isEmpty()) {
        return 0
    }
    
    // Check if today is completed
    var currentStreak = if (history[today.toString()] == true) 1 else 0
    
    // If today is not completed, check yesterday and work backwards
    var checkDate = if (currentStreak == 1) today.minusDays(1) else today
    
    // If we're not counting today, start with a streak of 0 and work backwards from yesterday
    if (currentStreak == 0) {
        checkDate = today.minusDays(1)
    }
    
    // Count consecutive days backwards from today (or yesterday if today isn't completed)
    while (checkDate.isAfter(completedDates.minOrNull()?.minusDays(1) ?: checkDate)) {
        val dateString = checkDate.toString()
        if (history[dateString] == true) {
            currentStreak++
        } else {
            // If we find a gap, stop counting
            break
        }
        checkDate = checkDate.minusDays(1)
    }
    
    return currentStreak
}

data class Habit(
    val id: String = "",
    val title: String = "",
    val frequency: String = "", // Daily, Weekly, Monthly, Custom
    val customInterval: Int = 1, // For custom habits, number of days between completions
    val streak: Int = 0,
    val completedToday: Boolean = false,
    val createdAt: Date = Date(),
    val history: Map<String, Boolean> = mapOf(),
    val icon: String = "",
    val color: String = "",
    val details: String = "" // Additional details for the habit
) {
    fun getCurrentStreak(): Int {
        return calculateCurrentStreak(this.history)
    }
}