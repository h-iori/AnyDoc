package com.ioristudios.anydoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

private val OrbitronFamily = FontFamily(Font(R.font.orbitron, FontWeight.Bold))
private val NeonHeaderSurface = Color(0xFF0A0A0F)
private val NeonPurple = Color(0xFFBF00FF)
private val NeonPurpleGlow = Color(0xFF9B30FF)
private val NeonPurpleLight = Color(0xFFD455FF)
private val CoreWhite = Color(0xFFFFFFFF)
private val CoreWhiteDim = Color(0xFFEEEEFF)

@Composable
fun TopAppBar(
    title: String = "AnyDoc",
    subtitle: String = "by IORI STUDIOS",
    onMenuClick: () -> Unit = {}
) {
    val spacing = rememberAppSpacing()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonHeaderSurface.copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(horizontal = spacing.screenPadding, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Box {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.4.sp,
                        shadow = Shadow(
                            color = NeonPurpleGlow.copy(alpha = 0.9f),
                            blurRadius = 44f
                        )
                    ),
                    color = NeonPurple.copy(alpha = 0.24f),
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.4.sp,
                        shadow = Shadow(
                            color = NeonPurple.copy(alpha = 0.95f),
                            blurRadius = 32f
                        )
                    ),
                    color = NeonPurple.copy(alpha = 0.14f),
                    modifier = Modifier.offset(x = (-1).dp, y = 1.dp)
                )
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.4.sp,
                        shadow = Shadow(
                            color = NeonPurple.copy(alpha = 0.75f),
                            blurRadius = 24f
                        )
                    ),
                    color = CoreWhite
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.1.sp,
                    shadow = Shadow(
                        color = NeonPurple.copy(alpha = 0.55f),
                        blurRadius = 12f
                    )
                ),
                color = NeonPurpleLight
            )
        }
        val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
        IconButton(onClick = {
            haptics.performHapticFeedback()
            onMenuClick()
        }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = CoreWhiteDim
            )
        }
    }
}
