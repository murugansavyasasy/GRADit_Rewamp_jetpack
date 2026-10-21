package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

/** Look of a template, shared by the on-screen preview and the PDF so they always match. */
internal data class ResumeStyle(
    val accent: Color,
    val nameSize: Float,
    val headingSize: Float,
    val bodySize: Float,
    /** Rule under each heading (Classic, Compact) or a bar before it (Modern). */
    val rule: Boolean,
    val sectionGap: Float,
)

internal fun ResumeTemplate.style(): ResumeStyle = when (this) {
    ResumeTemplate.CLASSIC -> ResumeStyle(Color(0xFF1F3FB0), nameSize = 26f, headingSize = 14f, bodySize = 14f, rule = true, sectionGap = 16f)
    ResumeTemplate.MODERN -> ResumeStyle(Color(0xFF17733F), nameSize = 28f, headingSize = 14f, bodySize = 14f, rule = false, sectionGap = 18f)
    ResumeTemplate.COMPACT -> ResumeStyle(Color(0xFF1F2937), nameSize = 22f, headingSize = 12f, bodySize = 12.5f, rule = true, sectionGap = 10f)
}

/**
 * Resumes go to recruiters, so the document's headings stay in English whatever the
 * app language; everything around the document follows the app language.
 */
internal fun englishContext(context: Context): Context =
    context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) })

/** Blocks of text the document is made of, in order. */
internal sealed interface ResumeBlock {
    data class Name(val text: String) : ResumeBlock
    data class Subline(val text: String, val muted: Boolean) : ResumeBlock
    data class Heading(val text: String) : ResumeBlock
    data class Body(val text: String) : ResumeBlock
}

internal fun resumeBlocks(english: Context, resume: Resume): List<ResumeBlock> = buildList {
    val c = resume.contact
    add(ResumeBlock.Name(resume.name))
    add(ResumeBlock.Subline("${c.degree} · ${c.college}", muted = false))
    add(ResumeBlock.Subline(listOf(c.email, c.phone, c.city).filter(String::isNotBlank).joinToString(" · "), muted = true))
    resume.printedSections.forEach { section ->
        add(ResumeBlock.Heading(english.getString(section.label).uppercase(Locale.ENGLISH)))
        val entries = resume.entries(section)
        when (section.layout) {
            EntryLayout.INLINE -> add(ResumeBlock.Body(entries.joinToString(" · ") { it.title }))
            EntryLayout.LINES -> entries.forEach { add(ResumeBlock.Body(it.line())) }
        }
    }
}

/** The resume as a white page, used by the preview screen. */
@Composable
internal fun ResumeDocument(resume: Resume, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val blocks = remember(resume) { resumeBlocks(englishContext(context), resume) }
    val style = resume.template.style()
    Column(modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ResumeBlock.Name -> Text(block.text, color = style.accent, fontSize = style.nameSize.sp, fontWeight = FontWeight.ExtraBold)
                is ResumeBlock.Subline -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        block.text,
                        color = if (block.muted) TextSecondary else TextPrimary,
                        fontSize = (style.bodySize - if (block.muted) 1f else 0f).sp,
                        lineHeight = (style.bodySize * 1.35f).sp,
                    )
                }
                is ResumeBlock.Heading -> {
                    Spacer(Modifier.height(style.sectionGap.dp))
                    if (style.rule) {
                        Text(block.text, color = style.accent, fontSize = style.headingSize.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                        Spacer(Modifier.height(3.dp))
                        HorizontalDivider(thickness = 1.dp, color = style.accent)
                    } else {
                        Row {
                            Box(Modifier.width(4.dp).height(16.dp).background(style.accent))
                            Spacer(Modifier.width(8.dp))
                            Text(block.text, color = style.accent, fontSize = style.headingSize.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                is ResumeBlock.Body -> {
                    if (blocks.getOrNull(index - 1) is ResumeBlock.Body) Spacer(Modifier.height(4.dp))
                    Text(
                        block.text,
                        Modifier.fillMaxWidth(),
                        color = TextPrimary.copy(alpha = 0.85f),
                        fontSize = style.bodySize.sp,
                        lineHeight = (style.bodySize * 1.4f).sp,
                    )
                }
            }
        }
        Spacer(Modifier.padding(bottom = 4.dp))
    }
}

/** Writes the resume as an A4 PDF that looks like the preview. */
object ResumePdf {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val MIME_PDF = "application/pdf"

    /** PDF bytes and how many pages they fill. */
    internal fun render(context: Context, resume: Resume): Pair<ByteArray, Int> {
        val style = resume.template.style()
        val blocks = resumeBlocks(englishContext(context), resume)
        val accent = style.accent.toArgb()
        val width = (PAGE_WIDTH - 2 * MARGIN).toInt()
        // The PDF canvas is in points; the preview's sp sizes map across 1:1, scaled down to fit A4.
        val scale = 0.82f

        val doc = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN

        fun newPage() {
            page?.let(doc::finishPage)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++pageNumber).create())
            canvas = page!!.canvas
            y = MARGIN
        }

        fun draw(text: String, size: Float, color: Int, bold: Boolean, spacingBefore: Float = 0f, lineMult: Float = 1.3f) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size * scale
                this.color = color
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, lineMult)
                .build()
            if (y + spacingBefore + layout.height > PAGE_HEIGHT - MARGIN) newPage() else y += spacingBefore
            canvas!!.save()
            canvas!!.translate(MARGIN, y)
            layout.draw(canvas!!)
            canvas!!.restore()
            y += layout.height
        }

        newPage()
        val body = TextPrimary.copy(alpha = 0.85f).toArgb()
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ResumeBlock.Name -> draw(block.text, style.nameSize, accent, bold = true, lineMult = 1.1f)
                is ResumeBlock.Subline -> draw(block.text, style.bodySize - if (block.muted) 1f else 0f, if (block.muted) TextSecondary.toArgb() else TextPrimary.toArgb(), bold = false, spacingBefore = 3f)
                is ResumeBlock.Heading -> {
                    draw(block.text, style.headingSize, accent, bold = true, spacingBefore = style.sectionGap)
                    val line = Paint().apply { color = accent; strokeWidth = if (style.rule) 0.8f else 2.5f }
                    if (style.rule) {
                        canvas!!.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, line)
                    } else {
                        canvas!!.drawLine(MARGIN, y + 2f, MARGIN + 36f, y + 2f, line)
                    }
                    y += 8f
                }
                is ResumeBlock.Body -> draw(block.text, style.bodySize, body, bold = false, spacingBefore = if (blocks.getOrNull(index - 1) is ResumeBlock.Body) 3f else 0f)
            }
        }
        page?.let(doc::finishPage)
        val bytes = ByteArrayOutputStream().use { out ->
            doc.writeTo(out)
            doc.close()
            out.toByteArray()
        }
        return bytes to pageNumber
    }

    private fun fileName(resume: Resume) = "${resume.name.replace(' ', '_')}_Resume.pdf"

    /** Saves the PDF to Downloads and opens it. */
    fun download(context: Context, resume: Resume) {
        val name = fileName(resume)
        val uri = runCatching { FacultyExport.save(context, name, MIME_PDF, render(context, resume).first) }.getOrNull()
        if (uri == null) {
            Toast.makeText(context, R.string.resume_download_failed, Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, context.getString(R.string.resume_download_done, name), Toast.LENGTH_SHORT).show()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, MIME_PDF).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: ActivityNotFoundException) {
            // Saved to Downloads already; nothing installed to open it.
        }
    }

    /** Shares the PDF through the system share sheet. */
    fun share(context: Context, resume: Resume) {
        val file = runCatching {
            val dir = File(context.cacheDir, "shared/resume").apply { mkdirs() }
            File(dir, fileName(resume)).apply { writeBytes(render(context, resume).first) }
        }.getOrNull()
        if (file == null) {
            Toast.makeText(context, R.string.resume_download_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(MIME_PDF)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.resume_share_subject, resume.name))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(send, null))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.placement_no_app, Toast.LENGTH_SHORT).show()
        }
    }
}
