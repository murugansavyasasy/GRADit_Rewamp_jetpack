package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.Calendar
import java.util.UUID

/**
 * In-memory fee accounts shared by the fee details and upload-proof screens.
 *
 * TODO: replace the seeded sample data with the fees API and the college's payment gateway.
 */
object FeeRepository {

    private val _accounts = MutableStateFlow(sampleAccounts())
    val accounts: StateFlow<List<FeeAccount>> = _accounts.asStateFlow()

    private var hasLoaded = false
    private val _lastSyncedAt = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /**
     * Whose fees are shown: the parent's children, or the student themself.
     * TODO: take the linked children / the signed-in student from the session.
     */
    fun studentIdsFor(role: Role): List<String> = when (role) {
        Role.PARENT -> listOf(AARAV, DIYA)
        Role.STUDENT -> listOf(AARAV)
        Role.FACULTY, Role.MANAGEMENT -> emptyList()
    }

    /**
     * Fetches the latest accounts. Offline, the last loaded figures stay on screen; with
     * nothing loaded yet this throws so the screen can show "Couldn't load".
     * TODO: call the fees API instead of simulating latency.
     */
    suspend fun refresh() {
        delay(LOAD_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) {
            if (!hasLoaded) throw IOException("offline")
            return
        }
        hasLoaded = true
        _lastSyncedAt.value = System.currentTimeMillis()
    }

    fun account(studentId: String): FeeAccount? = _accounts.value.firstOrNull { it.studentId == studentId }

    /**
     * Records a payment proof for the accounts office to verify.
     * TODO: upload the receipt and post the proof to the fees API.
     */
    suspend fun submitProof(
        studentId: String,
        feeId: String,
        amount: Long,
        paidOn: Long,
        method: PaymentMethod,
        reference: String,
        receiptName: String,
    ) {
        delay(SUBMIT_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) throw IOException("offline")
        addPayment(studentId, FeePayment(newId(), feeId, amount, paidOn, method, reference, PaymentStatus.IN_REVIEW, receiptName))
    }

    /**
     * Pays [feeIds] in full online.
     * TODO: hand over to the college's payment gateway; this only records a successful payment.
     */
    suspend fun payOnline(studentId: String, feeIds: Set<String>) {
        delay(SUBMIT_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) throw IOException("offline")
        val account = account(studentId) ?: return
        val now = System.currentTimeMillis()
        account.fees.filter { it.id in feeIds }.forEach { fee ->
            val balance = account.balanceOf(fee)
            if (balance > 0) {
                val reference = "TXN " + newId().take(10).uppercase()
                addPayment(studentId, FeePayment(newId(), fee.id, balance, now, PaymentMethod.ONLINE, reference, PaymentStatus.VERIFIED))
            }
        }
    }

    private fun addPayment(studentId: String, payment: FeePayment) {
        _accounts.update { list -> list.map { if (it.studentId == studentId) it.copy(payments = it.payments + payment) else it } }
    }

    private fun newId() = UUID.randomUUID().toString().replace("-", "")

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L
    private const val SUBMIT_DELAY_MS = 900L
    private const val AARAV = "aarav"
    private const val DIYA = "diya"

    /** Epoch millis for 10:00 on the day [dayOffset] days from today. */
    private fun day(dayOffset: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Due dates fall at the end of the day. */
    private fun dueOn(dayOffset: Int): Long = day(dayOffset) + 14 * 60 * 60 * 1000L - 60 * 1000L

    private fun sampleAccounts(): List<FeeAccount> = listOf(
        FeeAccount(
            studentId = AARAV, name = "Aarav Sharma", firstName = "Aarav", initials = "AR",
            programme = "B.E. CSE", semester = 5, academicYear = "2026–27",
            fees = listOf(
                FeeItem("a-tuition", FeeCategory.TUITION, "2026–27", 85_000, dueOn(-75)),
                FeeItem("a-transport", FeeCategory.TRANSPORT, "2026–27", 13_000, dueOn(-75)),
                FeeItem("a-hostel", FeeCategory.HOSTEL, "Term 1", 22_000, dueOn(2)),
                FeeItem("a-exam", FeeCategory.EXAM, "Nov 2026 exams", 2_500, dueOn(2)),
            ),
            payments = listOf(
                FeePayment("p1", "a-tuition", 85_000, day(-80), PaymentMethod.ONLINE, "TXN 8812 0046 3391", PaymentStatus.VERIFIED),
                FeePayment("p2", "a-transport", 13_000, day(-78), PaymentMethod.BANK, "UTR 5530 1182 0457", PaymentStatus.VERIFIED, "neft_transport.pdf"),
                FeePayment("p3", "a-hostel", 22_000, day(-2), PaymentMethod.UPI, "UTR 4271 9930 1185", PaymentStatus.IN_REVIEW, "upi_receipt_0510.jpg"),
            ),
        ),
        FeeAccount(
            studentId = DIYA, name = "Diya Sharma", firstName = "Diya", initials = "DR",
            programme = "B.Tech IT", semester = 3, academicYear = "2026–27",
            fees = listOf(
                FeeItem("d-tuition", FeeCategory.TUITION, "2026–27", 80_000, dueOn(-75)),
                FeeItem("d-transport", FeeCategory.TRANSPORT, "Term 2", 6_500, dueOn(-1)),
                FeeItem("d-exam", FeeCategory.EXAM, "Nov 2026 exams", 2_500, dueOn(2)),
            ),
            payments = listOf(
                FeePayment("p4", "d-tuition", 80_000, day(-79), PaymentMethod.ONLINE, "TXN 7710 2290 1184", PaymentStatus.VERIFIED),
                FeePayment(
                    "p5", "d-transport", 6_500, day(-6), PaymentMethod.DD_CASH, "DD 004512", PaymentStatus.REJECTED, "dd_scan.jpg",
                    note = "Receipt image is not readable",
                ),
            ),
        ),
    )
}
