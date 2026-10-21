package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeeModelsTest {

    private val aarav = FeeRepository.accounts.value.first { it.studentId == "aarav" }
    private val diya = FeeRepository.accounts.value.first { it.studentId == "diya" }

    @Test
    fun aaravSummaryMatchesTheDesign() {
        assertEquals(122_500L, aarav.total)
        assertEquals(98_000L, aarav.paid)
        assertEquals(22_000L, aarav.inReview)
        assertEquals(24_500L, aarav.amountDue)
        assertEquals(80, aarav.percentPaid)
    }

    @Test
    fun hostelIsInReviewAndOnlyTheExamFeeCanBePaid() {
        val hostel = aarav.fees.first { it.category == FeeCategory.HOSTEL }
        assertEquals(FeeState.IN_REVIEW, aarav.stateOf(hostel))
        assertEquals("UTR 4271 9930 1185", aarav.pendingProof(hostel.id)?.reference)
        assertEquals(listOf("a-exam"), aarav.payableFees().map { it.id })
        assertEquals(setOf(FeeCategory.TUITION, FeeCategory.TRANSPORT), aarav.paidFees.map { it.category }.toSet())
    }

    @Test
    fun rejectedProofLeavesTheFeeOverdueAndPayable() {
        val transport = diya.fees.first { it.category == FeeCategory.TRANSPORT }
        assertEquals(FeeState.OVERDUE, diya.stateOf(transport))
        assertNull(diya.pendingProof(transport.id))
        assertEquals("Receipt image is not readable", diya.rejectedProof(transport.id)?.note)
        assertTrue(transport in diya.payableFees())
    }

    @Test
    fun rupeesUseIndianGrouping() {
        assertEquals("₹1,22,500", rupees(122_500))
        assertEquals("₹2,500", rupees(2_500))
        assertEquals("₹0", rupees(0))
    }

    @Test
    fun proofFormValidation() {
        val base = UploadProofUiState(
            studentId = "aarav",
            fees = aarav.payableFees(),
            balances = mapOf("a-exam" to 2_500L),
            feeId = "a-exam",
        )
        assertEquals(
            setOf(ProofError.AMOUNT_MISSING, ProofError.REFERENCE_MISSING, ProofError.RECEIPT_MISSING),
            base.errors,
        )
        val tooMuch = base.copy(amount = "3000", reference = "UTR 1", receipt = FileAttachment("r.jpg", 10, AttachmentType.IMAGE))
        assertEquals(setOf(ProofError.AMOUNT_TOO_HIGH), tooMuch.errors)
        assertFalse(tooMuch.errorShown(ProofError.AMOUNT_TOO_HIGH))
        assertTrue(tooMuch.copy(showErrors = true).errorShown(ProofError.AMOUNT_TOO_HIGH))
        assertTrue(tooMuch.copy(amount = "2500").errors.isEmpty())
    }
}
