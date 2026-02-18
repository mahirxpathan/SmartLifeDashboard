package com.example.smartlifedashboard.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.smartlifedashboard.R

// Profile image placeholders using custom JPG avatar resources
@Composable
fun ProfileImagePlaceholder(imageId: String, isSelected: Boolean = false, size: androidx.compose.ui.unit.Dp = 60.dp) {
    val avatarResource = when (imageId) {
        "avatar1" -> R.drawable.avatar_1
        "avatar2" -> R.drawable.avatar_2
        "avatar3" -> R.drawable.avatar_3
        "avatar4" -> R.drawable.avatar_4
        "avatar5" -> R.drawable.avatar_5
        else -> R.drawable.avatar_1
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = avatarResource),
            contentDescription = "Profile Avatar",
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
    }
}