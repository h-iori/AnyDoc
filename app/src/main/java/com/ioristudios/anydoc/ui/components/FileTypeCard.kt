package com.ioristudios.anydoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ioristudios.anydoc.ui.theme.rememberAppSizes
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@Composable
fun FileTypeCard(
    title: String,
    fileNameForIcon: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null
) {
    val spacing = rememberAppSpacing()
    val sizes = rememberAppSizes()
    val visual = FileTypeIconRegistry.resolveFileVisual(fileNameForIcon)
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    Surface(
        modifier = modifier
            .heightIn(min = sizes.fileCardMinHeight)
            .then(if (onClick != null) Modifier.clickable { 
                haptics.performHapticFeedback()
                onClick() 
            } else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = visual.containerColor.copy(alpha = 0.35f),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, visual.borderColor, MaterialTheme.shapes.medium)
                .padding(spacing.cardPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = horizontalAlignment
        ) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 10.dp, shape = MaterialTheme.shapes.small, ambientColor = visual.glowColor, spotColor = visual.glowColor)
                    .size(sizes.fileIconContainer)
                    .background(visual.containerColor, MaterialTheme.shapes.small)
                    .border(1.dp, visual.borderColor, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = visual.iconRes),
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(sizes.fileIcon)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = visual.accentColor
            )
        }
    }
}
