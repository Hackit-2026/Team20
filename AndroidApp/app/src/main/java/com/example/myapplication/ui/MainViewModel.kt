package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppRepo
import com.example.myapplication.data.AppSettings
import com.example.myapplication.data.ChallengeStats
import com.example.myapplication.data.TimelinePost
import com.example.myapplication.logic.StageLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val todayCount: Int = 10,
    val tempCount: Int = 10,
    val points: Int = 0,
    val isConfirmedToday: Boolean = false,
    val stageIndex: Int = 0,
    val currentEmoji: String = StageLogic.stageEmojis[0],
    val stageName: String = StageLogic.stageNames[0],
    val currentLine: String = "",
    val challengeStats: ChallengeStats? = null,
    val settings: AppSettings = AppSettings(),
    val allCounts: Map<String, Int> = emptyMap(),
    val feed: List<TimelinePost> = emptyList(),
    val isInitialized: Boolean = false,
    val isWithdrawal: Boolean = false,
    val isPosting: Boolean = false
)

class MainViewModel(private val repo: AppRepo) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        val settings = repo.getAppSettings()
        val todayCount = repo.getTodayCount()
        val points = repo.getPoints()
        val stageIndex = StageLogic.getStageIndex(points)
        val stats = repo.getChallengeStats()
        val allCounts = repo.getAllCounts()
        val confirmedToday = repo.isConfirmedToday()
        val feed = repo.getFeed()
        
        val isWithdrawal = todayCount == 0 && confirmedToday

        _uiState.update {
            it.copy(
                todayCount = todayCount,
                tempCount = if (!confirmedToday) settings.dailyGoal else todayCount,
                points = points,
                isConfirmedToday = confirmedToday,
                stageIndex = stageIndex,
                currentEmoji = StageLogic.stageEmojis[stageIndex],
                stageName = StageLogic.stageNames[stageIndex],
                currentLine = if (it.currentLine.isEmpty() || it.stageIndex != stageIndex || it.isWithdrawal != isWithdrawal) {
                    StageLogic.getRandomMessage(points, isWithdrawal)
                } else {
                    it.currentLine
                },
                challengeStats = stats,
                settings = settings,
                allCounts = allCounts,
                feed = feed,
                isInitialized = repo.isInitialized(),
                isWithdrawal = isWithdrawal
            )
        }
    }

    fun incrementTempCount() {
        _uiState.update { it.copy(tempCount = it.tempCount + 1) }
    }

    fun decrementTempCount() {
        _uiState.update { it.copy(tempCount = (it.tempCount - 1).coerceAtLeast(0)) }
    }

    fun confirmTodayCount() {
        repo.updateTodayCount(_uiState.value.tempCount)
        refreshState()
    }

    fun saveSettings(days: Int, goal: Int, notifyHour: Int, notifyMinute: Int) {
        val currentSettings = repo.getAppSettings()
        val newSettings = currentSettings.copy(
            days = days,
            dailyGoal = goal,
            notifyHour = notifyHour,
            notifyMinute = notifyMinute
        )
        repo.saveSettings(newSettings)
        _uiState.update { it.copy(tempCount = goal) }
        refreshState()
    }

    fun saveServerUrl(url: String) {
        repo.saveServerUrl(url)
        refreshState()
    }

    /**
     * 🌐 サーバー (FastAPI + LM Studio) へ投稿を非同期送信し、4人の短い返信コメントを取得
     */
    fun postToTimeline(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true) }
            repo.postToTimelineServer(text)
            _uiState.update { it.copy(isPosting = false) }
            refreshState()
        }
    }

    fun resetData() {
        repo.resetAllData()
        _uiState.update { UiState() }
        refreshState()
    }

    fun injectDummyData() {
        repo.injectDummyData()
        refreshState()
    }

    fun simulateSensorTrigger() {
        _uiState.update { it.copy(currentLine = "（ﾋﾟﾋﾟﾋﾟ!）心拍上昇と酸素低下を検知したぞ！さあ一本いこうぜ！") }
    }
}
