package com.example.touchgrassirl.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.local.entity.ActivityEntity
import com.example.touchgrassirl.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityFeedUiState(
    val activities: List<ActivityEntity> = emptyList(),
)

class ActivityFeedViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityFeedUiState())
    val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepository.observeActivities().collect { activities ->
                _uiState.update {
                    it.copy(activities = activities)
                }
            }
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ActivityFeedViewModel::class.java)) {
                return ActivityFeedViewModel(socialRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
