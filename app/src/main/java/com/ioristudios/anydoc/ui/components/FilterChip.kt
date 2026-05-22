package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppGlow
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.PillShape
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AppColors.Brand,
    activeContainerColor: Color = AppColors.Brand.copy(alpha = 0.24f),
    activeBorderColor: Color = AppColors.BrandStrong,
    activeGlowColor: Color = AppColors.Brand.copy(alpha = AppGlow.StrongAlpha)
) {
    val spacing = rememberAppSpacing()
    val bgColor = animateColorAsState(
        targetValue = if (isSelected) activeContainerColor else AppColors.SurfaceElevated,
        animationSpec = tween(AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "chipBg"
    )

    val borderColor = animateColorAsState(
        targetValue = if (isSelected) activeBorderColor else AppColors.BorderSubtle,
        animationSpec = tween(AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "chipBorder"
    )
    val textColor = animateColorAsState(
        targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "chipText"
    )
    val chipScale = animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.StandardEasing),
        label = "chipScale"
    )

    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    Box(
        modifier = modifier
            .scale(chipScale.value)
            .background(bgColor.value, PillShape)
            .border(width = 1.dp, color = borderColor.value, shape = PillShape)
            .neonGlow(
                color = if (isSelected) activeGlowColor else AppColors.Brand.copy(alpha = 0.12f),
                radius = if (isSelected) AppGlow.Md else AppGlow.Sm,
                shape = PillShape
            )
            .clickable(role = Role.Tab, onClick = {
                haptics.performHapticFeedback()
                onClick()
            })
            .padding(horizontal = spacing.cardPadding, vertical = spacing.itemGap)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor.value
        )
    }
}
