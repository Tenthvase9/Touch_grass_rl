package com.example.touchgrassirl.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.touchgrassirl.R
import com.example.touchgrassirl.domain.NatureSpot
import com.example.touchgrassirl.domain.NatureSpotType
import com.example.touchgrassirl.ui.theme.CreamBackground
import com.example.touchgrassirl.ui.theme.DeepForest
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private val cartoVoyagerTileSource = XYTileSource(
    "CartoDBVoyager",
    0,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
)

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onOpenWeekly: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.lastDiscoveryMessage) {
        if (state.lastDiscoveryMessage != null) {
            kotlinx.coroutines.delay(4_000)
            viewModel.clearDiscoveryMessage()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = CreamBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.map_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            if (state.lastDiscoveryMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MeadowGreen.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = state.lastDiscoveryMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepForest,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ForestGreen,
                    )
                } else if (state.userLocation != null) {
                    AdventureMap(
                        spots = state.spots,
                        userLocation = state.userLocation!!,
                    )
                } else {
                    SpotListFallback(
                        spots = state.spots,
                    )
                }
            }

            CollectiblesRow(
                collectibles = state.collectibles,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun AdventureMap(
    spots: List<NatureSpot>,
    userLocation: GeoPoint,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(cartoVoyagerTileSource)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(15.5)
            controller.setCenter(userLocation)
        }
    }

    // Update markers and user location overlay
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.overlays.clear()

            // User location overlay
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mv)
            locationOverlay.enableMyLocation()
            mv.overlays.add(locationOverlay)

            // Nature spots markers
            spots.forEach { spot ->
                val marker = Marker(mv)
                marker.position = GeoPoint(spot.latitude, spot.longitude)
                marker.title = spot.name
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.snippet = if (spot.visited) {
                    context.getString(R.string.map_spot_visited)
                } else {
                    context.getString(R.string.map_spot_unvisited)
                }
                mv.overlays.add(marker)
            }
                mv.invalidate()
            }
        )
        Text(
            text = "© OpenStreetMap contributors © CARTO",
            style = MaterialTheme.typography.labelSmall,
            color = DeepForest,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .background(MeadowGreen.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }

    LaunchedEffect(userLocation) {
        mapView.controller.animateTo(userLocation)
    }
}

@Composable
private fun SpotListFallback(
    spots: List<NatureSpot>,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(spots, key = { it.id }) { spot ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (spot.visited) {
                        MeadowGreen.copy(alpha = 0.25f)
                    } else {
                        MeadowGreen.copy(alpha = 0.1f)
                    },
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${spotEmoji(spot)} ${spot.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepForest,
                    )
                    Text(
                        text = if (spot.visited) {
                            stringResource(R.string.map_spot_visited)
                        } else {
                            stringResource(R.string.map_spot_unvisited)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectiblesRow(
    collectibles: List<CollectedItemUi>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MeadowGreen.copy(alpha = 0.12f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.map_collectibles_title),
            style = MaterialTheme.typography.labelLarge,
            color = ForestGreen,
        )
        Text(
            text = collectibles.joinToString("  ") { item ->
                if (item.collected) item.definition.emoji else "❓"
            },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun spotEmoji(spot: NatureSpot): String = when (spot.type) {
    NatureSpotType.PARK -> "🌳"
    NatureSpotType.TRAIL -> "🥾"
    NatureSpotType.BEACH -> "🏖️"
    NatureSpotType.GARDEN -> "🌻"
}
