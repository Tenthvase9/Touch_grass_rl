package com.example.touchgrassirl.ui.weekly

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.data.repository.WeeklyStats
import com.example.touchgrassirl.ui.theme.MeadowGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScreen(
    repository: TouchGrassRepository,
    onBack: () -> Unit,
) {
    val stats by produceState<WeeklyStats?>(
        initialValue = null,
        key1 = repository,
    ) {
        value = repository.getWeeklyStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2D6A4F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (stats == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Loading...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val data = stats!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    Text(
                        text = "This Week",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MiniStatCard(
                            emoji = "\uD83C\uDF31",
                            value = "${data.totalMinutes} min",
                            label = "Outdoor time",
                            modifier = Modifier.weight(1f),
                        )
                        MiniStatCard(
                            emoji = "\uD83C\uDFC3",
                            value = "${data.totalSessions}",
                            label = "Sessions",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MiniStatCard(
                            emoji = "\uD83D\uDC63",
                            value = "${data.totalSteps}",
                            label = "Steps",
                            modifier = Modifier.weight(1f),
                        )
                        MiniStatCard(
                            emoji = "\uD83D\uDD25",
                            value = "${data.streakDays} days",
                            label = "Best streak",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Daily Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        data.dailyBreakdown.forEachIndexed { index, minutes ->
                            DayBar(
                                day = days.getOrElse(index) { "?" },
                                minutes = minutes,
                                maxMinutes = data.dailyBreakdown.maxOrNull() ?: 1,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = weeklyMood(data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayBar(
    day: String,
    minutes: Int,
    maxMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (maxMinutes > 0) minutes.toFloat() / maxMinutes else 0f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (minutes > 0) "$minutes" else "-",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val barHeightDp = (fraction * 120f).dp.coerceAtLeast(4.dp)
        Surface(
            modifier = Modifier
                .size(width = 28.dp, height = barHeightDp),
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            color = if (minutes > 0) MeadowGreen else MaterialTheme.colorScheme.surfaceVariant,
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun weeklyMood(stats: WeeklyStats): String = when {
    stats.totalMinutes >= 300 -> "Amazing week! Nature guardian mode activated. \uD83C\uDF3F"
    stats.totalMinutes >= 180 -> "Great week! Your garden is thriving. \uD83C\uDF31"
    stats.totalMinutes >= 90 -> "Solid effort! Every minute counts. \uD83D\uDC4A"
    stats.totalMinutes >= 30 -> "Getting there! Try to get out a bit more. \uD83C\uDF1F"
    else -> "Rough week. Tomorrow is a fresh chance to touch grass! \uD83C\uDF1E"
}
