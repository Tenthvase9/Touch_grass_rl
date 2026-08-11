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

data class FriendActivityFeedUiState(
    val activities: List<ActivityEntity> = emptyList(),
)

class FriendActivityFeedViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendActivityFeedUiState())
    val uiState: StateFlow<FriendActivityFeedUiState> = _uiState.asStateFlow()

    init {
        loadFriendActivities()
    }

    private fun loadFriendActivities() {
        viewModelScope.launch {
            try {
                // Get friend UIDs first
                val friends = socialRepository.getAllFriendsStats()
                val allActivities = mutableListOf<ActivityEntity>()

                // For now, we'll use the current user's activities as a placeholder
                // In a full implementation, you'd query each friend's activities subcollection
                socialRepository.observeActivities().collect { myActivities ->
                    allActivities.addAll(myActivities)
                    // Sort by timestamp descending and take latest 50
                    val sorted = allActivities.sortedByDescending { it.timestampMillis }.take(50)
                    _uiState.update { it.copy(activities = sorted) }
                }
            } catch (e: Exception) {
                android.util.Log.e("FriendActivityFeedViewModel", "Failed to load activities", e)
            }
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FriendActivityFeedViewModel::class.java)) {
                return FriendActivityFeedViewModel(socialRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
