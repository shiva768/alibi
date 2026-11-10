# Alibi

ログイン通知メールと照合するため、指定した金融系アプリの起動/終了イベント(`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`)を
`UsageStatsManager` から定期収集し、JSONL で共有フォルダへ追記する Android アプリ。

- 出力: `usage-events-YYYYMM.jsonl`(JST 月別)。各収集で `heartbeat`(`coverage_until`)を追記
- 収集: WorkManager 15分周期。`watermark − 5分` から読み直し、`(pkg, ts, event)` で重複排除。書き込み成功時のみ watermark を進める
- 権限: 使用状況へのアクセスのみ。Foreground Service なし
- 初回収集は「実行時点」から(過去分はさかのぼらない)

## 使い方
1. `./gradlew assembleDebug` で APK を作って Pixel にインストール
2. 「使用状況へのアクセスを開く」→ Alibi を許可
3. 「対象アプリを選ぶ」(SBI証券 / SBI証券 Plus など)
4. 「出力フォルダを選ぶ」→ FolderSync 等で Google Drive に同期するフォルダ
5. 「今すぐ収集」で動作確認
