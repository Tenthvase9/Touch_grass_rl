package com.example.touchgrassirl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.touchgrassirl.R
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.LeafLight
import com.example.touchgrassirl.ui.theme.MeadowGreen

@Composable
fun DailyGoalProgress(
    outdoorMinutes: Int,
    goalMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (goalMinutes > 0) {
        (outdoorMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LeafLight.copy(alpha = 0.5f))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.daily_goal_label),
            style = MaterialTheme.typography.labelLarge,
            color = ForestGreen,
        )
        Text(
            text = stringResource(
                R.string.daily_goal_progress,
                outdoorMinutes.coerceAtMost(goalMinutes),
                goalMinutes,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = DeepForest,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
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
                    .fillMaxWidth(progress)
                    .height(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MeadowGreen),
            )
        }
    }
}
