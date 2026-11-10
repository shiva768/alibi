package app.alibi

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** SAF で選ばれたフォルダの月別 JSONL に追記する。 */
class JsonlWriter(private val context: Context, private val treeUri: Uri) {

    /** lines: 月別ファイル名 → 追記する行。書き込み失敗時は例外(呼び出し側で watermark を進めない)。 */
    fun append(linesByFile: Map<String, List<String>>) {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("出力フォルダにアクセスできません")
        if (!dir.canWrite()) throw IllegalStateException("出力フォルダに書き込めません(再選択してください)")
        for ((name, lines) in linesByFile) {
            if (lines.isEmpty()) continue
            // MIME を octet-stream にしないと ".txt" が付与され月別ファイル名が崩れる
            val file = dir.findFile(name) ?: dir.createFile("application/octet-stream", name)
                ?: throw IllegalStateException("$name を作成できません")
            val text = lines.joinToString(separator = "\n", postfix = "\n")
            val out = context.contentResolver.openOutputStream(file.uri, "wa")
                ?: throw IllegalStateException("$name を開けません")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }
}
