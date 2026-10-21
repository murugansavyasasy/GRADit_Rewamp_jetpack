package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import java.io.ByteArrayOutputStream

/**
 * One-page PDF fee statement — summary, fee structure and payments — saved to Downloads
 * and opened. TODO: download the college's official statement once the fees API serves it.
 */
object FeeStatement {

    fun download(context: Context, account: FeeAccount) {
        val name = "Fee_statement_${account.firstName}_${account.academicYear.replace('–', '-')}.pdf"
        val uri = runCatching { FacultyExport.save(context, name, MIME_PDF, pdf(context, account)) }.getOrNull()
        if (uri == null) {
            Toast.makeText(context, R.string.fee_statement_failed, Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, context.getString(R.string.fee_statement_saved, name), Toast.LENGTH_SHORT).show()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, MIME_PDF).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: ActivityNotFoundException) {
            // Saved to Downloads already; nothing installed to open it.
        }
    }

    private fun pdf(context: Context, account: FeeAccount): ByteArray {
        val title = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val heading = Paint().apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { textSize = 10f; isAntiAlias = true }
        val muted = Paint(body).apply { color = 0xFF6A7183.toInt() }
        val right = Paint(body).apply { textAlign = Paint.Align.RIGHT }
        val rightBold = Paint(heading).apply { textAlign = Paint.Align.RIGHT }

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        val end = PAGE_WIDTH - MARGIN
        var y = MARGIN

        fun text(value: String, paint: Paint, x: Float = MARGIN, advance: Float = paint.textSize + 8f) {
            canvas.drawText(value, x, y + paint.textSize, paint)
            y += advance
        }
        fun row(label: String, amount: String, labelPaint: Paint = body, amountPaint: Paint = right) {
            canvas.drawText(label, MARGIN, y + labelPaint.textSize, labelPaint)
            canvas.drawText(amount, end, y + amountPaint.textSize, amountPaint)
            y += labelPaint.textSize + 8f
        }
        fun rule() {
            canvas.drawLine(MARGIN, y, end, y, muted)
            y += 10f
        }

        text(context.getString(R.string.fee_statement_title), title, advance = 26f)
        text(context.getString(R.string.fee_subtitle, account.name, account.programme, account.semester, account.academicYear), muted, advance = 24f)

        row(context.getString(R.string.fee_total), rupees(account.total))
        row(context.getString(R.string.fee_paid), rupees(account.paid))
        row(context.getString(R.string.fee_in_review), rupees(account.inReview))
        row(context.getString(R.string.fee_amount_due), rupees(account.amountDue), heading, rightBold)
        y += 12f

        text(context.getString(R.string.fee_tab_structure), heading)
        rule()
        account.fees.sortedBy { it.dueAt }.forEach { fee ->
            val label = "${context.getString(fee.category.label)} · ${context.getString(R.string.fee_due_on, fee.period, dayMonthYear(fee.dueAt))}"
            row(label, rupees(fee.amount))
        }
        rule()
        row(context.getString(R.string.fee_total_for_year), rupees(account.total), heading, rightBold)
        y += 12f

        text(context.getString(R.string.fee_tab_payments), heading)
        rule()
        account.payments.sortedBy { it.paidOn }.forEach { payment ->
            val fee = account.fees.firstOrNull { it.id == payment.feeId }?.let { context.getString(it.category.label) }.orEmpty()
            val status = context.getString(
                when (payment.status) {
                    PaymentStatus.VERIFIED -> R.string.fee_payment_verified
                    PaymentStatus.IN_REVIEW -> R.string.fee_state_in_review
                    PaymentStatus.REJECTED -> R.string.fee_payment_rejected
                },
            )
            row("$fee · $status", rupees(payment.amount))
            text(
                context.getString(R.string.fee_payment_line, dayMonthYear(payment.paidOn), context.getString(payment.method.label), payment.reference),
                muted,
            )
        }

        y = PAGE_HEIGHT - MARGIN
        canvas.drawText(context.getString(R.string.fee_statement_note), MARGIN, y, muted)
        doc.finishPage(page)
        return ByteArrayOutputStream().use { out ->
            doc.writeTo(out)
            doc.close()
            out.toByteArray()
        }
    }

    private const val MIME_PDF = "application/pdf"

    /** A4 at 72 dpi. */
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
}
