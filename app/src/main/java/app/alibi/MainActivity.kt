package app.alibi

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.DateFormat
import java.util.Date

/** 権限状態・対象アプリ・最終収集時刻・coverage だけの最小 UI。 */
class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var status: TextView

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.folderUri = uri
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 15f }
        root.addView(status)
        fun button(label: String, onClick: () -> Unit) = root.addView(Button(this).apply {
            text = label; setOnClickListener { onClick() }
        })
        button("使用状況へのアクセスを開く") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        button("対象アプリを選ぶ") { pickApps() }
        button("出力フォルダを選ぶ") { pickFolder.launch(prefs.folderUri) }
        button("収集間隔を選ぶ") { pickInterval() }
        button("今すぐ収集") {
            Thread {
                Collector.run(applicationContext)
                runOnUiThread { refresh() }
            }.start()
        }
        setContentView(ScrollView(this).apply { addView(root) })

        CollectWorker.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun pickApps() {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val checked = BooleanArray(apps.size) { apps[it].first in prefs.targets }
        AlertDialog.Builder(this)
            .setTitle("対象アプリ")
            .setMultiChoiceItems(apps.map { "${it.second}\n${it.first}" }.toTypedArray(), checked) { _, i, on ->
                checked[i] = on
            }
            .setPositiveButton("保存") { _, _ ->
                // 一覧に出ない既存の選択は維持しない(ピッカーが単一の真実)
                prefs.targets = apps.filterIndexed { i, _ -> checked[i] }.map { it.first }.toSet()
                refresh()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun pickInterval() {
        val options = Intervals.OPTIONS_MIN
        AlertDialog.Builder(this)
            .setTitle("収集間隔")
            .setMessage("間隔を空けるほど省電力ですが、照合側が「未確定」で待つ時間も長くなります。実際の実行は省電力機能で多少遅れることがあります。")
            .setSingleChoiceItems(options.map(Intervals::label).toTypedArray(), options.indexOf(prefs.intervalMinutes)) { dialog, i ->
                prefs.intervalMinutes = options[i]
                CollectWorker.schedule(this)
                refresh()
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun refresh() {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        fun t(ms: Long) = if (ms == 0L) "未収集" else fmt.format(Date(ms))
        val pm = packageManager
        fun label(pkg: String) = try {
            "${pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))} ($pkg)"
        } catch (e: Exception) { pkg }

        status.text = buildString {
            appendLine("権限(使用状況へのアクセス): " + if (Permission.hasUsageAccess(this@MainActivity)) "許可済み" else "未許可")
            appendLine("出力フォルダ: " + (prefs.folderUri?.let { Uri.decode(it.toString()) } ?: "未設定"))
            appendLine("対象アプリ:")
            if (prefs.targets.isEmpty()) appendLine("  (未選択)") else prefs.targets.sorted().forEach { appendLine("  ・" + label(it)) }
            appendLine("収集間隔: " + Intervals.label(prefs.intervalMinutes))
            appendLine("最終収集時刻: " + t(prefs.lastCollectAt))
            appendLine("coverage_until: " + t(prefs.coverageUntil))
            prefs.lastError?.let { appendLine("直近のエラー: $it") }
        }
    }
}
