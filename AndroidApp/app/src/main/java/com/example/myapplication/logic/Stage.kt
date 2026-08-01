package com.example.myapplication.logic

import kotlin.random.Random

enum class CharacterStage(
    val level: Int,
    val label: String,
    val emoji: String,
    val messages: List<String>
) {
    STAGE_0(0, "けんこう", "😊", listOf("今日も空気がうまい！", "肺が喜んでるよ。", "この調子でいこう！")),
    STAGE_1(1, "喫煙し始め", "😮‍💨", listOf("一本くらい…が命取りだよ。", "まだ引き返せるぞ。", "吸っちゃったか…次は我慢だ。")),
    STAGE_2(2, "ヘビースモーカー", "😵", listOf("煙たいやつだなぁ。", "喉がイガイガしない？", "もう立派なヤニカス予備軍。")),
    STAGE_3(3, "ヤニモンスター", "👹", listOf("ギギギ…ヤニをよこせ…", "お前の肺、真っ黒だぞ！", "もう手遅れかもしれない。"));

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
    }
}
