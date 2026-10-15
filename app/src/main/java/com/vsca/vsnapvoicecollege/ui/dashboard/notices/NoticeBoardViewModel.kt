package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NoticeLoad { LOADING, ERROR, READY }

data class NoticeBoardOptions(
    val query: String = "",
    val category: NoticeCategory? = null,
    val audience: NoticeRole? = null,
    /** Null = All; also driven by the Status filter. */
    val status: NoticeStatus? = null,
    val sort: NoticeSort = NoticeSort.NEWEST,
)

data class NoticeBoardUiState(
    val load: NoticeLoad = NoticeLoad.LOADING,
    val author: NoticeAuthor? = null,
    val options: NoticeBoardOptions = NoticeBoardOptions(),
    /** Counts per status tab (null key = All) after search and filters. */
    val counts: Map<NoticeStatus?, Int> = emptyMap(),
    val notices: List<Notice> = emptyList(),
)

/** The management / faculty notice board: search, filters, status tabs and sort. */
class NoticeBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val author = NoticeRepository.currentAuthor(SessionPreferences.getRole(application))
    private val options = MutableStateFlow(NoticeBoardOptions())
    private val load = MutableStateFlow(NoticeLoad.LOADING)

    val uiState: StateFlow<NoticeBoardUiState> =
        combine(NoticeRepository.notices, options, load) { notices, options, load -> build(notices, options, load) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoticeBoardUiState(author = author))

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = NoticeLoad.LOADING
            load.value = try {
                NoticeRepository.refresh()
                NoticeLoad.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NoticeLoad.ERROR
            }
        }
    }

    fun onQueryChange(value: String) = options.update { it.copy(query = value) }
    fun onCategory(value: NoticeCategory?) = options.update { it.copy(category = value) }
    fun onAudience(value: NoticeRole?) = options.update { it.copy(audience = value) }
    fun onStatus(value: NoticeStatus?) = options.update { it.copy(status = value) }
    fun onSort(value: NoticeSort) = options.update { it.copy(sort = value) }

    private fun build(all: List<Notice>, options: NoticeBoardOptions, load: NoticeLoad): NoticeBoardUiState {
        val now = System.currentTimeMillis()
        val needle = options.query.trim()
        val filtered = all.filter { notice ->
            (needle.isEmpty() || notice.title.contains(needle, ignoreCase = true)) &&
                (options.category == null || notice.category == options.category) &&
                (options.audience == null || options.audience in notice.roles)
        }
        val counts = buildMap<NoticeStatus?, Int> {
            put(null, filtered.size)
            NoticeStatus.entries.forEach { status -> put(status, filtered.count { it.status(now) == status }) }
        }
        val visible = if (load == NoticeLoad.READY) {
            options.sort.apply(filtered.filter { options.status == null || it.status(now) == options.status })
        } else {
            emptyList()
        }
        return NoticeBoardUiState(
            load = load,
            author = author,
            options = options,
            counts = if (load == NoticeLoad.READY) counts else counts.mapValues { 0 },
            notices = visible,
        )
    }
}
