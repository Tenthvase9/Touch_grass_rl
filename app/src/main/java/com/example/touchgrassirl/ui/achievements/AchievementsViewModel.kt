package com.example.touchgrassirl.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.domain.AchievementCatalog
import com.example.touchgrassirl.domain.AchievementDefinition
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AchievementItemUi(
    val definition: AchievementDefinition,
    val unlocked: Boolean,
)

data class AchievementsUiState(
    val items: List<AchievementItemUi> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = AchievementCatalog.all.size,
)

class AchievementsViewModel(
    repository: TouchGrassRepository,
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = repository.observeUnlockedAchievements()
        .map { unlocked ->
            val ids = unlocked.map { it.id }.toSet()
            val items = AchievementCatalog.all.map { def ->
                AchievementItemUi(definition = def, unlocked = ids.contains(def.id))
            }
            AchievementsUiState(
                items = items,
                unlockedCount = ids.size,
                totalCount = AchievementCatalog.all.size,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementsUiState(),
        )

    class Factory(
        private val repository: TouchGrassRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
                return AchievementsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
