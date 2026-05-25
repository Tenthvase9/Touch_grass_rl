package com.example.touchgrassirl.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.touchgrassirl.R
import com.example.touchgrassirl.ui.components.DailyGoalProgress
import com.example.touchgrassirl.ui.components.GardenPreview
import com.example.touchgrassirl.ui.components.LevelProgressBar
import com.example.touchgrassirl.ui.theme.CreamBackground
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.SunGold
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartSession: () -> Unit,
    onViewMap: () -> Unit,
    onViewAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CreamBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_header),
                style = MaterialTheme.typography.titleLarge,
                color = ForestGreen,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (state.touchedGrassToday) "🌿" else "🌱",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = if (state.touchedGrassToday) {
                    stringResource(R.string.home_touched_today)
                } else {
                    stringResource(R.string.home_not_touched)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = DeepForest,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Text(
                text = if (state.touchedGrassToday) {
                    stringResource(R.string.home_today_minutes, state.todayOutdoorMinutes)
                } else {
                    stringResource(R.string.home_go_outside_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
            )

            DailyStatsGrid(
                outdoorMinutes = state.todayOutdoorMinutes,
                steps = state.todaySteps,
                distanceMeters = state.todayDistanceMeters,
                xpToday = state.todayXp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            StreakCard(streak = state.currentStreak)

            Spacer(modifier = Modifier.height(16.dp))

            DailyGoalProgress(
                outdoorMinutes = state.todayOutdoorMinutes,
                goalMinutes = state.dailyGoalMinutes,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    R.string.level_with_title,
                    state.level,
                    stringResource(state.levelTitleRes),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = DeepForest,
            )
            LevelProgressBar(
                level = state.level,
                xpInLevel = state.xpInLevel,
                xpForLevel = state.xpForLevel,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            GardenPreview(
                plotCount = state.gardenPlotCount,
                touchedGrassToday = state.touchedGrassToday,
                title = stringResource(R.string.companion_title),
            )

            Spacer(modifier = Modifier.height(16.dp))

            DailyChallengeCard(
                title = stringResource(state.dailyChallenge.titleRes),
                description = stringResource(state.dailyChallenge.descriptionRes),
                completed = state.challengeCompleted,
            )

            Spacer(modifier = Modifier.height(16.dp))

            QuickActionsRow(
                onStartWalk = onStartSession,
                onViewMap = onViewMap,
                onViewAchievements = onViewAchievements,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (state.hasActiveSession) {
                        stringResource(R.string.continue_session)
                    } else if (state.touchedGrassToday) {
                        stringResource(R.string.more_outdoor_time)
                    } else {
                        stringResource(R.string.start_outdoor_time)
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DailyStatsGrid(
    outdoorMinutes: Int,
    steps: Int,
    distanceMeters: Int,
    xpToday: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = stringResource(R.string.stat_outdoor_time),
                value = stringResource(R.string.stat_minutes_short, outdoorMinutes),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_steps),
                value = stringResource(R.string.stat_steps_short, steps),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = stringResource(R.string.stat_distance),
                value = stringResource(
                    R.string.stat_distance_short,
                    distanceMeters / 1000f,
                ),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_xp_today),
                value = stringResource(R.string.stat_xp_short, xpToday),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MeadowGreen.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = DeepForest)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ForestGreen,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun StreakCard(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SunGold.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = if (streak > 0) {
                stringResource(R.string.streak_section, streak)
            } else {
                stringResource(R.string.streak_start)
            },
            style = MaterialTheme.typography.titleMedium,
            color = DeepForest,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DailyChallengeCard(
    title: String,
    description: String,
    completed: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) MeadowGreen.copy(alpha = 0.25f) else MeadowGreen.copy(alpha = 0.12f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.daily_challenge_title),
                style = MaterialTheme.typography.labelLarge,
                color = ForestGreen,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DeepForest,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = if (completed) {
                    stringResource(R.string.challenge_complete)
                } else {
                    description
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onStartWalk: () -> Unit,
    onViewMap: () -> Unit,
    onViewAchievements: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onStartWalk,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.DirectionsWalk, contentDescription = null)
            Text(
                text = stringResource(R.string.quick_start_walk),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        OutlinedButton(
            onClick = onViewMap,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Map, contentDescription = null)
            Text(
                text = stringResource(R.string.quick_view_map),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        OutlinedButton(
            onClick = onViewAchievements,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null)
            Text(
                text = stringResource(R.string.quick_challenges),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
