package com.example.timesheet.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * ДОБАВЛЕНО (ТЗ): «возможность отправки этого документа (в соц. сети) в формате xls и pdf».
 *
 * ВАЖНО — требует ручной донастройки проекта (нет доступа к текущему AndroidManifest.xml,
 * поэтому не мог внести изменения автоматически):
 * 1) В AndroidManifest.xml внутри <application> добавить:
 *
 *    <provider
 *        android:name="androidx.core.content.FileProvider"
 *        android:authorities="${applicationId}.fileprovider"
 *        android:exported="false"
 *        android:grantUriPermissions="true">
 *        <meta-data
 *            android:name="android.support.FILE_PROVIDER_PATHS"
 *            android:resource="@xml/file_paths" />
 *    </provider>
 *
 * 2) Файл res/xml/file_paths.xml уже добавлен в этот патч.
 * 3) Убедиться, что в build.gradle подключён androidx.core:core-ktx (обычно уже подключён).
 *
 * Формат .xls реализован через HTML-таблицу с расширением .xls — Excel/Google Sheets
 * открывают такой файл корректно. Полноценный бинарный XLSX через Apache POI не подключался,
 * чтобы не тянуть в проект новую тяжёлую зависимость без согласования.
 */
object ReportExport {

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Отправить отчёт").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /** rows: список строк, каждая строка — список ячеек (обычно 2 колонки: наименование / значение). */
    fun shareAsXls(context: Context, title: String, rows: List<List<String>>) {
        val html = buildString {
            append("<html><head><meta charset=\"utf-8\"></head><body>")
            append("<table border=\"1\">")
            append("<tr><th colspan=\"2\">").append(escapeHtml(title)).append("</th></tr>")
            rows.forEach { row ->
                append("<tr>")
                row.forEach { cell -> append("<td>").append(escapeHtml(cell)).append("</td>") }
                append("</tr>")
            }
            append("</table></body></html>")
        }
        val file = File(cacheDir(context), "${safeFileName(title)}.xls")
        FileOutputStream(file).use { it.write(html.toByteArray(Charsets.UTF_8)) }
        shareFile(context, file, "application/vnd.ms-excel")
    }

    fun shareAsPdf(context: Context, title: String, rows: List<List<String>>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 @ 72dpi
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 14f }

        var y = 40f
        canvas.drawText(title, 40f, y, titlePaint)
        y += 30f

        rows.forEach { row ->
            val line = row.joinToString("   —   ")
            if (y > 800f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            canvas.drawText(line, 40f, y, textPaint)
            y += 22f
        }
        document.finishPage(page)

        val file = File(cacheDir(context), "${safeFileName(title)}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        shareFile(context, file, "application/pdf")
    }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-zА-Яа-я0-9 _-]"), "").replace(" ", "_").ifBlank { "report" }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
