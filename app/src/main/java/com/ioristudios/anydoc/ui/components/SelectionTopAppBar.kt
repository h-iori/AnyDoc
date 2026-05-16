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

private val OrbitronFamily = FontFamily(Font(R.font.orbitron, FontWeight.Bold))
private val NeonHeaderSurface = Color(0xFF0A0A0F)
private val NeonPurple = Color(0xFFBF00FF)
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
                onDelete()
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = AppColors.Danger
                )
            }
        }
    }
}
