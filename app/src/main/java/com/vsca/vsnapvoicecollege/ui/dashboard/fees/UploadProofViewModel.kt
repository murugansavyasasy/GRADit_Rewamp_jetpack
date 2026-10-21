package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ProofError { AMOUNT_MISSING, AMOUNT_TOO_HIGH, REFERENCE_MISSING, RECEIPT_MISSING }

enum class SubmitState { IDLE, SUBMITTING, DONE, FAILED }

data class UploadProofUiState(
    val studentId: String = "",
    /** Fees that still need paying and have no proof waiting. */
    val fees: List<FeeItem> = emptyList(),
    val balances: Map<String, Long> = emptyMap(),
    val feeId: String? = null,
    val amount: String = "",
    val paidOn: Long = startOfToday(),
    val method: PaymentMethod = PaymentMethod.UPI,
    val reference: String = "",
    val receipt: FileAttachment? = null,
    /** Errors appear only after the first submit attempt. */
    val showErrors: Boolean = false,
    val submit: SubmitState = SubmitState.IDLE,
) {
    val fee: FeeItem? get() = fees.firstOrNull { it.id == feeId }
    val balance: Long get() = feeId?.let { balances[it] } ?: 0L

    val errors: Set<ProofError>
        get() = buildSet {
            val value = amount.toLongOrNull() ?: 0L
            if (value <= 0) add(ProofError.AMOUNT_MISSING) else if (value > balance) add(ProofError.AMOUNT_TOO_HIGH)
            if (reference.isBlank()) add(ProofError.REFERENCE_MISSING)
            if (receipt == null) add(ProofError.RECEIPT_MISSING)
        }

    fun errorShown(error: ProofError): Boolean = showErrors && error in errors
}

/** "Upload payment proof": which fee, how much, when, how, the reference and a receipt. */
class UploadProofViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(initialState(savedStateHandle.get<String>(ARG_STUDENT_ID).orEmpty(), savedStateHandle.get<String>(ARG_FEE_ID)))
    val uiState: StateFlow<UploadProofUiState> = _uiState.asStateFlow()

    private fun initialState(studentId: String, feeId: String?): UploadProofUiState {
        val account = FeeRepository.account(studentId) ?: return UploadProofUiState(studentId = studentId)
        val fees = account.payableFees()
        val chosen = fees.firstOrNull { it.id == feeId } ?: fees.firstOrNull()
        val balances = fees.associate { it.id to account.balanceOf(it) }
        return UploadProofUiState(
            studentId = studentId,
            fees = fees,
            balances = balances,
            feeId = chosen?.id,
            amount = chosen?.let { balances[it.id] }?.toString().orEmpty(),
        )
    }

    fun onFee(feeId: String) = _uiState.update { it.copy(feeId = feeId, amount = it.balances[feeId]?.toString().orEmpty()) }

    /** Whole rupees only. */
    fun onAmount(value: String) = _uiState.update { it.copy(amount = value.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)) }

    fun onPaidOn(value: Long) = _uiState.update { it.copy(paidOn = value) }

    fun onMethod(value: PaymentMethod) = _uiState.update { it.copy(method = value) }

    fun onReference(value: String) = _uiState.update { it.copy(reference = value.take(MAX_REFERENCE_LENGTH)) }

    fun onReceipt(value: FileAttachment?) = _uiState.update { it.copy(receipt = value) }

    fun submit() {
        val state = _uiState.value
        if (state.submit == SubmitState.SUBMITTING) return
        if (state.errors.isNotEmpty() || state.feeId == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(submit = SubmitState.SUBMITTING) }
            val result = try {
                FeeRepository.submitProof(
                    studentId = state.studentId,
                    feeId = state.feeId,
                    amount = state.amount.toLong(),
                    paidOn = state.paidOn,
                    method = state.method,
                    reference = state.reference.trim(),
                    receiptName = state.receipt!!.name,
                )
                SubmitState.DONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SubmitState.FAILED
            }
            _uiState.update { it.copy(submit = result) }
        }
    }

    fun onFailureShown() = _uiState.update { it.copy(submit = SubmitState.IDLE) }

    companion object {
        const val ARG_STUDENT_ID = "studentId"
        const val ARG_FEE_ID = "feeId"
        private const val MAX_AMOUNT_DIGITS = 9
        private const val MAX_REFERENCE_LENGTH = 40
    }
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
