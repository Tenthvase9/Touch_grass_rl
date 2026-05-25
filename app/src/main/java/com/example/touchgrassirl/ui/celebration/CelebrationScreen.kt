package com.example.touchgrassirl.ui.celebration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.repository.SessionResult
import com.example.touchgrassirl.domain.AchievementCatalog
import com.example.touchgrassirl.domain.CollectibleCatalog
import com.example.touchgrassirl.ui.theme.CreamBackground
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.SunGold

@Composable
fun CelebrationScreen(
    result: SessionResult,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstAchievement = result.newlyUnlockedAchievementIds.contains("first_grass")
    val headline = when {
        result.countedThisSession && firstAchievement ->
            stringResource(R.string.celebration_first_grass)
        result.countedThisSession && result.challengeCompleted ->
            stringResource(R.string.celebration_challenge)
        result.countedThisSession ->
            stringResource(R.string.celebration_success)
        else ->
            stringResource(R.string.celebration_too_short)
    }

    val emoji = when {
        result.countedThisSession -> "🎉"
        else -> "🌱"
    }

    Surface(modifier = modifier.fillMaxSize(), color = CreamBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(text = emoji, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SunGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    RewardRow(
                        label = stringResource(R.string.reward_minutes),
                        value = "${result.durationMinutes}",
                    )
                    RewardRow(
                        label = stringResource(R.string.reward_xp),
                        value = "+${result.xpEarned}",
                    )
                    if (result.countedThisSession) {
                        RewardRow(
                            label = stringResource(R.string.reward_session_steps),
                            value = "${result.sessionSteps}",
                        )
                        RewardRow(
                            label = stringResource(R.string.reward_session_distance),
                            value = String.format("%.2f km", result.sessionDistanceMeters / 1000f),
                        )
                        if (result.stepsXp > 0) {
                            RewardRow(
                                label = stringResource(R.string.reward_steps_xp),
                                value = "+${result.stepsXp}",
                            )
                        }
                        if (result.streakXp > 0) {
                            RewardRow(
                                label = stringResource(R.string.reward_streak_xp),
                                value = "+${result.streakXp}",
                            )
                        }
                        if (result.challengeXp > 0) {
                            RewardRow(
                                label = stringResource(R.string.reward_challenge_xp),
                                value = "+${result.challengeXp}",
                            )
                        }
                        RewardRow(
                            label = stringResource(R.string.reward_streak),
                            value = "${result.newStreak} 🔥",
                        )
                        RewardRow(
                            label = stringResource(R.string.reward_level),
                            value = "${result.newLevel} · ${stringResource(result.levelTitleRes)}",
                        )
                        RewardRow(
                            label = stringResource(R.string.reward_garden),
                            value = "${result.gardenPlots} plots",
                        )
                    }
                }
            }

            if (result.leveledUp) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.celebration_level_up),
                    style = MaterialTheme.typography.titleLarge,
                    color = ForestGreen,
                )
            }

            result.newlyVisitedSpotNames.forEach { name ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.celebration_discovered, name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DeepForest,
                    textAlign = TextAlign.Center,
                )
            }

            result.newlyCollectedIds.forEach { id ->
                CollectibleCatalog.byId(id)?.let { def ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.celebration_collectible)} ${def.emoji} ${stringResource(def.titleRes)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ForestGreen,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            result.newlyUnlockedAchievementIds.forEach { id ->
                AchievementCatalog.byId(id)?.let { def ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${def.emoji} ${stringResource(def.titleRes)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepForest,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.back_home),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun RewardRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyLarge,
        color = DeepForest,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
