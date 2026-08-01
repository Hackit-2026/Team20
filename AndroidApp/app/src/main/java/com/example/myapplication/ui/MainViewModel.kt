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
    // 「吸った」を選んでから[申告する]を押すまでの一時入力状態。null = まだ選んでいない
    val tempSmoked: Boolean? = null,
    val tempCount: Int = 1,
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

    /** 「吸った」を選択 → 本数ステッパーを表示する */
    fun chooseSmoked() {
        _uiState.update { it.copy(tempSmoked = true, tempCount = 1) }
    }

    /** 「吸わなかった」を選択 → その場で確定保存する */
    fun reportNoSmoke() {
        repo.submitReport(smoked = false, count = 0)
        refreshState()
    }

    fun incrementTempCount() {
        _uiState.update { it.copy(tempCount = it.tempCount + 1) }
    }

    fun decrementTempCount() {
        _uiState.update { it.copy(tempCount = (it.tempCount - 1).coerceAtLeast(1)) }
    }

    /** 本数を確定して「吸った」申告を保存する */
    fun confirmSmokedReport() {
        val count = _uiState.value.tempCount
        repo.submitReport(smoked = true, count = count)
        refreshState()
    }
}
