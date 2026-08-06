package com.example.touchgrassirl.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.touchgrassirl.ui.components.GlassCard
import com.example.touchgrassirl.ui.components.MotivationalBanner
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.ForestGreenLight
import com.example.touchgrassirl.ui.theme.MeadowGreen
import com.example.touchgrassirl.ui.theme.MeadowGreenDark
import com.example.touchgrassirl.ui.theme.SoftSage
import com.example.touchgrassirl.ui.theme.SkyBlue
import com.example.touchgrassirl.ui.theme.SunGold

@Composable
fun GardenScreen(
    repository: com.example.touchgrassirl.data.repository.TouchGrassRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: GardenViewModel = viewModel(
        factory = GardenViewModel.Factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "My Garden",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${state.totalOutdoorHours}h ${state.totalOutdoorMinutesRemainder}m total · ${state.plots.size} plants",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Garden grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(state.plots, key = { it.id }) { plot ->
                    PlantPlotCard(plot = plot)
                }

                if (state.plots.isEmpty()) {
                    item {
                        EmptyGardenState()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantPlotCard(
    plot: GardenPlotUi,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(plot.id) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    val (emoji, stageName, bgColors) = when (plot.stage) {
        PlantStage.SEED -> Triple("\uD83C\uDF31", "Seed", listOf(Color(0xFFF5E6D3), Color(0xFFEDD9B8)))
        PlantStage.SPROUT -> Triple("\uD83C\uDF3F", "Sprout", listOf(SoftSage, MeadowGreen.copy(alpha = 0.3f)))
        PlantStage.GROWING -> Triple("\uD83C\uDF37", "Growing", listOf(MeadowGreen.copy(alpha = 0.3f), MeadowGreen.copy(alpha = 0.5f)))
        PlantStage.BLOOMING -> Triple("\uD83C\uDF38", "Bloom!", listOf(SunGold.copy(alpha = 0.3f), SunGold.copy(alpha = 0.5f)))
        PlantStage.HARVEST -> Triple("\uD83C\uDF43", "Mature", listOf(SkyBlue.copy(alpha = 0.2f), SkyBlue.copy(alpha = 0.4f)))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(bgColors),
                    RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = emoji,
                    fontSize = 36.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyGardenState(
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\uD83C\uDF31",
                fontSize = 48.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your garden awaits",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Go outside to grow your first plant!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
