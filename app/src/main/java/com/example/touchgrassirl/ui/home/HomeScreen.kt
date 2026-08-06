package com.example.touchgrassirl.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.touchgrassirl.ui.components.AnimatedProgressRing
import com.example.touchgrassirl.ui.components.GlassCard
import com.example.touchgrassirl.ui.components.GradientCard
import com.example.touchgrassirl.ui.components.MotivationalBanner
import com.example.touchgrassirl.ui.components.StatPill
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.ForestGreenLight
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.MeadowGreenDark
import com.example.touchgrassirl.ui.theme.SkyBlue
import com.example.touchgrassirl.ui.theme.SoftSage
import com.example.touchgrassirl.ui.theme.SunGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenActivityFeed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { },
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = greetingForHour(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Touch Grass \uD83C\uDF31",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero ring
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedProgressRing(
                    progress = if (state.dailyGoal > 0) state.todayMinutes.toFloat() / state.dailyGoal else 0f,
                    size = 220,
                    emoji = if (state.todayMinutes >= state.dailyGoal) "\uD83C\uDF3B" else "\uD83C\uDF31",
                    centerText = "${state.todayMinutes}",
                    subtext = if (state.todayMinutes >= state.dailyGoal) "Goal reached!" else "of ${state.dailyGoal} min goal",
                    ringColor = if (state.isOutdoors) MeadowGreen else ForestGreen,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill(
                    emoji = "\uD83D\uDD25",
                    value = "${state.streakDays}",
                    label = "day streak",
                    modifier = Modifier.weight(1f),
                    accentColor = SunGold,
                )
                StatPill(
                    emoji = "\uD83C\uDFC6",
                    value = "${state.level}",
                    label = "level",
                    modifier = Modifier.weight(1f),
                    accentColor = ForestGreen,
                )
                StatPill(
                    emoji = "\u2B50",
                    value = "${state.totalXp}",
                    label = "total XP",
                    modifier = Modifier.weight(1f),
                    accentColor = SkyBlue,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Outdoor status banner
            if (state.isOutdoors) {
                MotivationalBanner(
                    message = "You're outside! Time is being tracked automatically.",
                    emoji = "\uD83C\uDF31",
                )
            } else {
                MotivationalBanner(
                    message = motivationalMessage(state.todayMinutes, state.dailyGoal),
                    emoji = if (state.todayMinutes > 0) "\uD83C\uDF3F" else "\uD83D\uDEAA",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Activity feed link
            GlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenActivityFeed)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\uD83D\uDCCA",
                        fontSize = 24.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activity Feed",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "See your outdoor activities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View feed",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weekly overview mini chart
            GlassCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "This week",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklySparkline(
                        minutes = state.weeklyMinutes,
                        todayIndex = state.todayIndex,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick tip
            GradientCard(
                gradient = Brush.verticalGradient(
                    colors = listOf(ForestGreenLight, ForestGreen),
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "\uD83D\uDCA1",
                        fontSize = 24.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pro tip",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftSage,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No need to open the app — outdoor time is tracked automatically when you leave home.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WeeklySparkline(
    minutes: List<Int>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = (minutes.maxOrNull() ?: 1).coerceAtLeast(1)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        minutes.forEachIndexed { index, min ->
            val fraction = min.toFloat() / maxMinutes
            val isToday = index == todayIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((60 * fraction).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
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
private fun greetingForHour(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun motivationalMessage(minutes: Int, goal: Int): String = when {
    minutes == 0 -> "Step outside to start tracking — no buttons needed!"
    minutes < goal / 2 -> "Great start! Keep exploring the outdoors."
    minutes < goal -> "Almost there — just ${goal - minutes} more minutes!"
    else -> "Goal crushed! Your garden is thriving."
}
