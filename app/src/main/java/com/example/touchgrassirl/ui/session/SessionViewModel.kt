package com.example.touchgrassirl.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.touchgrassirl.data.motion.SessionMotionTracker
import com.example.touchgrassirl.data.repository.SessionResult
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.data.service.TrackingService
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.domain.ProgressCalculator
import com.example.touchgrassirl.domain.SessionMotionSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val sessionId: Long? = null,
    val elapsedSeconds: Long = 0,
    val isEnding: Boolean = false,
    val minMinutesRequired: Int = GameConstants.MIN_OUTDOOR_MINUTES,
    val steps: Int = 0,
    val distanceMeters: Int = 0,
    val sessionXpEarned: Int = 0,
)

class SessionViewModel(
    private val repository: TouchGrassRepository,
    private val motionTracker: SessionMotionTracker,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _sessionEnded = MutableSharedFlow<SessionResult>(extraBufferCapacity = 1)
    val sessionEnded: SharedFlow<SessionResult> = _sessionEnded.asSharedFlow()

    private var timerJob: Job? = null
    private var startMillis: Long = 0L

    init {
        TrackingService.start(appContext)
        viewModelScope.launch {
            repository.ensureProgressInitialized()
            val session = repository.startSession()
            startMillis = session.startMillis
            _uiState.update { it.copy(sessionId = session.id) }
            motionTracker.start(viewModelScope)
            startTimer()
            observeMotion()
        }
    }

    private fun observeMotion() {
        viewModelScope.launch {
            motionTracker.stats.collect { snapshot ->
                val totalMinutes = ((System.currentTimeMillis() - startMillis) / 60_000L).toInt()
                val xp = ProgressCalculator.outdoorMinutesXp(totalMinutes)
                _uiState.update {
                    it.copy(
                        steps = snapshot.steps,
                        distanceMeters = snapshot.distanceMeters,
                        sessionXpEarned = xp,
                    )
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = ((System.currentTimeMillis() - startMillis) / 1000L)
                    .coerceAtLeast(0)
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
                delay(1_000)
            }
        }
    }

    fun endSession() {
        val sessionId = _uiState.value.sessionId ?: return
        if (_uiState.value.isEnding) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEnding = true) }
            timerJob?.cancel()
            val motion: SessionMotionSnapshot = motionTracker.stop()
            val result = repository.endSession(sessionId, motion)
            TrackingService.stop(appContext)
            _sessionEnded.emit(result)
        }
    }

    override fun onCleared() {
        motionTracker.stop()
        TrackingService.stop(appContext)
        super.onCleared()
    }

    class Factory(
        private val repository: TouchGrassRepository,
        private val motionTracker: SessionMotionTracker,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
                return SessionViewModel(repository, motionTracker, appContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
