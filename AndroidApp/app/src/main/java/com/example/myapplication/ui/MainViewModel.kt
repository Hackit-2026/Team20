package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.AppRepo
import com.example.myapplication.data.DailyReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val today: DailyReport = DailyReport(),
    val tempCount: Int = 10,
    val daysUntilGoal: Long = 0,
    val goalAchieved: Boolean = false,
    val weeklyTotal: Int = 0,
    val weeklyDailyAverage: Double = 0.0,
    val weightedAverage: Double = 0.0,
    val weightedPenaltyValue: Double = 0.0, // 新ペナルティ評価値
    val currentGoal: Int = 10,
    val calculatedNextGoal: Int = 10,
    val notifyHour: Int = 21,
    val notifyMinute: Int = 0,
    val allReports: Map<String, DailyReport> = emptyMap(),
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
        _uiState.update {
            UiState(
                today = today,
                tempCount = if (today.reported) today.count else currentGoal,
                daysUntilGoal = repo.daysUntilGoal(),
                goalAchieved = repo.isGoalAchieved(),
                weeklyTotal = repo.getWeeklyTotal(),
                weeklyDailyAverage = repo.getWeeklyDailyAverage(),
                weightedAverage = repo.calculateWeightedAverage(),
                weightedPenaltyValue = repo.calculateWeightedPenaltyValue(),
                currentGoal = currentGoal,
                calculatedNextGoal = repo.calculateNextGoal(1.5), // デフォルトきつさ 1.5
                notifyHour = repo.getNotifyHour(),
                notifyMinute = repo.getNotifyMinute(),
                allReports = repo.getAllReports(),
            )
        }
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
}
