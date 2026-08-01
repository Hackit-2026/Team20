package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class AppSettings(
    val startDate: String = LocalDate.now().toString(),
    val days: Int = 7,
    val dailyGoal: Int = 0,
    val notifyHour: Int = 21
)

@Serializable
data class AppData(
    val settings: AppSettings = AppSettings(),
    val counts: Map<String, Int> = emptyMap(),
    val isInitialized: Boolean = false
)

class AppRepo(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getTodayDate(): String = LocalDate.now().format(dateFormatter)

    private fun loadData(): AppData {
        val jsonString = prefs.getString("app_data", null) ?: return AppData()
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            AppData()
        }
    }

    private fun saveData(data: AppData) {
        val jsonString = json.encodeToString(data)
        prefs.edit {
            putString("app_data", jsonString)
        }
    }

    fun getAppSettings(): AppSettings = loadData().settings

    fun saveSettings(settings: AppSettings) {
        val data = loadData()
        saveData(data.copy(settings = settings, isInitialized = true))
    }

    fun isInitialized(): Boolean = loadData().isInitialized

    fun getCount(date: String): Int = loadData().counts[date] ?: 0

    fun getTodayCount(): Int = getCount(getTodayDate())

    fun updateTodayCount(delta: Int) {
        val data = loadData()
        val today = getTodayDate()
        val currentCount = data.counts[today] ?: 0
        val newCount = (currentCount + delta).coerceAtLeast(0)
        val newCounts = data.counts.toMutableMap().apply {
            put(today, newCount)
        }
        saveData(data.copy(counts = newCounts))
    }

    fun getAllCounts(): Map<String, Int> = loadData().counts

    fun getChallengeStats(): ChallengeStats {
        val data = loadData()
        val startDate = LocalDate.parse(data.settings.startDate, dateFormatter)
        val endDate = startDate.plusDays(data.settings.days.toLong() - 1)
        val today = LocalDate.now()
        
        val daysLeft = if (today.isAfter(endDate)) 0 
                       else if (today.isBefore(startDate)) data.settings.days
                       else (java.time.temporal.ChronoUnit.DAYS.between(today, endDate) + 1).toInt()

        val totalCount = data.counts.values.sum()
        val averageDaily = if (data.counts.isEmpty()) 0f else totalCount.toFloat() / data.counts.size
        val targetTotal = data.settings.dailyGoal * data.settings.days
        val isTargetAchieved = totalCount <= targetTotal

        return ChallengeStats(
            daysLeft = daysLeft,
            totalCount = totalCount,
            averageDaily = averageDaily,
            isTargetAchieved = isTargetAchieved,
            targetTotal = targetTotal
        )
    }

    fun resetAllData() {
        saveData(AppData())
    }

    fun addDummyData() {
        val data = loadData()
        val today = LocalDate.now()
        val newCounts = data.counts.toMutableMap()
        for (i in 1..3) {
            val date = today.minusDays(i.toLong()).format(dateFormatter)
            newCounts[date] = (2..12).random()
        }
        saveData(data.copy(counts = newCounts))
    }
}

data class ChallengeStats(
    val daysLeft: Int,
    val totalCount: Int,
    val averageDaily: Float,
    val isTargetAchieved: Boolean,
    val targetTotal: Int
)
