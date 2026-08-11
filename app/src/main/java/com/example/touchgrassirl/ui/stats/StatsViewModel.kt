package com.example.touchgrassirl.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.domain.ProgressCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StatsUiState(
    val todayMinutes: Int = 0,
    val streakDays: Int = 0,
    val totalHours: Int = 0,
    val totalMinutesRemainder: Int = 0,
    val level: Int = 1,
    val totalXp: Int = 0,
    val weeklyMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val monthlyMinutes: List<Int> = listOf(0, 0, 0, 0),
    val todayIndex: Int = 0,
    val selectedPeriod: String = "week",
    val motivationMessage: String = "",
)

class StatsViewModel(
    private val repository: TouchGrassRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProgressInitialized()
            repository.observeProgress().collect { progress ->
                val totalMinutes = progress?.totalOutdoorMinutes ?: 0
                val todayMinutes = repository.getTodayOutdoorMinutes()
                val level = ProgressCalculator.levelFromTotalXp(progress?.totalXp ?: 0)
                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value - 1

                _uiState.update {
                    it.copy(
                        todayMinutes = todayMinutes,
                        streakDays = progress?.currentStreak ?: 0,
                        totalHours = totalMinutes / 60,
                        totalMinutesRemainder = totalMinutes % 60,
                        level = level,
                        totalXp = progress?.totalXp ?: 0,
                        todayIndex = dayOfWeek,
                        motivationMessage = motivationForMinutes(todayMinutes),
                    )
                }
            }
        }

        viewModelScope.launch {
            try {
                val weekly = repository.getWeeklyStats()
                val monthly = repository.getMonthlyStats()
                _uiState.update {
                    it.copy(
                        weeklyMinutes = weekly.dailyBreakdown,
                        monthlyMinutes = monthly.dailyBreakdown,
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun selectPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    private fun motivationForMinutes(minutes: Int): String = when {
        minutes == 0 -> "Step outside today to start growing your garden!"
        minutes < 15 -> "Good start! Aim for 30 minutes to grow a new plant."
        minutes < 30 -> "Nice progress! Keep going to reach your daily goal."
        minutes < 60 -> "Over half an hour — your garden is thriving!"
        else -> "Over an hour outside! You're a nature guardian."
    }

    class Factory(
        private val repository: TouchGrassRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
                return StatsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
