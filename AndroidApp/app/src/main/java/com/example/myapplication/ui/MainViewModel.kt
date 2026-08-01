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
    // 本数入力の一時状態。未申告時は初期値10本、申告済みなら既存の値を編集できるように
    // その値を読み込む(本日のデータの修正用)
    val tempCount: Int = 10,
    // 目標は0本からスタートし、吸わずに3か月経つと達成
    val daysUntilGoal: Long = 0,
    val goalAchieved: Boolean = false,
    // 過去7日間の合計本数。2本以上で重いペナルティの対象になる
    val weeklyTotal: Int = 0,
    // 日付(YYYY-MM-DD) → その日の申告、の一覧。新しい日付順ではなくストア順そのまま
    val allReports: Map<String, DailyReport> = emptyMap(),
)

class MainViewModel(private val repo: AppRepo) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    private fun refreshState() {
        val today = repo.getTodayReport()
        _uiState.update {
            UiState(
                today = today,
                tempCount = if (today.reported) today.count else 10,
                daysUntilGoal = repo.daysUntilGoal(),
                goalAchieved = repo.isGoalAchieved(),
                weeklyTotal = repo.getWeeklyTotal(),
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

    /** 全データを削除して最初からやり直す(デバッグ用) */
    fun resetAll() {
        repo.resetAllData()
        refreshState()
    }
}
