package com.example.touchgrassirl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    barColor: Color = Color(0xFF52B788),
    trackColor: Color = Color(0xFFB7E4C7),
    gradientColors: List<Color>? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .then(
                    if (gradientColors != null) {
                        Modifier.background(
                            Brush.horizontalGradient(gradientColors)
                        )
                    } else {
                        Modifier.background(barColor)
                    }
                ),
        )
    }
}
