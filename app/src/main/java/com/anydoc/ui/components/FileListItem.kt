package com.anydoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anydoc.data.FileItem
import com.anydoc.data.FileType
import com.anydoc.data.MockData
import com.anydoc.ui.theme.AnyDocTheme
import com.anydoc.ui.theme.CardSurface
import com.anydoc.ui.theme.CoreWhiteDim
import com.anydoc.ui.theme.SecondaryText
import com.anydoc.ui.theme.SurfaceDark
import com.anydoc.ui.theme.rememberHapticFeedback

@Composable
fun FileListItem(
    file: FileItem,
    modifier: Modifier = Modifier,
    onClick: (FileItem) -> Unit = {}
) {
    val onTapHaptic = rememberHapticFeedback()
    val iconMeta = MockData.iconMetadata[file.fileType] ?: MockData.iconMetadata[FileType.ALL]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CardSurface)
            .clickable {
                onTapHaptic()
                onClick(file)
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (iconMeta != null) {
                Icon(
                    imageVector = iconMeta.icon,
                    contentDescription = file.extension,
                    tint = iconMeta.accent
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                color = CoreWhiteDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.extension.uppercase()} • ${file.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun FileListItemPreview() {
    AnyDocTheme {
        FileListItem(
            file = MockData.recentFiles.first(),
            modifier = Modifier
                .background(SurfaceDark)
                .padding(16.dp)
        )
    }
}
