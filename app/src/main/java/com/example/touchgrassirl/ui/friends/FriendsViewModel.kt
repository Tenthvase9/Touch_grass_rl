package com.example.touchgrassirl.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.repository.PendingRequestInfo
import com.example.touchgrassirl.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<FriendEntity> = emptyList(),
    val pendingRequests: List<PendingRequestInfo> = emptyList(),
)

class FriendsViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepository.ensureProfileCreated()
        }

        viewModelScope.launch {
            socialRepository.observeFriends().collect { friends ->
                _uiState.update {
                    it.copy(friends = friends.filter { f -> f.status == "accepted" })
                }
            }
        }

        viewModelScope.launch {
            socialRepository.observePendingRequests().collect { requests ->
                _uiState.update {
                    it.copy(pendingRequests = requests)
                }
            }
        }
    }

    fun sendFriendRequest(profileId: String) {
        viewModelScope.launch {
            socialRepository.sendFriendRequest(profileId)
        }
    }

    fun acceptRequest(profileId: String) {
        viewModelScope.launch {
            socialRepository.acceptFriendRequest(profileId)
        }
    }

    fun declineRequest(profileId: String) {
        viewModelScope.launch {
            socialRepository.declineFriendRequest(profileId)
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
                return FriendsViewModel(socialRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
