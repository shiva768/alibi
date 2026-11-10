package app.alibi

import android.content.Context
import android.net.Uri

/** 端末内に保存する設定と状態。 */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("alibi", Context.MODE_PRIVATE)

    var targets: Set<String>
        get() = sp.getStringSet("targets", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("targets", v.toSet()).apply()

    var folderUri: Uri?
        get() = sp.getString("folder", null)?.let(Uri::parse)
        set(v) = sp.edit().putString("folder", v?.toString()).apply()

    /** 次回クエリの起点(前回の取得終了時刻)。0 = 未実行 */
    var watermark: Long
        get() = sp.getLong("watermark", 0L)
        set(v) = sp.edit().putLong("watermark", v).apply()

    var recentKeys: Set<String>
        get() = sp.getStringSet("recentKeys", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("recentKeys", v.toSet()).apply()

    var lastCollectAt: Long
        get() = sp.getLong("lastCollectAt", 0L)
        set(v) = sp.edit().putLong("lastCollectAt", v).apply()

    var coverageUntil: Long
        get() = sp.getLong("coverageUntil", 0L)
        set(v) = sp.edit().putLong("coverageUntil", v).apply()

    var lastError: String?
        get() = sp.getString("lastError", null)
        set(v) = sp.edit().putString("lastError", v).apply()
}
