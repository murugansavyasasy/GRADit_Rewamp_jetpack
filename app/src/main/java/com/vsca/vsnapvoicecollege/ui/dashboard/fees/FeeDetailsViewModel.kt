package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FeeLoad { LOADING, READY, ERROR }

enum class FeeTab(@param:StringRes val label: Int) {
    DUE(R.string.fee_tab_due),
    STRUCTURE(R.string.fee_tab_structure),
    PAYMENTS(R.string.fee_tab_payments),
}

/** One-shot results shown as a toast. */
sealed interface FeeMessage {
    data class Paid(val amount: Long) : FeeMessage
    data object PayFailed : FeeMessage
}

data class FeeDetailsUiState(
    val load: FeeLoad = FeeLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    /** The parent's children (just the student for a student). */
    val accounts: List<FeeAccount> = emptyList(),
    val account: FeeAccount? = null,
    val tab: FeeTab = FeeTab.DUE,
    /** Fees ticked for the next online payment. */
    val selected: Set<String> = emptySet(),
    val isPaying: Boolean = false,
    val message: FeeMessage? = null,
) {
    val showChildSwitcher: Boolean get() = accounts.size > 1

    val payAmount: Long
        get() = account?.let { a -> a.fees.filter { it.id in selected }.sumOf { a.balanceOf(it) } } ?: 0L
}

/** Fee summary, dues, fee structure and payment history; parents switch between children. */
class FeeDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val studentIds = FeeRepository.studentIdsFor(SessionPreferences.getRole(application))
    private val selectedStudent = MutableStateFlow(studentIds.firstOrNull())
    private val tab = MutableStateFlow(FeeTab.DUE)

    /** Ticked fees per student; null until the student is first shown, then all payable fees. */
    private val ticked = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val load = MutableStateFlow(FeeLoad.LOADING)
    private val paying = MutableStateFlow(false)
    private val message = MutableStateFlow<FeeMessage?>(null)

    val uiState: StateFlow<FeeDetailsUiState> = combine(
        combine(FeeRepository.accounts, FeeRepository.lastSyncedAt, ::Pair),
        combine(selectedStudent, tab, ticked, ::Triple),
        combine(load, paying, message, ::Triple),
        NetworkMonitor.isOnline,
    ) { (all, synced), (studentId, tab, ticked), (load, paying, message), online ->
        val accounts = studentIds.mapNotNull { id -> all.firstOrNull { it.studentId == id } }
        val account = accounts.firstOrNull { it.studentId == studentId }
        val payable = account?.payableFees()?.map { it.id }?.toSet().orEmpty()
        FeeDetailsUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            accounts = accounts,
            account = if (load == FeeLoad.READY) account else null,
            tab = tab,
            // Default to every payable fee; drop anything paid or sent for review since.
            selected = (account?.let { ticked[it.studentId] } ?: payable).intersect(payable),
            isPaying = paying,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeeDetailsUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = FeeLoad.LOADING
            load.value = try {
                FeeRepository.refresh()
                FeeLoad.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FeeLoad.ERROR
            }
        }
    }

    fun onStudent(studentId: String) {
        selectedStudent.value = studentId
    }

    fun onTab(value: FeeTab) {
        tab.value = value
    }

    fun onToggleFee(feeId: String) {
        val state = uiState.value
        val studentId = state.account?.studentId ?: return
        val current = state.selected
        ticked.value = ticked.value + (studentId to if (feeId in current) current - feeId else current + feeId)
    }

    fun pay() {
        val state = uiState.value
        val account = state.account ?: return
        val amount = state.payAmount
        if (amount <= 0 || paying.value) return
        viewModelScope.launch {
            paying.value = true
            message.value = try {
                FeeRepository.payOnline(account.studentId, state.selected)
                FeeMessage.Paid(amount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FeeMessage.PayFailed
            } finally {
                paying.value = false
            }
        }
    }

    fun onMessageShown() {
        message.value = null
    }
}
