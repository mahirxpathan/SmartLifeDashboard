package com.example.smartlifedashboard.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.QuickNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    navController: NavController,
    viewModel: QuickNotesViewModel,
    noteId: String? = null
) {
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Determine if we're in view mode (no edit) or edit mode
    var isInEditMode by remember { mutableStateOf(noteId == null) } // Start in edit mode for new notes, view mode for existing notes
    
    // Load note data when the screen is created
    LaunchedEffect(noteId) {
        if (noteId != null) {
            // Find the note in the ViewModel's notes list
            val note = viewModel.notes.find { it.id == noteId }
            if (note != null) {
                title = note.title
                content = note.content
                category = note.category
                // Ensure we stay in view mode for existing notes
                isInEditMode = false
            } else {
                // If not found in current list, refresh notes and try again
                viewModel.loadNotesFromFirestore()
                // Small delay to allow refresh to complete
                kotlinx.coroutines.delay(100)
                val refreshedNote = viewModel.notes.find { it.id == noteId }
                if (refreshedNote != null) {
                    title = refreshedNote.title
                    content = refreshedNote.content
                    category = refreshedNote.category
                    // Ensure we stay in view mode for existing notes
                    isInEditMode = false
                }
            }
        } else {
            // For new notes, start with empty values
            title = ""
            content = ""
        }
    }
    
    fun saveNote() {
        if (title.isBlank() && content.isBlank()) {
            // Don't save empty notes
            navDebouncer.debouncePopBackStack(navController, coroutineScope)
            return
        }
        
        // Validate character limits
        if (title.length > 25) {
            // Title too long
            return
        }
        if (content.length > 9999) {
            // Content too long
            return
        }
        
        if (noteId != null) {
            // Editing existing note
            viewModel.editNote(noteId, title, content, category)
            // Switch back to view mode after saving
            isInEditMode = false
        } else {
            // Adding new note
            viewModel.addNote(title, content, category)
            navDebouncer.debouncePopBackStack(navController, coroutineScope)
        }
    }
    
    fun deleteNote() {
        if (noteId != null) {
            viewModel.deleteNote(noteId)
            navDebouncer.debouncePopBackStack(navController, coroutineScope)
        }
    }
    
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (isInEditMode) "Edit Note" else if (noteId != null) "Note Details" else "New Note") },
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
                    if (noteId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete note"
                            )
                        }
                    }
                    if (isInEditMode) {
                        // Show save button when in edit mode
                        IconButton(onClick = { saveNote() }) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save note"
                            )
                        }
                    } else {
                        // Show edit button when in view mode
                        IconButton(onClick = { isInEditMode = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit note"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Title field (only shown in edit mode)
            if (isInEditMode) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        if (it.length <= 25) { // Limit title to 25 characters
                            title = it
                        }
                    },
                    label = { Text("Title (optional)") },
                    placeholder = { Text("Enter note title (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    supportingText = {
                        Text("${title.length}/25 characters")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
                )
                
                // Category field (only shown in edit mode)
                OutlinedTextField(
                    value = category,
                    onValueChange = { 
                        category = it
                    },
                    label = { Text("Category (optional)") },
                    placeholder = { Text("Enter category (e.g., Work, Personal, Ideas)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    supportingText = {
                        Text("${category.length}/50 characters")
                    }
                )
            } else {
                // Show title in view mode
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                // Show category in view mode
                if (category.isNotBlank()) {
                    Text(
                        text = "Category: $category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Content field (different appearance in view vs edit mode)
            if (isInEditMode) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { 
                        if (it.length <= 9999) { // Total character limit
                            content = it
                        }
                    },
                    label = { Text("Content") },
                    placeholder = { Text("Start typing your note...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    supportingText = {
                        Text("${content.length}/9999 characters")
                    },
                    maxLines = Int.MAX_VALUE,
                    minLines = 5
                )
            } else {
                // Show content in view mode
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteNote()
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
            }
        )
    }
}