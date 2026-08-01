# AI活用プロンプト集（Gemini & Claude & エージェント用）

本プロジェクト（「タバコを吸った本数でキャラを育てる反面教師アプリ」）の設計書 v2（`MYProgress.md`）に基づき、**「担当 A: コア & データ」**を確実かつ迅速に実装するためのプロンプトセットです。

---

## 担当 A 用 決定版プロンプト（コード実装・データ層・ロジック一括生成用）

以下のプロンプトを Claude または ChatGPT / Gemini、あるいは本AIに入力することで、担当 A の全コード（データ保存・ステージ判定・ViewModel・画面遷移）を即座かつ確実に作成できます。

```text
あなたは高度なAndroid（Kotlin / Jetpack Compose）エンジニアです。
プロジェクトの設計書（MYProgress.md）に基づき、「担当 A: コア & データ」の責任範囲である以下のクラス・処理を完全実装してください。

【担当 A の責任範囲】
1. ステージ判定ロジック (Stage.kt)
2. データ保存処理 (data/Repo.kt) - SharedPreferences + JSON
3. ViewModel / 状態管理 (MainViewModel.kt)
4. 画面遷移ナビゲーション (NavHost) の骨組み

【厳密な仕様要件】
1. ステージ判定 (Stage.kt):
   - 本数 >= 10 → Stage 3 ("ヤニモンスター", "👹")
   - 本数 >= 5  → Stage 2 ("ヘビースモーカー", "😵")
   - 本数 >= 2  → Stage 1 ("喫煙し始め", "😮‍💨")
   - それ以外  → Stage 0 ("けんこう", "😊")
   - セリフ配列（各ステージのランダムセリフ）を定義。

2. データ保存 (data/Repo.kt):
   - SharedPreferences を使用。
   - 保存形式:
     {
       "settings": { "startDate": "YYYY-MM-DD", "days": 7, "dailyGoal": 0, "notifyHour": 21 },
       "counts": { "YYYY-MM-DD": 3, "YYYY-MM-DD+1": 0 }
     }
   - 今日の日付のカウント取得・インクリメント(+1)・デクリメント(-1)・特定日付のデータ取得。
   - チャレンジ期間中の実際合計・1日平均・目標合計との達成判定ロジック。

3. ViewModel (MainViewModel.kt):
   - StateFlow で Compose UI (担当B) に状態を提供。
   - UI用状態データクラス UiState:
     (todayCount, currentStage, currentEmoji, currentLine, daysLeft, isTargetAchieved, etc.)
   - メソッド: incrementCount(), decrementCount(), saveSettings(days, goal, notifyHour), getResult()

4. ナビゲーション (MainActivity.kt / Navigation.kt):
   - Navigation Compose (NavHost) を使用して 4画面のルート定義:
     "onboarding" -> "home" -> "calendar" -> "result"

【出力コードの制約】
- 他のメンバー（担当B: UI/デザイン, 担当C: 通知）が画面開発しやすいよう、明確なインターフェースとプレースホルダー画面を提供してください。
- コンパイルエラーが発生しない、完全に動作するKotlinコードとして出力してください。
```

---

## 使い方・進め方のフロー
1. 上記のプロンプトをAIに投げて基盤コードを取得する。
2. 取得したコードを以下のファイル構成で配置する：
   - `logic/Stage.kt`
   - `data/Repo.kt`
   - `ui/MainViewModel.kt`
   - `MainActivity.kt` (NavHost組み込み)
3. 動作確認（`+1` で絵文字・ステージが変わるか、アプリを再起動してもカウントが維持されるか）。
4. 完了したらコミット・Pushして担当B・Cへ引き渡す。
