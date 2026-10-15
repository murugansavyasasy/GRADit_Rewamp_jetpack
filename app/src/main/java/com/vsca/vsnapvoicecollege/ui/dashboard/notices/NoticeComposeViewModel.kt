package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.components.EditResult
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.toggleBullets
import com.vsca.vsnapvoicecollege.ui.components.wrapSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Opening the composer for a new notice, to edit one, or to copy one. */
enum class ComposeMode { NEW, EDIT, DUPLICATE }

data class NoticeComposeUiState(
    val mode: ComposeMode = ComposeMode.NEW,
    /** The notice being edited; null for new notices and copies. */
    val editingId: String? = null,
    /** True when editing a notice that receivers can already see. */
    val editingLive: Boolean = false,
    val title: String = "",
    val body: TextFieldValue = TextFieldValue(),
    val category: NoticeCategory = NoticeCategory.GENERAL,
    val priority: NoticePriority = NoticePriority.NORMAL,
    val pinned: Boolean = false,
    val classIds: Set<String> = emptySet(),
    val roles: Set<NoticeRole> = setOf(NoticeRole.STUDENTS, NoticeRole.PARENTS),
    val scheduledAt: Long? = null,
    val expiresAt: Long? = null,
    val sms: Boolean = true,
    val attachments: List<FileAttachment> = emptyList(),
    /** Errors are shown once the user has tried to publish. */
    val showErrors: Boolean = false,
    val author: NoticeAuthor? = null,
    /** The notice just published / scheduled, for the confirmation screen. */
    val published: Notice? = null,
) {
    val classes: List<NoticeClass> get() = NoticeRepository.classes.filter { it.id in classIds }
    val recipientCount: Int get() = recipientCount(classes, roles)
    fun count(role: NoticeRole): Int = if (role in roles) classes.sumOf { it.count(role) } else 0

    val issues: List<NoticeIssue>
        get() = validateNotice(title, body.text, attachments.size, classes, roles, scheduledAt, expiresAt)

    val isScheduled: Boolean get() = scheduledAt != null

    companion object {
        const val MAX_TITLE = 100
        const val MAX_BODY = 1500
        const val MAX_ATTACHMENTS = 5
        const val BOLD_MARKER = "**"
    }
}

/**
 * Holds the create / edit notice form, its validation and publishing. Shared by the
 * compose, preview and published screens (scoped to the compose back-stack entry).
 */
class NoticeComposeViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val author = NoticeRepository.currentAuthor(SessionPreferences.getRole(application))
    private val noticeId: String? = savedStateHandle.get<String>(ARG_NOTICE_ID)?.takeIf { it.isNotBlank() }
    private val mode: ComposeMode = savedStateHandle.get<String>(ARG_MODE)
        ?.let { name -> ComposeMode.entries.firstOrNull { it.name == name } }
        ?: ComposeMode.NEW

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<NoticeComposeUiState> = _uiState.asStateFlow()

    private fun initialState(): NoticeComposeUiState {
        val base = NoticeComposeUiState(mode = ComposeMode.NEW, author = author)
        val source = noticeId?.let(NoticeRepository::byId) ?: return base
        val copy = mode == ComposeMode.DUPLICATE
        val status = source.status()
        return base.copy(
            mode = mode,
            editingId = if (copy) null else source.id,
            editingLive = !copy && (status == NoticeStatus.PUBLISHED || status == NoticeStatus.EXPIRED),
            title = source.title,
            body = TextFieldValue(source.body, TextRange(source.body.length)),
            category = source.category,
            priority = source.priority,
            pinned = source.pinned,
            classIds = source.classes.map { it.id }.toSet(),
            roles = source.roles,
            // A copy goes out now unless the user schedules it again; old dates don't carry over.
            scheduledAt = if (copy) null else source.scheduledAt,
            expiresAt = if (copy) null else source.expiresAt,
            sms = source.smsEnabled,
            attachments = source.attachments,
        )
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value.take(NoticeComposeUiState.MAX_TITLE)) }

    fun onBodyChange(value: TextFieldValue) = _uiState.update {
        it.copy(body = if (value.text.length <= NoticeComposeUiState.MAX_BODY) value else it.body)
    }

    fun onToggleBold() = applyEdit { text, start, end -> wrapSelection(text, start, end, NoticeComposeUiState.BOLD_MARKER) }

    fun onToggleBullets() = applyEdit(::toggleBullets)

    fun onCategory(value: NoticeCategory) = _uiState.update { it.copy(category = value) }
    fun onPriority(value: NoticePriority) = _uiState.update { it.copy(priority = value) }
    fun onPinned(value: Boolean) = _uiState.update { it.copy(pinned = value) }
    fun onClassToggled(id: String) = _uiState.update {
        it.copy(classIds = if (id in it.classIds) it.classIds - id else it.classIds + id)
    }
    fun onRoleToggled(role: NoticeRole) = _uiState.update {
        it.copy(roles = if (role in it.roles) it.roles - role else it.roles + role)
    }
    fun onSchedule(value: Long?) = _uiState.update { it.copy(scheduledAt = value) }
    fun onExpiry(value: Long?) = _uiState.update { it.copy(expiresAt = value) }
    fun onSms(value: Boolean) = _uiState.update { it.copy(sms = value) }

    /** Adds an attachment; returns false when the limit is reached. */
    fun onAttachment(value: FileAttachment): Boolean {
        if (_uiState.value.attachments.size >= NoticeComposeUiState.MAX_ATTACHMENTS) return false
        _uiState.update { it.copy(attachments = it.attachments + value) }
        return true
    }

    fun onRemoveAttachment(value: FileAttachment) = _uiState.update { it.copy(attachments = it.attachments - value) }

    /** Turns error display on; returns true when the notice can go out. */
    fun checkReady(): Boolean {
        _uiState.update { it.copy(showErrors = true) }
        return _uiState.value.issues.isEmpty()
    }

    /** Saves a new or draft notice without publishing (scheduled ones keep their schedule). */
    fun onSaveDraft() {
        val state = _uiState.value
        val existing = state.editingId?.let(NoticeRepository::byId)
        NoticeRepository.save(buildNotice(state, existing).copy(isDraft = existing?.isDraft ?: true))
    }

    /** Publishes (or schedules) the notice; returns false if it isn't ready. */
    fun onPublish(): Boolean {
        if (!checkReady()) return false
        val state = _uiState.value
        val existing = state.editingId?.let(NoticeRepository::byId)
        val now = System.currentTimeMillis()
        val notice = buildNotice(state, existing).copy(
            isDraft = false,
            publishedAt = if (state.scheduledAt != null) null else existing?.publishedAt ?: now,
        )
        NoticeRepository.save(notice)
        _uiState.update { it.copy(published = notice) }
        return true
    }

    /** Clears the form for "Create another". */
    fun onReset() {
        _uiState.value = NoticeComposeUiState(author = author)
    }

    /** The form as a notice, keeping read data and identity when editing. */
    fun previewNotice(): Notice = buildNotice(_uiState.value, _uiState.value.editingId?.let(NoticeRepository::byId))

    private fun buildNotice(state: NoticeComposeUiState, existing: Notice?): Notice = Notice(
        id = existing?.id ?: NoticeRepository.newId(),
        title = state.title.trim(),
        body = state.body.text.trim(),
        category = state.category,
        priority = state.priority,
        pinned = state.pinned,
        author = existing?.author ?: author,
        classes = state.classes,
        roles = state.roles,
        isDraft = existing?.isDraft ?: true,
        createdAt = System.currentTimeMillis(),
        publishedAt = existing?.publishedAt,
        scheduledAt = state.scheduledAt,
        expiresAt = state.expiresAt,
        smsEnabled = state.sms,
        attachments = state.attachments,
        readCount = existing?.readCount ?: 0,
        isReadByMe = existing?.isReadByMe ?: false,
    )

    private fun applyEdit(edit: (String, Int, Int) -> EditResult) = _uiState.update { state ->
        val value = state.body
        val result = edit(value.text, value.selection.min, value.selection.max)
        if (result.text.length > NoticeComposeUiState.MAX_BODY) {
            state
        } else {
            state.copy(body = TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd)))
        }
    }

    companion object {
        const val ARG_NOTICE_ID = "noticeId"
        const val ARG_MODE = "mode"
    }
}
