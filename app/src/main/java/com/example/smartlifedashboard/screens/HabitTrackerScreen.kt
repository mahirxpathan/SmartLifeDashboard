package com.example.smartlifedashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

import com.example.smartlifedashboard.model.Habit
import com.example.smartlifedashboard.viewmodel.HabitTrackerViewModel
import com.example.smartlifedashboard.viewmodel.HabitSortOrder
import com.example.smartlifedashboard.util.NavigationDebouncer

@OptIn(ExperimentalMaterial3Api::class)












@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var customDays by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var customDaysExpanded by remember { mutableStateOf(false) }
    
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Custom")
    val maxDetailsLength = 100
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Habit") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g., Drink 8 glasses of water") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = details,
                    onValueChange = { newValue ->
                        if (newValue.length <= maxDetailsLength) {
                            details = newValue
                        }
                    },
                    label = { Text("Details (Optional)") },
                    placeholder = { Text("Add extra details about your habit...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq
                                    expanded = false
                                    customDays = "" // Reset custom days when changing frequency
                                }
                            )
                        }
                    }
                }
                
                if (frequency == "Custom") {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { newValue ->
                            // Only allow numeric input
                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                customDays = newValue
                            }
                        },
                        label = { Text("Custom Days") },
                        placeholder = { Text("e.g., 3 for every 3 days") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
                

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (habitName.isNotBlank()) {
                        val frequencyValue = if (frequency == "Custom" && customDays.isNotEmpty()) {
                            "Every ${customDays} days"
                        } else {
                            frequency
                        }
                        onConfirm(habitName, frequencyValue, customDays, details)
                    }
                },
                enabled = habitName.isNotBlank() && (frequency != "Custom" || customDays.isNotEmpty())
            ) {
                Text("Add Habit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCard(
    habit: Habit,
    onToggle: (String) -> Unit,
    onLongClick: (String) -> Unit,  // This is now for delete
    onNavigateToDetail: (String) -> Unit,
    onEdit: (Habit) -> Unit  // New parameter for editing
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = { 
                    showDeleteDialog = true
                },
                onDoubleClick = { onNavigateToDetail(habit.id) }
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Habit info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (habit.completedToday) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Streak counter
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${habit.getCurrentStreak()} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Habit Details
                if (habit.details.isNotEmpty()) {
                    Text(
                        text = habit.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                // Frequency
                Text(
                    text = habit.frequency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right side - Complete toggle
            IconButton(
                onClick = { onToggle(habit.id) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (habit.completedToday) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (habit.completedToday) "Completed" else "Incomplete",
                    tint = if (habit.completedToday) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Delete/Edit confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Manage Habit") },
            text = { Text("Choose an action for this habit.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEdit(habit)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        showDeleteConfirmation = true  // Show delete confirmation instead
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Delete")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLongClick(habit.id)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitModal(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var customDays by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val frequencies = listOf("Daily", "Weekly", "Custom")
    val maxDetailsLength = 100
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Habit") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g., Drink 8 glasses of water") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq
                                    expanded = false
                                    if (freq != "Custom") {
                                        customDays = ""
                                    }
                                }
                            )
                        }
                    }
                }
                
                if (frequency == "Custom") {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { newValue ->
                            // Only allow numeric input
                            if (newValue.all { it.isDigit() }) {
                                customDays = newValue
                            }
                        },
                        label = { Text("Interval (days)") },
                        placeholder = { Text("Enter number of days") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (habitName.isNotBlank()) {
                        val freq = if (frequency == "Custom" && customDays.isNotBlank()) {
                            "Every ${customDays} days"
                        } else {
                            frequency
                        }
                        val customInterval = if (frequency == "Custom" && customDays.isNotBlank()) {
                            customDays.toIntOrNull() ?: 1
                        } else {
                            when (frequency) {
                                "Daily" -> 1
                                "Weekly" -> 7
                                else -> 1
                            }
                        }
                        onConfirm(habitName, freq, customInterval.toString(), "")
                        habitName = ""
                        frequency = "Daily"
                        customDays = ""
                        details = ""
                    }
                },
                enabled = habitName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    habitName = ""
                    frequency = "Daily"
                    customDays = ""
                }
            ) {
                Text("Cancel")
            }
        }
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(
    navController: NavController,
    viewModel: HabitTrackerViewModel = hiltViewModel()
) {
    val habits by viewModel.filteredHabits.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    
    var searchExpanded by remember { mutableStateOf(false) }
    
    // Enhanced statistics
    val completedToday = habits.count { it.completedToday }
    val completionRate = if (habits.isNotEmpty()) (completedToday * 100) / habits.size else 0
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Habit Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navDebouncer.debouncePopBackStack(navController, coroutineScope) 
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    var showSortDialog by remember { mutableStateOf(false) }
                    
                    // Sort button
                    IconButton(onClick = { 
                        showSortDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort habits",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // Search button
                    IconButton(onClick = { 
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) {
                            viewModel.setSearchQuery("")
                        }
                    }) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchExpanded) "Close search" else "Search",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // Sort options dialog
                    if (showSortDialog) {
                        AlertDialog(
                            onDismissRequest = { showSortDialog = false },
                            title = { Text("Sort Habits By") },
                            text = {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == HabitSortOrder.NEWEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(HabitSortOrder.NEWEST_FIRST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Newest First",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == HabitSortOrder.OLDEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(HabitSortOrder.OLDEST_FIRST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Oldest First",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == HabitSortOrder.TITLE_A_TO_Z,
                                            onClick = {
                                                viewModel.setSortOrder(HabitSortOrder.TITLE_A_TO_Z)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Title A-Z",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == HabitSortOrder.TITLE_Z_TO_A,
                                            onClick = {
                                                viewModel.setSortOrder(HabitSortOrder.TITLE_Z_TO_A)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Title Z-A",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == HabitSortOrder.STREAK_HIGHEST,
                                            onClick = {
                                                viewModel.setSortOrder(HabitSortOrder.STREAK_HIGHEST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Highest Streak",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == HabitSortOrder.FREQUENCY_DAILY_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(HabitSortOrder.FREQUENCY_DAILY_FIRST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Frequency (Daily First)",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { showSortDialog = false }
                                ) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddHabitDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add habit",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Search bar
                if (searchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search habits...") },
                        leadingIcon = { 
                            Icon(Icons.Default.Search, contentDescription = "Search") 
                        },
                        trailingIcon = {
                            IconButton(onClick = { 
                                viewModel.setSearchQuery("") 
                                searchExpanded = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                        singleLine = true
                    )
                }
                
                if (habits.isEmpty()) {
                    // Empty state UI - following Material 3 guidelines
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = if (searchExpanded) 0.dp else 8.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "No habits",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No habits match your search" else "No habits yet",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try different search terms" else "Start building your routines by adding your first habit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = if (searchExpanded) 0.dp else 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(habits) { habit ->
                            HabitCard(
                                habit = habit,
                                onToggle = { habitId ->
                                    if (habit.completedToday) {
                                        viewModel.markHabitAsIncomplete(habitId)
                                    } else {
                                        viewModel.markHabitAsCompleted(habitId)
                                    }
                                },
                                onLongClick = { habitId ->
                                    viewModel.deleteHabit(habitId)
                                },
                                onNavigateToDetail = { habitId ->
                                    navController.navigate("habitDetail/$habitId")
                                },
                                onEdit = { editedHabit ->
                                    editingHabit = editedHabit
                                }
                            )
                        }
                    }
                }
            }
        }
        }
    }
    
    // Add Habit Modal
    if (showAddHabitDialog) {
        AddHabitModal(
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { title, frequency, customDays, details ->
                val customInterval = customDays.toIntOrNull() ?: 1
                viewModel.addHabit(title, frequency, "", "", details, customInterval)
                showAddHabitDialog = false
            }
        )
    }
    
    // Edit Habit Modal
    if (editingHabit != null) {
        EditHabitModal(
            habit = editingHabit!!,  // Safe to use !! because we checked for null
            onDismiss = { editingHabit = null },
            onConfirm = { title, frequency, customDays, details ->
                val customInterval = customDays.toIntOrNull() ?: 1
                viewModel.updateHabit(
                    editingHabit!!.id,
                    title,
                    frequency,
                    editingHabit!!.icon,
                    editingHabit!!.color,
                    details,
                    customInterval
                )
                editingHabit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitModal(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var habitName by remember { mutableStateOf(habit.title) }
    var frequency by remember { mutableStateOf(
        if (habit.frequency.contains("Every") && habit.frequency.contains("days")) "Custom" else habit.frequency
    ) }
    var customDays by remember { mutableStateOf(
        if (habit.frequency.contains("Every") && habit.frequency.contains("days")) {
            habit.frequency.replace("Every ", "").replace(" days", "")
        } else {
            habit.customInterval.toString()
        }
    ) }
    var details by remember { mutableStateOf(habit.details) }
    var expanded by remember { mutableStateOf(false) }
    
    val frequencies = listOf("Daily", "Weekly", "Custom")
    val maxDetailsLength = 100
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Habit") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g., Drink 8 glasses of water") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq
                                    expanded = false
                                    if (freq != "Custom") {
                                        customDays = ""
                                    }
                                }
                            )
                        }
                    }
                }
                
                if (frequency == "Custom") {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { newValue ->
                            // Only allow numeric input
                            if (newValue.all { it.isDigit() }) {
                                customDays = newValue
                            }
                        },
                        label = { Text("Interval (days)") },
                        placeholder = { Text("Enter number of days") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (habitName.isNotBlank()) {
                        val freq = if (frequency == "Custom" && customDays.isNotBlank()) {
                            "Every ${customDays} days"
                        } else {
                            frequency
                        }
                        val customInterval = if (frequency == "Custom" && customDays.isNotBlank()) {
                            customDays.toIntOrNull() ?: 1
                        } else {
                            when (frequency) {
                                "Daily" -> 1
                                "Weekly" -> 7
                                else -> 1
                            }
                        }
                        onConfirm(habitName, freq, customInterval.toString(), details)
                        habitName = ""
                        frequency = "Daily"
                        customDays = ""
                    }
                },
                enabled = habitName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    habitName = ""
                    frequency = "Daily"
                    customDays = ""
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
