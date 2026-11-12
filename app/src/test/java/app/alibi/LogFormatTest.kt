package app.alibi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFormatTest {
    @Test fun eventLine_matchesSpec() {
        assertEquals(
            """{"ts":"2026-11-10T17:04:52+09:00","epoch":1794297892000,"pkg":"jp.co.sbisec.x","event":"resumed"}""",
            LogFormat.eventLine(1794297892000L, "jp.co.sbisec.x", "resumed")
        )
    }

    @Test fun heartbeat_matchesSpec() {
        assertEquals(
            """{"type":"heartbeat","coverage_until":"2026-11-10T17:15:00+09:00","generated_at":"2026-11-10T17:15:03+09:00"}""",
            LogFormat.heartbeatLine(1794298500000L, 1794298503000L)
        )
    }

    @Test fun dayFile_usesJst() {
        // 2026-10-31T15:30:00Z = 2026-11-01 00:30 JST
        assertEquals("usage-events-20261101.jsonl", LogFormat.dayFileName(1793460600000L))
        // 2026-10-31T14:59:59Z = 2026-10-31 23:59:59 JST
        assertEquals("usage-events-20261031.jsonl", LogFormat.dayFileName(1793458799000L))
    }

    @Test fun dedupPrune_keepsOnlyOverlapWindow() {
        val wm = 10_000_000L
        val keep = LogFormat.dedupKey("p", wm - 60_000, "resumed")
        val drop = LogFormat.dedupKey("p", wm - Dedup.OVERLAP_MS - 1, "paused")
        assertEquals(setOf(keep), Dedup.prune(setOf(keep, drop), wm))
    }

    @Test fun intervals_defaultIsOption_andAtLeast15min() {
        assertTrue(Intervals.DEFAULT_MIN in Intervals.OPTIONS_MIN)
        assertTrue(Intervals.OPTIONS_MIN.all { it >= 15 })
        assertEquals("3時間", Intervals.label(180))
        assertEquals("15分", Intervals.label(15))
    }
}
