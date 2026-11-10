package app.alibi

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** 1回分の収集。成功時のみ watermark / 重複排除キーを更新する。 */
object Collector {
    // 古い JSONL が続く間の最大遡り。OS保持期間(1〜2週間)より短く取る
    private const val MAX_LOOKBACK_MS = 7 * 24 * 3600 * 1000L

    sealed class Result {
        data class Ok(val events: Int) : Result()
        data class Skipped(val reason: String) : Result()
        data class Failed(val reason: String) : Result()
    }

    fun run(context: Context, now: Long = System.currentTimeMillis()): Result {
        val prefs = Prefs(context)
        if (!Permission.hasUsageAccess(context)) return fail(prefs, "使用状況へのアクセスが未許可")
        val folder = prefs.folderUri ?: return fail(prefs, "出力フォルダ未設定")
        val targets = prefs.targets
        if (targets.isEmpty()) return fail(prefs, "対象アプリ未選択")

        val wm = prefs.watermark
        // 初回は「今」から。以後は watermark − 5分 から読み直す
        val begin = if (wm == 0L) now else maxOf(wm - Dedup.OVERLAP_MS, now - MAX_LOOKBACK_MS)
        val seen = prefs.recentKeys.toMutableSet()

        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usm.queryEvents(begin, now)
            val e = UsageEvents.Event()
            val byFile = linkedMapOf<String, MutableList<String>>()
            var count = 0
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                val name = when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> "resumed"
                    UsageEvents.Event.ACTIVITY_PAUSED -> "paused"
                    else -> continue
                }
                if (e.packageName !in targets) continue
                val key = LogFormat.dedupKey(e.packageName, e.timeStamp, name)
                if (!seen.add(key)) continue
                byFile.getOrPut(LogFormat.monthFileName(e.timeStamp)) { mutableListOf() }
                    .add(LogFormat.eventLine(e.timeStamp, e.packageName, name))
                count++
            }
            // heartbeat: 「now までは欠落なく記録できている」保証
            byFile.getOrPut(LogFormat.monthFileName(now)) { mutableListOf() }
                .add(LogFormat.heartbeatLine(coverageUntilMs = now, generatedAtMs = System.currentTimeMillis()))

            JsonlWriter(context, folder).append(byFile)

            // 書き込み成功後にだけ状態を進める(失敗時は次回同じ範囲を再取得)
            prefs.recentKeys = Dedup.prune(seen, now)
            prefs.watermark = now
            prefs.coverageUntil = now
            prefs.lastCollectAt = now
            prefs.lastError = null
            Result.Ok(count)
        } catch (ex: Exception) {
            fail(prefs, ex.message ?: ex.javaClass.simpleName)
        }
    }

    private fun fail(prefs: Prefs, reason: String): Result {
        prefs.lastError = reason
        return Result.Failed(reason)
    }
}
