package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.DailyReport
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    allReports: Map<String, DailyReport>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 1週間, 1: 1ヶ月
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // 選択されたタブ（7日 or 30日）の日別本数データを算出
    val daysCount = if (selectedTab == 0) 7 else 30
    val chartData = remember(allReports, selectedTab) {
        (0 until daysCount).map { i ->
            val dateStr = today.minusDays((daysCount - 1 - i).toLong()).format(formatter)
            val shortLabel = dateStr.takeLast(5) // MM-dd
            val report = allReports[dateStr]
            val count = if (report != null && report.smoked) report.count else 0
            Triple(dateStr, shortLabel, count)
        }
    }

    val sortedDates = remember(allReports) { allReports.keys.sortedDescending() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 喫煙履歴 & 統計グラフ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScreenBg)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 📊 【新要件】最上部に1週間 ＆ 1ヶ月の切り替え可視化グラフを配置
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📈 1日に吸ったタバコ量の推移グラフ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )

                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("1週間 (7日)", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("1ヶ月 (30日)", fontWeight = FontWeight.Bold) }
                            )
                        }

                        SmokingHistoryChart(chartData = chartData)
                    }
                }
            }

            item {
                Text(
                    text = "📋 日別記録ログ (${sortedDates.size}日分)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            items(sortedDates) { date ->
                val entry = allReports.getValue(date)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = date, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (entry.smoked) "吸った (${entry.count}本)" else "吸わなかった (0本)",
                                color = if (entry.smoked) Warn else Accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // 🚬 本数をタバコの絵で直感的に表現(0本の日は葉っぱでクリーン表示)
                        Text(
                            text = if (entry.smoked) "🚬".repeat(entry.count.coerceAtMost(40)) else "🌿 クリーンな1日!",
                            fontSize = if (entry.smoked) 18.sp else 13.sp,
                            color = if (entry.smoked) Ink else Accent,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            if (sortedDates.isEmpty()) {
                item {
                    Text(
                        text = "まだ履歴データがありません",
                        color = MutedSoft,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 📊 1週間 / 1ヶ月 喫煙本数棒グラフコンポーネント
 */
@Composable
fun SmokingHistoryChart(chartData: List<Triple<String, String, Int>>) {
    val maxCount = (chartData.maxOfOrNull { it.third } ?: 10).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            chartData.forEach { (_, shortLabel, count) ->
                val heightRatio = (count.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1.0f)
                val barColor = if (count == 0) Color(0xFF4CAF50) else Color(0xFFE91E63)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(if (chartData.size > 10) 6.dp else 18.dp)
                            .fillMaxHeight(heightRatio)
                            .background(barColor, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            chartData.forEachIndexed { index, (_, shortLabel, _) ->
                val showLabel = chartData.size <= 7 || index % 5 == 0 || index == chartData.size - 1
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (showLabel) shortLabel else "",
                        fontSize = 9.sp,
                        color = Muted
                    )
                }
            }
        }
    }
}
