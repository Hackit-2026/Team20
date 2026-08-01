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
import kotlin.math.floor

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
    val currentGoal: Int = 10,
    val streakStartDate: String = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString(),
    val isInitialized: Boolean = false,
)

private val JST: ZoneId = ZoneId.of("Asia/Tokyo")

class AppRepo(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

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

    fun getCurrentGoal(): Int = loadData().currentGoal

    fun getNotifyHour(): Int = loadData().notifyHour

    fun getNotifyMinute(): Int = loadData().notifyMinute

    fun saveNotifyTime(hour: Int, minute: Int) {
        val data = loadData()
        saveData(data.copy(notifyHour = hour, notifyMinute = minute))
    }

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

    fun submitReportForDate(dateStr: String, smoked: Boolean, count: Int) {
        val data = loadData()
        val newReports = data.reports.toMutableMap().apply {
            put(dateStr, DailyReport(reported = true, smoked = smoked, count = if (smoked) count.coerceAtLeast(0) else 0))
        }
        saveData(data.copy(reports = newReports, isInitialized = true))
    }

    fun getAllReports(): Map<String, DailyReport> = loadData().reports

    fun getWeeklyTotal(): Int {
        val data = loadData()
        val today = LocalDate.now(JST)
        return (0..6).sumOf { offset ->
            val date = today.minusDays(offset.toLong()).format(dateFormatter)
            data.reports[date]?.count ?: 0
        }
    }

    fun getWeeklyDailyAverage(): Double {
        return getWeeklyTotal() / 7.0
    }

    /**
     * 🚨 【新ペナルティ計算公式】
     * PenaltyValue = (前日 * 2.0) + (2日前 * 1.5) + (3日前 * 1.2) + (4日前 * 1.0) + (5日前 * 0.8) + (6日前 * 0.5)
     */
    fun calculateWeightedPenaltyValue(): Double {
        val data = loadData()
        val today = LocalDate.now(JST)
        val weights = listOf(2.0, 1.5, 1.2, 1.0, 0.8, 0.5)
        var totalPenalty = 0.0

        for (i in 0 until 6) {
            val date = today.minusDays((i + 1).toLong()).format(dateFormatter)
            val count = data.reports[date]?.count ?: 0
            totalPenalty += count * weights[i]
        }
        return totalPenalty
    }

    fun calculateWeightedAverage(): Double {
        val data = loadData()
        val today = LocalDate.now(JST)
        val weights = listOf(1.0, 0.85, 0.7, 0.55, 0.4, 0.25, 0.1)
        var weightedSum = 0.0
        var totalWeight = 0.0

        for (i in 0 until 7) {
            val date = today.minusDays((i + 1).toLong()).format(dateFormatter)
            val count = data.reports[date]?.count ?: 0
            val w = weights[i]
            weightedSum += count * w
            totalWeight += w
        }
        return if (totalWeight > 0) weightedSum / totalWeight else 0.0
    }

    /**
     * 🎯 【新・目標引き下げ計算公式】
     * 次の目標 = floor(今週の1日あたり平均吸った本数 / ユーザーが決めたきつさの値)
     * きつさの値の範囲: 1.2 ～ 20.0
     * ※ 今回の目標が 0 であれば無条件で 0 を出力。
     */
    fun calculateNextGoal(difficulty: Double): Int {
        val data = loadData()
        val currentGoal = data.currentGoal
        if (currentGoal <= 0) return 0

        val safeDifficulty = difficulty.coerceIn(1.2, 20.0)
        val weeklyAvg = getWeeklyDailyAverage()
        val nextGoalCalc = floor(weeklyAvg / safeDifficulty).toInt()
        return nextGoalCalc.coerceIn(0, currentGoal)
    }

    fun applyNextGoal(difficulty: Double) {
        val nextGoal = calculateNextGoal(difficulty)
        val data = loadData()
        saveData(data.copy(currentGoal = nextGoal))
    }

    fun inject30DaysDemoData() {
        val data = loadData()
        val today = LocalDate.now(JST)
        val newReports = data.reports.toMutableMap()

        for (i in 1..30) {
            val dateStr = today.minusDays(i.toLong()).format(dateFormatter)
            val smoked = (0..100).random() > 30
            val count = if (smoked) (1..15).random() else 0
            newReports[dateStr] = DailyReport(reported = true, smoked = smoked, count = count)
        }

        saveData(data.copy(reports = newReports, isInitialized = true))
    }

    fun isInitialized(): Boolean = loadData().isInitialized

    fun resetAllData() {
        saveData(AppData())
    }

    private fun getGoalDate(): LocalDate = LocalDate.parse(loadData().streakStartDate).plusMonths(3)

    fun isGoalAchieved(): Boolean = !LocalDate.now(JST).isBefore(getGoalDate())

    fun daysUntilGoal(): Long =
        ChronoUnit.DAYS.between(LocalDate.now(JST), getGoalDate()).coerceAtLeast(0)
}
