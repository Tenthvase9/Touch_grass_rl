package com.example.touchgrassirl.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GiftsUiState(
    val gifts: List<GiftEntity> = emptyList(),
)

class GiftsViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GiftsUiState())
    val uiState: StateFlow<GiftsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepository.observeGifts().collect { gifts ->
                _uiState.update {
                    it.copy(gifts = gifts.filter { g -> !g.claimed }.reversed())
                }
            }
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GiftsViewModel::class.java)) {
                return GiftsViewModel(socialRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
