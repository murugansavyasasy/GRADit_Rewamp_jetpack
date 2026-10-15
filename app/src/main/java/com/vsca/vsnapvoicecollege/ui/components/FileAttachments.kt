package com.vsca.vsnapvoicecollege.ui.components

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import java.io.File

/** Largest attachment accepted when posting / sending. */
const val MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024

/** MIME types accepted by the attachment pickers. */
val IMAGE_MIME_TYPES = arrayOf("image/jpeg", "image/png")
val PDF_MIME_TYPES = arrayOf("application/pdf")

enum class AttachmentType { IMAGE, PDF }

/** A file attached to a post / message. */
data class FileAttachment(
    val name: String,
    val sizeBytes: Long,
    val type: AttachmentType,
    /** Content URI of a locally picked file; null for remote / sample attachments. */
    val uri: String? = null,
)

/** PDF (red) or image (blue) file icon. */
@Composable
fun AttachmentIcon(type: AttachmentType, size: Dp) {
    Icon(
        painter = painterResource(if (type == AttachmentType.PDF) R.drawable.ic_file else R.drawable.ic_image_outline),
        contentDescription = null,
        tint = if (type == AttachmentType.PDF) BadgeRed else BrandBlue,
        modifier = Modifier.size(size),
    )
}

/** "420 KB" / "1.2 MB". */
@Composable
fun fileSizeLabel(bytes: Long): String {
    val kb = bytes / 1024
    return if (kb < 1024) {
        stringResource(R.string.file_size_kb, kb.coerceAtLeast(1).toInt())
    } else {
        stringResource(R.string.file_size_mb, kb / 1024f)
    }
}

/**
 * Reads a picked file's name, size and type. Returns null (after telling the user)
 * when the file is over [MAX_ATTACHMENT_BYTES].
 */
fun readAttachment(context: Context, uri: Uri): FileAttachment? {
    val resolver = context.contentResolver
    var name: String? = null
    var size = 0L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
                if (!cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
    if (size > MAX_ATTACHMENT_BYTES) {
        Toast.makeText(context, R.string.attach_too_large, Toast.LENGTH_SHORT).show()
        return null
    }
    val type = if (resolver.getType(uri)?.startsWith("image/") == true) AttachmentType.IMAGE else AttachmentType.PDF
    return FileAttachment(
        name = name ?: uri.lastPathSegment.orEmpty(),
        sizeBytes = size,
        type = type,
        uri = uri.toString(),
    )
}

private fun mimeTypeOf(attachment: FileAttachment) =
    if (attachment.type == AttachmentType.PDF) "application/pdf" else "image/jpeg"

/**
 * Copies [attachment]'s bytes into the app cache (once) so they can be rendered or handed
 * to another app through the FileProvider. Returns null if the bytes aren't available.
 */
fun cachedAttachmentCopy(context: Context, attachment: FileAttachment): File? {
    val source = attachment.uri ?: return null
    val dir = File(context.cacheDir, "shared/attachments/${source.hashCode().toUInt()}").apply { mkdirs() }
    val target = File(dir, attachment.name.ifBlank { "attachment" })
    if (target.exists() && target.length() > 0) return target
    return runCatching {
        context.contentResolver.openInputStream(source.toUri())!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        target
    }.getOrNull()
}

/**
 * Opens an attachment in a viewer app. The bytes are shared through the FileProvider so
 * bundled and picked files open the same way.
 * TODO: download remote attachments once the API lands.
 */
fun openAttachment(context: Context, attachment: FileAttachment) {
    val copy = cachedAttachmentCopy(context, attachment)
    if (copy == null) {
        Toast.makeText(context, R.string.attach_open_failed, Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", copy)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeTypeOf(attachment))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.attach_open_failed, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves [attachment] to the device's Downloads folder (app-specific on Android 9 and
 * below, so no storage permission is needed) and reports the result.
 * TODO: download from the attachment URL via DownloadManager once the API serves files.
 */
fun saveToDownloads(context: Context, attachment: FileAttachment) {
    val source = attachment.uri
    val saved = source != null && runCatching {
        context.contentResolver.openInputStream(source.toUri())!!.use { input ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, attachment.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeTypeOf(attachment))
                }
                val target = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                context.contentResolver.openOutputStream(target)!!.use { input.copyTo(it) }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                File(dir, attachment.name).outputStream().use { input.copyTo(it) }
            }
        }
    }.isSuccess
    Toast.makeText(
        context,
        if (saved) context.getString(R.string.msg_download_done, attachment.name) else context.getString(R.string.msg_download_failed),
        Toast.LENGTH_SHORT,
    ).show()
}
