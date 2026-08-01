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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 1週間, 1: 1ヶ月

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // 選択されたタブに応じた過去データ期間を取得
    val daysCount = if (selectedTab == 0) 7 else 30
    val chartData = remember(uiState.allCounts, selectedTab) {
        (0 until daysCount).map { i ->
            val dateStr = today.minusDays((daysCount - 1 - i).toLong()).format(formatter)
            val shortLabel = dateStr.takeLast(5) // MM-dd
            val count = uiState.allCounts[dateStr] ?: 0
            Triple(dateStr, shortLabel, count)
        }
    }

    val allCountsSorted: List<Pair<String, Int>> = uiState.allCounts.toList().sortedByDescending { it.first }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 喫煙履歴 & 統計", fontWeight = FontWeight.Bold) },
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
        ) {
            // 🚨 【一番上】1週間 & 1ヶ月の切り替え可視化グラフ
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📈 喫煙本数の推移グラフ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // 1週間 / 1ヶ月 切替タブ
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

                        // 棒グラフ表示コンポーネント
                        SmokingHistoryChart(
                            chartData = chartData,
                            dailyGoal = uiState.settings.dailyGoal
                        )
                    }
                }
            }

            // 履歴ログリストの見出し
            item {
                Text(
                    text = "📋 日別記録ログ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 日別ログ一覧
            items(allCountsSorted) { pair ->
                val date = pair.first
                val count = pair.second
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = date, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${count}本",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (count == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (allCountsSorted.isEmpty()) {
                item {
                    Text(
                        text = "まだ履歴データがありません",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun SmokingHistoryChart(
    chartData: List<Triple<String, String, Int>>,
    dailyGoal: Int
) {
    val maxCount = (chartData.maxOfOrNull { it.third } ?: dailyGoal).coerceAtLeast(dailyGoal).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // グラフエリア
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            chartData.forEach { (_, shortLabel, count) ->
                val heightRatio = (count.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1.0f)
                val isGoalAchieved = count <= dailyGoal
                val barColor = if (count == 0) Color(0xFF4CAF50) // 0本は緑
                               else if (isGoalAchieved) Color(0xFF2196F3) // 目標以内は青
                               else Color(0xFFE91E63) // 目標オーバーは赤

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

        // X軸日付ラベル（1ヶ月表示時は間引いて表示）
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
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
