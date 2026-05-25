package com.example.touchgrassirl.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.domain.DailyChallengeCatalog
import com.example.touchgrassirl.domain.DailyChallengeDefinition
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.domain.LevelTitles
import com.example.touchgrassirl.domain.ProgressCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val touchedGrassToday: Boolean = false,
    val todayOutdoorMinutes: Int = 0,
    val todayXp: Int = 0,
    val todaySteps: Int = 0,
    val todayDistanceMeters: Int = 0,
    val currentStreak: Int = 0,
    val level: Int = 1,
    @StringRes val levelTitleRes: Int = LevelTitles.titleResForLevel(1),
    val xpInLevel: Int = 0,
    val xpForLevel: Int = GameConstants.XP_PER_LEVEL,
    val dailyGoalMinutes: Int = GameConstants.DEFAULT_DAILY_GOAL_MINUTES,
    val gardenPlotCount: Int = 1,
    val hasActiveSession: Boolean = false,
    val dailyChallenge: DailyChallengeDefinition = DailyChallengeCatalog.forDate(),
    val challengeCompleted: Boolean = false,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: TouchGrassRepository,
) : ViewModel() {

    private val todaySnapshot = MutableStateFlow(TodaySnapshot())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeProgress(),
        repository.observeActiveSession(),
        todaySnapshot,
    ) { progress, activeSession, today ->
        val (xpInLevel, xpForLevel) = ProgressCalculator.xpProgressInLevel(progress.totalXp)
        val level = ProgressCalculator.levelFromTotalXp(progress.totalXp)
        HomeUiState(
            touchedGrassToday = today.touchedGrass,
            todayOutdoorMinutes = today.outdoorMinutes,
            todayXp = today.xpEarned,
            todaySteps = today.steps,
            todayDistanceMeters = today.distanceMeters,
            currentStreak = progress.currentStreak,
            level = level,
            levelTitleRes = LevelTitles.titleResForLevel(level),
            xpInLevel = xpInLevel,
            xpForLevel = xpForLevel,
            dailyGoalMinutes = progress.dailyGoalMinutes,
            gardenPlotCount = progress.gardenPlotCount,
            hasActiveSession = activeSession != null,
            dailyChallenge = today.challenge,
            challengeCompleted = today.challengeCompleted,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        refreshToday()
        viewModelScope.launch {
            repository.ensureProgressInitialized()
            repository.ensureTodayLog()
            refreshToday()
        }
    }

    fun refreshToday() {
        viewModelScope.launch {
            val log = repository.ensureTodayLog()
            val challenge = log.challengeId?.let { id ->
                DailyChallengeCatalog.all.find { it.id == id }
            } ?: DailyChallengeCatalog.forDate()
            todaySnapshot.value = TodaySnapshot(
                touchedGrass = log.touchedGrass,
                outdoorMinutes = log.outdoorMinutes,
                xpEarned = log.xpEarned,
                steps = log.steps,
                distanceMeters = log.distanceMeters,
                challenge = challenge,
                challengeCompleted = log.challengeCompleted,
            )
        }
    }

    private data class TodaySnapshot(
        val touchedGrass: Boolean = false,
        val outdoorMinutes: Int = 0,
        val xpEarned: Int = 0,
        val steps: Int = 0,
        val distanceMeters: Int = 0,
        val challenge: DailyChallengeDefinition = DailyChallengeCatalog.forDate(),
        val challengeCompleted: Boolean = false,
    )

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
