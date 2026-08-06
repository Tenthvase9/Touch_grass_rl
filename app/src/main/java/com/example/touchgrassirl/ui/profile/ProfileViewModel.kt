package com.example.touchgrassirl.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.touchgrassirl.domain.AchievementCatalog
import com.example.touchgrassirl.domain.CollectibleCatalog
import com.example.touchgrassirl.domain.LevelTitles
import com.example.touchgrassirl.domain.ProgressCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val level: Int = 1,
    @StringRes val levelTitleRes: Int = LevelTitles.titleResForLevel(1),
    val totalXp: Int = 0,
    val longestStreak: Int = 0,
    val totalOutdoorMinutes: Int = 0,
    val totalSessions: Int = 0,
    val unlockedAchievements: Int = 0,
    val totalAchievements: Int = AchievementCatalog.all.size,
    val collectedCount: Int = 0,
    val totalCollectibles: Int = CollectibleCatalog.all.size,
)

class ProfileViewModel(
    private val repository: TouchGrassRepository,
    private val socialRepository: SocialRepository? = null,
) : ViewModel() {

    var profileId: String = "GRASS-XXXXXX"
        private set

    init {
        viewModelScope.launch {
            val id = socialRepository?.ensureProfileCreated()
            if (id != null) {
                profileId = id
            }
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeProgress(),
        repository.observeUnlockedAchievements(),
        repository.observeCollectedCollectibles(),
    ) { progress, achievements, collectibles ->
        val level = ProgressCalculator.levelFromTotalXp(progress.totalXp)
        ProfileUiState(
            level = level,
            levelTitleRes = LevelTitles.titleResForLevel(level),
            totalXp = progress.totalXp,
            longestStreak = progress.longestStreak,
            totalOutdoorMinutes = progress.totalOutdoorMinutes,
            totalSessions = progress.totalSessionsCompleted,
            unlockedAchievements = achievements.size,
            totalAchievements = AchievementCatalog.all.size,
            collectedCount = collectibles.size,
            totalCollectibles = CollectibleCatalog.all.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    class Factory(
        private val repository: TouchGrassRepository,
        private val socialRepository: SocialRepository? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(repository, socialRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
