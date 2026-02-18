package com.example.smartlifedashboard.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlifedashboard.util.FavoriteQuote
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.FavoritesViewModel
import com.example.smartlifedashboard.util.Quote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    
    Log.d("FavoritesScreen", "UI State changed - quotes count: ${uiState.favoriteQuotes.size}, isLoading: ${uiState.isLoading}, error: ${uiState.error}")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Quotes") },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
        snackbarHost = {
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { 
                            Log.d("FavoritesScreen", "Dismiss error button clicked")
                            viewModel.clearError() 
                        }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error ?: "")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.isLoading) {
                // Loading state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading favorites...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.favoriteQuotes.isEmpty()) {
                // Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "No favorites",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorite quotes yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the heart icon on quotes to save them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Display favorite quotes
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    items(uiState.favoriteQuotes) { favoriteQuote ->
                        FavoriteQuoteItem(
                            favoriteQuote = favoriteQuote,
                            onRemove = { viewModel.removeFavorite(favoriteQuote) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FavoriteQuoteItem(
    favoriteQuote: FavoriteQuote,
    onRemove: () -> Unit
) {
    Log.d("FavoritesScreen", "Displaying favorite quote item: ${favoriteQuote.quote.text}")
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "\"${favoriteQuote.quote.text}\"",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontStyle = FontStyle.Italic
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.9f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— ${favoriteQuote.quote.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from favorites",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}