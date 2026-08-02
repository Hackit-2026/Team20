package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.AppRepo
import com.example.myapplication.data.DailyReport
import com.example.myapplication.data.GoalMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val today: DailyReport = DailyReport(),
    val tempCount: Int = 10,
    val initialCount: Int = 10,
    val daysUntilGoal: Long = 0,
    val goalAchieved: Boolean = false,
    val weeklyTotal: Int = 0,
    val weeklyDailyAverage: Double = 0.0,
    val weightedAverage: Double = 0.0,
    val weightedPenaltyValue: Double = 0.0,
    val isHeavyPenaltyActive: Boolean = false,
    val remainingPenaltySeconds: Int = 0,
    val currentGoal: Int = 10,
    val calculatedNextGoal: Int = 10,
    val mode1Goal: Int = 0, // Mode 1: 今週平均 / 1.5 (切り捨て) 算出値
    val notifyHour: Int = 21,
    val notifyMinute: Int = 0,
    val allReports: Map<String, DailyReport> = emptyMap(),
    val characterStage: Int = 0, // 🐣 前日〜1ヶ月の累積本数(0〜20の21段階)
    // ペナルティ判定(今日の申告は含めない。保存した当日には発動せず、日付が変わってから発動する)
    val isPenaltyThresholdMet: Boolean = false,
    val isInitialized: Boolean = false, // 初回オンボーディングが完了しているか
)

class MainViewModel(private val repo: AppRepo) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        val today = repo.getTodayReport()
        val currentGoal = repo.getCurrentGoal()
        val initialCount = repo.getInitialCountForToday()
        _uiState.update {
            UiState(
                today = today,
                tempCount = if (today.reported) today.count else initialCount,
                initialCount = initialCount,
                daysUntilGoal = repo.daysUntilGoal(),
                goalAchieved = repo.isGoalAchieved(),
                weeklyTotal = repo.getWeeklyTotal(),
                weeklyDailyAverage = repo.getWeeklyDailyAverage(),
                weightedAverage = repo.calculateWeightedAverage(),
                weightedPenaltyValue = repo.calculateWeightedPenaltyValue(),
                isHeavyPenaltyActive = repo.isHeavyPenaltyActive(),
                remainingPenaltySeconds = repo.getRemainingPenaltySeconds(),
                currentGoal = currentGoal,
                calculatedNextGoal = repo.calculateNextGoal(1.5),
                mode1Goal = repo.calculateMode1Goal(),
                notifyHour = repo.getNotifyHour(),
                notifyMinute = repo.getNotifyMinute(),
                allReports = repo.getAllReports(),
                characterStage = repo.getCharacterStage(),
                isPenaltyThresholdMet = repo.getWeeklyTotalExcludingToday() >= 2 || repo.calculateWeightedPenaltyValue() >= 5.0,
                isInitialized = repo.isInitialized(),
            )
        }
    }

    fun triggerHeavyPenaltyLock() {
        repo.triggerHeavyPenaltyLock()
        refreshState()
    }

    fun clearHeavyPenaltyLock() {
        repo.clearHeavyPenaltyLock()
        refreshState()
    }

    fun applyGoalMode(mode: GoalMode) {
        repo.applyGoalMode(mode)
        refreshState()
    }

    fun incrementTempCount() {
        _uiState.update { it.copy(tempCount = it.tempCount + 1) }
    }

    fun decrementTempCount() {
        _uiState.update { it.copy(tempCount = (it.tempCount - 1).coerceAtLeast(0)) }
    }

    fun saveReport() {
        val count = _uiState.value.tempCount
        repo.submitReport(smoked = count > 0, count = count)
        refreshState()
    }

    fun submitReportForDate(dateStr: String, count: Int) {
        repo.submitReportForDate(dateStr, smoked = count > 0, count = count)
        refreshState()
    }

    fun applyNextGoal(difficulty: Double) {
        repo.applyNextGoal(difficulty)
        refreshState()
    }

    fun inject30DaysDemoData() {
        repo.inject30DaysDemoData()
        refreshState()
    }

    fun saveNotifyTime(hour: Int, minute: Int) {
        repo.saveNotifyTime(hour, minute)
        refreshState()
    }

    fun resetAll() {
        repo.resetAllData()
        refreshState()
    }

    fun completeOnboarding(initialDailyGoal: Int) {
        repo.completeOnboarding(initialDailyGoal)
        refreshState()
    }
}
