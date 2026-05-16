package com.ioristudios.anydoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.LogoTextStyle
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

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
            .background(AppColors.SurfaceBase.copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(horizontal = spacing.screenPadding, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = LogoTextStyle,
                color = AppColors.BrandStrong
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

