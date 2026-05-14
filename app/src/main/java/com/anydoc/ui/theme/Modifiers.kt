package com.anydoc.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val screenHorizontalPadding = 20.dp
val screenVerticalPadding = 16.dp

fun Modifier.neonGlow(
    color: Color = NeonPurpleGlow,
    radius: Dp = 18.dp
): Modifier = drawBehind {
    val glow = Paint()
    val frameworkPaint = glow.asFrameworkPaint()
    frameworkPaint.color = color.copy(alpha = 0.55f).toArgb()
    frameworkPaint.maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.NORMAL)

    drawIntoCanvas { canvas ->
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            paint = glow
        )
    }
}
