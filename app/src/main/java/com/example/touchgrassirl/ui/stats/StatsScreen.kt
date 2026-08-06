package com.example.touchgrassirl.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.ui.components.GlassCard
import com.example.touchgrassirl.ui.components.StatPill
import com.example.touchgrassirl.ui.components.MotivationalBanner
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.SkyBlue
import com.example.touchgrassirl.ui.theme.SunGold

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Top stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill(
                    emoji = "\uD83C\uDF31",
                    value = "${state.todayMinutes}m",
                    label = "today",
                    modifier = Modifier.weight(1f),
                    accentColor = ForestGreen,
                )
                StatPill(
                    emoji = "\uD83D\uDD25",
                    value = "${state.streakDays}",
                    label = "streak",
                    modifier = Modifier.weight(1f),
                    accentColor = SunGold,
                )
                StatPill(
                    emoji = "\uD83C\uDFC6",
                    value = "${state.level}",
                    label = "level",
                    modifier = Modifier.weight(1f),
                    accentColor = SkyBlue,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weekly chart
            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "This week",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    WeeklyBarChart(
                        minutes = state.weeklyMinutes,
                        todayIndex = state.todayIndex,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Total stats
            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "All time",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StatRow(label = "Total outdoor time", value = "${state.totalHours}h ${state.totalMinutesRemainder}m")
                    StatRow(label = "Total XP earned", value = "${state.totalXp}")
                    StatRow(label = "Current streak", value = "${state.streakDays} days")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Motivation
            MotivationalBanner(
                message = state.motivationMessage,
                emoji = "\uD83C\uDF31",
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WeeklyBarChart(
    minutes: List<Int>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = (minutes.maxOrNull() ?: 1).coerceAtLeast(1)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        minutes.forEachIndexed { index, min ->
            val fraction = min.toFloat() / maxMinutes
            val animatedHeight = remember { Animatable(0f) }

            LaunchedEffect(min) {
                animatedHeight.animateTo(
                    targetValue = fraction.coerceIn(0.02f, 1f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }

            val isToday = index == todayIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${min}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height((80 * animatedHeight.value).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            if (isToday) MeadowGreen else MeadowGreen.copy(alpha = 0.3f),
                        ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = days.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
