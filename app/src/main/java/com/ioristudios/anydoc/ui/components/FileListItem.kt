package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ioristudios.anydoc.model.FileItem
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.rememberAppSizes
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import kotlinx.coroutines.delay

@Composable
fun FileListItem(
    fileItem: FileItem,
    index: Int = 0,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val spacing = rememberAppSpacing()
    val sizes = rememberAppSizes()
    val visual = FileTypeIconRegistry.resolveFileVisual(fileItem.name)
    var visible by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (index < 12) {
            delay((index * 20L).coerceAtMost(200L))
        }
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(AppMotion.Normal),
        label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(AppMotion.Normal),
        label = "translationY"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sizes.fileRowMinHeight)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
            }
            .background(AppColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = spacing.cardPadding, vertical = spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(sizes.fileIconContainer)
                .background(visual.tint.copy(alpha = 0.16f), MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.label,
                tint = visual.tint,
                modifier = Modifier.size(sizes.fileIcon)
            )
        }

        Spacer(modifier = Modifier.size(spacing.itemGap))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${fileItem.size} • ${fileItem.metadata}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = AppColors.BorderStrong
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(AppColors.SurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = AppColors.Danger) },
                    onClick = { menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppColors.Danger) }
                )
            }
        }
    }
}
