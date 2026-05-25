package com.example.touchgrassirl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.LeafLight
import com.example.touchgrassirl.ui.theme.MeadowGreen

@Composable
fun LevelProgressBar(
    level: Int,
    xpInLevel: Int,
    xpForLevel: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (xpForLevel > 0) xpInLevel.toFloat() / xpForLevel else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Level $level",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LeafLight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MeadowGreen),
            )
        }
        Text(
            text = "$xpInLevel / $xpForLevel XP",
            style = MaterialTheme.typography.labelLarge,
            color = ForestGreen,
        )
    }
}
