package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.neonGlow

private val OrbitronFamily = FontFamily(Font(R.font.orbitron, FontWeight.Bold))
private val SidebarScrim = Color(0xCC050508)
private val SidebarSurface = Color(0xFF12121A)
private val SidebarEdge = Color(0xFFBF00FF)
private val SidebarGlow = Color(0xFF9B30FF)
private val SidebarText = Color(0xFFEEEEFF)
private val SidebarMuted = Color(0xFF7A7A8E)

@Composable
fun AppSidebar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAboutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SidebarScrim)
                    .clickable { onDismiss() }
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .neonGlow(
                        color = SidebarGlow,
                        radius = 11.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
                    ),
                color = SidebarSurface,
                tonalElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            SidebarEdge.copy(alpha = 0.48f),
                            SidebarGlow.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SidebarHeader()

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = SidebarEdge.copy(alpha = 0.32f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    }

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
                            onClick = { onAboutClick() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Version 1.1.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = SidebarMuted,
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
                .background(SidebarEdge.copy(alpha = 0.18f))
                .border(2.dp, SidebarEdge.copy(alpha = 0.7f), CircleShape)
                .neonGlow(color = SidebarGlow, radius = 8.dp, shape = CircleShape)
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
            color = SidebarText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AI Engineer - IORI STUDIOS",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = OrbitronFamily,
                letterSpacing = 0.6.sp,
                shadow = Shadow(color = SidebarGlow.copy(alpha = 0.6f), blurRadius = 9f)
            ),
            color = Color(0xFFD455FF),
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
            .background(SidebarEdge.copy(alpha = 0.08f))
            .border(1.dp, SidebarEdge.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
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
            color = SidebarEdge.copy(alpha = 0.14f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SidebarEdge,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label,
            color = SidebarText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
