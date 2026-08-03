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
     * 🎯 2つの目標調整モードの適用
     */
    fun applyGoalMode(mode: GoalMode) {
        val nextGoal = when (mode) {
            GoalMode.MODE_1_GRADUAL -> calculateMode1Goal()
            GoalMode.MODE_2_ZERO -> 0
        }
        val data = loadData()
        saveData(data.copy(currentGoal = nextGoal))
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

    fun getPenaltyIntervalMs(count: Int): Long {
        return when {
            count >= 20 -> 30_000L // 20本以上: 30秒ごとにスマホ使用不可
            count in 1..5 -> 2 * 3600 * 1000L // 5本まで: 2時間ごとにスマホ使用不可
            count > 5 -> {
                val ratio = (count - 5).toDouble() / (20 - 5)
                val twoHoursMs = 2 * 3600 * 1000L
                val thirtySecMs = 30_000L
                (twoHoursMs - ratio * (twoHoursMs - thirtySecMs)).toLong()
            }
            else -> 2 * 3600 * 1000L
        }
    }

    fun triggerHeavyPenaltyLock(count: Int = getTodayReport().count) {
        val data = loadData()
        val now = System.currentTimeMillis()
        if (data.heavyPenaltyLockUntil < now && data.penaltyDismissedUntil < now) {
            val lockDurationMs = 30_000L // 30秒間操作ブロック
            val cooldownMs = getPenaltyIntervalMs(count)
            saveData(data.copy(
                heavyPenaltyLockUntil = now + lockDurationMs,
                penaltyDismissedUntil = now + lockDurationMs + cooldownMs
            ))
        }
    }

    fun isHeavyPenaltyActive(): Boolean {
        val data = loadData()
        return System.currentTimeMillis() < data.heavyPenaltyLockUntil
    }

    fun clearHeavyPenaltyLock(count: Int = getTodayReport().count) {
        val data = loadData()
        val now = System.currentTimeMillis()
        val cooldownMs = getPenaltyIntervalMs(count)
        saveData(data.copy(
            heavyPenaltyLockUntil = 0L,
            penaltyDismissedUntil = now + cooldownMs
        ))
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

    /**
     * 🐣 キャラ成長度: 前日から1ヶ月前(30日間)までの累積本数。
     * 1本 = 1段階、上限20(char_stage_0〜20 の21段階に対応)
     */
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

    fun resetAllData() {
        saveData(AppData())
    }

    private fun getGoalDate(): LocalDate = LocalDate.parse(loadData().streakStartDate).plusMonths(3)

    fun isGoalAchieved(): Boolean = !LocalDate.now(JST).isBefore(getGoalDate())

    fun daysUntilGoal(): Long =
        ChronoUnit.DAYS.between(LocalDate.now(JST), getGoalDate()).coerceAtLeast(0)
}
