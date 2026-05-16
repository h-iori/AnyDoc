package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.*


@Composable
fun AppSidebar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAboutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { onDismiss() }
            )
        }

        // Sidebar Content
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                color = SurfaceDarkSheet,
                tonalElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(NeonPurpleSubtle, Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Profile Header
                    SidebarHeader()

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = NeonPurpleFaint
                    )

                    // 2. Navigation Items (Staggered)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Items space (currently only About)
                    }

                    // 3. Footer / Bottom Items

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StaggeredSidebarItem(
                            index = 2,
                            visible = isVisible,
                            icon = Icons.Filled.Info,
                            label = "About Developer",
                            onClick = {
                                onAboutClick()
                            }
                        )

                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Version 1.1.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(top = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NeonPurpleSubtle)
                .border(2.dp, NeonPurple, CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.dev_profile),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Harsh Swatantra Upadhyay",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AI Engineer • IORI STUDIOS",
            style = MaterialTheme.typography.bodySmall,
            color = NeonPurple,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StaggeredSidebarItem(
    index: Int,
    visible: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val animAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 100 + (index * 50)),
        label = "itemAlpha"
    )
    
    val animTranslationX by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = tween(durationMillis = 400, delayMillis = 100 + (index * 50)),
        label = "itemSlide"
    )

    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animAlpha
                translationX = animTranslationX
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable { 
                haptics.performHapticFeedback()
                onClick() 
            }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = NeonPurpleFaint
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
