package com.ioristudios.anydoc.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AboutScreen(onBack: () -> Unit) {
    // ── Staggered entrance fade-in flags ─────────────────────────────────────
    var avatarVisible by remember { mutableStateOf(false) }
    var nameVisible by remember { mutableStateOf(false) }
    var bioVisible by remember { mutableStateOf(false) }
    var contactVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); avatarVisible = true
        delay(300); nameVisible = true
        delay(300); bioVisible = true
        delay(200); contactVisible = true
    }

    // ── Continuous infinite animations ────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "about")

    val ringRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.25f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val avatarScale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "avatarScale"
    )
    val particleFloat by infinite.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "particle"
    )

    // ── Fade-in alpha for each section ────────────────────────────────────────
    val avatarAlpha by animateFloatAsState(
        targetValue = if (avatarVisible) 1f else 0f,
        animationSpec = tween(600), label = "aA"
    )
    val nameAlpha by animateFloatAsState(
        targetValue = if (nameVisible) 1f else 0f,
        animationSpec = tween(600), label = "nA"
    )
    val bioAlpha by animateFloatAsState(
        targetValue = if (bioVisible) 1f else 0f,
        animationSpec = tween(700), label = "bA"
    )
    val contactAlpha by animateFloatAsState(
        targetValue = if (contactVisible) 1f else 0f,
        animationSpec = tween(700), label = "cA"
    )

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            "About",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Meet the developer.",
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Developer avatar with animated rings ─────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .alpha(avatarAlpha)
                    .scale(avatarScale)
            ) {
                // Rotating arc ring
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(ringRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                NeonPurpleAbout.copy(alpha = 0f),
                                NeonPurpleAbout.copy(alpha = 0.9f),
                                NeonPurpleAbout.copy(alpha = 0f)
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }

                // Pulsing glow ring
                Canvas(modifier = Modifier.size(136.dp)) {
                    drawCircle(
                        color = NeonPurpleGlow.copy(alpha = glowAlpha * 0.25f),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 22f)
                    )
                }

                // Floating orbital dots
                Canvas(modifier = Modifier.size(160.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val orbitRadius = size.minDimension / 2f - 8f
                    val dotColors = listOf(NeonPurpleAbout, SuccessGreenAbout, NeonPurpleLight, NeonPurpleGlow)
                    val baseAngles = listOf(0f, 90f, 180f, 270f)
                    baseAngles.forEachIndexed { i, baseAngle ->
                        val angle = baseAngle + ringRotation * 0.4f
                        val rad = Math.toRadians(angle.toDouble())
                        val floatOffset = if (i % 2 == 0) particleFloat else -particleFloat
                        val px = cx + orbitRadius * cos(rad).toFloat()
                        val py = cy + orbitRadius * sin(rad).toFloat() + floatOffset * 0.2f
                        drawCircle(
                            color = dotColors[i],
                            radius = 5.5f,
                            center = Offset(px, py)
                        )
                    }
                }

                // Developer photo circle
                Surface(
                    shape = CircleShape,
                    color = SurfaceDark,
                    border = BorderStroke(3.dp, NeonPurpleAbout.copy(alpha = 0.6f)),
                    modifier = Modifier.size(118.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.background(
                            Brush.radialGradient(
                                listOf(
                                    SurfaceDarkElevated,
                                    SurfaceDark
                                )
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

            // ── Name & title ─────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(nameAlpha)
            ) {
                Text(
                    "Harsh Swatantra Upadhyay",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "AI Engineer • Tech Enthusiast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonPurpleAbout,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "📍 Mumbai, India",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // ── Bio ──────────────────────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = SurfaceDarkCard,
                border = BorderStroke(1.dp, NeonPurpleSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(bioAlpha)
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
                            "About Me",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "I am an AI Engineer and Tech Enthusiast based in Mumbai, India. " +
                            "AnyDoc was independently designed and developed end to end by me, " +
                            "with a focus on high-performance document processing, clean architecture, and a polished user experience. " +
                            "I build practical, production-minded software with a strong emphasis on purpose, precision, and quality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }


            // ── App info ─────────────────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = SurfaceDarkCard,
                border = BorderStroke(1.dp, NeonPurpleSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contactAlpha)
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
                            "App Info",
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

            Text(
                "Built independently with 💪 purpose",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(contactAlpha)
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
            value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
