package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The "Message teacher" sheet. */
data class MessageDraft(val teacherId: String, val text: String = "", val sending: Boolean = false)

enum class MessageNotice { SENT, QUEUED, FLUSHED }

/**
 * Writing to a teacher, shared by the teachers list and a teacher's profile. Offline,
 * messages wait in the repository and go out when the connection is back.
 */
abstract class TeacherMessagingViewModel(application: Application) : AndroidViewModel(application) {

    private val _draft = MutableStateFlow<MessageDraft?>(null)
    val draft: StateFlow<MessageDraft?> = _draft.asStateFlow()

    private val _notice = MutableStateFlow<MessageNotice?>(null)
    val notice: StateFlow<MessageNotice?> = _notice.asStateFlow()

    init {
        NetworkMonitor.start(application)
        viewModelScope.launch {
            NetworkMonitor.isOnline.filter { it }.collect {
                if (FacultyRepository.flushPending() > 0) _notice.value = MessageNotice.FLUSHED
            }
        }
    }

    fun onMessage(teacherId: String) {
        _draft.value = MessageDraft(teacherId)
    }

    fun onDraftChange(text: String) = _draft.update { it?.copy(text = text) }

    fun onDismissMessage() {
        _draft.value = null
    }

    fun onSend() {
        val d = _draft.value ?: return
        if (d.text.isBlank() || d.sending) return
        _draft.value = d.copy(sending = true)
        viewModelScope.launch {
            val sent = FacultyRepository.sendMessage(d.teacherId, d.text.trim())
            _draft.value = null
            _notice.value = if (sent) MessageNotice.SENT else MessageNotice.QUEUED
        }
    }

    fun onNoticeShown() {
        _notice.value = null
    }
}

data class FamilyFacultyUiState(
    val load: LoadState = LoadState.LOADING,
    val children: List<Child> = emptyList(),
    val childId: String = "",
    val data: ChildFaculty? = null,
    /** Chosen semester per child; defaults to the current one. */
    val semesterByChild: Map<String, Int> = emptyMap(),
    val tab: TeacherTab = TeacherTab.ALL,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
) {
    val child: Child? get() = children.firstOrNull { it.id == childId }
    val semester: Int get() = semesterByChild[childId] ?: child?.currentSemester ?: 1
    val semesterClass: SemesterClass? get() = data?.semester(semester)
}

/** A parent's view of each child's teachers, semester by semester. */
class FamilyFacultyViewModel(application: Application) : TeacherMessagingViewModel(application) {

    private val state = MutableStateFlow(
        FacultyRepository.children.let { FamilyFacultyUiState(children = it, childId = it.first().id) },
    )
    val uiState: StateFlow<FamilyFacultyUiState> = state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            NetworkMonitor.isOnline.collect { online -> state.update { it.copy(isOnline = online) } }
        }
        viewModelScope.launch {
            FacultyRepository.familySyncedAt.collect { synced -> state.update { it.copy(lastSyncedAt = synced) } }
        }
        load()
    }

    fun load() {
        val childId = state.value.childId
        loadJob?.cancel()
        state.update { it.copy(load = LoadState.LOADING) }
        loadJob = viewModelScope.launch {
            try {
                val data = FacultyRepository.loadChild(childId)
                state.update { it.copy(load = LoadState.READY, data = data) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(load = LoadState.ERROR) }
            }
        }
    }

    fun onChild(childId: String) {
        if (childId == state.value.childId) return
        state.update { it.copy(childId = childId, data = null, tab = TeacherTab.ALL) }
        load()
    }

    fun onSemester(semester: Int) = state.update { it.copy(semesterByChild = it.semesterByChild + (it.childId to semester), tab = TeacherTab.ALL) }

    fun onCurrentSemester() = state.value.child?.let { onSemester(it.currentSemester) }

    fun onTab(tab: TeacherTab) = state.update { it.copy(tab = tab) }
}

/** One teacher, as seen by a parent for a given child and semester. */
class FamilyTeacherViewModel(application: Application, savedStateHandle: SavedStateHandle) : TeacherMessagingViewModel(application) {

    val childId: String = savedStateHandle[ARG_CHILD] ?: ""
    val semester: Int = savedStateHandle.get<Int>(ARG_SEMESTER) ?: 1
    val teacherId: String = savedStateHandle[ARG_TEACHER] ?: ""

    val data: ChildFaculty? = FacultyRepository.cachedChild(childId)

    companion object {
        const val ARG_CHILD = "childId"
        const val ARG_SEMESTER = "semester"
        const val ARG_TEACHER = "teacherId"
    }
}
