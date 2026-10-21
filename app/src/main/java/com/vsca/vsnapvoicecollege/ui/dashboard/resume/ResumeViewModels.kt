package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ResumeLoad { LOADING, READY, ERROR }

data class ResumeUiState(
    val load: ResumeLoad = ResumeLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    /** Parents view and download; only the student edits. */
    val canEdit: Boolean = false,
    val resumes: List<Resume> = emptyList(),
    val resume: Resume? = null,
) {
    val showChildSwitcher: Boolean get() = !canEdit && resumes.size > 1
}

/** Resume strength, template and sections; the student edits, parents switch between children. */
class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val role = SessionPreferences.getRole(application)
    private val studentIds = ResumeRepository.studentIdsFor(role)
    private val selected = MutableStateFlow(studentIds.firstOrNull())
    private val load = MutableStateFlow(ResumeLoad.LOADING)

    val uiState: StateFlow<ResumeUiState> = combine(
        ResumeRepository.resumes,
        ResumeRepository.lastSyncedAt,
        selected,
        load,
        NetworkMonitor.isOnline,
    ) { all, synced, studentId, load, online ->
        val resumes = studentIds.mapNotNull { id -> all.firstOrNull { it.studentId == id } }
        ResumeUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            canEdit = role == Role.STUDENT,
            resumes = resumes,
            resume = resumes.firstOrNull { it.studentId == studentId }.takeIf { load == ResumeLoad.READY },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumeUiState(canEdit = role == Role.STUDENT))

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = ResumeLoad.LOADING
            load.value = try {
                ResumeRepository.refresh()
                ResumeLoad.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ResumeLoad.ERROR
            }
        }
    }

    fun onStudent(studentId: String) {
        selected.value = studentId
    }

    fun onTemplate(template: ResumeTemplate) {
        val resume = uiState.value.resume ?: return
        if (uiState.value.canEdit) ResumeRepository.setTemplate(resume.studentId, template)
    }
}

/** The student's editor for one section they fill in themselves (skills, projects …). */
class ResumeSectionViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val studentId: String = savedStateHandle.get<String>(ARG_STUDENT_ID).orEmpty()
    val section: ResumeSection = savedStateHandle.get<String>(ARG_SECTION)
        ?.let { name -> ResumeSection.entries.firstOrNull { it.name == name } }
        ?: ResumeSection.SKILLS

    val entries: StateFlow<List<ResumeEntry>> = ResumeRepository.resumes
        .map { list -> list.firstOrNull { it.studentId == studentId }?.entries(section).orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumeRepository.resume(studentId)?.entries(section).orEmpty())

    fun save(entryId: String?, title: String, detail: String?) {
        if (title.isBlank() || section.fromCollege) return
        if (entryId == null) {
            ResumeRepository.addEntry(studentId, section, title, detail)
        } else {
            ResumeRepository.updateEntry(studentId, section, entryId, title, detail)
        }
    }

    fun remove(entryId: String) {
        if (!section.fromCollege) ResumeRepository.removeEntry(studentId, section, entryId)
    }

    companion object {
        const val ARG_STUDENT_ID = "studentId"
        const val ARG_SECTION = "section"
    }
}
