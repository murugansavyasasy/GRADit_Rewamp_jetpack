package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue

/**
 * Plain-text formatting used by the composer toolbar. Messages are sent as
 * lightweight markdown (**bold**, _italic_, "• " bullets, [text](url) links),
 * which the recipients' apps render.
 *
 * Each function takes the text and the selection and returns the edited text with
 * the new selection, so it can be unit tested without Compose.
 */
data class EditResult(val text: String, val selectionStart: Int, val selectionEnd: Int)

/** Wraps the selection in [marker] (e.g. "**"); with no selection, inserts a marker pair around the cursor. */
fun wrapSelection(text: String, start: Int, end: Int, marker: String): EditResult {
    val from = minOf(start, end).coerceIn(0, text.length)
    val to = maxOf(start, end).coerceIn(0, text.length)
    val selected = text.substring(from, to)
    // Already wrapped → unwrap, so the toolbar button toggles.
    if (from >= marker.length && text.startsWith(marker, from - marker.length) &&
        text.startsWith(marker, to)
    ) {
        val unwrapped = text.removeRange(to, to + marker.length).removeRange(from - marker.length, from)
        return EditResult(unwrapped, from - marker.length, to - marker.length)
    }
    val wrapped = text.substring(0, from) + marker + selected + marker + text.substring(to)
    return EditResult(wrapped, from + marker.length, to + marker.length)
}

/** Adds "• " to the start of every selected line, or removes it if all of them already have it. */
fun toggleBullets(text: String, start: Int, end: Int): EditResult {
    val from = minOf(start, end).coerceIn(0, text.length)
    val to = maxOf(start, end).coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', from - 1).let { if (it < 0) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', to).let { if (it < 0) text.length else it }
    val lines = text.substring(lineStart, lineEnd).split('\n')
    val allBulleted = lines.all { it.startsWith(BULLET) }
    val edited = lines.joinToString("\n") { line ->
        if (allBulleted) line.removePrefix(BULLET) else if (line.startsWith(BULLET)) line else BULLET + line
    }
    val result = text.substring(0, lineStart) + edited + text.substring(lineEnd)
    val delta = edited.length - (lineEnd - lineStart)
    return EditResult(result, lineStart, lineEnd + delta)
}

/** Turns the selection into a "[text](url)" link, or inserts the bare URL at the cursor. */
fun insertLink(text: String, start: Int, end: Int, url: String): EditResult {
    val from = minOf(start, end).coerceIn(0, text.length)
    val to = maxOf(start, end).coerceIn(0, text.length)
    val selected = text.substring(from, to)
    val insert = if (selected.isBlank()) url else "[$selected]($url)"
    val result = text.substring(0, from) + insert + text.substring(to)
    val cursor = from + insert.length
    return EditResult(result, cursor, cursor)
}

const val BULLET = "• "

private val BOLD = Regex("""\*\*(.+?)\*\*""")
private val ITALIC = Regex("""(?<![\w*])_(.+?)_(?!\w)""")
private val LINK = Regex("""\[([^\]]+)]\((https?://[^)\s]+)\)""")
private val ANY_MARKUP = Regex("${BOLD.pattern}|${ITALIC.pattern}|${LINK.pattern}")

/**
 * Renders the composer's lightweight markup — **bold**, _italic_ and [text](url) links —
 * as styled text. Unmatched markers are shown as typed.
 */
fun renderMarkup(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    for (match in ANY_MARKUP.findAll(text)) {
        append(text.substring(index, match.range.first))
        val token = match.value
        when {
            BOLD.matches(token) -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(BOLD.find(token)!!.groupValues[1])
            }
            LINK.matches(token) -> {
                val (label, url) = LINK.find(token)!!.destructured
                withLink(
                    LinkAnnotation.Url(
                        url,
                        TextLinkStyles(SpanStyle(color = BrandBlue, textDecoration = TextDecoration.Underline)),
                    ),
                ) { append(label) }
            }
            else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(ITALIC.find(token)!!.groupValues[1])
            }
        }
        index = match.range.last + 1
    }
    append(text.substring(index))
}
