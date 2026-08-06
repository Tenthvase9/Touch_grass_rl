package com.example.touchgrassirl.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlantStage {
    SEED, SPROUT, GROWING, BLOOMING, HARVEST
}

data class GardenPlotUi(
    val id: Int,
    val stage: PlantStage,
)

data class GardenUiState(
    val plots: List<GardenPlotUi> = emptyList(),
    val totalOutdoorHours: Int = 0,
    val totalOutdoorMinutesRemainder: Int = 0,
)

class GardenViewModel(
    private val repository: TouchGrassRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProgressInitialized()
            repository.observeProgress().collect { progress ->
                val totalMinutes = progress?.totalOutdoorMinutes ?: 0
                val hours = totalMinutes / 60
                val minutesRemainder = totalMinutes % 60
                val plots = buildPlots(totalMinutes)
                _uiState.update {
                    it.copy(
                        plots = plots,
                        totalOutdoorHours = hours,
                        totalOutdoorMinutesRemainder = minutesRemainder,
                    )
                }
            }
        }
    }

    private fun buildPlots(totalMinutes: Int): List<GardenPlotUi> {
        val plots = mutableListOf<GardenPlotUi>()
        var remainingMinutes = totalMinutes
        var id = 0

        while (remainingMinutes > 0) {
            val plotMinutes = minOf(remainingMinutes, 30)
            val stage = stageForMinutes(plotMinutes)
            plots.add(GardenPlotUi(id = id, stage = stage))
            remainingMinutes -= 30
            id++
        }

        return plots
    }

    private fun stageForMinutes(minutes: Int): PlantStage = when {
        minutes >= 30 -> PlantStage.HARVEST
        minutes >= 20 -> PlantStage.BLOOMING
        minutes >= 10 -> PlantStage.GROWING
        minutes >= 5 -> PlantStage.SPROUT
        else -> PlantStage.SEED
    }

    class Factory(
        private val repository: TouchGrassRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GardenViewModel::class.java)) {
                return GardenViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
