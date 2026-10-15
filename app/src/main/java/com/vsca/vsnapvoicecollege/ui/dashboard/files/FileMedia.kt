package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Authority of the app's FileProvider (see AndroidManifest). */
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/**
 * Base of the in-app link to a shared file. Opening it requires signing in to GRADit.
 * TODO: switch to the college's https deep-link domain once it is set up.
 */
private const val FILE_LINK_BASE = "gradit://files/"

fun fileLink(file: SharedFile): String = FILE_LINK_BASE + file.id

/**
 * Copies [file]'s bytes into the app cache (once) so they can be rendered, shared through
 * the FileProvider or opened by another app. Returns null if the bytes aren't available.
 */
suspend fun cachedCopy(context: Context, file: SharedFile): File? = withContext(Dispatchers.IO) {
    val source = file.uri ?: return@withContext null
    val target = File(File(context.cacheDir, "shared/${file.id}").apply { mkdirs() }, file.fileName)
    if (target.exists() && target.length() > 0) return@withContext target
    runCatching {
        context.contentResolver.openInputStream(source.toUri())!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        target
    }.getOrNull()
}

fun contentUriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)

/** Opens [file] in a viewer app (used for Word / Excel / PowerPoint files). */
suspend fun openInViewer(context: Context, file: SharedFile) {
    val copy = cachedCopy(context, file)
    if (copy == null) {
        Toast.makeText(context, R.string.files_not_bundled, Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(contentUriFor(context, copy), file.format.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.files_no_viewer, Toast.LENGTH_SHORT).show()
    }
}

/** Where the share sheet sends the link or file. */
enum class ShareTarget { MESSAGES, MAIL, COPY_LINK, MORE }

/** Shares a sign-in link to [file]. */
fun shareLink(context: Context, file: SharedFile, target: ShareTarget) {
    val link = fileLink(file)
    val text = context.getString(R.string.files_share_text, file.title, link)
    val intent = when (target) {
        ShareTarget.COPY_LINK -> {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(file.title, link))
            Toast.makeText(context, R.string.files_link_copied, Toast.LENGTH_SHORT).show()
            return
        }
        ShareTarget.MESSAGES -> Intent(Intent.ACTION_SENDTO, "smsto:".toUri()).putExtra("sms_body", text)
        ShareTarget.MAIL -> Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_SUBJECT, file.title)
            .putExtra(Intent.EXTRA_TEXT, text)
        ShareTarget.MORE -> Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            null,
        )
    }
    startOrToast(context, intent)
}

/** Shares a copy of [file] itself. */
suspend fun shareFileCopy(context: Context, file: SharedFile, target: ShareTarget) {
    val copy = cachedCopy(context, file)
    if (copy == null) {
        Toast.makeText(context, R.string.files_not_bundled, Toast.LENGTH_SHORT).show()
        return
    }
    val uri = contentUriFor(context, copy)
    if (target == ShareTarget.COPY_LINK) {
        shareLink(context, file, ShareTarget.COPY_LINK)
        return
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType(file.format.mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, file.title)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val intent = when (target) {
        ShareTarget.MAIL -> send.apply { selector = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()) }
        else -> Intent.createChooser(send, null)
    }
    startOrToast(context, intent)
}

private fun startOrToast(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.files_no_app, Toast.LENGTH_SHORT).show()
    }
}

/** Outcome of saving a file to the phone. */
sealed interface DownloadResult {
    /** [toGallery] is true for photos / videos saved to the Gallery; false for the Downloads folder. */
    data class Saved(val uri: Uri, val toGallery: Boolean, val mimeType: String) : DownloadResult
    data class StorageFull(val neededBytes: Long, val freeBytes: Long) : DownloadResult
    data object Offline : DownloadResult
    data object NotAvailable : DownloadResult
    data object Failed : DownloadResult
}

/** Free space on shared storage, where downloads are written. */
fun freeStorageBytes(): Long = StatFs(Environment.getExternalStorageDirectory().path).availableBytes

/**
 * Saves [file] to the phone: photos and videos into the Gallery, everything else into
 * Downloads. Needs WRITE_EXTERNAL_STORAGE only on Android 9 and below (callers ask first).
 * TODO: stream from the files API instead of copying the bundled sample.
 */
suspend fun saveToDevice(context: Context, file: SharedFile): DownloadResult = withContext(Dispatchers.IO) {
    if (!NetworkMonitor.isOnline.value) return@withContext DownloadResult.Offline
    val source = file.uri ?: return@withContext DownloadResult.NotAvailable
    val free = freeStorageBytes()
    if (free < file.sizeBytes) return@withContext DownloadResult.StorageFull(file.sizeBytes, free)

    val toGallery = file.kind == FileKind.IMAGE || file.kind == FileKind.VIDEO
    runCatching {
        val resolver = context.contentResolver
        val saved: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when (file.kind) {
                FileKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                FileKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, file.format.mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    when (file.kind) {
                        FileKind.IMAGE -> Environment.DIRECTORY_PICTURES + "/GRADit"
                        FileKind.VIDEO -> Environment.DIRECTORY_MOVIES + "/GRADit"
                        else -> Environment.DIRECTORY_DOWNLOADS
                    },
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val target = resolver.insert(collection, values)!!
            try {
                resolver.openInputStream(source.toUri())!!.use { input ->
                    resolver.openOutputStream(target)!!.use { input.copyTo(it) }
                }
                resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                // Nothing half-written is left behind on the phone.
                resolver.delete(target, null, null)
                throw e
            }
            target
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(
                when (file.kind) {
                    FileKind.IMAGE -> Environment.DIRECTORY_PICTURES
                    FileKind.VIDEO -> Environment.DIRECTORY_MOVIES
                    else -> Environment.DIRECTORY_DOWNLOADS
                },
            ).apply { mkdirs() }
            val target = File(dir, file.fileName)
            resolver.openInputStream(source.toUri())!!.use { input -> target.outputStream().use { input.copyTo(it) } }
            contentUriFor(context, target)
        }
        DownloadResult.Saved(saved, toGallery, file.format.mimeType)
    }.getOrElse { DownloadResult.Failed }
}

/** Whether saving needs the legacy storage permission on this Android version. */
val needsLegacyStoragePermission: Boolean get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

/** Opens a file the user just saved. */
fun openSaved(context: Context, result: DownloadResult.Saved) {
    startOrToast(
        context,
        Intent(Intent.ACTION_VIEW).setDataAndType(result.uri, result.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )
}

/** A file picked for upload, before validation. */
data class PickedFile(val uri: Uri, val name: String, val sizeBytes: Long, val format: FileFormat?)

/** Reads a picked file's name, size and format. */
fun readPickedFile(context: Context, uri: Uri): PickedFile {
    val resolver = context.contentResolver
    var name: String? = null
    var size = 0L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            val sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
            if (!cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    val displayName = name ?: uri.lastPathSegment.orEmpty()
    val format = FileFormat.fromFileName(displayName) ?: FileFormat.fromMimeType(resolver.getType(uri))
    return PickedFile(uri, displayName, size, format)
}

/** A fresh FileProvider URI for the camera to write a photo into. */
fun newCameraPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val photo = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    return contentUriFor(context, photo)
}
