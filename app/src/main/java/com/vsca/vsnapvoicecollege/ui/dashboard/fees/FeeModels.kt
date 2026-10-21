package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground

/** Fee head; picks the icon tile on the cards. */
enum class FeeCategory(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val background: Color,
    val content: Color,
) {
    TUITION(R.string.fee_cat_tuition, R.drawable.ic_menu_book, SelectedRowBackground, BrandBlue),
    HOSTEL(R.string.fee_cat_hostel, R.drawable.ic_home, AmberChipBackground, OnboardingAmberCard),
    TRANSPORT(R.string.fee_cat_transport, R.drawable.ic_bus, OnboardingGreenDisc, BrandGreen),
    EXAM(R.string.fee_cat_exam, R.drawable.ic_assignment, SelectedRowBackground, BrandBlue),
}

enum class PaymentMethod(@param:StringRes val label: Int) {
    UPI(R.string.fee_method_upi),
    BANK(R.string.fee_method_bank),
    DD_CASH(R.string.fee_method_dd_cash),

    /** Paid in the app; only the college's gateway records these. */
    ONLINE(R.string.fee_method_online),
}

enum class PaymentStatus { VERIFIED, IN_REVIEW, REJECTED }

/** Where a fee stands, from the payments made against it. */
enum class FeeState { PAID, IN_REVIEW, DUE, OVERDUE }

data class FeeItem(
    val id: String,
    val category: FeeCategory,
    /** "Term 1", "Nov 2026 exams". */
    val period: String,
    val amount: Long,
    val dueAt: Long,
)

data class FeePayment(
    val id: String,
    val feeId: String,
    val amount: Long,
    val paidOn: Long,
    val method: PaymentMethod,
    /** UTR / transaction / DD number. */
    val reference: String,
    val status: PaymentStatus,
    val receiptName: String? = null,
    /** Why the accounts office sent a proof back. */
    val note: String? = null,
)

/** One student's fees for the academic year. */
data class FeeAccount(
    val studentId: String,
    val name: String,
    val firstName: String,
    val initials: String,
    val programme: String,
    val semester: Int,
    val academicYear: String,
    val fees: List<FeeItem>,
    val payments: List<FeePayment>,
) {
    val total: Long get() = fees.sumOf { it.amount }

    /** Verified payments only. */
    val paid: Long get() = payments.filter { it.status == PaymentStatus.VERIFIED }.sumOf { it.amount }

    val inReview: Long get() = payments.filter { it.status == PaymentStatus.IN_REVIEW }.sumOf { it.amount }

    /** Everything not yet verified, including proofs still in review. */
    val amountDue: Long get() = (total - paid).coerceAtLeast(0)

    val percentPaid: Int get() = if (total == 0L) 100 else (paid * 100 / total).toInt()

    fun paidFor(feeId: String): Long =
        payments.filter { it.feeId == feeId && it.status == PaymentStatus.VERIFIED }.sumOf { it.amount }

    fun balanceOf(fee: FeeItem): Long = (fee.amount - paidFor(fee.id)).coerceAtLeast(0)

    /** The proof waiting on the accounts office for [feeId], if any. */
    fun pendingProof(feeId: String): FeePayment? =
        payments.lastOrNull { it.feeId == feeId && it.status == PaymentStatus.IN_REVIEW }

    /** The latest proof sent back for [feeId], when nothing newer is in review. */
    fun rejectedProof(feeId: String): FeePayment? =
        if (pendingProof(feeId) != null) null else payments.lastOrNull { it.feeId == feeId && it.status == PaymentStatus.REJECTED }

    fun stateOf(fee: FeeItem, now: Long = System.currentTimeMillis()): FeeState = when {
        balanceOf(fee) == 0L -> FeeState.PAID
        pendingProof(fee.id) != null -> FeeState.IN_REVIEW
        fee.dueAt < now -> FeeState.OVERDUE
        else -> FeeState.DUE
    }

    val unpaidFees: List<FeeItem> get() = fees.filter { balanceOf(it) > 0 }.sortedBy { it.dueAt }

    val paidFees: List<FeeItem> get() = fees.filter { balanceOf(it) == 0L }

    /** Fees that can still be paid now: not settled and no proof waiting. */
    fun payableFees(now: Long = System.currentTimeMillis()): List<FeeItem> =
        unpaidFees.filter { stateOf(it, now) != FeeState.IN_REVIEW }

    /** Earliest due date among unpaid fees. */
    val nextDueAt: Long? get() = unpaidFees.minOfOrNull { it.dueAt }
}

/**
 * "₹1,22,500": Indian grouping (last three digits, then pairs), whatever the app language.
 * Done by hand because java.text.NumberFormat supports only one grouping size.
 */
fun rupees(amount: Long): String {
    val digits = kotlin.math.abs(amount).toString()
    val grouped = if (digits.length <= 3) {
        digits
    } else {
        val head = digits.dropLast(3)
        head.reversed().chunked(2).joinToString(",").reversed() + "," + digits.takeLast(3)
    }
    return (if (amount < 0) "-₹" else "₹") + grouped
}
