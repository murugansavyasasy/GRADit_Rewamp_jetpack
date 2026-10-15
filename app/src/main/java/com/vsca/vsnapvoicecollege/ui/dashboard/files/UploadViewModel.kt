package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Most files accepted in one upload. */
const val MAX_FILES_PER_UPLOAD = 10

enum class RejectReason { UNSUPPORTED, TOO_LARGE, LIMIT }

sealed interface UploadStatus {
    data class Uploading(val progress: Float) : UploadStatus
    data object Uploaded : UploadStatus
    data class Failed(val progress: Float) : UploadStatus
    data class Rejected(val reason: RejectReason) : UploadStatus
}

data class UploadItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val format: FileFormat?,
    val status: UploadStatus,
)

/** The upload form, kept when the user taps "Save draft". */
data class UploadDraft(
    val items: List<UploadItem>,
    val title: String,
    val description: String,
    val category: FileCategory?,
    val folderId: String?,
    val groupId: String?,
    val roles: Set<AudienceRole>,
    val scheduledAt: Long?,
    val expiresAt: Long?,
    val downloadAllowed: Boolean,
    val notifySms: Boolean,
)

/** What was just published, for the confirmation screen. */
data class PublishSummary(
    val fileCount: Int,
    val people: Int,
    val groupLabel: String,
    val students: Int,
    val parents: Int,
    val staff: Int,
    val scheduledAt: Long?,
    val smsParents: Int?,
    val folderName: String?,
    val expiresAt: Long?,
)

data class UploadUiState(
    val items: List<UploadItem> = emptyList(),
    val title: String = "",
    val description: String = "",
    val category: FileCategory? = null,
    val folderId: String? = null,
    val group: AudienceGroup? = null,
    val roles: Set<AudienceRole> = setOf(AudienceRole.STUDENTS, AudienceRole.PARENTS),
    val scheduledAt: Long? = null,
    val expiresAt: Long? = null,
    val downloadAllowed: Boolean = true,
    val notifySms: Boolean = true,
    val senderName: String = "",
    val senderInitials: String = "",
    val published: PublishSummary? = null,
) {
    val acceptedCount: Int get() = items.count { it.status !is UploadStatus.Rejected }
    val readyItems: List<UploadItem> get() = items.filter { it.status == UploadStatus.Uploaded }
    val failedCount: Int get() = items.count { it.status is UploadStatus.Failed }
    val rejectedCount: Int get() = items.count { it.status is UploadStatus.Rejected }
    val uploadingCount: Int get() = items.count { it.status is UploadStatus.Uploading }
    val audienceCount: Int get() = group?.count(roles) ?: 0

    /** The first reason Preview / Publish can't go ahead yet, or null when ready. */
    @get:StringRes
    val blocker: Int?
        get() = when {
            readyItems.isEmpty() && uploadingCount == 0 -> R.string.files_block_no_files
            uploadingCount > 0 -> R.string.files_block_uploading
            failedCount > 0 -> R.string.files_block_failed
            title.isBlank() -> R.string.files_block_title
            category == null -> R.string.files_block_category
            group == null || roles.isEmpty() -> R.string.files_block_audience
            else -> null
        }

    companion object {
        const val MAX_TITLE = 120
        const val MAX_DESCRIPTION = 500
    }
}

/**
 * Holds the two-step upload (add files + details → preview → publish). Shared by the
 * Upload, Preview and Published screens, scoped to the Upload screen's back-stack entry.
 */
class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val role = SessionPreferences.getRole(application)
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()
    private val uploads = mutableMapOf<String, Job>()

    init {
        NetworkMonitor.start(application)
    }

    private fun initialState(): UploadUiState {
        val base = UploadUiState(
            senderName = FileRepository.senderName(role),
            senderInitials = FileRepository.senderInitials(role),
            group = FileRepository.audienceGroups.first(),
        )
        val draft = FileRepository.draft ?: return base
        return base.copy(
            items = draft.items,
            title = draft.title,
            description = draft.description,
            category = draft.category,
            folderId = draft.folderId,
            group = FileRepository.audienceGroups.firstOrNull { it.id == draft.groupId },
            roles = draft.roles,
            scheduledAt = draft.scheduledAt,
            expiresAt = draft.expiresAt,
            downloadAllowed = draft.downloadAllowed,
            notifySms = draft.notifySms,
        )
    }

    // ---- Adding files ----

    fun onFilesPicked(picked: List<PickedFile>) {
        var accepted = _uiState.value.acceptedCount
        val newItems = picked.map { file ->
            val status = when {
                file.format == null -> UploadStatus.Rejected(RejectReason.UNSUPPORTED)
                file.sizeBytes > file.format.kind.maxBytes -> UploadStatus.Rejected(RejectReason.TOO_LARGE)
                accepted >= MAX_FILES_PER_UPLOAD -> UploadStatus.Rejected(RejectReason.LIMIT)
                else -> {
                    accepted++
                    UploadStatus.Uploading(0f)
                }
            }
            UploadItem(UUID.randomUUID().toString(), file.uri, file.name, file.sizeBytes, file.format, status)
        }
        _uiState.update { it.copy(items = it.items + newItems) }
        newItems.filter { it.status is UploadStatus.Uploading }.forEach { startUpload(it.id, from = 0f) }
        // Suggest a title from the first file when the user hasn't typed one.
        if (_uiState.value.title.isBlank()) {
            newItems.firstOrNull { it.status !is UploadStatus.Rejected }?.let { onTitleChange(titleFromFileName(it.name)) }
        }
    }

    fun onRetry(itemId: String) {
        val item = _uiState.value.items.firstOrNull { it.id == itemId } ?: return
        val from = (item.status as? UploadStatus.Failed)?.progress ?: 0f
        updateItem(itemId) { it.copy(status = UploadStatus.Uploading(from)) }
        startUpload(itemId, from)
    }

    fun onRemove(itemId: String) {
        uploads.remove(itemId)?.cancel()
        _uiState.update { state -> state.copy(items = state.items.filterNot { it.id == itemId }) }
    }

    /**
     * Uploads one file, failing (keeping its progress) if the connection drops.
     * TODO: upload to the files API; progress currently follows file size.
     */
    private fun startUpload(itemId: String, from: Float) {
        uploads[itemId]?.cancel()
        uploads[itemId] = viewModelScope.launch {
            val size = _uiState.value.items.firstOrNull { it.id == itemId }?.sizeBytes ?: 0L
            val step = (UPLOAD_BYTES_PER_TICK.toFloat() / size.coerceAtLeast(1L)).coerceIn(0.02f, 0.35f)
            var progress = from
            while (progress < 1f) {
                delay(UPLOAD_TICK_MS)
                if (!NetworkMonitor.isOnline.value) {
                    updateItem(itemId) { it.copy(status = UploadStatus.Failed(progress)) }
                    return@launch
                }
                progress = (progress + step).coerceAtMost(1f)
                updateItem(itemId) { it.copy(status = UploadStatus.Uploading(progress)) }
            }
            updateItem(itemId) { it.copy(status = UploadStatus.Uploaded) }
        }
    }

    private fun updateItem(itemId: String, transform: (UploadItem) -> UploadItem) {
        _uiState.update { state -> state.copy(items = state.items.map { if (it.id == itemId) transform(it) else it }) }
    }

    // ---- Details ----

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value.take(UploadUiState.MAX_TITLE)) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value.take(UploadUiState.MAX_DESCRIPTION)) }
    fun onCategorySelected(value: FileCategory) = _uiState.update { it.copy(category = value) }
    fun onFolderSelected(value: String?) = _uiState.update { it.copy(folderId = value) }
    fun onGroupSelected(value: AudienceGroup) = _uiState.update { it.copy(group = value) }
    fun onRoleToggled(value: AudienceRole) = _uiState.update {
        it.copy(roles = if (value in it.roles) it.roles - value else it.roles + value)
    }
    fun onScheduleChange(value: Long?) = _uiState.update { it.copy(scheduledAt = value) }
    fun onExpiryChange(value: Long?) = _uiState.update { it.copy(expiresAt = value) }
    fun onDownloadAllowedChange(value: Boolean) = _uiState.update { it.copy(downloadAllowed = value) }
    fun onNotifySmsChange(value: Boolean) = _uiState.update { it.copy(notifySms = value) }

    fun onSaveDraft() {
        val state = _uiState.value
        FileRepository.draft = UploadDraft(
            items = state.readyItems,
            title = state.title,
            description = state.description,
            category = state.category,
            folderId = state.folderId,
            groupId = state.group?.id,
            roles = state.roles,
            scheduledAt = state.scheduledAt,
            expiresAt = state.expiresAt,
            downloadAllowed = state.downloadAllowed,
            notifySms = state.notifySms,
        )
    }

    /** Publishes every ready file; returns false (doing nothing) if the form isn't ready. */
    fun onPublish(): Boolean {
        val state = _uiState.value
        val group = state.group
        if (state.blocker != null || group == null) return false
        val now = System.currentTimeMillis()
        val scheduledAt = state.scheduledAt?.takeIf { it > now }
        val files = state.readyItems.mapIndexed { index, item ->
            val format = item.format ?: FileFormat.PDF
            SharedFile(
                id = UUID.randomUUID().toString(),
                title = if (index == 0) state.title.trim() else titleFromFileName(item.name),
                fileName = item.name,
                format = format,
                sizeBytes = item.sizeBytes,
                category = state.category ?: FileCategory.OTHER,
                folderId = state.folderId,
                sender = state.senderName,
                description = state.description.trim(),
                publishedAt = scheduledAt ?: now,
                scheduledAt = scheduledAt,
                expiresAt = state.expiresAt,
                audience = group,
                roles = state.roles,
                viewedCount = if (scheduledAt == null) 0 else null,
                downloadAllowed = state.downloadAllowed,
                uri = item.uri.toString(),
            )
        }
        FileRepository.publish(files)
        FileRepository.draft = null
        _uiState.update {
            it.copy(
                published = PublishSummary(
                    fileCount = files.size,
                    people = group.count(state.roles),
                    groupLabel = group.label,
                    students = if (AudienceRole.STUDENTS in state.roles) group.students else 0,
                    parents = if (AudienceRole.PARENTS in state.roles) group.parents else 0,
                    staff = if (AudienceRole.STAFF in state.roles) group.staff else 0,
                    scheduledAt = scheduledAt,
                    smsParents = group.parents.takeIf { state.notifySms && AudienceRole.PARENTS in state.roles },
                    folderName = FileRepository.folderName(state.folderId),
                    expiresAt = state.expiresAt,
                ),
            )
        }
        return true
    }

    /** Clears the form for "Upload more". */
    fun onReset() {
        uploads.values.forEach { it.cancel() }
        uploads.clear()
        _uiState.value = initialState()
    }

    companion object {
        private const val UPLOAD_TICK_MS = 150L
        private const val UPLOAD_BYTES_PER_TICK = 256L * 1024L

        /** "Exam_rules_IA2.docx" → "Exam rules IA2". */
        fun titleFromFileName(name: String): String =
            name.substringBeforeLast('.').replace('_', ' ').replace(Regex("\\s+"), " ").trim()
    }
}
