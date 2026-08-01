package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.AppRepo
import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.ChallengeStats
import com.example.myapplication.logic.CharacterStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val todayCount: Int = 0,
    val currentStage: Int = 0,
    val challengeStats: ChallengeStats? = null,
    val settings: AppSettings = AppSettings(),
    val allCounts: Map<String, Int> = emptyMap(),
    val isInitialized: Boolean = false
)

class MainViewModel(private val repo: AppRepo) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    private fun refreshState() {
        val todayCount = repo.getTodayCount()
        val stage = CharacterStage.fromCount(todayCount)
        val stats = repo.getChallengeStats()
        val settings = repo.getAppSettings()
        val allCounts = repo.getAllCounts()
        val initialized = repo.isInitialized()

        _uiState.update {
            it.copy(
                todayCount = todayCount,
                currentStage = stage,
                challengeStats = stats,
                settings = settings,
                allCounts = allCounts,
                isInitialized = initialized
            )
        }
    }

    fun incrementCount() {
        repo.updateTodayCount(1)
        refreshState()
    }

    fun decrementCount() {
        repo.updateTodayCount(-1)
        refreshState()
    }

    fun saveSettings(days: Int, goal: Int, notifyHour: Int) {
        val currentSettings = repo.getAppSettings()
        val newSettings = currentSettings.copy(
            days = days,
            dailyGoal = goal,
            notifyHour = notifyHour
        )
        repo.saveSettings(newSettings)
        refreshState()
    }
}
