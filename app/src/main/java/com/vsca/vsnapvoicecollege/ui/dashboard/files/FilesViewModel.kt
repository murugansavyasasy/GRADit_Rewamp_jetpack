package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Special folder filter value for files that aren't in any folder. */
const val UNSORTED_FOLDER_ID = "__unsorted__"

/** What the user has chosen on the list: tab, search, filters, sort and layout. */
data class FileListOptions(
    val tab: FileTypeTab = FileTypeTab.ALL,
    val query: String = "",
    /** Null = all folders; [UNSORTED_FOLDER_ID] = files without a folder. */
    val folderId: String? = null,
    val category: FileCategory? = null,
    val audience: AudienceRole? = null,
    val sort: FileSort = FileSort.NEWEST,
    val gridView: Boolean = false,
)

/**
 * Immutable UI state for a file list (the staff "Files" screen and the family view).
 */
data class FilesUiState(
    val loading: Boolean = true,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val options: FileListOptions = FileListOptions(),
    val folders: List<FileFolder> = emptyList(),
    /** All files this user can see, before tab / search / filters. */
    val totalCount: Int = 0,
    val counts: Map<FileTypeTab, Int> = emptyMap(),
    /** Files matching every option, sorted. */
    val files: List<SharedFile> = emptyList(),
)

/**
 * Backs both file lists. Staff see everything the college shared; parents and students
 * ([familyView]) see only files published to their class.
 */
abstract class BaseFilesViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val familyView: Boolean,
) : AndroidViewModel(application) {

    private val options = MutableStateFlow(
        FileListOptions(
            tab = savedStateHandle.get<String>(ARG_TAB)
                ?.let { name -> FileTypeTab.entries.firstOrNull { it.name == name } }
                ?: FileTypeTab.ALL,
        ),
    )
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<FilesUiState> = combine(
        combine(FileRepository.files, FileRepository.folders, ::Pair),
        options,
        loading,
        NetworkMonitor.isOnline,
        FileRepository.lastSyncedAt,
    ) { (files, folders), options, loading, online, syncedAt ->
        buildState(files, folders, options, loading, online, syncedAt)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FilesUiState(),
    )

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            FileRepository.refresh()
            loading.value = false
        }
    }

    fun onTabSelected(value: FileTypeTab) = options.update { it.copy(tab = value) }
    fun onQueryChange(value: String) = options.update { it.copy(query = value) }
    fun onFolderSelected(value: String?) = options.update { it.copy(folderId = value) }
    fun onCategorySelected(value: FileCategory?) = options.update { it.copy(category = value) }
    fun onAudienceSelected(value: AudienceRole?) = options.update { it.copy(audience = value) }
    fun onSortSelected(value: FileSort) = options.update { it.copy(sort = value) }
    fun onLayoutChange(grid: Boolean) = options.update { it.copy(gridView = grid) }

    private fun buildState(
        all: List<SharedFile>,
        folders: List<FileFolder>,
        options: FileListOptions,
        loading: Boolean,
        online: Boolean,
        syncedAt: Long,
    ): FilesUiState {
        val visible = if (familyView) all.filter { it.isVisibleToFamily } else all
        val needle = options.query.trim()
        val filtered = visible.filter { file ->
            (needle.isEmpty() || file.title.contains(needle, true) || file.fileName.contains(needle, true)) &&
                when (options.folderId) {
                    null -> true
                    UNSORTED_FOLDER_ID -> file.folderId == null
                    else -> file.folderId == options.folderId
                } &&
                (options.category == null || file.category == options.category) &&
                (options.audience == null || options.audience in file.roles)
        }
        return FilesUiState(
            loading = loading,
            isOnline = online,
            lastSyncedAt = syncedAt,
            options = options,
            folders = folders,
            totalCount = visible.size,
            counts = FileTypeTab.entries.associateWith { tab -> filtered.count(tab::matches) },
            files = options.sort.apply(filtered.filter(options.tab::matches)),
        )
    }

    companion object {
        const val ARG_TAB = "tab"
    }
}

/** The staff (management / faculty) file list. */
class FilesViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    BaseFilesViewModel(application, savedStateHandle, familyView = false)

/** The parent / student file list. */
class FamilyFilesViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    BaseFilesViewModel(application, savedStateHandle, familyView = true)
