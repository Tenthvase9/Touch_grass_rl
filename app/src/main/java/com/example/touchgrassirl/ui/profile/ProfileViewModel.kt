package com.example.touchgrassirl.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.touchgrassirl.domain.AchievementCatalog
import com.example.touchgrassirl.domain.CollectibleCatalog
import com.example.touchgrassirl.domain.LevelTitles
import com.example.touchgrassirl.domain.ProgressCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val level: Int = 1,
    @StringRes val levelTitleRes: Int = LevelTitles.titleResForLevel(1),
    val totalXp: Int = 0,
    val totalOutdoorMinutes: Int = 0,
    val totalSessions: Int = 0,
    val unlockedAchievements: Int = 0,
    val totalAchievements: Int = AchievementCatalog.all.size,
    val collectedCount: Int = 0,
    val totalCollectibles: Int = CollectibleCatalog.all.size,
    val weatherBadges: Map<String, Int> = emptyMap(),
)

class ProfileViewModel(
    private val repository: TouchGrassRepository,
    private val socialRepository: SocialRepository? = null,
) : ViewModel() {

    private val _profileId = MutableStateFlow("GRASS-XXXXXX")
    val profileId: StateFlow<String> = _profileId.asStateFlow()

    private val _displayName = MutableStateFlow("Nature Explorer")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    private val _avatar = MutableStateFlow("\uD83C\uDF31")
    val avatar: StateFlow<String> = _avatar.asStateFlow()

    private val _weatherBadges = MutableStateFlow<Map<String, Int>>(emptyMap())
    val weatherBadges: StateFlow<Map<String, Int>> = _weatherBadges.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val repo = socialRepository
                if (repo == null) {
                    _profileId.value = "GRASS-NO-REPO"
                    return@launch
                }
                val id = repo.ensureProfileCreated()
                _profileId.value = id
                val name = repo.getMyDisplayName()
                _displayName.value = name
                val profile = repo.getMyProfile()
                _bio.value = (profile["bio"] as? String) ?: ""
                _avatar.value = (profile["avatar"] as? String) ?: "\uD83C\uDF31"
                @Suppress("UNCHECKED_CAST")
                _weatherBadges.value = (profile["weatherBadges"] as? Map<String, Int>) ?: emptyMap()
                val streakPair = repo.getStreak()
                _currentStreak.value = streakPair.first
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to load profile", e)
                _profileId.value = "GRASS-ERROR"
            }
        }
    }

    fun updateDisplayName(name: String) {
        _displayName.value = name
        viewModelScope.launch {
            try {
                socialRepository?.updateDisplayName(name)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to update name", e)
            }
        }
    }

    fun updateProfile(bio: String, avatar: String) {
        _bio.value = bio
        _avatar.value = avatar
        viewModelScope.launch {
            try {
                socialRepository?.updateProfile(bio, avatar)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to update profile", e)
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
