package com.example.touchgrassirl.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.repository.SessionResult
import com.example.touchgrassirl.ui.theme.CreamBackground
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import java.util.Locale
import kotlin.math.min

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
    val progress = min(
        state.elapsedSeconds.toFloat() / (state.minMinutesRequired * 60),
        1f,
    )
    val goalReached = minutes >= state.minMinutesRequired

    Surface(modifier = modifier.fillMaxSize(), color = CreamBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "🚶‍♂️🌳", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.session_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.session_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.height(120.dp),
                color = if (goalReached) MeadowGreen else ForestGreen,
                strokeWidth = 10.dp,
            )

            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.headlineLarge,
                color = DeepForest,
                modifier = Modifier.padding(top = 24.dp),
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
                style = MaterialTheme.typography.bodyLarge,
                color = if (goalReached) MeadowGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )

            Text(
                text = stringResource(
                    R.string.session_motion_stats,
                    state.steps,
                    state.distanceMeters / 1000f,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = ForestGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Button(
                onClick = viewModel::endSession,
                enabled = !state.isEnding,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.isEnding) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.end_session),
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !state.isEnding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.keep_session_background))
            }
        }
    }
}
