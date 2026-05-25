package com.example.touchgrassirl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.touchgrassirl.ui.theme.EarthBrown
import com.example.touchgrassirl.ui.theme.SkyMist

@Composable
fun GardenPreview(
    plotCount: Int,
    touchedGrassToday: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Your garden",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SkyMist)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (touchedGrassToday) {
                "Today's sunshine is helping things grow."
            } else {
                "Go outside to wake up your plots."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = EarthBrown,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(plotCount.coerceAtMost(6)) { index ->
                val emoji = when {
                    touchedGrassToday && index == 0 -> "🌻"
                    touchedGrassToday -> "🌿"
                    index == 0 -> "🪴"
                    else -> "🟫"
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
