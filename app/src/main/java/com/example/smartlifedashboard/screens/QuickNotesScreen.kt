package com.example.smartlifedashboard.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.QuickNotesViewModel
import com.example.smartlifedashboard.viewmodel.SortOrder
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotesScreen(
    navController: NavController,
    viewModel: QuickNotesViewModel = viewModel()
) {
    val notes = viewModel.filteredNotes
    val errorMessage = viewModel.errorMessage
    val isLoading = viewModel.isLoading.value
    val currentUser = FirebaseAuth.getInstance().currentUser
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    
    // Search bar state
    var searchExpanded by remember { mutableStateOf(false) }
    val searchQuery = viewModel.searchQueryValue

    
    // Reset ViewModel state when user changes
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.resetState()
            viewModel.forceRefresh()
        }
    }
    
    // Load notes when screen is composed if user is already authenticated
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            viewModel.loadNotesFromFirestore()
        }
    }

    // Function to confirm note deletion
    fun confirmDelete(noteId: String) {
        noteToDelete = noteId
        showDeleteDialog = true
    }

    // Function to actually delete the note
    fun deleteNote() {
        viewModel.deleteNote(noteToDelete)
        showDeleteDialog = false
    }

    // Function to navigate to detail screen for adding a new note
    fun addNote() {
        navController.navigate("note_detail")
        showAddNoteDialog = false
    }
    


    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    // Show error message if there is one
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarMessage = errorMessage
            showSnackbar = true
            viewModel.clearErrorMessage()
        }
    }
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Quick Notes") },
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
                            contentDescription = "Sort notes"
                        )
                    }
                    
                    // Sort options dialog
                    if (showSortDialog) {
                        AlertDialog(
                            onDismissRequest = { showSortDialog = false },
                            title = { Text("Sort Notes By") },
                            text = {
                                Column {
                                    val currentSortOrder = viewModel.getSortOrder()
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentSortOrder == SortOrder.NEWEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(SortOrder.NEWEST_FIRST)
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
                                            selected = currentSortOrder == SortOrder.OLDEST_FIRST,
                                            onClick = {
                                                viewModel.setSortOrder(SortOrder.OLDEST_FIRST)
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
                                            selected = currentSortOrder == SortOrder.TITLE_A_TO_Z,
                                            onClick = {
                                                viewModel.setSortOrder(SortOrder.TITLE_A_TO_Z)
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
                                            selected = currentSortOrder == SortOrder.TITLE_Z_TO_A,
                                            onClick = {
                                                viewModel.setSortOrder(SortOrder.TITLE_Z_TO_A)
                                                showSortDialog = false
                                            }
                                        )
                                        Text(
                                            text = "Title Z-A",
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
                    // Directly navigate to the note detail screen instead of showing dialog
                    navController.navigate("note_detail")
                },
                icon = { Icon(Icons.Default.Add, "Add note") },
                text = { Text("Add Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(
                            onClick = { showSnackbar = false }
                        ) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
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
                    placeholder = { Text("Search Quick Notes...") },
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
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
        ) {
            // Display saved notes
            // Show loading state
            if (isLoading) {
                // Show loading/syncing animation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Simple text indicator for now - could be replaced with a proper loading animation
                    Text(
                        text = "Loading notes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Show empty state for when no notes exist
            else if (notes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "No notes",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching notes" else "No notes yet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to create your first note",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                            )
                        }
                    }
                }
            }
            // Show notes list for existing users with data
            else {
                AnimatedVisibility(
                    visible = notes.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(notes) { index, note ->
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(initialAlpha = 0.3f) + scaleIn(initialScale = 0.9f),
                                exit = fadeOut(targetAlpha = 0.3f) + scaleOut(targetScale = 0.9f)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            navController.navigate("note_detail/${note.id}")
                                        },
                                        
                                        // Removed double tap since single tap now opens the note
                                        // Double tap functionality is no longer needed
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                    ) {
                                        // Title
                                        if (note.title.isNotBlank()) {
                                            Text(
                                                text = note.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        
                                        // Content preview - show only first 2 lines
                                        if (note.content.isNotBlank()) {
                                            val lines = note.content.split("\n")
                                            val previewText = if (lines.size > 2) {
                                                "${lines.take(2).joinToString("\n")}..."
                                            } else {
                                                note.content
                                            }
                                            Text(
                                                text = previewText,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .padding(bottom = 12.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Timestamp
                                            Text(
                                                text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            
                                            // Delete button
                                            IconButton(
                                                onClick = { confirmDelete(note.id) },
                                                modifier = Modifier
                                                    .size(24.dp),
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete note",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Divider between notes
                                HorizontalDivider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            text = "Delete Note",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to delete this note?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { deleteNote() }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Add Note Dialog - just shows the choice to add a note
            if (showAddNoteDialog) {
                AlertDialog(
                    onDismissRequest = { showAddNoteDialog = false },
                    title = {
                        Text(
                            text = "Add New Note",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Text("Would you like to create a new note?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { addNote() }
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAddNoteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            

        }
    }
}
}