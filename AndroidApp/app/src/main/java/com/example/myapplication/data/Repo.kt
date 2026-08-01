package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.myapplication.logic.StageLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
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
    val name: String = "",
    val avatar: String = "",
    val comment: String = ""
)

@Serializable
data class TimelinePost(
    val postId: String = "",
    val author: String = "",
    val isNpc: Boolean = false,
    val text: String = "",
    val timestamp: String = "",
    val memberComments: List<MemberComment> = emptyList(),
    val comments: List<MemberComment> = emptyList()
) {
    fun getCommentsList(): List<MemberComment> {
        val list = if (comments.isNotEmpty()) comments else memberComments
        return list.filter { it.comment.isNotBlank() }
    }
}

@Serializable
data class CreatePostReq(
    val author: String = "あなた",
    val text: String
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

    /**
     * 🚨 1日通知放置時の自動ペナルティ加算処理
     */
    fun checkAndApplyPenalty() {
        val data = loadData()
        if (!data.isInitialized) return

        val today = LocalDate.now()
        val lastConfirmedStr = data.lastConfirmedDate ?: data.settings.startDate
        val lastConfirmed = try {
            LocalDate.parse(lastConfirmedStr, dateFormatter)
        } catch (e: Exception) {
            today.minusDays(1)
        }

        if (lastConfirmed.isBefore(today)) {
            val missedDays = java.time.temporal.ChronoUnit.DAYS.between(lastConfirmed, today).toInt()
            
            if (missedDays > 0) {
                Log.w("AppRepo", "🚨 Notification ignored! Applying penalty for $missedDays missed day(s).")
                val newCounts = data.counts.toMutableMap()
                val goal = data.settings.dailyGoal
                
                for (i in 1..missedDays) {
                    val missedDate = lastConfirmed.plusDays(i.toLong()).format(dateFormatter)
                    if (missedDate != getTodayDate() && !newCounts.containsKey(missedDate)) {
                        newCounts[missedDate] = goal
                    }
                }

                var totalPoints = 0
                newCounts.values.forEach { c -> totalPoints += (c - 1) }

                saveData(data.copy(
                    counts = newCounts,
                    points = totalPoints.coerceIn(0, StageLogic.MAX_POINTS)
                ))
            }
        }
    }

    /** 手動テスト用ペナルティ加算 */
    fun applyManualPenalty() {
        val data = loadData()
        val goal = data.settings.dailyGoal
        val yesterday = LocalDate.now().minusDays(1).format(dateFormatter)
        
        val newCounts = data.counts.toMutableMap()
        newCounts[yesterday] = goal

        var totalPoints = 0
        newCounts.values.forEach { c -> totalPoints += (c - 1) }

        saveData(data.copy(
            counts = newCounts,
            points = totalPoints.coerceIn(0, StageLogic.MAX_POINTS)
        ))
    }

    fun updateTodayCount(count: Int) {
        val data = loadData()
        val today = getTodayDate()
        
        val newCounts = data.counts.toMutableMap().apply {
            put(today, count)
        }

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

    /**
     * 🌐 サーバー (GET /api/feed) から最新のタイムラインとSQL生成済コメントを取得し、完全上書き保存
     */
    suspend fun fetchFeedFromServer(): List<TimelinePost> = withContext(Dispatchers.IO) {
        val rawConfigured = getServerUrl().trimEnd('/')
        val candidateUrls = listOf(
            "$rawConfigured/api/feed",
            "http://192.168.25.42:8000/api/feed",
            "http://192.168.16.31:8000/api/feed",
            "http://10.0.2.2:8000/api/feed",
            "http://localhost:8000/api/feed"
        ).distinct()

        for (urlString in candidateUrls) {
            try {
                Log.d("AppRepo", "Fetching feed from: $urlString")
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2500
                conn.readTimeout = 4000

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val serverPosts = json.decodeFromString<List<TimelinePost>>(responseText)
                    val fixedPosts = serverPosts.map { p ->
                        val validComments = p.getCommentsList()
                        p.copy(
                            memberComments = validComments,
                            comments = validComments
                        )
                    }
                    
                    // 🚨 取得した最新フィードでローカルDBを完全上書き（旧空データの消去）
                    val data = loadData()
                    saveData(data.copy(feed = fixedPosts))
                    Log.d("AppRepo", "✅ Successfully fetched & overwrote ${fixedPosts.size} posts with comments from $urlString")
                    return@withContext fixedPosts
                }
            } catch (e: Exception) {
                Log.d("AppRepo", "Fetch feed from $urlString failed: ${e.message}")
            }
        }
        return@withContext getFeed()
    }

    /**
     * 🌐 投稿受信時: 即座（0.05秒）にサーバーへPOSTし、ローカルフィードへ即反映。
     */
    suspend fun postToTimelineServer(text: String): TimelinePost? = withContext(Dispatchers.IO) {
        val rawConfigured = getServerUrl().trimEnd('/')
        val candidateUrls = listOf(
            "$rawConfigured/api/posts",
            "http://192.168.25.42:8000/api/posts",
            "http://192.168.16.31:8000/api/posts",
            "http://10.0.2.2:8000/api/posts",
            "http://localhost:8000/api/posts"
        ).distinct()

        for (urlString in candidateUrls) {
            Log.d("AppRepo", "Attempting fast HTTP POST to server: $urlString")
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 2500
                conn.readTimeout = 4000

                val reqBody = json.encodeToString(CreatePostReq(author = "あなた", text = text))
                conn.outputStream.use { os ->
                    os.write(reqBody.toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                Log.d("AppRepo", "Server fast response code from $urlString: $responseCode")

                if (responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d("AppRepo", "Server fast response body: $responseText")
                    val rawPost = json.decodeFromString<TimelinePost>(responseText)
                    
                    val data = loadData()
                    saveData(data.copy(feed = listOf(rawPost) + data.feed.filter { it.postId != rawPost.postId }))
                    return@withContext rawPost
                }
            } catch (e: Exception) {
                Log.e("AppRepo", "Fast HTTP connection failed for $urlString: ${e.message}")
            }
        }
        
        Log.w("AppRepo", "All server fast connection attempts failed. Using local fallback.")
        val fallbackPost = TimelinePost(
            postId = "p_${System.currentTimeMillis()}",
            author = "あなた",
            isNpc = false,
            text = text,
            timestamp = "たった今",
            memberComments = emptyList(),
            comments = emptyList()
        )
        val data = loadData()
        saveData(data.copy(feed = listOf(fallbackPost) + data.feed))
        return@withContext fallbackPost
    }

    fun postToTimeline(text: String) {
        val data = loadData()
        val newPost = TimelinePost(
            postId = "p_${System.currentTimeMillis()}",
            author = "あなた",
            isNpc = false,
            text = text,
            timestamp = "たった今",
            memberComments = emptyList(),
            comments = emptyList()
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
                generateMemberComments(), generateMemberComments()
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
