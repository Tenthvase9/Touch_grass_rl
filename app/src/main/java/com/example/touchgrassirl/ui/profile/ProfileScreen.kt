package com.example.touchgrassirl.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.touchgrassirl.R
import com.example.touchgrassirl.ui.theme.CreamBackground
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.SkyMist

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val outdoorHours = state.totalOutdoorMinutes / 60f

    Surface(modifier = modifier.fillMaxSize(), color = CreamBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SkyMist),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "🌿", style = MaterialTheme.typography.displaySmall)
                    Text(
                        text = stringResource(
                            R.string.level_with_title,
                            state.level,
                            stringResource(state.levelTitleRes),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepForest,
                    )
                    Text(
                        text = stringResource(R.string.profile_total_xp) + ": ${state.totalXp}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ForestGreen,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileStatRow(
                label = stringResource(R.string.profile_longest_streak),
                value = stringResource(R.string.profile_days, state.longestStreak),
            )
            ProfileStatRow(
                label = stringResource(R.string.profile_outdoor_hours),
                value = stringResource(R.string.profile_hours, outdoorHours),
            )
            ProfileStatRow(
                label = stringResource(R.string.profile_sessions),
                value = "${state.totalSessions}",
            )
            ProfileStatRow(
                label = stringResource(R.string.profile_achievements),
                value = "${state.unlockedAchievements} / ${state.totalAchievements}",
            )
            ProfileStatRow(
                label = stringResource(R.string.profile_collectibles),
                value = "${state.collectedCount} / ${state.totalCollectibles}",
            )
        }
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MeadowGreen.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = ForestGreen)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = DeepForest,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
