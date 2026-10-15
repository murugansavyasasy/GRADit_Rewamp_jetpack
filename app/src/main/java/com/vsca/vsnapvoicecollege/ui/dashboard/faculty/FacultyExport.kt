package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.vsca.vsnapvoicecollege.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
}

/** Where an export landed: [uri] opens or shares it. */
data class ExportedFile(val name: String, val uri: Uri, val format: ExportFormat, val count: Int, val savedAt: Long)

/**
 * Writes the filtered faculty list — designation, department, divisions, subjects and
 * today's status, grouped by department — and saves it to Downloads.
 */
object FacultyExport {

    fun export(context: Context, directory: FacultyDirectory, faculty: List<Faculty>, baseName: String, format: ExportFormat): ExportedFile {
        val rows = rowsFor(context, directory, faculty)
        val bytes = when (format) {
            ExportFormat.PDF -> pdf(context, directory, rows, faculty.size)
            ExportFormat.EXCEL -> xlsx(header(context), rows.map { it.cells })
        }
        val name = "$baseName.${format.extension}"
        return ExportedFile(name, save(context, name, format.mimeType, bytes), format, faculty.size, System.currentTimeMillis())
    }

    fun open(context: Context, file: ExportedFile) = launch(
        context,
        Intent(Intent.ACTION_VIEW).setDataAndType(file.uri, file.format.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    fun share(context: Context, file: ExportedFile) = launch(
        context,
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType(file.format.mimeType)
                .putExtra(Intent.EXTRA_STREAM, file.uri)
                .putExtra(Intent.EXTRA_SUBJECT, file.name)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            null,
        ),
    )

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.faculty_export_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    private class Row(val department: String, val cells: List<String>)

    private fun header(context: Context) = listOf(
        R.string.faculty_col_name, R.string.faculty_col_designation, R.string.faculty_col_department,
        R.string.faculty_col_divisions, R.string.faculty_col_subjects, R.string.faculty_col_today,
    ).map(context::getString)

    private fun rowsFor(context: Context, directory: FacultyDirectory, faculty: List<Faculty>): List<Row> = faculty.map { f ->
        val dept = directory.department(f.departmentId)?.name.orEmpty()
        val divisions = f.teaching.mapNotNull { directory.division(it.divisionId) }.distinct()
            .joinToString(", ") { context.getString(R.string.faculty_division_short, it.programme, it.semester, it.section) }
        val status = when (f.presence.status) {
            PresenceStatus.PRESENT -> f.presence.punchMinute?.let { context.getString(R.string.faculty_present_at, clock(context, it)) }
                ?: context.getString(R.string.faculty_present)
            PresenceStatus.ABSENT -> context.getString(R.string.faculty_absent)
            PresenceStatus.ON_LEAVE -> context.getString(R.string.faculty_on_leave)
            PresenceStatus.NOT_IN -> context.getString(R.string.faculty_not_in)
        }
        Row(dept, listOf(f.name, f.designation, dept, divisions, f.teaching.map { it.subject }.distinct().joinToString(", "), status))
    }.sortedBy { it.department }

    private fun clock(context: Context, minute: Int): String {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, minute / 60)
            set(java.util.Calendar.MINUTE, minute % 60)
        }
        return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
    }

    // ---- PDF (A4 portrait) ----

    private fun pdf(context: Context, directory: FacultyDirectory, rows: List<Row>, count: Int): ByteArray {
        val doc = PdfDocument()
        val title = Paint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val heading = Paint().apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { textSize = 9f; isAntiAlias = true }
        val muted = Paint(body).apply { color = 0xFF6A7183.toInt() }
        val width = 595
        val height = 842
        val margin = 40f
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let(doc::finishPage)
            page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, ++pageNumber).create())
            y = margin
        }

        fun ensure(space: Float) {
            if (page == null || y + space > height - margin) newPage()
        }

        fun line(text: String, paint: Paint, x: Float = margin, advance: Float = paint.textSize + 5f) {
            ensure(advance)
            page!!.canvas.drawText(ellipsize(text, paint, width - margin - x), x, y + paint.textSize, paint)
            y += advance
        }

        newPage()
        line(context.getString(R.string.faculty_export_pdf_title, directory.institution), title, advance = 24f)
        line(context.getString(R.string.faculty_export_pdf_count, count), muted, advance = 20f)
        rows.groupBy { it.department }.forEach { (dept, people) ->
            ensure(40f)
            y += 6f
            line(dept, heading, advance = 18f)
            people.forEach { r ->
                ensure(42f)
                line("${r.cells[0]}  ·  ${r.cells[1]}  ·  ${r.cells[5]}", body, advance = 13f)
                if (r.cells[4].isNotBlank()) line(r.cells[4], muted, x = margin + 12f, advance = 12f)
                if (r.cells[3].isNotBlank()) line(r.cells[3], muted, x = margin + 12f, advance = 15f)
            }
        }
        page?.let(doc::finishPage)
        return ByteArrayOutputStream().also { doc.writeTo(it); doc.close() }.toByteArray()
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) end--
        return text.take(end) + "…"
    }

    // ---- Excel (.xlsx: one sheet, inline strings) ----

    private fun xlsx(header: List<String>, rows: List<List<String>>): ByteArray {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        fun col(i: Int) = ('A' + i).toString()
        val sheetRows = (listOf(header) + rows).mapIndexed { r, cells ->
            "<row r=\"${r + 1}\">" + cells.mapIndexed { c, v ->
                "<c r=\"${col(c)}${r + 1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>"
            }.joinToString("") + "</row>"
        }.joinToString("")
        val files = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Faculty" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/worksheets/sheet1.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$sheetRows</sheetData></worksheet>""",
        )
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    // ---- Saving ----

    private fun save(context: Context, name: String, mimeType: String, bytes: ByteArray): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
            context.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
            uri
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
            val file = File(dir, name).apply { writeBytes(bytes) }
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        }
}
