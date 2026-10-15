package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface ProfileDialog {
    data class AssignClassTeacher(val divisionId: String?) : ProfileDialog
    data object MakeHod : ProfileDialog
}

/** Confirmation after a principal action. */
enum class ProfileNotice { CLASS_TEACHER_ASSIGNED, HOD_CHANGED }

data class FacultyProfileUiState(
    val directory: FacultyDirectory,
    val faculty: Faculty?,
    val dialog: ProfileDialog? = null,
    val notice: ProfileNotice? = null,
)

/** One teacher's profile for staff, with the principal's class-teacher and HOD actions. */
class FacultyProfileViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val facultyId: String = savedStateHandle[ARG_FACULTY_ID] ?: ""

    val canManage = SessionPreferences.getRole(application) == Role.MANAGEMENT

    private val dialog = MutableStateFlow<ProfileDialog?>(null)
    private val notice = MutableStateFlow<ProfileNotice?>(null)

    val uiState: StateFlow<FacultyProfileUiState> =
        combine(FacultyRepository.directory, dialog, notice) { d, dlg, n ->
            FacultyProfileUiState(d, d.faculty(facultyId), dlg, n)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FacultyRepository.directory.value.let { FacultyProfileUiState(it, it.faculty(facultyId)) },
        )

    /** Opens the picker with the first division that has someone else as class teacher. */
    fun onAssignClassTeacher() {
        val d = FacultyRepository.directory.value
        val first = teachableDivisions(d).firstOrNull { it.classTeacherId != facultyId }?.id
        dialog.value = ProfileDialog.AssignClassTeacher(first)
    }

    fun onPickDivision(divisionId: String) = dialog.update { (it as? ProfileDialog.AssignClassTeacher)?.copy(divisionId = divisionId) ?: it }

    fun onConfirmAssign() {
        val divisionId = (dialog.value as? ProfileDialog.AssignClassTeacher)?.divisionId ?: return
        FacultyRepository.assignClassTeacher(facultyId, divisionId)
        dialog.value = null
        notice.value = ProfileNotice.CLASS_TEACHER_ASSIGNED
    }

    fun onMakeHod() {
        dialog.value = ProfileDialog.MakeHod
    }

    fun onConfirmHod() {
        FacultyRepository.makeHeadOfDepartment(facultyId)
        dialog.value = null
        notice.value = ProfileNotice.HOD_CHANGED
    }

    fun onDismissDialog() {
        dialog.value = null
    }

    fun onNoticeShown() {
        notice.value = null
    }

    /** Divisions this teacher takes classes in — the ones they can be class teacher of. */
    fun teachableDivisions(d: FacultyDirectory): List<Division> {
        val f = d.faculty(facultyId) ?: return emptyList()
        val ids = f.teaching.map { it.divisionId }.toSet() + d.classTeacherOf(f.id).map { it.id }
        return d.divisions.filter { it.id in ids }.sortedWith(compareBy({ it.classTeacherId != facultyId }, { it.programme }, { -it.semester }))
    }

    companion object {
        const val ARG_FACULTY_ID = "facultyId"
    }
}
