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
    val currentGoal: Int = 10, // 現在の1日あたり目標本数（初期値10本）
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

    /**
     * 申告を保存する。
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

    /**
     * 🛠️ デバッグ機能: 何月何日 (YYYY-MM-DD) を指定してタバコのデータを入れる
     */
    fun submitReportForDate(dateStr: String, smoked: Boolean, count: Int) {
        val data = loadData()
        val newReports = data.reports.toMutableMap().apply {
            put(dateStr, DailyReport(reported = true, smoked = smoked, count = if (smoked) count.coerceAtLeast(0) else 0))
        }
        saveData(data.copy(reports = newReports, isInitialized = true))
    }

    fun getAllReports(): Map<String, DailyReport> = loadData().reports

    /** 過去7日間(今日を含む)の合計本数 */
    fun getWeeklyTotal(): Int {
        val data = loadData()
        val today = LocalDate.now(JST)
        return (0..6).sumOf { offset ->
            val date = today.minusDays(offset.toLong()).format(dateFormatter)
            data.reports[date]?.count ?: 0
        }
    }

    /** 過去7日間の1日あたり単純平均本数 */
    fun getWeeklyDailyAverage(): Double {
        return getWeeklyTotal() / 7.0
    }

    /**
     * ⚖️ 先日から1週間前までのタバコの吸った量の重みづけ (Weighted Average)
     * 昨日=1.0, 2日前=0.85, 3日前=0.7, 4日前=0.55, 5日前=0.4, 6日前=0.25, 7日前=0.1
     */
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
     * 🎯 目標を少しずつ減らす計算
     * 計算式: 次の目標 = floor(今週の1日あたり平均吸った本数 - ユーザーが決めたきつさの値)
     * ※ 今回の目標が 0 であれば 0 で出力する。
     */
    fun calculateNextGoal(difficulty: Double): Int {
        val data = loadData()
        val currentGoal = data.currentGoal
        if (currentGoal <= 0) return 0 // 今回の目標が0であれば0で出力

        val weeklyAvg = getWeeklyDailyAverage()
        val nextGoalCalc = floor(weeklyAvg - difficulty).toInt()
        return nextGoalCalc.coerceIn(0, currentGoal)
    }

    fun applyNextGoal(difficulty: Double) {
        val nextGoal = calculateNextGoal(difficulty)
        val data = loadData()
        saveData(data.copy(currentGoal = nextGoal))
    }

    /**
     * 📊 デバッグ用: 過去30日間のデモデータを一括自動挿入
     */
    fun inject30DaysDemoData() {
        val data = loadData()
        val today = LocalDate.now(JST)
        val newReports = data.reports.toMutableMap()

        for (i in 1..30) {
            val dateStr = today.minusDays(i.toLong()).format(dateFormatter)
            val smoked = (0..100).random() > 30 // 70%の確率で吸った
            val count = if (smoked) (1..15).random() else 0
            newReports[dateStr] = DailyReport(reported = true, smoked = smoked, count = count)
        }

        saveData(data.copy(reports = newReports, isInitialized = true))
    }

    fun isInitialized(): Boolean = loadData().isInitialized

    /** 全データをリセットし始めからスタートする機能 */
    fun resetAllData() {
        saveData(AppData())
    }

    private fun getGoalDate(): LocalDate = LocalDate.parse(loadData().streakStartDate).plusMonths(3)

    fun isGoalAchieved(): Boolean = !LocalDate.now(JST).isBefore(getGoalDate())

    fun daysUntilGoal(): Long =
        ChronoUnit.DAYS.between(LocalDate.now(JST), getGoalDate()).coerceAtLeast(0)
}
