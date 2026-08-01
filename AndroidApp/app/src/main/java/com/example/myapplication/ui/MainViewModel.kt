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
    // 本数入力の一時状態。0本のまま保存すると「吸わなかった」扱いになる
    val tempCount: Int = 0,
    // 目標は0本からスタートし、吸わずに3か月経つと達成
    val daysUntilGoal: Long = 0,
    val goalAchieved: Boolean = false,
)

class MainViewModel(private val repo: AppRepo) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    private fun refreshState() {
        _uiState.update {
            UiState(
                today = repo.getTodayReport(),
                daysUntilGoal = repo.daysUntilGoal(),
                goalAchieved = repo.isGoalAchieved(),
            )
        }
    }

    fun incrementTempCount() {
        _uiState.update { it.copy(tempCount = it.tempCount + 1) }
    }

    fun decrementTempCount() {
        _uiState.update { it.copy(tempCount = (it.tempCount - 1).coerceAtLeast(0)) }
    }

    /** 本数を保存する。0本なら「吸わなかった」、1本以上なら「吸った」扱い。 */
    fun saveReport() {
        val count = _uiState.value.tempCount
        repo.submitReport(smoked = count > 0, count = count)
        refreshState()
    }
}
