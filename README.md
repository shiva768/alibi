# Alibi

ログイン通知メールと照合するため、指定した金融系アプリの起動/終了イベント(`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`)を
`UsageStatsManager` から定期収集し、JSONL で出力フォルダ(Google Drive など)へ書き込む Android アプリ。
照合そのものは照合側(Muse)の担当で、本アプリはログを書くところまで。

- 出力: `usage-events-YYYYMMDD.jsonl`(JST 日別)。各収集で `heartbeat`(`coverage_until`)を追記
- 収集: WorkManager 15分周期。`watermark − 5分` から読み直し、`(pkg, ts, event)` で重複排除。書き込み成功時のみ watermark を進める
- 権限: 使用状況へのアクセス(`PACKAGE_USAGE_STATS`)と、対象アプリを選ぶ一覧用の `QUERY_ALL_PACKAGES`。Foreground Service なし
- 初回収集は「実行時点」から(過去分はさかのぼらない)
- 記録するのは選んだアプリのイベントだけ(全アプリの履歴は残さない)

## 書き込みの挙動
出力フォルダは Storage Access Framework(SAF)で選ぶ。
- 端末内のフォルダなど追記モード(`"wa"`)に対応する場所: そのまま追記する
- Google Drive など追記に対応しない場所: 既存内容を読み出して連結し、ファイル全体を書き戻す。読み出せないときは書き込まずに中止する(次回の収集で取り直し)

Drive 上のファイルは、書き込み中の通信断で壊れる可能性がゼロではない。日別ファイルにして1回に書き直す量を小さくしている。
前日分のファイルの削除は運用側(Drive 側)で行う。

## 使い方
1. APK を入れる
   - ローカル: `./gradlew assembleDebug`(`app/build/outputs/apk/debug/`)
   - GitHub Actions: `Build APK` ワークフローの artifact(`alibi-debug-<日時>`)
2. 使用状況へのアクセスを許可する
   - 「使用状況へのアクセスを開く」→ Alibi を許可
   - 「アプリはアクセスを拒否されました」と出たら(Android 13+ で Play ストア外から入れたアプリの制限):
     設定 → アプリ → Alibi → 右上「⋮」→「制限付き設定を許可」→ 再度許可
3. 「対象アプリを選ぶ」(SBI証券 / SBI証券 Plus など)
4. 「出力フォルダを選ぶ」(例: Google Drive 内の `usage-log` フォルダ)
5. 「今すぐ収集」で動作確認(画面の「直近のエラー」が空で、最終収集時刻と `coverage_until` が更新されること)

## CI
`.github/workflows/build-apk.yml`: ユニットテスト + debug APK のビルド + artifact のアップロード(7日保持)。
リポジトリのシークレット `DEBUG_KEYSTORE_BASE64`(標準 debug.keystore の base64)を設定すると、固定鍵で署名され、APK を上書きインストールできる。未設定だとビルドごとに署名が変わる。
