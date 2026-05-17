package com.ioristudios.anydoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import kotlinx.coroutines.launch

private val OrbitronFamily = FontFamily(Font(R.font.orbitron, FontWeight.Bold))
private val NeonHeaderSurface = Color(0xFF0A0A0F)
private val CoreWhite = Color(0xFFFFFFFF)
private val CoreWhiteDim = Color(0xFFEEEEFF)

@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAllChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCloseSelection: () -> Unit
) {
    val spacing = rememberAppSpacing()
    val isAllSelected = selectedCount > 0 && selectedCount == totalCount
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonHeaderSurface.copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(horizontal = spacing.screenPadding, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                haptics.performHapticFeedback()
                onCloseSelection()
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close selection",
                    tint = CoreWhiteDim
                )
            }
            Checkbox(
                checked = isAllSelected,
                onCheckedChange = {
                    haptics.performHapticFeedback()
                    onSelectAllChange(it)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.BrandStrong,
                    uncheckedColor = CoreWhiteDim.copy(alpha = 0.5f),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$selectedCount Selected",
                style = MaterialTheme.typography.titleMedium,
                color = CoreWhite
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                haptics.performHapticFeedback()
                onShare()
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share selected",
                    tint = CoreWhiteDim
                )
            }
            IconButton(onClick = {
                haptics.performHapticFeedback()
                showDeleteDialog = true
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = AppColors.Danger
                )
            }
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete $selectedCount Files", style = MaterialTheme.typography.titleLarge, color = AppColors.Danger) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete $selectedCount selected files? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        haptics.performHapticFeedback()
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = AppColors.Danger)
                }
            },
            dismissButton = {
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                androidx.compose.material3.TextButton(onClick = {
                    coroutineScope.launch {
                        haptics.performHapticFeedback()
                        kotlinx.coroutines.delay(100)
                        haptics.performHapticFeedback()
                    }
                    showDeleteDialog = false
                }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = AppColors.SurfaceElevated
        )
    }
}
