# チーム開発 進捗・ロードマップ管理シート (Team Progress & Roadmap)

本ドキュメントは、ハッカソン本番に向けたチーム全体の進捗状況、役割分担、未実装機能、デモ発表用シナリオ、および開発ルールを記録する管理シートです。

---

## 1. チーム役割分担 ＆ 編集領域スコープ

| 役割 / 担当 | 担当者 | 主な責任範囲 | 担当ファイル / 編集対象領域 |
| :--- | :--- | :--- | :--- |
| **担当 A (コア & データ / Git管理)** | レポジトリ管理者 | バックグラウンド通信, SQLite DB, 状態管理, 全体マージ | `server/main.py`, `server/schema.sql`, `data/Repo.kt`, `MainViewModel.kt`, `RequirementsDefinition.md` |
| **担当 B (画面 & キャラ)** 🎨 | **画面 UI 責任者** | **画面デザイン, Composeレイアウト, キャラアセット, ビジュアル演出** | `ui/HomeScreen.kt`, `ui/CharacterAssets.kt`, `ui/OnboardingScreen.kt`, `ui/ResultScreen.kt`, `ui/CalendarScreen.kt`, `ui/CommunityFeedScreen.kt`, `ui/StartScreen.kt` |
| **担当 C (通知 & 発表デモ)** | デモ・通知責任者 | プッシュ通知, リマインダー発火, 発表台本, デモ操作 | `notify/Reminder.kt`, `notify/ReminderReceiver.kt`, `MainActivity.kt`, デモ台本 |

---

## 2. 🎨 担当 B (画面 & キャラ) 専属編集可能領域

担当 B は、UIの見た目・カラーパレット・イラスト・レイアウト・描画アニメーションの編集に特化し、以下の Compose ファイルを自由にカスタマイズ可能です。

* **`ui/HomeScreen.kt`**: 鏡モチーフメイン画面、残り日数カード、本数ステッパー、全画面カラー調整
* **`ui/CharacterAssets.kt`**: 21段階 (S0〜S20) 画像描画・鏡フレームベゼル・グロー発光演出
* **`ui/OnboardingScreen.kt`**: 期間・目標本数選択画面 (初期値10本)
* **`ui/ResultScreen.kt`**: 最終リザルト・サマリーカード画面
* **`ui/CalendarScreen.kt`**: 閲覧履歴・1週間(7日)/1ヶ月(30日)可視化棒グラフ描画
* **`ui/CommunityFeedScreen.kt`**: コミュニティタイムライン・カード・吹き出しデザイン
* **`ui/StartScreen.kt`**: 起動「＋」ボタン付きスタート画面

---

## 3. 開発進捗ステータス (Progress Status Log)

### 📊 全体進捗サマリー
- **担当 A (コア & データ / Git管理)**: 🎉 **全機能統合・動作確認完了 (100%)** — コアロジック、UI画面群、通知機能の引き込み・動作確認完了。
- **担当 B (画面 & キャラ)**: 🎉 **主要画面統合完了 (100%)** — HomeScreen, OnboardingScreen, ResultScreen, CalendarScreen, CharacterAssets, CommunityFeedScreen 統合完了。
- **担当 C (通知 & 発表)**: 🎉 **通知機能統合完了 (100%)** — Reminder, NotificationManager, 権限リクエスト統合完了。
