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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun FileTypeCard(
    title: String,
    fileNameForIcon: String,
    modifier: Modifier = Modifier,
    fileCount: Int? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null
) {
    val visual = FileTypeIconRegistry.resolveFileVisual(fileNameForIcon)
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    Surface(
        modifier = modifier
            .heightIn(min = 80.dp)
            .then(if (onClick != null) Modifier.clickable { 
                haptics.performHapticFeedback()
                onClick() 
            } else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = visual.containerColor.copy(alpha = 0.35f),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, visual.borderColor, MaterialTheme.shapes.medium)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = visual.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.2f)
                    .padding(16.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = horizontalAlignment
            ) {
                Spacer(modifier = Modifier.heightIn(min = 24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = visual.accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (fileCount != null) {
                        if (fileCount == 1) "1 file" else "$fileCount files"
                    } else "Loading...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
