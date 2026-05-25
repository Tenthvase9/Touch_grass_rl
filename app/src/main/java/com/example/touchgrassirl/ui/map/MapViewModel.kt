package com.example.touchgrassirl.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.domain.CollectibleCatalog
import com.example.touchgrassirl.domain.CollectibleDefinition
import com.example.touchgrassirl.domain.NatureSpot
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapUiState(
    val spots: List<NatureSpot> = emptyList(),
    val collectibles: List<CollectedItemUi> = emptyList(),
    val userLocation: GeoPoint? = null,
    val isLoading: Boolean = true,
    val lastDiscoveryMessage: String? = null,
)

data class CollectedItemUi(
    val definition: CollectibleDefinition,
    val collected: Boolean,
)

class MapViewModel(
    private val repository: TouchGrassRepository,
) : ViewModel() {

    private val spotsFlow = MutableStateFlow<List<NatureSpot>>(emptyList())
    private val userLocationFlow = MutableStateFlow<GeoPoint?>(null)
    private val discoveryMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        spotsFlow,
        userLocationFlow,
        discoveryMessage,
        repository.observeCollectedCollectibles(),
    ) { spots, location, message, collectedEntities ->
        val collectedIds = collectedEntities.map { it.id }.toSet()
        MapUiState(
            spots = spots,
            collectibles = CollectibleCatalog.all.map { def ->
                CollectedItemUi(definition = def, collected = def.id in collectedIds)
            },
            userLocation = location,
            isLoading = spots.isEmpty() && location == null,
            lastDiscoveryMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    private var locationJob: Job? = null

    init {
        refreshSpots()
        startLocationPolling()
    }

    fun refreshSpots() {
        viewModelScope.launch {
            spotsFlow.value = repository.getNatureSpots()
        }
    }

    private fun startLocationPolling() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            while (true) {
                val location = repository.locationTracker.getCurrentLocation()
                if (location != null) {
                    userLocationFlow.value = location
                    val result = repository.processLocationUpdate(
                        location.latitude,
                        location.longitude,
                    )
                    if (result.newlyVisitedSpots.isNotEmpty() || result.newlyCollectedIds.isNotEmpty()) {
                        val parts = buildList {
                            result.newlyVisitedSpots.forEach { add("Discovered ${it.name}") }
                            result.newlyCollectedIds.forEach { id ->
                                CollectibleCatalog.byId(id)?.let { add("Found ${it.emoji}") }
                            }
                        }
                        discoveryMessage.value = parts.joinToString(" · ")
                        spotsFlow.value = repository.getNatureSpots()
                    }
                }
                delay(8_000)
            }
        }
    }

    fun clearDiscoveryMessage() {
        discoveryMessage.value = null
    }

    override fun onCleared() {
        locationJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: TouchGrassRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
                return MapViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
