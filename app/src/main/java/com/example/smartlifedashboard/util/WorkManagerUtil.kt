package com.example.smartlifedashboard.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smartlifedashboard.work.HabitResetWorker
import com.example.smartlifedashboard.work.QuoteRefreshWorker
import java.util.concurrent.TimeUnit

object WorkManagerUtil {
    private const val QUOTE_REFRESH_WORK_NAME = "quote_refresh_work"
    private const val HABIT_RESET_WORK_NAME = "habit_reset_work"
    
    /**
     * Schedule periodic quote refresh with the specified interval
     * @param context Application context
     * @param interval Interval value
     * @param unit Time unit (MINUTES, HOURS, DAYS)
     */
    fun scheduleQuoteRefresh(context: Context, interval: Long, unit: TimeUnit) {
        // Constraints: require network connection
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create periodic work request
        val quoteRefreshWork = PeriodicWorkRequestBuilder<QuoteRefreshWorker>(
            interval, unit
        )
            .setConstraints(constraints)
            .build()
        
        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            QUOTE_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if already exists
            quoteRefreshWork
        )
    }
    
    /**
     * Schedule daily habit reset work
     * @param context Application context
     */
    fun scheduleHabitReset(context: Context) {
        // No constraints needed for habit reset
        val constraints = Constraints.Builder()
            .build()
        
        // Create periodic work request that runs daily
        val habitResetWork = PeriodicWorkRequestBuilder<HabitResetWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()
        
        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HABIT_RESET_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if already exists
            habitResetWork
        )
    }
    
    /**
     * Cancel the quote refresh work
     * @param context Application context
     */
    fun cancelQuoteRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(QUOTE_REFRESH_WORK_NAME)
    }
    
    /**
     * Cancel the habit reset work
     * @param context Application context
     */
    fun cancelHabitReset(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(HABIT_RESET_WORK_NAME)
    }
}