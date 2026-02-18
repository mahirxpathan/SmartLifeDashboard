package com.example.smartlifedashboard.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartlifedashboard.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val habitRepository = HabitRepository()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Get current date in YYYY-MM-DD format
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                // Get all habits for the current user
                val habits = habitRepository.getHabits()
                
                // For each habit, check if it needs to be reset based on the current date
                habits.forEach { habit ->
                    // Check if the habit was completed today and if it needs to be reset
                    val lastCompletedDate = habit.history.keys.maxOrNull() // Get the most recent date in history
                    
                    // Determine if we need to reset the habit based on frequency
                    val shouldReset = when (habit.frequency.lowercase()) {
                        "daily" -> {
                            // For daily habits, reset if not completed today
                            lastCompletedDate != today
                        }
                        "weekly" -> {
                            // For weekly habits, reset if last completion was more than 7 days ago
                            if (lastCompletedDate != null) {
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val lastDate = formatter.parse(lastCompletedDate)
                                val currentDate = formatter.parse(today)
                                
                                if (lastDate != null && currentDate != null) {
                                    val diffInDays = (currentDate.time - lastDate.time) / (1000 * 60 * 60 * 24)
                                    diffInDays >= 7
                                } else {
                                    true // Reset if we can't parse dates
                                }
                            } else {
                                true // Reset if no previous completion
                            }
                        }
                        "monthly" -> {
                            // For monthly habits, reset if last completion was more than a month ago
                            if (lastCompletedDate != null) {
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val lastDate = formatter.parse(lastCompletedDate)
                                val currentDate = formatter.parse(today)
                                
                                if (lastDate != null && currentDate != null) {
                                    val lastCalendar = java.util.Calendar.getInstance().apply { time = lastDate }
                                    val currentCalendar = java.util.Calendar.getInstance().apply { time = currentDate }
                                    
                                    val diffInMonths = (currentCalendar.get(java.util.Calendar.YEAR) - lastCalendar.get(java.util.Calendar.YEAR)) * 12 +
                                            (currentCalendar.get(java.util.Calendar.MONTH) - lastCalendar.get(java.util.Calendar.MONTH))
                                    
                                    diffInMonths >= 1
                                } else {
                                    true // Reset if we can't parse dates
                                }
                            } else {
                                true // Reset if no previous completion
                            }
                        }
                        "custom" -> {
                            // For custom habits, reset if last completion was more than customInterval days ago
                            if (lastCompletedDate != null) {
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val lastDate = formatter.parse(lastCompletedDate)
                                val currentDate = formatter.parse(today)
                                
                                if (lastDate != null && currentDate != null) {
                                    val diffInDays = (currentDate.time - lastDate.time) / (1000 * 60 * 60 * 24)
                                    diffInDays >= habit.customInterval
                                } else {
                                    true // Reset if we can't parse dates
                                }
                            } else {
                                true // Reset if no previous completion
                            }
                        }
                        else -> {
                            // Default to daily reset
                            lastCompletedDate != today
                        }
                    }
                    
                    // If the habit should be reset, update its status
                    if (shouldReset) {
                        val updatedHabit = habit.copy(
                            completedToday = false
                        )
                        habitRepository.updateHabit(habit.id, updatedHabit)
                    }
                }
                
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}