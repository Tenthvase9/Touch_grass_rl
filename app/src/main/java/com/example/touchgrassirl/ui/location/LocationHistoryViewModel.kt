package com.example.touchgrassirl.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.local.entity.LocationEntity
import com.example.touchgrassirl.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationHistoryUiState(
    val locations: List<LocationEntity> = emptyList(),
)

class LocationHistoryViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationHistoryUiState())
    val uiState: StateFlow<LocationHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepository.observeLocations().collect { locations ->
                _uiState.update { it.copy(locations = locations) }
            }
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocationHistoryViewModel::class.java)) {
                return LocationHistoryViewModel(socialRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
