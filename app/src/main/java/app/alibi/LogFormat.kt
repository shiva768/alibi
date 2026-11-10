package app.alibi

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** JSONL の行生成など、Android に依存しない純粋ロジック。 */
object LogFormat {
    val JST: ZoneId = ZoneId.of("Asia/Tokyo")
    private val ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /** 2026-11-10T17:04:52+09:00 (秒精度、JST) */
    fun iso(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(JST).withNano(0).format(ISO)

    fun eventLine(epochMs: Long, pkg: String, event: String): String =
        """{"ts":"${iso(epochMs)}","epoch":$epochMs,"pkg":"${esc(pkg)}","event":"${esc(event)}"}"""

    fun heartbeatLine(coverageUntilMs: Long, generatedAtMs: Long): String =
        """{"type":"heartbeat","coverage_until":"${iso(coverageUntilMs)}","generated_at":"${iso(generatedAtMs)}"}"""

    /** usage-events-YYYYMM.jsonl (JST 基準の月別) */
    fun monthFileName(epochMs: Long): String {
        val d = Instant.ofEpochMilli(epochMs).atZone(JST)
        return "usage-events-%04d%02d.jsonl".format(d.year, d.monthValue)
    }

    fun dedupKey(pkg: String, epochMs: Long, event: String) = "$pkg|$epochMs|$event"

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}

/** 再読込窓(watermark − OVERLAP)で重複するイベントを (pkg, ts, event) で除外する。 */
object Dedup {
    const val OVERLAP_MS = 5 * 60 * 1000L

    /** 書き込み済みキーのうち、次回の再読込窓に入りうるものだけ残す。 */
    fun prune(keys: Set<String>, newWatermarkMs: Long): Set<String> {
        val floor = newWatermarkMs - OVERLAP_MS
        return keys.filter { (it.split('|').getOrNull(1)?.toLongOrNull() ?: 0L) >= floor }.toSet()
    }
}
