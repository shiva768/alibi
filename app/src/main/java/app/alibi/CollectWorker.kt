package app.alibi

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/** WorkManager の標準挙動のみ(Foreground Service は使わない)。 */
class CollectWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // 設定不足/書き込み失敗は Prefs.lastError に記録済み。次回周期で再試行
        Collector.run(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "alibi-collect"

        /**
         * 設定された間隔で登録する。UPDATE なので、間隔の変更や旧版の登録済みスケジュールも置き換わる。
         * イベントは UsageStats から後追い回収できるので、間隔を空けても履歴は欠落しない。
         */
        fun schedule(context: Context) {
            val minutes = Prefs(context).intervalMinutes.toLong()
            val req = PeriodicWorkRequestBuilder<CollectWorker>(minutes, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<CollectWorker>().build())
        }
    }
}
