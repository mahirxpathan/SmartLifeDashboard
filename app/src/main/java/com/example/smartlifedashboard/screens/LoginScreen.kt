package com.example.smartlifedashboard.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartlifedashboard.R
import com.example.smartlifedashboard.ui.theme.GoogleBlue
import com.example.smartlifedashboard.util.FirebaseUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    val logoScale = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutQuart)
    )
    val contentAlpha = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200)
    )
    val buttonElevation = animateFloatAsState(
        targetValue = if (isLoading) 8f else 4f,
        animationSpec = tween(durationMillis = 300)
    )
    
    // Trigger entrance animation
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Get the web client ID from google-services.json
    val webClientId = FirebaseUtil.getWebClientId(context) ?: "1024626669966-3d8pj5lac962s3c990rod311ga8ibp0g.apps.googleusercontent.com"
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    
    val googleSignInClient = GoogleSignIn.getClient(context, gso)
    
    // Launcher for Google Sign-In intent
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        isLoading = false
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account: GoogleSignInAccount = task.getResult()
                if (account != null) {
                    Log.d("LoginScreen", "Successfully retrieved Google account: ${account.email}")
                    account.idToken?.let { idToken ->
                        firebaseAuthWithGoogle(idToken, auth, {
                            onNavigateToDashboard()
                        }, { errorMsg ->
                            errorMessage = errorMsg
                        })
                    }
                }
            } catch (e: ApiException) {
                Log.w("LoginScreen", "Google sign in failed", e)
                errorMessage = "Failed to sign in with Google: ${e.message}"
            }
        } else {
            errorMessage = "Sign in was cancelled"
        }
    }

    // Animated background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f)
        )
    )
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            // Floating decorative elements
            FloatingDecorations()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer(alpha = contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            AnimatedLogo(
                scale = logoScale.value,
                isLoading = isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Smart Life Dashboard",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Your world, organized.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Enhanced Google Sign-In Button
            EnhancedGoogleSignInButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                enabled = !isLoading,
                isLoading = isLoading,
                elevation = buttonElevation.value
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            }
        }
    }
}

@Composable
fun EnhancedGoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    elevation: Float = 4f
) {
    val buttonScale = animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = tween(durationMillis = 200)
    )
    
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer(scaleX = buttonScale.value, scaleY = buttonScale.value),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = elevation.dp,
            pressedElevation = 12.dp,
            disabledElevation = 2.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoogleBlue,
            disabledContainerColor = GoogleBlue.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            LoadingIndicator()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Bold,
                        color = GoogleBlue,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Continue with Google",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AnimatedLogo(scale: Float, isLoading: Boolean) {
    val rotation = animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Card(
        modifier = Modifier
            .size(140.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = if (isLoading) rotation.value else 0f
            ),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp 
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_selected),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.4f, scaleY = 1.4f)
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color.White,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Signing in...",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FloatingDecorations() {
    // Floating animated elements for visual interest
    val floatAnim1 = remember { Animatable(0f) }
    val floatAnim2 = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        floatAnim1.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4000
                    0f at 0
                    1f at 2000
                    0f at 4000
                },
                repeatMode = RepeatMode.Restart
            )
        )
    }
    
    LaunchedEffect(Unit) {
        floatAnim2.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 5000
                    0f at 0
                    1f at 2500
                    0f at 5000
                },
                repeatMode = RepeatMode.Restart
            )
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Top right large circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = (40 + floatAnim1.value * 30).dp)
                .graphicsLayer(alpha = 0.08f)
                .background(Color(0xFF4285F4), CircleShape)
        )
        
        // Bottom left large circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = (-(40 + floatAnim2.value * 40)).dp)
                .graphicsLayer(alpha = 0.05f)
                .background(Color(0xFF34A853), CircleShape)
        )
        
        // Center right small circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (10).dp, y = (floatAnim1.value * 50).dp)
                .graphicsLayer(alpha = 0.1f)
                .background(Color(0xFFFABB05), CircleShape)
        )
        
        // Scattered tiny bokeh elements
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size((4 + index * 2).dp)
                    .offset(
                        x = (50 + index * 60).dp,
                        y = (100 + index * 100).dp
                    )
                    .graphicsLayer(alpha = 0.15f)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

private fun firebaseAuthWithGoogle(
    idToken: String,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign in success
                Log.d("LoginScreen", "signInWithCredential:success")
                onSuccess()
            } else {
                // Sign in failed
                Log.w("LoginScreen", "signInWithCredential:failure", task.exception)
                onError("Authentication failed: ${task.exception?.message}")
            }
        }
}
