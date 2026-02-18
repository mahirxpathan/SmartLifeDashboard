package com.example.smartlifedashboard.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.smartlifedashboard.model.Habit
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.HabitTrackerViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    navController: NavController,
    habitId: String,
    viewModel: HabitTrackerViewModel = hiltViewModel()
) {
    val allHabits by viewModel.habits.collectAsState()
    val habit = allHabits.find { it.id == habitId } ?: return
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${habit.title} Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        navDebouncer.debouncePopBackStack(navController, coroutineScope) 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Habit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Habit Progress Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        val canCompleteHabit = remember(habit) {
                            canCompleteHabit(habit)
                        }
                        
                        IconButton(
                            onClick = {
                                if (habit.completedToday) {
                                    viewModel.markHabitAsIncomplete(habit.id)
                                } else if (canCompleteHabit) {
                                    viewModel.markHabitAsCompleted(habit.id)
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            enabled = canCompleteHabit || habit.completedToday
                        ) {
                            Icon(
                                imageVector = if (habit.completedToday) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (habit.completedToday) "Completed Today" else "Not Completed Today",
                                tint = if (habit.completedToday) MaterialTheme.colorScheme.primary 
                                    else if (canCompleteHabit) MaterialTheme.colorScheme.onSurfaceVariant 
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (habit.details.isNotEmpty()) {
                        Text(
                            text = habit.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Progress indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProgressStat(
                            title = "Streak",
                            value = habit.getCurrentStreak().toString(),
                            icon = Icons.Default.Favorite
                        )
                        
                        ProgressStat(
                            title = "Today",
                            value = if (habit.completedToday) "Done" else "Pending",
                            icon = if (habit.completedToday) Icons.Default.Check else Icons.Default.Schedule
                        )
                        
                        ProgressStat(
                            title = "Freq",
                            value = habit.frequency,
                            icon = Icons.Default.Schedule
                        )
                    }
                }
            }
            
            // Month Calendar view
            CalendarView(habit = habit)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(habit.id)
                        navDebouncer.debouncePopBackStack(navController, coroutineScope)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
}

@Composable
fun ProgressStat(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



@Composable
fun CalendarView(habit: Habit) {
    val currentMonth = remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val completionMap = remember(habit) {
        val map = mutableMapOf<LocalDate, Boolean>()
        habit.history.forEach { (dateString, completed) ->
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val date = LocalDate.parse(dateString, formatter)
                map[date] = completed
            } catch (e: Exception) {
                // Handle invalid date format
            }
        }
        map
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Month and Year Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { 
                        currentMonth.value = currentMonth.value.minusMonths(1) 
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "${currentMonth.value.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.value.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                IconButton(
                    onClick = { 
                        currentMonth.value = currentMonth.value.plusMonths(1) 
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Calendar grid for current month
            val firstDayOfMonth = currentMonth.value
            val daysInMonth = YearMonth.from(currentMonth.value).lengthOfMonth()
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Adjust to make Sunday first
            
            val days = mutableListOf<LocalDate?>()
            
            // Add empty cells for days before the first day of the month
            for (i in 0 until firstDayOfWeek) {
                days.add(null)
            }
            
            // Add all days of the month
            for (day in 1..daysInMonth) {
                days.add(firstDayOfMonth.withDayOfMonth(day))
            }
            
            // Create weeks (rows of 7 days)
            val weeks = days.chunked(7)
            
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Ensure each week has exactly 7 items
                    val weekWithPadding = week.toMutableList()
                    while (weekWithPadding.size < 7) {
                        weekWithPadding.add(null)
                    }
                    
                    weekWithPadding.forEach { date ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (date != null) {
                                val isCompleted = completionMap[date] ?: false
                                val isToday = date == LocalDate.now()
                                
                                DateCell(
                                    date = date,
                                    isCompleted = isCompleted,
                                    isToday = isToday
                                )
                            } else {
                                // Empty cell for days not in the current month
                                Spacer(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateCell(date: LocalDate, isCompleted: Boolean, isToday: Boolean) {
    val color = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .background(
                color = color,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.onPrimary
            } else if (isToday) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun DayCell(day: DayData) {
    val color = when {
        day.completed -> MaterialTheme.colorScheme.primary
        day.date == LocalDate.now() -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day.completed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        } else if (day.date == LocalDate.now()) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        } else {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

data class DayData(
    val date: LocalDate,
    val completed: Boolean
)

fun generateWeekGrid(habit: Habit): List<List<DayData>> {
    // Calculate a 4-week period for the calendar view
    val endDate = LocalDate.now()
    val startDate = endDate.minusWeeks(3) // 4 weeks including current week
    
    val allDays = mutableListOf<DayData>()
    
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        val completed = habit.history[currentDate.toString()] == true
        allDays.add(DayData(date = currentDate, completed = completed))
        currentDate = currentDate.plusDays(1)
    }
    
    // Group days into weeks (7 days per week)
    val weeks = allDays.chunked(7)
    
    // Pad the first week with empty days if needed
    val firstWeek = weeks.firstOrNull()
    if (firstWeek != null) {
        val firstDayOfWeek = firstWeek.firstOrNull()?.date?.dayOfWeek?.value ?: 1
        val paddingDays = (firstDayOfWeek - 1).coerceAtMost(6) // Monday is 1, so no padding needed if first day is Monday
        
        if (paddingDays > 0) {
            val paddedWeek = mutableListOf<DayData>()
            repeat(paddingDays) { index ->
                val paddingDate = firstWeek.first().date.minusDays((paddingDays - index).toLong())
                paddedWeek.add(DayData(date = paddingDate, completed = false))
            }
            paddedWeek.addAll(firstWeek)
            val updatedWeeks = weeks.toMutableList()
            updatedWeeks[0] = paddedWeek
            return updatedWeeks
        }
    }
    
    return weeks
}

@Composable
fun GitHubStyleHeatmap(habit: Habit) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    // Create a map of dates to completion status
    val completionMap = remember(habit) {
        val map = mutableMapOf<LocalDate, Boolean>()
        habit.history.forEach { (dateString, completed) ->
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val date = LocalDate.parse(dateString, formatter)
                map[date] = completed
            } catch (e: Exception) {
                // Handle invalid date format
            }
        }
        map
    }
    
    // Get the start date (beginning of the year or first completion date)
    val startDate = remember {
        val firstDate = completionMap.keys.minOrNull()
        if (firstDate != null) {
            LocalDate.of(firstDate.year, 1, 1)
        } else {
            LocalDate.now().withDayOfYear(1) // Start of current year
        }
    }
    
    // Get the end date (end of the year or today)
    val endDate = remember {
        val lastDate = completionMap.keys.maxOrNull()
        if (lastDate != null) {
            if (lastDate.year == currentYear) {
                LocalDate.now()
            } else {
                LocalDate.of(lastDate.year, 12, 31)
            }
        } else {
            LocalDate.now()
        }
    }
    
    Column {
        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Calculate month labels
            val months = remember {
                val list = mutableListOf<String>()
                var currentDate = startDate
                var lastMonth = -1
                
                while (!currentDate.isAfter(endDate)) {
                    if (currentDate.monthValue != lastMonth) { // First day of new month
                        list.add(
                            currentDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                        lastMonth = currentDate.monthValue
                    }
                    currentDate = currentDate.plusDays(1)
                }
                list
            }
            
            months.forEach { month ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Calendar grid - using a regular Column/Row approach instead of LazyVerticalGrid to avoid infinite height issue
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val weeksCount = if (totalDays > 0) (totalDays + 6) / 7 else 0 // Calculate number of weeks
        
        Column {
            // Day of week labels
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (dayIndex in 0..6) {
                    Text(
                        text = when (dayIndex) {
                            0 -> "M"
                            1 -> "T" 
                            2 -> "W"
                            3 -> "T"
                            4 -> "F"
                            5 -> "S"
                            6 -> "S"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
            
            // Calendar days in weeks
            for (weekIndex in 0 until weeksCount) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = weekIndex * 7 + dayOfWeek
                        if (dayIndex < totalDays) {
                            val date = startDate.plusDays(dayIndex.toLong())
                            val isCompleted = completionMap[date] ?: false
                            
                            val color = when {
                                isCompleted -> {
                                    // Calculate intensity based on completion frequency
                                    val intensity = min(4, completionMap.filter { it.value }.size / max(1, totalDays / 30)) // Normalize based on month
                                    when (intensity) {
                                        0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) // Low activity
                                        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) // Medium-low activity
                                        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) // Medium activity
                                        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) // High activity
                                        else -> MaterialTheme.colorScheme.primary // Max activity
                                    }
                                }
                                date.isAfter(LocalDate.now()) -> {
                                    // Future dates
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                else -> {
                                    // Not completed
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = color,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .border(
                                        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        } else {
                            // Empty space for days beyond the total
                            Spacer(modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (index) {
                                0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f) // No activity
                                1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // Low activity
                                2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Medium activity
                                3 -> MaterialTheme.colorScheme.primary // High activity
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(2.dp)
                        )
                        .border(
                            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
            
            Text(
                text = "More",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun canCompleteHabit(habit: Habit): Boolean {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // Get the most recent completed date
    val lastCompletedDate = habit.history.entries
        .filter { it.value } // Only completed entries
        .map { it.key }
        .maxOrNull() // Get the most recent date
        
    if (lastCompletedDate == null) {
        // If no previous completion, user can complete the habit
        return true
    }
    
    try {
        val lastCompletedLocalDate = LocalDate.parse(lastCompletedDate, formatter)
        val daysSinceLastCompletion = ChronoUnit.DAYS.between(lastCompletedLocalDate, today).toInt()
        
        return when (habit.frequency.lowercase()) {
            "daily" -> daysSinceLastCompletion >= 1
            "weekly" -> daysSinceLastCompletion >= 7
            "monthly" -> {
                // Check if at least one month has passed
                val expectedNextDate = lastCompletedLocalDate.plusMonths(1)
                !today.isBefore(expectedNextDate)
            }
            "custom" -> daysSinceLastCompletion >= habit.customInterval
            else -> daysSinceLastCompletion >= 1 // Default to daily
        }
    } catch (e: Exception) {
        // If there's an error parsing the date, allow completion
        return true
    }
}