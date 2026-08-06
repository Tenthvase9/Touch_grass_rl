package com.example.touchgrassirl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.touchgrassirl.ui.theme.EarthBrown
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.GradientAccent
import com.example.touchgrassirl.ui.theme.GradientEnd
import com.example.touchgrassirl.ui.theme.GradientStart
import com.example.touchgrassirl.ui.theme.SkyMist
import com.example.touchgrassirl.ui.theme.SunGold

@Composable
fun GardenPreview(
    plotCount: Int,
    touchedGrassToday: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Your garden",
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$plotCount plots",
                    style = MaterialTheme.typography.labelLarge,
                    color = ForestGreen,
                )
            }

            Text(
                text = if (touchedGrassToday) {
                    "Today's sunshine is helping things grow."
                } else {
                    "Go outside to wake up your plots."
                },
                style = MaterialTheme.typography.bodySmall,
                color = EarthBrown,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // Garden soil background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B7355),
                                Color(0xFF6B4F3A),
                                Color(0xFF5C4033),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    repeat(plotCount.coerceAtMost(6)) { index ->
                        val plant = PlantData.plants.getOrNull(index) ?: PlantData.default
                        PlantTile(
                            plant = plant,
                            active = touchedGrassToday && index < plotCount,
                            isFirst = index == 0,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private data class PlantEmoji(
    val sprout: String,
    val grown: String,
    val flower: String,
)

private object PlantData {
    val plants = listOf(
        PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF3A"),
        PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF37"),
        PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF39"),
        PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF3C"),
        PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF38"),
        PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF3B"),
    )
    val default = PlantEmoji("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF3F")
}

@Composable
private fun PlantTile(
    plant: PlantEmoji,
    active: Boolean,
    isFirst: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.7f,
        animationSpec = tween(500),
        label = "plantScale",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        val display = when {
            active && isFirst -> plant.flower
            active -> plant.grown
            else -> plant.sprout
        }
        Text(
            text = display,
            fontSize = 24.sp,
            modifier = Modifier.scale(scale),
            textAlign = TextAlign.Center,
        )
    }
}
