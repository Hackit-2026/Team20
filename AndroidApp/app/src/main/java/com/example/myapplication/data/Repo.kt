package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class DailyReport(
    val reported: Boolean = false,
    val smoked: Boolean = false,
    val count: Int = 0,
)

@Serializable
data class AppData(
    val reports: Map<String, DailyReport> = emptyMap(),
    val notifyHour: Int = 21,
    val notifyMinute: Int = 0,
    // 目標は最初から0本。この日から吸わずに3か月経てば目標達成。
    // 吸ったと申告された翌日にリセットされる。
    val streakStartDate: String = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString(),
    val isInitialized: Boolean = false,
)

private val JST: ZoneId = ZoneId.of("Asia/Tokyo")

class AppRepo(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // 端末のタイムゾーンに関わらず、常に日本時間で「今日」を判定する
    fun getTodayDate(): String = LocalDate.now(JST).format(dateFormatter)

    private fun loadData(): AppData {
        val jsonString = prefs.getString("app_data", null) ?: return AppData()
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            AppData()
        }
    }

    private fun saveData(data: AppData) {
        prefs.edit { putString("app_data", json.encodeToString(data)) }
    }

    fun getTodayReport(): DailyReport = loadData().reports[getTodayDate()] ?: DailyReport()

    fun isReportedToday(): Boolean = getTodayReport().reported

    /**
     * 申告を保存する。吸っていない場合は本数を強制的に0にする。
     * 「吸った」を申告した場合、3か月目標のカウントは翌日から再スタートする。
     */
    fun submitReport(smoked: Boolean, count: Int) {
        val data = loadData()
        val today = getTodayDate()
        val newReports = data.reports.toMutableMap().apply {
            put(today, DailyReport(reported = true, smoked = smoked, count = if (smoked) count.coerceAtLeast(0) else 0))
        }
        val newStreakStart = if (smoked) {
            LocalDate.parse(today).plusDays(1).toString()
        } else {
            data.streakStartDate
        }
        saveData(data.copy(reports = newReports, streakStartDate = newStreakStart, isInitialized = true))
    }

    fun getAllReports(): Map<String, DailyReport> = loadData().reports

    fun isInitialized(): Boolean = loadData().isInitialized

    private fun getGoalDate(): LocalDate = LocalDate.parse(loadData().streakStartDate).plusMonths(3)

    fun isGoalAchieved(): Boolean = !LocalDate.now(JST).isBefore(getGoalDate())

    fun daysUntilGoal(): Long =
        ChronoUnit.DAYS.between(LocalDate.now(JST), getGoalDate()).coerceAtLeast(0)
}
