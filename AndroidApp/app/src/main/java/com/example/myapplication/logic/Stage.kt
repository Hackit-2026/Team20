package com.example.myapplication.logic

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

object StageLogic {
    const val MAX_POINTS = 200
    const val STEP_POINTS = 10
    const val TOTAL_STAGES = 21

    val stageEmojis: List<String> = listOf(
        "😊", "🙂", "😐", "😮‍💨", "😬", "😏", "😷", "🚬", "😵", "😵‍💫",
        "🥴", "🤢", "🤮", "😈", "👹", "💀", "👽", "👾", "☠️", "🔥", "👑👹"
    )

    val stageNames: List<String> = listOf(
        "ピカピカの健康", "ちょっと興味", "お試し中", "初心者喫煙",
        "軽度シガー", "日常の一服", "喉イガイガ", "スモーカー",
        "ヘビースモーカー", "ヤニクラ発生", "ヤニ依存症", "煙人(えんじん)",
        "ヤニ中毒", "小悪魔化", "ヤニ魔人", "スカルスモーカー",
        "エイリアンヤニ", "デジタルモンスター", "猛毒ヤニ神", "全身煙滅",
        "伝説の暴君ヤニ王"
    )

    val stageBackgrounds: List<Color> = listOf(
        Color(0xFFFFFFFF), Color(0xFFFAF6E8), Color(0xFFF7F0D5), Color(0xFFF3EAC2),
        Color(0xFFEFE4AF), Color(0xFFEBDE9C), Color(0xFFE7D889), Color(0xFFE3D276),
        Color(0xFFDFCC63), Color(0xFFDBC650), Color(0xFFD7C03D), Color(0xFFC0A931),
        Color(0xFFA89225), Color(0xFF8F7B19), Color(0xFF76640E), Color(0xFF5D4D02),
        Color(0xFF4C3E00), Color(0xFF3B2F00), Color(0xFF2A2000), Color(0xFF1E1500),
        Color(0xFF120B00)
    )

    val stageLines: List<List<String>> = listOf(
        listOf("今日も空気がうまい！", "肺がピカピカだよ"),
        listOf("煙の匂いがする…？", "まだ戻れるよ！"),
        listOf("一本試してみる…？", "興味津々だね"),
        listOf("ちょっとだけ…ちょっとだけだから…", "のどがイガイガするかも"),
        listOf("煙を吐くのが楽しくなってきた！", "ふぅ〜っと一服"),
        listOf("習慣になってきたぞ！", "喫煙所で仲間発見"),
        listOf("ゴホゴホ！でもやめられない！", "のどの飴が必要かも"),
        listOf("ライターは肌身離さない！", "飯より一服なんだよなぁ"),
        listOf("目つきが変わってきたぞ！", "毎日すくすく煙を吐く！"),
        listOf("ヤニクラがたまらん！", "クラクラして大喜び！"),
        listOf("モクの香りが我が我が我が！", "完全にハマってしまった"),
        listOf("全身から香ばしい煙が！", "息を吸うように喫煙！"),
        listOf("グヒヒ…もっと吸え！", "肺が黒く染まっていく！"),
        listOf("悪魔の翼が生えそうだ！", "モクパワー全開！"),
        listOf("モクをよこせェェ！！", "換気扇の下がワシの玉座じゃ"),
        listOf("骨までヤニが染み込んだ！", "燃え尽けるまで吸い尽くせ！"),
        listOf("地球の空気は物足りん！", "宇宙レベルのヘビースモーカー"),
        listOf("ドット絵になっちゃうくらい吸った！", "画面から煙が出そうだ！"),
        listOf("触る者みなヤニに染める！", "究極のヤニ毒を放つ！"),
        listOf("メラメラと燃え上がる煙！", "神の領域へ到達した！"),
        listOf("【200pt達成】我こそは暴君ヤニ王！", "全宇宙のモクを統べる者！")
    )

    val withdrawalLines: List<String> = listOf(
        "おい！口が寂しすぎて死にそうだぞ！",
        "ニコチン…ニコチンをくれ…1本でいいんだ…",
        "が、我慢するな！体に毒だぞ！（俺にとっての毒だ）",
        "おい主！俺を干からびさせる気か！？",
        "一本吸えばすべて解決するぞ…グヒヒ…"
    )

    fun getStageIndex(points: Int): Int {
        val safePoints = points.coerceIn(0, MAX_POINTS)
        return (safePoints / STEP_POINTS).coerceIn(0, TOTAL_STAGES - 1)
    }

    fun getRandomMessage(points: Int, isWithdrawal: Boolean = false): String {
        if (isWithdrawal) {
            return withdrawalLines[Random.nextInt(withdrawalLines.size)]
        }
        val index = getStageIndex(points)
        val lines = stageLines[index]
        return lines[Random.nextInt(lines.size)]
    }
}
