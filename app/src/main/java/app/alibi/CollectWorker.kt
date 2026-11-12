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

        // 3時間間隔。イベントは UsageStats から後追い回収できるので履歴は欠落しない。
        // UPDATE にしてあるので、旧版(15分)の登録済みスケジュールもこの間隔に置き換わる
        private const val INTERVAL_HOURS = 3L

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CollectWorker>(INTERVAL_HOURS, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<CollectWorker>().build())
        }
    }
}
