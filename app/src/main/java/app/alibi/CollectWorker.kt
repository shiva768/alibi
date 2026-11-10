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

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CollectWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<CollectWorker>().build())
        }
    }
}
