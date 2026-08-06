package com.example.touchgrassirl.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.domain.ProgressCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val todayMinutes: Int = 0,
    val dailyGoal: Int = GameConstants.DEFAULT_DAILY_GOAL_MINUTES,
    val isOutdoors: Boolean = false,
    val streakDays: Int = 0,
    val level: Int = 1,
    val totalXp: Int = 0,
    val weeklyMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val todayIndex: Int = 0,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: TouchGrassRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProgressInitialized()
            repository.observeProgress().collect { progress ->
                val todayMinutes = repository.getTodayOutdoorMinutes()
                val level = ProgressCalculator.levelFromTotalXp(progress?.totalXp ?: 0)
                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value - 1

                _uiState.update {
                    it.copy(
                        todayMinutes = todayMinutes,
                        dailyGoal = progress?.dailyGoalMinutes ?: GameConstants.DEFAULT_DAILY_GOAL_MINUTES,
                        streakDays = progress?.currentStreak ?: 0,
                        level = level,
                        totalXp = progress?.totalXp ?: 0,
                        todayIndex = dayOfWeek,
                        isLoading = false,
                    )
                }
            }
        }

        viewModelScope.launch {
            try {
                val weekly = repository.getWeeklyStats()
                _uiState.update {
                    it.copy(weeklyMinutes = weekly.dailyBreakdown)
                }
            } catch (_: Exception) {
            }
        }
    }

    class Factory(
        private val repository: TouchGrassRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
