package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.PillShape
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAppSpacing()
    val bgColor = animateColorAsState(
        targetValue = if (isSelected) AppColors.Brand.copy(alpha = 0.18f) else AppColors.SurfaceElevated,
        animationSpec = tween(AppMotion.Fast),
        label = "chipBg"
    )

    val borderColor = animateColorAsState(
        targetValue = if (isSelected) AppColors.BrandStrong else AppColors.BorderSubtle,
        animationSpec = tween(AppMotion.Fast),
        label = "chipBorder"
    )

    Box(
        modifier = modifier
            .background(bgColor.value, PillShape)
            .border(width = 1.dp, color = borderColor.value, shape = PillShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = spacing.cardPadding, vertical = spacing.itemGap)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
