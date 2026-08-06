package com.example.touchgrassirl.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.repository.SessionResult
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.GradientAccent
import com.example.touchgrassirl.ui.theme.GradientEnd
import com.example.touchgrassirl.ui.theme.GradientStart
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.SunGold
import java.util.Locale

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onSessionEnded: (SessionResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sessionEnded.collect { result ->
            onSessionEnded(result)
        }
    }

    val minutes = state.elapsedSeconds / 60
    val seconds = state.elapsedSeconds % 60
    val totalRequiredSeconds = state.minMinutesRequired * 60L
    val progress = (state.elapsedSeconds.toFloat() / totalRequiredSeconds).coerceIn(0f, 1f)
    val goalReached = minutes >= state.minMinutesRequired

    val progressColor by animateColorAsState(
        targetValue = if (goalReached) SunGold else Color.White,
        animationSpec = tween(500),
        label = "progressColor",
    )

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (goalReached) {
                            listOf(GradientStart, GradientEnd, GradientAccent)
                        } else {
                            listOf(
                                Color(0xFF1B4332),
                                Color(0xFF2D6A4F),
                                Color(0xFF40916C),
                            )
                        },
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "\uD83C\uDF3F",
                    fontSize = 40.sp,
                )

                Text(
                    text = stringResource(R.string.session_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(R.string.session_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )

                // Animated timer ring
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(200.dp),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        strokeWidth = 12.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = if (goalReached) {
                                stringResource(R.string.session_goal_reached)
                            } else {
                                stringResource(
                                    R.string.session_minutes_remaining,
                                    (state.minMinutesRequired - minutes).coerceAtLeast(0),
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Live motion stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MotionStat(emoji = "\uD83D\uDC63", value = "${state.steps}", label = "Steps")
                        MotionStat(emoji = "\uD83D\uDCCD", value = "%.2f km".format(state.distanceMeters / 1000f), label = "Distance")
                        MotionStat(emoji = "\u26A1", value = "${state.sessionXpEarned}", label = "XP")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // End session button
                Button(
                    onClick = viewModel::endSession,
                    enabled = !state.isEnding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (goalReached) SunGold else Color.White,
                        contentColor = if (goalReached) Color.White else DeepForest,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                ) {
                    if (state.isEnding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.end_session),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onCancel,
                    enabled = !state.isEnding,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        stringResource(R.string.keep_session_background),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MotionStat(
    emoji: String,
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}
