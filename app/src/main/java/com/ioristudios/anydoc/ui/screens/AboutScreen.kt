package com.ioristudios.anydoc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.NeonPurpleAbout
import com.ioristudios.anydoc.ui.theme.NeonPurpleGlow
import com.ioristudios.anydoc.ui.theme.NeonPurpleLight
import com.ioristudios.anydoc.ui.theme.NeonPurpleSubtle
import com.ioristudios.anydoc.ui.theme.SuccessGreenAbout
import com.ioristudios.anydoc.ui.theme.SurfaceDark
import com.ioristudios.anydoc.ui.theme.SurfaceDarkCard
import com.ioristudios.anydoc.ui.theme.SurfaceDarkElevated
import com.ioristudios.anydoc.ui.theme.TextMuted
import com.ioristudios.anydoc.ui.theme.TextPrimary
import com.ioristudios.anydoc.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AboutScreen(onBack: () -> Unit) {
    var avatarVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        avatarVisible = true
        contentVisible = true
    }

    val infinite = rememberInfiniteTransition(label = "about")
    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val avatarScale by animateFloatAsState(
        targetValue = if (avatarVisible) 1f else 0.94f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "avatarScale"
    )

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
                    IconButton(onClick = {
                        haptics.performHapticFeedback()
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Meet the developer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceDark, SurfaceDarkElevated, SurfaceDark)
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = avatarVisible,
                enter = fadeIn(tween(360, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = tween(360, easing = FastOutSlowInEasing)
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(avatarScale)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(164.dp)
                            .rotate(ringRotation)
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    NeonPurpleAbout.copy(alpha = 0f),
                                    NeonPurpleAbout.copy(alpha = 0.95f),
                                    NeonPurpleAbout.copy(alpha = 0f)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 235f,
                            useCenter = false,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }

                    Canvas(modifier = Modifier.size(140.dp)) {
                        drawCircle(
                            color = NeonPurpleGlow.copy(alpha = glowAlpha * 0.26f),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 20f)
                        )
                    }

                    Canvas(modifier = Modifier.size(164.dp)) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val orbitRadius = size.minDimension / 2f - 10f
                        val colors = listOf(NeonPurpleAbout, SuccessGreenAbout, NeonPurpleLight, NeonPurpleGlow)
                        val baseAngles = listOf(0f, 90f, 180f, 270f)
                        baseAngles.forEachIndexed { i, base ->
                            val rad = Math.toRadians((base + ringRotation * 0.38f).toDouble())
                            val x = centerX + orbitRadius * cos(rad).toFloat()
                            val y = centerY + orbitRadius * sin(rad).toFloat()
                            drawCircle(color = colors[i], radius = 5f, center = Offset(x, y))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = SurfaceDark,
                        border = BorderStroke(3.dp, NeonPurpleAbout.copy(alpha = 0.62f)),
                        modifier = Modifier.size(118.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(
                                Brush.radialGradient(
                                    listOf(SurfaceDarkElevated, SurfaceDark)
                                )
                            )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dev_profile),
                                contentDescription = "Profile photo of Harsh Swatantra Upadhyay",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(380, delayMillis = 120, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        initialOffsetY = { it / 6 },
                        animationSpec = tween(380, delayMillis = 120, easing = FastOutSlowInEasing)
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Harsh Swatantra Upadhyay",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "AI Engineer - Tech Enthusiast",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonPurpleAbout,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Mumbai, India",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(420, delayMillis = 180, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        initialOffsetY = { it / 7 },
                        animationSpec = tween(420, delayMillis = 180, easing = FastOutSlowInEasing)
                    )
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = SurfaceDarkCard,
                    border = BorderStroke(1.dp, NeonPurpleSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = NeonPurpleAbout,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "About Me",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "I am an AI Engineer and Tech Enthusiast based in Mumbai, India. " +
                                "AnyDoc was independently designed and developed end to end by me, " +
                                "with a focus on high-performance document processing, clean architecture, and a polished user experience. " +
                                "I build practical, production-minded software with a strong emphasis on purpose, precision, and quality.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(420, delayMillis = 240, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        initialOffsetY = { it / 8 },
                        animationSpec = tween(420, delayMillis = 240, easing = FastOutSlowInEasing)
                    )
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = SurfaceDarkCard,
                    border = BorderStroke(1.dp, NeonPurpleSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = NeonPurpleAbout,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "App Info",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        AboutInfoRow("App Name", "AnyDoc")
                        AboutInfoRow("Version", "1.1")
                        AboutInfoRow("Contact", "harshupadhyay9702@gmail.com")
                        AboutInfoRow("GitHub", "https://www.github.com/h-iori")
                    }
                }
            }

            Text(
                text = "Built independently with purpose",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
