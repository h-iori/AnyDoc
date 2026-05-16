package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppGlow
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@Composable
fun DocumentPage(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true
) {
    val spacing = rememberAppSpacing()
    val pageShape = RoundedCornerShape(16.dp)
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha = shimmer.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.84f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppMotion.Slow, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.414f)
            .neonGlow(
                color = AppColors.Brand.copy(alpha = AppGlow.SubtleAlpha),
                radius = AppGlow.Md,
                shape = pageShape
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppColors.Surface,
                        AppColors.SurfaceElevated
                    )
                ),
                pageShape
            )
            .border(1.dp, AppColors.BorderSubtle, pageShape)
            .padding(spacing.cardPadding)
    ) {
        if (isLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                            .alpha(shimmerAlpha.value)
                            .background(AppColors.SurfaceHighest, RoundedCornerShape(8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .height(22.dp)
                            .alpha(shimmerAlpha.value)
                            .background(AppColors.SurfaceHighest, RoundedCornerShape(8.dp))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                repeat(8) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .alpha(shimmerAlpha.value)
                            .background(AppColors.SurfaceElevated, RoundedCornerShape(6.dp))
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonGlow(
                        color = AppColors.Brand.copy(alpha = 0.24f),
                        radius = AppGlow.Sm,
                        shape = pageShape
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                AppColors.Brand.copy(alpha = 0.16f),
                                AppColors.Brand.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        pageShape
                    )
                    .border(1.dp, AppColors.BorderSubtle, pageShape)
            )
        }
    }
}
