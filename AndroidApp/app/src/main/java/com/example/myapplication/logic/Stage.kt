package com.example.myapplication.logic

import kotlin.random.Random

enum class CharacterStage(
    val level: Int,
    val label: String,
    val emoji: String,
    val messages: List<String>,
    val colorHex: Long
) {
    STAGE_0(0, "けんこう", "😊", listOf("今日も空気がうまい！", "肺が喜んでるよ。", "この調子でいこう！"), 0xFFFFFFFF),
    STAGE_1(1, "喫煙し始め", "😮‍💨", listOf("一本くらい…が命取りだよ。", "まだ引き返せるぞ。", "吸っちゃったか…次は我慢だ。"), 0xFFFFF6D8),
    STAGE_2(2, "ヘビースモーカー", "😵", listOf("煙たいやつだなぁ。", "喉がイガイガしない？", "もう立派なヤニカス予備軍。"), 0xFFF2E0A8),
    STAGE_3(3, "ヤニモンスター", "👹", listOf("ギギギ…ヤニをよこせ…", "お前の肺、真っ黒だぞ！", "もう手遅れかもしれない。"), 0xFF3A3A3A);

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
