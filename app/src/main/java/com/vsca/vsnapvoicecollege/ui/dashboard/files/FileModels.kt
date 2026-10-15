package com.vsca.vsnapvoicecollege.ui.dashboard.files

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import java.util.Locale

/** The broad kind of a file; drives the type tabs, preview and upload limits. */
enum class FileKind(val maxBytes: Long) {
    IMAGE(10L * MB),
    PDF(25L * MB),
    DOCUMENT(25L * MB),
    VIDEO(200L * MB),
}

private const val MB = 1024L * 1024L

/** Upload formats the college accepts, by file extension. */
enum class FileFormat(val extension: String, val kind: FileKind, val mimeType: String) {
    JPG("jpg", FileKind.IMAGE, "image/jpeg"),
    JPEG("jpeg", FileKind.IMAGE, "image/jpeg"),
    PNG("png", FileKind.IMAGE, "image/png"),
    PDF("pdf", FileKind.PDF, "application/pdf"),
    DOC("doc", FileKind.DOCUMENT, "application/msword"),
    DOCX("docx", FileKind.DOCUMENT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLS("xls", FileKind.DOCUMENT, "application/vnd.ms-excel"),
    XLSX("xlsx", FileKind.DOCUMENT, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPT("ppt", FileKind.DOCUMENT, "application/vnd.ms-powerpoint"),
    PPTX("pptx", FileKind.DOCUMENT, "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    MP4("mp4", FileKind.VIDEO, "video/mp4"),
    MOV("mov", FileKind.VIDEO, "video/quicktime"),
    ;

    /** "JPG", "DOCX" … as shown on type chips (JPEG is shown as JPG). */
    val label: String get() = if (this == JPEG) "JPG" else extension.uppercase(Locale.ROOT)

    /** Tile / chip colours for this format. */
    val tint: FormatTint
        get() = when (this) {
            PDF -> FormatTint(AlertBackground, BadgeRed)
            PPT, PPTX -> FormatTint(AmberChipBackground, OnboardingAmberCard)
            XLS, XLSX -> FormatTint(OnboardingGreenDisc, BrandGreen)
            MP4, MOV -> FormatTint(BadgeNeutralBackground, TextPrimary)
            else -> FormatTint(SelectedRowBackground, BrandBlue)
        }

    companion object {
        fun fromFileName(name: String): FileFormat? {
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return entries.firstOrNull { it.extension == ext }
        }

        fun fromMimeType(mime: String?): FileFormat? = entries.firstOrNull { it.mimeType == mime }

        val acceptedMimeTypes: Array<String> get() = entries.map { it.mimeType }.distinct().toTypedArray()
        val documentMimeTypes: Array<String>
            get() = entries.filter { it.kind == FileKind.PDF || it.kind == FileKind.DOCUMENT }
                .map { it.mimeType }.distinct().toTypedArray()
    }
}

data class FormatTint(val background: Color, val content: Color)

enum class FileCategory(@param:StringRes val label: Int) {
    CIRCULAR(R.string.files_cat_circular),
    STUDY_MATERIAL(R.string.files_cat_study_material),
    SYLLABUS(R.string.files_cat_syllabus),
    QUESTION_PAPER(R.string.files_cat_question_paper),
    FORMS(R.string.files_cat_forms),
    GALLERY(R.string.files_cat_gallery),
    OTHER(R.string.files_cat_other),
}

enum class AudienceRole(@param:StringRes val label: Int) {
    STUDENTS(R.string.files_role_students),
    PARENTS(R.string.files_role_parents),
    STAFF(R.string.files_role_staff),
}

/** A class / group files can be shared with, with how many people of each role it has. */
data class AudienceGroup(
    val id: String,
    val label: String,
    val students: Int,
    val parents: Int,
    val staff: Int,
    /** True when students of this group are the signed-in family's class (or everyone). */
    val includesMyClass: Boolean,
) {
    fun count(roles: Set<AudienceRole>): Int = roles.sumOf { role ->
        when (role) {
            AudienceRole.STUDENTS -> students
            AudienceRole.PARENTS -> parents
            AudienceRole.STAFF -> staff
        }
    }
}

/** A file the college has shared (or scheduled). */
data class SharedFile(
    val id: String,
    val title: String,
    val fileName: String,
    val format: FileFormat,
    val sizeBytes: Long,
    val category: FileCategory,
    /** Null means "Unsorted". */
    val folderId: String?,
    val sender: String,
    val description: String,
    val publishedAt: Long,
    /** Set while the file is scheduled for a future time. */
    val scheduledAt: Long? = null,
    /** End of visibility; null = no expiry. */
    val expiresAt: Long? = null,
    val expiryIsEndOfSemester: Boolean = false,
    val audience: AudienceGroup,
    val roles: Set<AudienceRole>,
    val viewedCount: Int? = null,
    val downloadAllowed: Boolean = true,
    /** Pages (PDF) or slides (PPT); null when unknown. */
    val pageCount: Int? = null,
    val durationMs: Long? = null,
    /** Content / resource URI of the bytes; null when the file isn't available on this device. */
    val uri: String? = null,
    /** Unseen by the signed-in parent / student. */
    val isNew: Boolean = false,
) {
    val kind: FileKind get() = format.kind
    val audienceCount: Int get() = audience.count(roles)

    fun isScheduled(now: Long = System.currentTimeMillis()): Boolean = scheduledAt != null && scheduledAt > now

    /** Whether parents / students of the signed-in class receive this file right now. */
    val isVisibleToFamily: Boolean
        get() = !isScheduled() && audience.includesMyClass &&
            (AudienceRole.STUDENTS in roles || AudienceRole.PARENTS in roles)
}

data class FileFolder(val id: String, val name: String)

enum class FileTypeTab(@param:StringRes val label: Int) {
    ALL(R.string.files_tab_all),
    IMAGES(R.string.files_tab_images),
    PDFS(R.string.files_tab_pdfs),
    DOCUMENTS(R.string.files_tab_documents),
    VIDEOS(R.string.files_tab_videos),
    ;

    fun matches(file: SharedFile): Boolean = when (this) {
        ALL -> true
        IMAGES -> file.kind == FileKind.IMAGE
        PDFS -> file.kind == FileKind.PDF
        DOCUMENTS -> file.kind == FileKind.DOCUMENT
        VIDEOS -> file.kind == FileKind.VIDEO
    }
}

enum class FileSort(@param:StringRes val label: Int) {
    NEWEST(R.string.files_sort_newest),
    OLDEST(R.string.files_sort_oldest),
    NAME(R.string.files_sort_name),
    LARGEST(R.string.files_sort_largest),
    ;

    fun apply(files: List<SharedFile>): List<SharedFile> = when (this) {
        NEWEST -> files.sortedByDescending { it.scheduledAt ?: it.publishedAt }
        OLDEST -> files.sortedBy { it.scheduledAt ?: it.publishedAt }
        NAME -> files.sortedBy { it.title.lowercase(Locale.getDefault()) }
        LARGEST -> files.sortedByDescending { it.sizeBytes }
    }
}

/** A recorded class video in the "Video" list for students and parents. */
data class LessonVideo(
    val id: String,
    val title: String,
    val subject: String,
    val teacher: String?,
    val durationMs: Long,
    val postedAt: Long,
)
