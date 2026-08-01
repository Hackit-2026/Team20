package com.example.myapplication.logic

import kotlin.random.Random

enum class CharacterStage(
    val level: Int,
    val label: String,
    val emoji: String,
    val messages: List<String>,
    val colorHex: Long
) {
    STAGE_0(0, "フレッシュマン", "🍼", listOf("吸う準備はいいかい？", "ここからが伝説の始まりだ。", "最初の一本が未来を変える！"), 0xFFE0F7FA),
    STAGE_1(1, "スモーク・ベビー", "👶", listOf("いい煙だ、もっと欲しくなるね。", "ニコチンが細胞に染み渡る…", "成長の予感がするよ！"), 0xFFB2EBF2),
    STAGE_2(2, "ヤニ・マスター", "💨", listOf("紫煙こそが王の証だ。", "肺が黒く染まるほど強くなる！", "この調子で極みを目指そう。"), 0xFF4DD0E1),
    STAGE_3(3, "スモーク・ゴッド", "🐉", listOf("世界を煙で包み込もう。", "究極の満足感…これぞ神の領域！", "お前の肺は伝説になった。"), 0xFF006064);

    fun getRandomMessage(): String = messages[Random.nextInt(messages.size)]

    companion object {
        fun fromCount(count: Int): CharacterStage {
            return when {
                count >= 10 -> STAGE_3
                count >= 5 -> STAGE_2
                count >= 2 -> STAGE_1
                else -> STAGE_0
            }
        }

        fun fromAverage(avg: Float): CharacterStage {
            return when {
                avg >= 10f -> STAGE_3
                avg >= 5f -> STAGE_2
                avg >= 2f -> STAGE_1
                else -> STAGE_0
            }
        }
    }
}
