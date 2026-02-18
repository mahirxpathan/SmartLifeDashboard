package com.example.smartlifedashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.smartlifedashboard.model.Task
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.TaskReminderViewModel
import com.example.smartlifedashboard.viewmodel.TaskSortOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReminderScreen(
    navController: NavController,
    viewModel: TaskReminderViewModel = hiltViewModel()
) {
    val tasks by viewModel.filteredTasks.collectAsState()  // Use filtered tasks
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<String?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    
    // Search bar state
    var searchExpanded by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Task Reminders") },
                navigationIcon = {
                    IconButton(onClick = { 
                        navDebouncer.debouncePopBackStack(navController, coroutineScope) 
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                            contentDescription = "Sort tasks"
                        )
                    }
                    
                    IconButton(onClick = { 
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) {
                            viewModel.setSearchQuery("")
                        }
                    }) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchExpanded) "Close search" else "Search"
                        )
                    }
                    
                    // Sort options dialog
                    if (showSortDialog) {
                        AlertDialog(
                            onDismissRequest = { showSortDialog = false },
                            title = { Text("Sort Tasks By") },
                            text = {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == TaskSortOrder.NEWEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(TaskSortOrder.NEWEST_FIRST)
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
                                            selected = sortOrder == TaskSortOrder.OLDEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(TaskSortOrder.OLDEST_FIRST)
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
                                            selected = sortOrder == TaskSortOrder.TITLE_A_TO_Z,
                                            onClick = {
                                                viewModel.setSortOrder(TaskSortOrder.TITLE_A_TO_Z)
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
                                            selected = sortOrder == TaskSortOrder.TITLE_Z_TO_A,
                                            onClick = {
                                                viewModel.setSortOrder(TaskSortOrder.TITLE_Z_TO_A)
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
                                            selected = sortOrder == TaskSortOrder.DUE_DATE_SOONEST,
                                            onClick = {
                                                viewModel.setSortOrder(TaskSortOrder.DUE_DATE_SOONEST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Due Date Soonest",
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
                                            selected = sortOrder == TaskSortOrder.PRIORITY_HIGH_TO_LOW,
                                            onClick = {
                                                viewModel.setSortOrder(TaskSortOrder.PRIORITY_HIGH_TO_LOW)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Priority High to Low",
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    showAddTaskDialog = true
                },
                icon = { Icon(Icons.Default.Add, "Add task") },
                text = { Text("Add Task") },
            )
        }
    ) { paddingValues ->
        if (isLoading && tasks.isEmpty()) {
            // Loading state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading tasks...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar
                if (searchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search tasks...") },
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
                
                // Task list or empty state
                if (tasks.isEmpty()) {
                    // Empty state UI - following Material 3 guidelines
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = if (searchExpanded) 0.dp else 8.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
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
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "No tasks",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No tasks match your search" else "No tasks yet",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try different search terms" else "Stay productive by adding your first task reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    // Task list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = if (searchExpanded) 0.dp else 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tasks) { task ->
                            TaskCard(
                                task = task,
                                onToggle = { taskId, isCompleted ->
                                    viewModel.markTaskAsCompleted(taskId, isCompleted)
                                },
                                onDelete = { taskId ->
                                    viewModel.deleteTask(taskId)
                                },
                                onEdit = { taskBeingEdited ->
                                    // Set the task to be edited
                                    taskToEdit = taskBeingEdited
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, description, dueDate, priority, category ->
                viewModel.addTask(title, description, dueDate, priority, category)
                showAddTaskDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                taskToDelete = null
            },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToDelete?.let { taskId ->
                            viewModel.deleteTask(taskId)
                        }
                        showDeleteDialog = false
                        taskToDelete = null
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
                    onClick = { 
                        showDeleteDialog = false
                        taskToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
    
    // Edit Task Dialog
    val currentTaskToEdit = taskToEdit
    if (currentTaskToEdit != null) {
        EditTaskDialog(
            task = currentTaskToEdit,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedTask ->
                // Update the task using the ViewModel
                viewModel.updateTask(updatedTask)
                taskToEdit = null
            }
        )
    }
    
}

@Composable
fun TaskCard(
    task: Task,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Task) -> Unit
) {
        var showDeleteDialog by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }
    
    ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (task.isCompleted) 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Task info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggle(task.id, it) }
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (task.isCompleted)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Category tag
                            if (task.category.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = task.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }

                            if (task.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (task.isCompleted)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Show due date and/or time based on what was selected
                            task.dueDate?.let { dueDate ->
                                Spacer(modifier = Modifier.height(4.dp))
                                val dateFormat =
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                val timeFormat =
                                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                                val daysUntilDue =
                                    ((dueDate.time - Date().time) / (1000 * 60 * 60 * 24)).toInt()

                                val dueDateColor = when {
                                    daysUntilDue < 0 -> MaterialTheme.colorScheme.error // Overdue
                                    daysUntilDue == 0 -> MaterialTheme.colorScheme.primary // Due today
                                    daysUntilDue <= 3 -> MaterialTheme.colorScheme.secondary // Due soon
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val dueDateText = if (daysUntilDue < 0) {
                                    "Overdue: ${dateFormat.format(dueDate)}"
                                } else if (daysUntilDue == 0) {
                                    "Due today"
                                } else if (daysUntilDue <= 7) {
                                    "Due in $daysUntilDue days"
                                } else {
                                    "Due: ${dateFormat.format(dueDate)}"
                                }

                                // Check if this is a time-only task (created today with specific time)
                                val calendar = java.util.Calendar.getInstance().apply { time = dueDate }
                                val currentCalendar = java.util.Calendar.getInstance()
                                val isTimeOnlyTask = daysUntilDue == 0 && 
                                    calendar.get(java.util.Calendar.HOUR_OF_DAY) != 0 && calendar.get(java.util.Calendar.MINUTE) != 0 && 
                                    (calendar.get(java.util.Calendar.HOUR_OF_DAY) != currentCalendar.get(java.util.Calendar.HOUR_OF_DAY) || 
                                     calendar.get(java.util.Calendar.MINUTE) != currentCalendar.get(java.util.Calendar.MINUTE))
                                
                                // Check if this is a date-only task (time is 12:00 AM or 00:00)
                                val isDateOnlyTask = calendar.get(java.util.Calendar.HOUR_OF_DAY) == 0 && 
                                    calendar.get(java.util.Calendar.MINUTE) == 0

                                // Date display (show for date-only tasks or when date is not today)
                                if (isDateOnlyTask || daysUntilDue != 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Due date",
                                            modifier = Modifier.size(14.dp),
                                            tint = dueDateColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dueDateText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = dueDateColor
                                        )
                                    }
                                    
                                    // Add spacing if we're also showing time
                                    if (!isDateOnlyTask) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }

                                // Time display (show for time-only tasks or when both date and time were selected)
                                if (isTimeOnlyTask || (!isDateOnlyTask && daysUntilDue == 0)) {
                                    if (!isTimeOnlyTask) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "Due time",
                                            modifier = Modifier.size(14.dp),
                                            tint = dueDateColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val timeLabelText = if (daysUntilDue == 0 && !isTimeOnlyTask) "Today" else "Time"
                                        Text(
                                            text = "$timeLabelText: ${timeFormat.format(dueDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = dueDateColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right side - More menu
                Box {
                    IconButton(
                        onClick = { expanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEdit(task)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete(task.id)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    
    
}
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditTaskDialog(
        task: Task,
        onDismiss: () -> Unit,
        onConfirm: (Task) -> Unit
    ) {
        var title by rememberSaveable { mutableStateOf(task.title) }
        var description by rememberSaveable { mutableStateOf(task.description) }
        var priority by rememberSaveable { mutableStateOf(task.priority) }
        var category by rememberSaveable { mutableStateOf(task.category) }
        var selectedDate by remember { mutableStateOf<Date?>(task.dueDate) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(task.dueDate?.let { date -> 
            val calendar = java.util.Calendar.getInstance().apply { time = date }
            calendar.get(java.util.Calendar.HOUR_OF_DAY) to calendar.get(java.util.Calendar.MINUTE)
        }) }
    
        val priorities = listOf("low", "medium", "high")
    
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Task") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = title.isBlank() && title.isNotEmpty() // Show error only when field has been interacted with and is empty
                    )
    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
    
                    // Category selection with predefined options
                    var categoryExpanded by remember { mutableStateOf(false) }
                    val predefinedCategories = listOf(
                        "Work",
                        "Personal",
                        "Shopping",
                        "Health",
                        "Family",
                        "Education",
                        "Finance",
                        "Home",
                        "Other"
                    )
    
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            placeholder = { Text("Select a category") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            }
                        )
    
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = {
                                categoryExpanded = false
                            }
                        ) {
                            predefinedCategories.forEach { predefinedCategory ->
                                DropdownMenuItem(
                                    text = { Text(predefinedCategory) },
                                    onClick = {
                                        category = predefinedCategory
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
    
                    // Priority selection
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = priority,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
    
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false // Close dropdown after selection
                            }
                        ) {
                            priorities.forEach { priorityOption ->
                                DropdownMenuItem(
                                    text = { Text(priorityOption.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        priority = priorityOption
                                        expanded = false // Close dropdown after selection
                                    }
                                )
                            }
                        }
                    }
    
                    // Due date and time selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedDate != null) {
                                    "Due: ${
                                        SimpleDateFormat(
                                            "MMM dd, yyyy",
                                            Locale.getDefault()
                                        ).format(selectedDate!!)
                                    }"
                                } else {
                                    "Select due date (Optional)"
                                },
                                color = if (selectedDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
        
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Pick Date")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTime != null) {
                                    "Time: ${String.format("%02d:%02d", selectedTime!!.first, selectedTime!!.second)}"
                                } else {
                                    "Select time (Optional)"
                                },
                                color = if (selectedTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
        
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Pick Time")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            // Combine date and time, create today's date if only time is selected
                            val finalDueDate = if (selectedDate != null && selectedTime != null) {
                                // Both date and time selected
                                val calendar = java.util.Calendar.getInstance().apply {
                                    time = selectedDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                    set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.time
                            } else if (selectedTime != null) {
                                // Only time selected - create today's date with selected time
                                val calendar = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                    set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.time
                            } else {
                                // Only date or neither selected
                                selectedDate
                            }
                            
                            val updatedTask = task.copy(
                                title = title,
                                description = description,
                                dueDate = finalDueDate,
                                priority = priority,
                                category = category
                            )
                            onConfirm(updatedTask)
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    
        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate?.time
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = Date(millis)
                            }
                            showDatePicker = false 
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState
                )
            }
        }
        
        // Time picker dialog
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = selectedTime?.first ?: 12,
                initialMinute = selectedTime?.second ?: 0
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = {
                    TimePicker(
                        state = timePickerState
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            timePickerState.hour?.let { hour ->
                                timePickerState.minute?.let { minute ->
                                    selectedTime = hour to minute
                                }
                            }
                            showTimePicker = false 
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showTimePicker = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddTaskDialog(
        onDismiss: () -> Unit,
        onConfirm: (String, String, Date?, String, String) -> Unit
    ) {
        var title by rememberSaveable { mutableStateOf("") }
        var description by rememberSaveable { mutableStateOf("") }
        var priority by rememberSaveable { mutableStateOf("medium") }
        var category by rememberSaveable { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<Date?>(null) }
        var showTimePicker by remember { mutableStateOf(false) }
        var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        val priorities = listOf("low", "medium", "high")

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add New Task") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Title") },
                            placeholder = { Text("e.g., Buy groceries") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = title.isBlank() && title.isNotEmpty() // Show error only when field has been interacted with and is empty
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            placeholder = { Text("Add details about your task...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        // Category selection with predefined options
                        var categoryExpanded by remember { mutableStateOf(false) }
                        val predefinedCategories = listOf(
                            "Work",
                            "Personal",
                            "Shopping",
                            "Health",
                            "Family",
                            "Education",
                            "Finance",
                            "Home",
                            "Other"
                        )

                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category") },
                                placeholder = { Text("Select a category") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = {
                                    categoryExpanded = false
                                }
                            ) {
                                predefinedCategories.forEach { predefinedCategory ->
                                    DropdownMenuItem(
                                        text = { Text(predefinedCategory) },
                                        onClick = {
                                            category = predefinedCategory
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Priority selection
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = priority,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Priority") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = {
                                    expanded = false // Close dropdown after selection
                                }
                            ) {
                                priorities.forEach { priorityOption ->
                                    DropdownMenuItem(
                                        text = { Text(priorityOption.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            priority = priorityOption
                                            expanded = false // Close dropdown after selection
                                        }
                                    )
                                }
                            }
                        }

                        // Due time and date selection
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedTime != null) {
                                        "Time: ${
                                            String.format(
                                                "%02d:%02d",
                                                selectedTime!!.first,
                                                selectedTime!!.second
                                            )
                                        }"
                                    } else {
                                        "Select time (Optional)"
                                    },
                                    color = if (selectedTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Text("Pick Time")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedDate != null) {
                                        "Due: ${
                                            SimpleDateFormat(
                                                "MMM dd, yyyy",
                                                Locale.getDefault()
                                            ).format(selectedDate!!)
                                        }"
                                    } else {
                                        "Select due date (Optional)"
                                    },
                                    color = if (selectedDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Text("Pick Date")
                                }
                            }
                        }

                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                // Combine date and time, create today's date if only time is selected
                                val finalDueDate =
                                    if (selectedDate != null && selectedTime != null) {
                                        // Both date and time selected
                                        val calendar = java.util.Calendar.getInstance().apply {
                                            time = selectedDate!!
                                            set(
                                                java.util.Calendar.HOUR_OF_DAY,
                                                selectedTime!!.first
                                            )
                                            set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        calendar.time
                                    } else if (selectedTime != null) {
                                        // Only time selected - create today's date with selected time
                                        val calendar = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                            set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        calendar.time
                                    } else {
                                        // Only date or neither selected
                                        selectedDate
                                    }

                                onConfirm(title, description, finalDueDate, priority, category)
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Add Task")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )

            // Date picker dialog
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate?.time
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedDate = Date(millis)
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDatePicker = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState
                    )
                }
            }

            // Time picker dialog
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = selectedTime?.first ?: 12,
                    initialMinute = selectedTime?.second ?: 0
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Time") },
                    text = {
                        TimePicker(
                            state = timePickerState
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                timePickerState.hour?.let { hour ->
                                    timePickerState.minute?.let { minute ->
                                        selectedTime = hour to minute
                                    }
                                }
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showTimePicker = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
