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
                currentGoal = currentGoal,
                calculatedNextGoal = repo.calculateNextGoal(1.0), // デフォルト難易度 1.0
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

    /** 本数を保存する。0本なら「吸わなかった」、1本以上なら「吸った」扱い。本日分の修正もこれで行う。 */
    fun saveReport() {
        val count = _uiState.value.tempCount
        repo.submitReport(smoked = count > 0, count = count)
        refreshState()
    }

    /** 🛠️ デバッグ機能: 何月何日 (YYYY-MM-DD) を指定してタバコデータを投入 */
    fun submitReportForDate(dateStr: String, count: Int) {
        repo.submitReportForDate(dateStr, smoked = count > 0, count = count)
        refreshState()
    }

    /** 🎯 きつさ(難易度)に応じた次の目標を計算＆適用 */
    fun applyNextGoal(difficulty: Double) {
        repo.applyNextGoal(difficulty)
        refreshState()
    }

    /** 📊 過去30日間のデモデータを一括挿入 */
    fun inject30DaysDemoData() {
        repo.inject30DaysDemoData()
        refreshState()
    }

    /** ⏰ 通知時刻を変更 */
    fun saveNotifyTime(hour: Int, minute: Int) {
        repo.saveNotifyTime(hour, minute)
        refreshState()
    }

    /** 🔄 全データを削除して最初からやり直す(完全リセット) */
    fun resetAll() {
        repo.resetAllData()
        refreshState()
    }
}
