package com.example.touchgrassirl.ui.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.repository.SessionResult
import com.example.touchgrassirl.domain.AchievementCatalog
import com.example.touchgrassirl.domain.CollectibleCatalog
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.SunGold
import com.example.touchgrassirl.ui.theme.SunsetOrange

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
        result.countedThisSession && result.leveledUp -> "\uD83C\uDF89"
        result.countedThisSession && result.newStreak >= 7 -> "\uD83C\uDF1F"
        result.countedThisSession -> "\uD83C\uDF3F"
        else -> "\uD83C\uDF31"
    }

    val showConfetti = result.countedThisSession
    val scaleAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Confetti canvas
        if (showConfetti) {
            ConfettiBackground()
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = emoji,
                    fontSize = 64.sp,
                    modifier = Modifier.scale(scaleAnim.value),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rewards card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        RewardRow(emoji = "\u23F1\uFE0F", label = stringResource(R.string.reward_minutes), value = "${result.durationMinutes}")
                        RewardRow(emoji = "\u2728", label = stringResource(R.string.reward_xp), value = "+${result.xpEarned}")

                        if (result.countedThisSession) {
                            RewardRow(emoji = "\uD83D\uDC63", label = stringResource(R.string.reward_session_steps), value = "${result.sessionSteps}")
                            RewardRow(emoji = "\uD83D\uDCCD", label = stringResource(R.string.reward_session_distance), value = "%.2f km".format(result.sessionDistanceMeters / 1000f))

                            if (result.stepsXp > 0) {
                                RewardRow(emoji = "\uD83C\uDFC3", label = stringResource(R.string.reward_steps_xp), value = "+${result.stepsXp}")
                            }
                            if (result.streakXp > 0) {
                                RewardRow(emoji = "\uD83D\uDD25", label = stringResource(R.string.reward_streak_xp), value = "+${result.streakXp}")
                            }
                            if (result.challengeXp > 0) {
                                RewardRow(emoji = "\uD83C\uDFC6", label = stringResource(R.string.reward_challenge_xp), value = "+${result.challengeXp}")
                            }

                            RewardRow(emoji = "\uD83D\uDD25", label = stringResource(R.string.reward_streak), value = "${result.newStreak}")
                            RewardRow(emoji = "\uD83C\uDF31", label = stringResource(R.string.reward_level), value = "${result.newLevel} \u00B7 ${stringResource(result.levelTitleRes)}")
                            RewardRow(emoji = "\uD83C\uDF3C", label = stringResource(R.string.reward_garden), value = "${result.gardenPlots} plots")
                        }
                    }
                }

                if (result.leveledUp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SunGold.copy(alpha = 0.3f),
                    ) {
                        Text(
                            text = "\uD83C\uDF89 ${stringResource(R.string.celebration_level_up)} ${result.newLevel}!",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                // Discoveries
                result.newlyVisitedSpotNames.forEach { name ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\uD83D\uDCCD ${stringResource(R.string.celebration_discovered, name)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                    )
                }

                // Collectibles
                result.newlyCollectedIds.forEach { id ->
                    CollectibleCatalog.byId(id)?.let { def ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SunGold.copy(alpha = 0.25f),
                        ) {
                            Text(
                                text = "${stringResource(R.string.celebration_collectible)} ${def.emoji} ${stringResource(def.titleRes)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                // Achievements
                result.newlyUnlockedAchievementIds.forEach { id ->
                    AchievementCatalog.byId(id)?.let { def ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MeadowGreen.copy(alpha = 0.3f),
                        ) {
                            Text(
                                text = "${def.emoji} ${stringResource(def.titleRes)} unlocked!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DeepForest,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.back_home),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ConfettiBackground() {
    val particles = remember {
        (0..40).map {
            ConfettiParticleData(
                x = Math.random().toFloat(),
                y = Math.random().toFloat(),
                color = listOf(
                    Color(0xFFFFB703),
                    Color(0xFF52B788),
                    Color(0xFFFF8FAB),
                    Color(0xFF8ECAE6),
                    Color(0xFFE91E63),
                ).random(),
                radius = (kotlin.random.Random.nextInt(4, 13)).toFloat(),
                speed = kotlin.random.Random.nextFloat() * 0.006f + 0.002f,
                drift = (kotlin.random.Random.nextFloat() * 0.007f + 0.003f) * if (Math.random() > 0.5) 1f else -1f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val time = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    ).value

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = (p.y + time * p.speed) % 1.2f - 0.1f
            val x = (p.x + (time * p.drift)) % 1.1f
            drawCircle(
                color = p.color.copy(alpha = 0.5f),
                radius = p.radius,
                center = Offset(x * size.width, y * size.height),
            )
        }
    }
}

private data class ConfettiParticleData(
    val x: Float,
    val y: Float,
    val color: Color,
    val radius: Float,
    val speed: Float,
    val drift: Float,
)

@Composable
private fun RewardRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}
