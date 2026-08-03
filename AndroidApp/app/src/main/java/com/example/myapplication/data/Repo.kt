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

enum class GoalMode {
    MODE_1_GRADUAL, // Mode 1: 今日の目標 = 今までの1週間の平均値 / 1.5 (切り捨て)
    MODE_2_ZERO     // Mode 2: 今日の目標は常に0にする
}

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
    val heavyPenaltyLockUntil: Long = 0L,
    val penaltyDismissedUntil: Long = 0L,
    val lastOverlayTriggerTime: Long = 0L, // 🚨 最後に他アプリ上でペナルティを表示した時刻
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

    /**
     * 🎯 【新要件】Mode 1 算出計算
     * 今日の目標 = 今までの1週間の平均値 / 1.5 (小数点以下切り捨て)
     */
    fun calculateMode1Goal(): Int {
        val weeklyAvg = getWeeklyDailyAverage()
        return floor(weeklyAvg / 1.5).toInt().coerceAtLeast(0)
    }

    /**
     * 🎯 2つの目標調整モードの適用。
     */
    fun applyGoalMode(mode: GoalMode, manualAverage: Int? = null) {
        val nextGoal = when (mode) {
            GoalMode.MODE_1_GRADUAL -> manualAverage?.coerceAtLeast(0) ?: calculateMode1Goal()
            GoalMode.MODE_2_ZERO -> 0
        }
        val data = loadData()
        saveData(data.copy(currentGoal = nextGoal))
    }

    /**
     * 🚨 【新要件】吸った本数に応じて他アプリ起動時のペナルティ発生頻度（インターバルミリ秒）を計算。
     * - 吸った本数が少ない（0本付近）: 2時間ごと (7,200,000 ms)
     * - 吸った本数が多い（20本以上）: 30秒ごと (30,000 ms)
     */
    fun calculatePenaltyIntervalMs(): Long {
        val count = getWeeklyTotalExcludingToday()
        val clampedCount = count.coerceIn(0, 20)
        val maxIntervalMs = 7_200_000L // 2時間 (7,200,000 ms)
        val minIntervalMs = 30_000L    // 30秒 (30,000 ms)
        val interval = maxIntervalMs - ((clampedCount / 20.0) * (maxIntervalMs - minIntervalMs)).toLong()
        return interval.coerceIn(minIntervalMs, maxIntervalMs)
    }

    fun isOverlayIntervalPassed(): Boolean {
        val data = loadData()
        val now = System.currentTimeMillis()
        val interval = calculatePenaltyIntervalMs()
        return (now - data.lastOverlayTriggerTime) >= interval
    }

    fun recordOverlayTriggerTime() {
        val data = loadData()
        saveData(data.copy(lastOverlayTriggerTime = System.currentTimeMillis()))
    }

    fun getInitialCountForToday(): Int {
        val data = loadData()
        val today = LocalDate.now(JST)
        var maxCount = 0

        for (i in 1..28) {
            val dateStr = today.minusDays(i.toLong()).format(dateFormatter)
            val report = data.reports[dateStr]
            if (report != null && report.smoked) {
                if (report.count > maxCount) {
                    maxCount = report.count
                }
            }
        }

        return if (maxCount > 0) {
            floor(maxCount * 2.0 / 3.0).toInt().coerceAtLeast(0)
        } else {
            data.currentGoal
        }
    }

    fun getNotifyHour(): Int = loadData().notifyHour

    fun getNotifyMinute(): Int = loadData().notifyMinute

    fun saveNotifyTime(hour: Int, minute: Int) {
        val data = loadData()
        saveData(data.copy(notifyHour = hour, notifyMinute = minute))
    }

    fun triggerHeavyPenaltyLock() {
        val data = loadData()
        val now = System.currentTimeMillis()
        if (data.heavyPenaltyLockUntil < now && data.penaltyDismissedUntil < now) {
            // ペナルティ時間: 10秒間固定操作ロック
            saveData(data.copy(heavyPenaltyLockUntil = now + 10_000L))
        }
    }

    fun isHeavyPenaltyActive(): Boolean {
        val data = loadData()
        return System.currentTimeMillis() < data.heavyPenaltyLockUntil
    }

    fun clearHeavyPenaltyLock() {
        val data = loadData()
        saveData(data.copy(heavyPenaltyLockUntil = 0L, penaltyDismissedUntil = 0L))
    }

    fun getRemainingPenaltySeconds(): Int {
        val remainingMs = loadData().heavyPenaltyLockUntil - System.currentTimeMillis()
        return (remainingMs / 1000).coerceAtLeast(0).toInt()
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

    fun getCharacterStage(): Int {
        val data = loadData()
        val today = LocalDate.now(JST)
        var total = 0
        for (i in 1..30) {
            val date = today.minusDays(i.toLong()).format(dateFormatter)
            total += data.reports[date]?.count ?: 0
        }
        return total.coerceIn(0, 20)
    }

    fun getWeeklyTotal(): Int {
        val data = loadData()
        val today = LocalDate.now(JST)
        return (0..6).sumOf { offset ->
            val date = today.minusDays(offset.toLong()).format(dateFormatter)
            data.reports[date]?.count ?: 0
        }
    }

    fun getWeeklyTotalExcludingToday(): Int {
        val data = loadData()
        val today = LocalDate.now(JST)
        return (1..7).sumOf { offset ->
            val date = today.minusDays(offset.toLong()).format(dateFormatter)
            data.reports[date]?.count ?: 0
        }
    }

    fun getWeeklyDailyAverage(): Double {
        return getWeeklyTotal() / 7.0
    }

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

    fun completeOnboarding(initialDailyGoal: Int) {
        val data = loadData()
        saveData(data.copy(currentGoal = initialDailyGoal.coerceAtLeast(0), isInitialized = true))
    }

    fun resetAllData() {
        saveData(AppData())
    }

    private fun getGoalDate(): LocalDate = LocalDate.parse(loadData().streakStartDate).plusMonths(3)

    fun isGoalAchieved(): Boolean = !LocalDate.now(JST).isBefore(getGoalDate())

    fun daysUntilGoal(): Long =
        ChronoUnit.DAYS.between(LocalDate.now(JST), getGoalDate()).coerceAtLeast(0)
}
