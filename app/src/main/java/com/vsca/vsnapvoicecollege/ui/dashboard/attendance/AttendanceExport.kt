package com.vsca.vsnapvoicecollege.ui.dashboard.attendance

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportedFile
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import java.io.ByteArrayOutputStream

/** A titled table, written as a PDF (landscape A4) or a one-sheet Excel file. */
data class AttendanceTable(
    val title: String,
    val subtitle: String,
    val header: List<String>,
    val rows: List<List<String>>,
    /** Relative column widths for the PDF; equal when empty. */
    val weights: List<Float> = emptyList(),
)

/** Saves attendance reports to Downloads, sharing the faculty export's writers. */
object AttendanceExport {

    fun export(context: Context, baseName: String, format: ExportFormat, table: AttendanceTable): ExportedFile {
        val bytes = when (format) {
            ExportFormat.PDF -> pdf(table)
            ExportFormat.EXCEL -> FacultyExport.xlsx(table.header, table.rows)
        }
        val name = "$baseName.${format.extension}"
        return ExportedFile(name, FacultyExport.save(context, name, format.mimeType, bytes), format, table.rows.size, System.currentTimeMillis())
    }

    /** PDF page count for a table of [rows] rows, used on "Export complete". */
    fun pdfPages(rows: Int): Int = 1 + maxOf(0, rows - FIRST_PAGE_ROWS + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE

    private const val WIDTH = 842
    private const val HEIGHT = 595
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 16f
    private const val ROWS_PER_PAGE = 30
    private const val FIRST_PAGE_ROWS = 27

    private fun pdf(table: AttendanceTable): ByteArray {
        val doc = PdfDocument()
        val title = Paint().apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val heading = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { textSize = 9f; isAntiAlias = true }
        val muted = Paint(body).apply { color = 0xFF6A7183.toInt() }
        val rule = Paint().apply { color = 0xFFE6E8EF.toInt(); strokeWidth = 0.8f }

        val usable = WIDTH - 2 * MARGIN
        val weights = table.weights.takeIf { it.size == table.header.size } ?: List(table.header.size) { 1f }
        val total = weights.sum()
        val columnX = weights.runningFold(MARGIN) { x, w -> x + usable * w / total }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun drawRow(cells: List<String>, paint: Paint) {
            val canvas = page!!.canvas
            cells.forEachIndexed { i, cell ->
                val width = columnX[i + 1] - columnX[i] - 6f
                canvas.drawText(FacultyExport.ellipsize(cell, paint, width), columnX[i], y + paint.textSize, paint)
            }
            y += ROW_HEIGHT
            canvas.drawLine(MARGIN, y - 3f, WIDTH - MARGIN, y - 3f, rule)
        }

        fun newPage() {
            page?.let(doc::finishPage)
            page = doc.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, ++pageNumber).create())
            y = MARGIN
            if (pageNumber == 1) {
                page!!.canvas.drawText(table.title, MARGIN, y + title.textSize, title)
                y += 22f
                page!!.canvas.drawText(table.subtitle, MARGIN, y + muted.textSize, muted)
                y += 22f
            }
            drawRow(table.header, heading)
        }

        newPage()
        table.rows.forEach { cells ->
            if (y + ROW_HEIGHT > HEIGHT - MARGIN) newPage()
            drawRow(cells, body)
        }
        page?.let(doc::finishPage)
        return ByteArrayOutputStream().also { doc.writeTo(it); doc.close() }.toByteArray()
    }
}
