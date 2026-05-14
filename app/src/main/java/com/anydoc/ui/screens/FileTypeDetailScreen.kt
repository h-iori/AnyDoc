package com.anydoc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anydoc.data.FileItem
import com.anydoc.data.FileType
import com.anydoc.data.MockData
import com.anydoc.ui.theme.AnyDocHaptics
import com.anydoc.ui.theme.AnyDocTheme
import com.anydoc.ui.theme.rememberAnyDocHaptics

@Composable
fun FileTypeDetailScreen(
    fileType: FileType,
    onBackClick: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    haptics: AnyDocHaptics = rememberAnyDocHaptics()
) {
    var query by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }

    val files = remember(fileType) {
        MockData.filesByType[fileType].orEmpty()
    }
    val filteredFiles = remember(query, files) {
        files.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    val titlePulse = rememberInfiniteTransition(label = "titlePulse")
    val accentAlpha by titlePulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accentAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        haptics.navigate()
                        onBackClick()
                    }
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = if (fileType == FileType.ALL) "All Files" else "${fileType.name} Files",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(3.dp)
                        .fillMaxWidth(0.35f)
                        .background(
                            color = MockData.iconMetadata[fileType]?.accent?.copy(alpha = accentAlpha)
                                ?: MaterialTheme.colorScheme.primary.copy(alpha = accentAlpha),
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                haptics.confirm()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .onFocusChanged {
                    if (searchFocused != it.isFocused) {
                        searchFocused = it.isFocused
                        haptics.navigate()
                    }
                }
                .border(
                    width = if (searchFocused) 1.dp else 0.dp,
                    color = if (searchFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            placeholder = {
                Text("Search in ${fileType.name.lowercase()}")
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        AnimatedVisibility(visible = filteredFiles.isEmpty()) {
            EmptyState(
                icon = MockData.iconMetadata[fileType]?.icon ?: Icons.Outlined.Search,
                label = if (query.isBlank()) "No files available" else "No matches for \"$query\""
            )
        }

        if (filteredFiles.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.name }) { file ->
                    FileListItem(file = file) {
                        haptics.navigate()
                        onFileClick(file)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    label: String
) {
    val transition = rememberInfiniteTransition(label = "emptyPulse")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .alpha(alpha)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun FileListItem(
    file: FileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fileTypeMeta = MockData.iconMetadata[file.fileType]
        Icon(
            imageVector = fileTypeMeta?.icon ?: Icons.Outlined.Search,
            contentDescription = null,
            tint = fileTypeMeta?.accent ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${file.extension.uppercase()} • ${file.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FileTypeDetailScreenPreview() {
    AnyDocTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FileTypeDetailScreen(
                fileType = FileType.PDF,
                onBackClick = {},
                onFileClick = {}
            )
        }
    }
}
