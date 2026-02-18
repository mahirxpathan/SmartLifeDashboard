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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import com.example.smartlifedashboard.model.EventCountdown
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.EventCountdownViewModel
import com.example.smartlifedashboard.viewmodel.EventSortOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCountdownScreen(
    navController: NavController,
    viewModel: EventCountdownViewModel = hiltViewModel()
) {
    val events by viewModel.filteredEvents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<String?>(null) }
    
    var searchExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Event Countdown") },
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
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort events",
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
                            title = { Text("Sort Events By") },
                            text = {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sortOrder == EventSortOrder.SOONEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(EventSortOrder.SOONEST_FIRST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Soonest First",
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
                                            selected = sortOrder == EventSortOrder.FARTHEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(EventSortOrder.FARTHEST_FIRST)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Farthest First",
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
                                            selected = sortOrder == EventSortOrder.TITLE_A_TO_Z,
                                            onClick = {
                                                viewModel.setSortOrder(EventSortOrder.TITLE_A_TO_Z)
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
                                            selected = sortOrder == EventSortOrder.TITLE_Z_TO_A,
                                            onClick = {
                                                viewModel.setSortOrder(EventSortOrder.TITLE_Z_TO_A)
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
                                            selected = sortOrder == EventSortOrder.NEWEST_ADDED,
                                            onClick = {
                                                viewModel.setSortOrder(EventSortOrder.NEWEST_ADDED)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Newest Added",
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
                                            selected = sortOrder == EventSortOrder.OLDEST_ADDED,
                                            onClick = {
                                                viewModel.setSortOrder(EventSortOrder.OLDEST_ADDED)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Oldest Added",
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
            ExtendedFloatingActionButton(
                onClick = { 
                    showAddEventDialog = true
                },
                icon = { Icon(Icons.Default.Add, "Add event") },
                text = { Text("Add Event") },
            )
        }
    ) { paddingValues ->
        if (isLoading && events.isEmpty()) {
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
                    text = "Loading events...",
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
                        placeholder = { Text("Search events...") },
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
                
                if (events.isEmpty()) {
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
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "No events",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No events match your search" else "No events yet",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try different search terms" else "Count down to important events by adding your first countdown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Event list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = if (searchExpanded) 0.dp else 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(events) { event ->
                            EventCountdownCard(
                                event = event,
                                onDelete = { eventId ->
                                    eventToDelete = eventId
                                    showDeleteDialog = true
                                },
                                onEdit = { editedEvent ->
                                    // Pass the edit event to the card's internal state
                                    // The edit functionality is handled inside the card
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Event Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, eventDate, description ->
                viewModel.addEvent(title, eventDate, description)
                showAddEventDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                eventToDelete = null
            },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        eventToDelete?.let { eventId ->
                            viewModel.deleteEvent(eventId)
                        }
                        showDeleteDialog = false
                        eventToDelete = null
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
                        eventToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
}

@Composable
fun EventCountdownCard(
    event: EventCountdown,
    onDelete: (String) -> Unit,
    onEdit: (EventCountdown) -> Unit,
    viewModel: EventCountdownViewModel
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    val daysRemaining = viewModel.calculateDaysRemaining(event.eventDate)
    val eventDateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(event.eventDate)
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            // Left side - Event info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Countdown display
                Text(
                    text = if (daysRemaining > 0) {
                        "$daysRemaining days remaining"
                    } else if (daysRemaining == 0) {
                        "Today!"
                    } else {
                        "${-daysRemaining} days ago"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (daysRemaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else if (daysRemaining == 0) {
                        MaterialTheme.colorScheme.primary
                    } else if (daysRemaining <= 7) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Check if time was intentionally set or if it's just the default time
                val calendar = java.util.Calendar.getInstance()
                calendar.time = event.eventDate
                val hours = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val minutes = calendar.get(java.util.Calendar.MINUTE)
                val seconds = calendar.get(java.util.Calendar.SECOND)
                
                val eventTimeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(event.eventDate)
                Text(
                    text = if (hours == 0 && minutes == 0 && seconds == 0) {
                        "Date: $eventDateString"
                    } else {
                        "Date: $eventDateString | Time: $eventTimeString"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (event.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Right side - More menu
            Box {
                var expanded by remember { mutableStateOf(false) }
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
                            showEditDialog = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showDeleteDialog = true
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
    
    // Edit Event Dialog
    if (showEditDialog) {
        EditEventDialog(
            event = event,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, eventDate, description ->
                // Update the event using the viewModel
                val updatedEvent = event.copy(
                    title = title,
                    eventDate = eventDate,
                    description = description
                )
                viewModel.updateEvent(event.id, updatedEvent)
                showEditDialog = false
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(event.id)
                        showDeleteDialog = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: EventCountdown,
    onDismiss: () -> Unit,
    onConfirm: (String, Date, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(event.title) }
    var description by rememberSaveable { mutableStateOf(event.description) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(event.eventDate) }
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = java.util.Calendar.getInstance().apply { time = event.eventDate }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(
        if (calendar.get(java.util.Calendar.HOUR_OF_DAY) != 0 || calendar.get(java.util.Calendar.MINUTE) != 0) {
            Pair(calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
        } else {
            null
        }
    ) }
    var titleHasBeenEdited by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleHasBeenEdited = true
                    },
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = title.isBlank() && titleHasBeenEdited
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Time selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTime != null) {
                            "Time: ${String.format("%02d:%02d", selectedTime!!.first, selectedTime!!.second)}"
                        } else {
                            "Select event time"
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
                
                // Date selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate != null) {
                            "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!)}"
                        } else {
                            "Select event date"
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && (selectedDate != null || selectedTime != null)) {
                        // Handle different combinations of date and time
                        val finalEventDate = when {
                            selectedDate != null && selectedTime != null -> {
                                // Both date and time selected
                                val calendar = java.util.Calendar.getInstance().apply {
                                    time = selectedDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                    set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.time
                            }
                            selectedTime != null -> {
                                // Only time selected - create today's date with selected time
                                val calendar = java.util.Calendar.getInstance()
                                calendar.set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                calendar.set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                calendar.set(java.util.Calendar.SECOND, 0)
                                calendar.set(java.util.Calendar.MILLISECOND, 0)
                                calendar.time
                            }
                            else -> {
                                // Only date selected - ensure time is set to midnight
                                val calendar = java.util.Calendar.getInstance().apply {
                                    time = selectedDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.time
                            }
                        }
                        onConfirm(title, finalEventDate, description)
                    }
                },
                enabled = title.isNotBlank() && (selectedDate != null || selectedTime != null)
            ) {
                Text("Update Event")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Date, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var titleHasBeenEdited by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleHasBeenEdited = true
                    },
                    label = { Text("Event Title") },
                    placeholder = { Text("e.g., Birthday Party") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = title.isBlank() && titleHasBeenEdited
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add details about your event...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Time selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTime != null) {
                            "Time: ${String.format("%02d:%02d", selectedTime!!.first, selectedTime!!.second)}"
                        } else {
                            "Select event time"
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
                
                // Date selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate != null) {
                            "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!)}"
                        } else {
                            "Select event date"
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && (selectedDate != null || selectedTime != null)) {
                        // Handle different combinations of date and time
                        val finalEventDate = when {
                            selectedDate != null && selectedTime != null -> {
                                // Both date and time selected
                                val calendar = java.util.Calendar.getInstance().apply {
                                    time = selectedDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                    set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.time
                            }
                            selectedTime != null -> {
                                // Only time selected - create today's date with selected time
                                val calendar = java.util.Calendar.getInstance()
                                calendar.set(java.util.Calendar.HOUR_OF_DAY, selectedTime!!.first)
                                calendar.set(java.util.Calendar.MINUTE, selectedTime!!.second)
                                calendar.set(java.util.Calendar.SECOND, 0)
                                calendar.set(java.util.Calendar.MILLISECOND, 0)
                                calendar.time
                            }
                            else -> {
                                // Only date selected - ensure time is set to midnight
                                val calendar = java.util.Calendar.getInstance().apply {
                                    time = selectedDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.time
                            }
                        }
                        onConfirm(title, finalEventDate, description)
                    }
                },
                enabled = title.isNotBlank() && (selectedDate != null || selectedTime != null)
            ) {
                Text("Add Event")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
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
}
