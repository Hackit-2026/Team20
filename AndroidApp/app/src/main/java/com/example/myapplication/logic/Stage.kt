package com.example.myapplication.logic

/**
 * 1日に吸った本数(0〜20本)をそのままキャラの段階として扱う。
 * 21段階のキャラシート(S0〜S20)と1:1対応させるための変換のみ。
 */
object CharacterStage {
    const val MAX_STAGE = 20

    fun fromCount(count: Int): Int = count.coerceIn(0, MAX_STAGE)

    fun fromAverage(avg: Float): Int = avg.toInt().coerceIn(0, MAX_STAGE)
}
