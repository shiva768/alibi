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
            val text = lines.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)
            appendBytes(file.uri, name, text)
        }
    }

    /**
     * まず追記モード("wa")で開く。Google Drive など追記非対応のプロバイダでは
     * 開く段階で失敗するので、既存内容を読んで連結し、全体を書き戻す。
     */
    private fun appendBytes(uri: Uri, name: String, add: ByteArray) {
        val resolver = context.contentResolver
        val appendOut = try {
            resolver.openOutputStream(uri, "wa")
        } catch (e: Exception) {
            null // Unsupported mode 等 → フォールバック
        }
        if (appendOut != null) {
            appendOut.use { it.write(add) }
            return
        }
        // 読めないまま "w" で書くと既存ログを消すので、読み出し失敗時は中止する
        val existing = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("$name を読み出せません")
        val out = resolver.openOutputStream(uri, "w")
            ?: throw IllegalStateException("$name を開けません")
        out.use {
            it.write(existing)
            it.write(add)
        }
    }
}
