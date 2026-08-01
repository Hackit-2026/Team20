package com.example.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ChartRange(val label: String, val days: Int) {
    WEEK("1週間 (7日)", 7),
    MONTH("1ヶ月 (30日)", 30)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedRange by remember { mutableStateOf(ChartRange.WEEK) }

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val labelFormatter = DateTimeFormatter.ofPattern("MM/dd")

    // 過去N日分のデータを生成
    val chartData: List<Pair<String, Int>> = remember(uiState.allCounts, selectedRange) {
        (0 until selectedRange.days).map { i ->
            val date = today.minusDays((selectedRange.days - 1 - i).toLong())
            val dateStr = date.format(dateFormatter)
            val labelStr = date.format(labelFormatter)
            val count = uiState.allCounts[dateStr] ?: 0
            labelStr to count
        }
    }

    val allCountsList: List<Pair<String, Int>> = uiState.allCounts.toList().sortedByDescending { it.first }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("喫煙・我慢履歴", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 期間切り替えタブ
            TabRow(
                selectedTabIndex = selectedRange.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                ChartRange.values().forEach { range ->
                    Tab(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        text = { Text(range.label, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // グラフエリア
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 ${selectedRange.label} 喫煙数グラフ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "目標: ${uiState.settings.dailyGoal}本/日",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // カスタム描画 棒グラフ
                    SmokingHistoryChart(
                        data = chartData,
                        dailyGoal = uiState.settings.dailyGoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "📅 日別詳細ログ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 日別ログ一覧
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(allCountsList) { (date, count) ->
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
                            color = if (count <= uiState.settings.dailyGoal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (allCountsList.isEmpty()) {
                    item {
                        Text(
                            text = "まだ履歴データがありません",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmokingHistoryChart(
    data: List<Pair<String, Int>>,
    dailyGoal: Int,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val goalLineColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.hashCode()
    val scrollState = rememberScrollState()

    val maxVal = remember(data, dailyGoal) {
        val maxData = data.maxOfOrNull { it.second } ?: 0
        maxOf(maxData, dailyGoal, 5)
    }

    Box(
        modifier = modifier
            .horizontalScroll(scrollState)
    ) {
        val minWidth = if (data.size > 10) (data.size * 28).dp else 320.dp
        Canvas(modifier = Modifier.width(minWidth).fillMaxHeight()) {
            val width = size.width
            val height = size.height
            val bottomPadding = 40f
            val topPadding = 30f
            val availableHeight = height - bottomPadding - topPadding

            val barWidth = (width / data.size) * 0.6f
            val spacing = (width / data.size) * 0.4f

            // 目標線の描画
            val goalY = height - bottomPadding - (dailyGoal.toFloat() / maxVal * availableHeight)
            drawLine(
                color = goalLineColor.copy(alpha = 0.6f),
                start = Offset(0f, goalY),
                end = Offset(width, goalY),
                strokeWidth = 2f
            )

            // バーの描画
            data.forEachIndexed { index, (label, count) ->
                val x = index * (barWidth + spacing) + spacing / 2
                val barHeight = (count.toFloat() / maxVal) * availableHeight
                val y = height - bottomPadding - barHeight

                // 棒の描画
                drawRect(
                    color = if (count <= dailyGoal) barColor else goalLineColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // 本数ラベル（上部）
                drawContext.canvas.nativeCanvas.drawText(
                    "${count}",
                    x + barWidth / 2,
                    y - 8f,
                    android.graphics.Paint().apply {
                        color = textColor
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )

                // 日付ラベル（下部）
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x + barWidth / 2,
                    height - 8f,
                    android.graphics.Paint().apply {
                        color = textColor
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}
