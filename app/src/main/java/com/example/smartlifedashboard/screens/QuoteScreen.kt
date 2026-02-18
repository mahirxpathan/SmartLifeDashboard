package com.example.smartlifedashboard.screens

import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlifedashboard.util.Quote
import com.example.smartlifedashboard.util.NavigationDebouncer
import com.example.smartlifedashboard.viewmodel.QuoteViewModel
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(navController: NavController, viewModel: QuoteViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navDebouncer = remember { NavigationDebouncer() }
    val coroutineScope = rememberCoroutineScope()
    
    // Log UI state changes for debugging
    LaunchedEffect(uiState) {
        Log.d("QuoteScreen", "UI State changed - quote: ${uiState.quote?.text}, isLoading: ${uiState.isLoading}, isRefreshing: ${uiState.isRefreshing}, error: ${uiState.error}")
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (uiState.quote != null && !uiState.isLoading) 1f else 0f, 
        animationSpec = tween(500), 
        label = "quoteAlpha"
    )
    
    val favoriteScale by animateFloatAsState(
        targetValue = if (uiState.isFavorite) 1.2f else 1f, 
        animationSpec = tween(200), 
        label = "favoriteScale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (uiState.isRefreshing) 360f else 0f, 
        animationSpec = tween(1000), 
        label = "refreshRotation"
    )
    
    val context = LocalContext.current
    
    // Load initial quote
    LaunchedEffect(Unit) {
        Log.d("QuoteScreen", "Loading initial quote")
        Log.d("QuoteScreen", "Calling viewModel.loadQuote()")
        viewModel.loadQuote()
        Log.d("QuoteScreen", "Finished calling viewModel.loadQuote()")
    }
    
    // Log when refresh happens
    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing) {
            Log.d("QuoteScreen", "UI State - Refresh started")
        } else {
            Log.d("QuoteScreen", "UI State - Refresh ended")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote of the Day") },
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
                            Log.d("QuoteScreen", "Dismiss error button clicked")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with visual accent centered
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (uiState.isLoading || uiState.isRefreshing) {
                        // Loading state with shimmer animation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Animated loading dots
                            Row {
                                repeat(3) { index ->
                                    val currentTime = System.currentTimeMillis()
                                    val pulseScale = if (currentTime / 300 % 3 == index.toLong()) 1.5f else 1f
                                    Text(
                                        text = ".",
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier
                                            .scale(pulseScale)
                                            .alpha(
                                                if (currentTime / 300 % 3 == index.toLong()) 1f else 0.3f
                                            )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Fetching inspiration...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        uiState.quote?.let { quote ->
                            Text(
                                text = "\"${quote.text}\"",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(alpha),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "— ${quote.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } ?: run {
                            Text(
                                text = "Loading quote...",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(alpha),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Card footer with share, refresh, and favorite buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share button (icon only) on the left
                        IconButton(
                            onClick = { 
                                Log.d("QuoteScreen", "Share button clicked")
                                uiState.quote?.let { quote ->
                                    Log.d("QuoteScreen", "Sharing quote: ${quote.text}")
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "\"${quote.text}\"\n\n— ${quote.author}\n\nShared from Smart Life Dashboard")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Refresh button (icon only) in the center
                        IconButton(
                            onClick = { 
                                Log.d("QuoteScreen", "Refresh button clicked")
                                Log.d("QuoteScreen", "Current quote before refresh: ${uiState.quote?.text}")
                                Log.d("QuoteScreen", "Calling viewModel.refreshQuote()")
                                viewModel.refreshQuote()
                                Log.d("QuoteScreen", "Finished calling viewModel.refreshQuote()")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Quote",
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(rotation),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Favorite button (icon only) on the right
                        IconButton(
                            onClick = { 
                                Log.d("QuoteScreen", "Favorite button clicked, current favorite status: ${uiState.isFavorite}")
                                Log.d("QuoteScreen", "Current quote: ${uiState.quote?.text}")
                                Log.d("QuoteScreen", "Calling viewModel.toggleFavorite()")
                                viewModel.toggleFavorite()
                                Log.d("QuoteScreen", "Finished calling viewModel.toggleFavorite()")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(favoriteScale)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Favorites button outside the card
            Button(
                onClick = { 
                    Log.d("QuoteScreen", "Navigate to favorites button clicked")
                    navController.navigate("favorites")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "View Favorites",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Favorite Quotes")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    

}