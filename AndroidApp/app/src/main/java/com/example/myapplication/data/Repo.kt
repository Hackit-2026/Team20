package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.myapplication.logic.StageLogic
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class AppSettings(
    val startDate: String = LocalDate.now().toString(),
    val days: Int = 7,
    val dailyGoal: Int = 10,
    val notifyHour: Int = 21,
    val notifyMinute: Int = 0,
    val serverUrl: String = "http://192.168.25.42:8000"
)

@Serializable
data class MemberComment(
    val name: String,
    val avatar: String,
    val comment: String
)

@Serializable
data class TimelinePost(
    val postId: String,
    val author: String,
    val isNpc: Boolean,
    val text: String,
    val timestamp: String,
    val memberComments: List<MemberComment>
)

@Serializable
data class AppData(
    val settings: AppSettings = AppSettings(),
    val counts: Map<String, Int> = emptyMap(),
    val points: Int = 0,
    val isInitialized: Boolean = false,
    val lastConfirmedDate: String? = null,
    val feed: List<TimelinePost> = emptyList()
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

    fun getServerUrl(): String = loadData().settings.serverUrl

    fun saveServerUrl(url: String) {
        val data = loadData()
        val updatedSettings = data.settings.copy(serverUrl = url)
        saveData(data.copy(settings = updatedSettings))
    }

    fun isInitialized(): Boolean = loadData().isInitialized

    fun getCount(date: String): Int = loadData().counts[date] ?: loadData().settings.dailyGoal

    /** 本日の至福の本数（初期値ははじめに設定した1日の目標本数） */
    fun getTodayCount(): Int = loadData().counts[getTodayDate()] ?: loadData().settings.dailyGoal
    
    fun isConfirmedToday(): Boolean = loadData().lastConfirmedDate == getTodayDate()

    fun updateTodayCount(count: Int) {
        val data = loadData()
        val today = getTodayDate()
        
        val newCounts = data.counts.toMutableMap().apply {
            put(today, count)
        }

        // ポイント計算ルール: (本数 - 1) pt
        var totalPoints = 0
        newCounts.values.forEach { c ->
            totalPoints += (c - 1)
        }
        
        saveData(data.copy(
            counts = newCounts, 
            points = totalPoints.coerceIn(0, StageLogic.MAX_POINTS),
            lastConfirmedDate = today
        ))
    }

    fun getPoints(): Int = loadData().points

    fun getFeed(): List<TimelinePost> = loadData().feed

    fun postToTimeline(text: String) {
        val data = loadData()
        val newPost = TimelinePost(
            postId = "p_${System.currentTimeMillis()}",
            author = "あなた",
            isNpc = false,
            text = text,
            timestamp = "たった今",
            memberComments = generateMemberComments()
        )
        saveData(data.copy(feed = listOf(newPost) + data.feed))
    }

    private fun generateMemberComments(): List<MemberComment> {
        return listOf(
            MemberComment("熱血仲間・修造", "🔥", "あきらめるな！気合で乗り越えろ！！🔥"),
            MemberComment("ツンデレ友達・アスカ", "😳", "べ、別に心配してないんだからね！でも…今日我慢できたのは偉いわよ…"),
            MemberComment("Dr.ヘルス", "👨‍⚕️", "医学的にも最初の3日が山場です。素晴らしい我慢です！"),
            MemberComment("ヤニモグラ", "👹", "おい！我慢するな！吸って俺を育てろ〜！")
        )
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
        val isTargetAchieved = totalCount >= targetTotal

        return ChallengeStats(
            daysLeft = daysLeft,
            totalCount = totalCount,
            averageDaily = averageDaily,
            isTargetAchieved = isTargetAchieved,
            targetTotal = targetTotal,
            currentPoints = data.points
        )
    }

    fun resetAllData() {
        saveData(AppData())
    }

    fun injectDummyData() {
        val data = loadData()
        val today = LocalDate.now()
        val newCounts = data.counts.toMutableMap()
        for (i in 1..30) {
            val date = today.minusDays(i.toLong()).format(dateFormatter)
            newCounts[date] = (0..15).random()
        }
        
        var totalPoints = 0
        newCounts.values.forEach { c -> totalPoints += (c - 1) }

        val dummyFeed = listOf(
            TimelinePost(
                "d001", "仲間のたかし", true, "今日は一本も吸わずに過ごせた！奇跡！", "3時間前",
                generateMemberComments()
            )
        )
        
        saveData(data.copy(
            counts = newCounts, 
            points = totalPoints.coerceIn(0, StageLogic.MAX_POINTS),
            feed = dummyFeed,
            isInitialized = true
        ))
    }
}

data class ChallengeStats(
    val daysLeft: Int,
    val totalCount: Int,
    val averageDaily: Float,
    val isTargetAchieved: Boolean,
    val targetTotal: Int,
    val currentPoints: Int = 0
)
