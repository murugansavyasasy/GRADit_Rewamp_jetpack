package com.vsca.vsnapvoicecollege.ui.dashboard.credits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditModelsTest {

    private val aarav = CreditRepository.children[0]
    private val diya = CreditRepository.children[1]

    @Test
    fun aaravSummaryMatchesTheDesign() {
        assertEquals(165, aarav.totalCredits)
        assertEquals(80, aarav.obtainedCredits)
        assertEquals(85, aarav.toBeObtained)
        assertEquals(48, aarav.percent)
        assertEquals(4, aarav.completedSemesters)
        assertEquals(22, aarav.creditsThisSemester)
    }

    @Test
    fun aaravArrearsArePendingInSemesterThree() {
        val pending = aarav.pendingSemesters.single()
        assertEquals(3, pending.semester)
        assertEquals(8, pending.credits)
        assertEquals(listOf("MA3354", "CS3351"), pending.courses.map { it.code })
        assertTrue(pending.courses.all { it.arrearExam == "Nov 2026" })
        assertEquals(SemesterState.PENDING, aarav.semesterState(3))
        assertEquals(Totals(15, 23), aarav.semesterTotals(3))
    }

    @Test
    fun categoryTotalsMatchTheDesign() {
        assertEquals(Totals(8, 12), aarav.categoryTotals(CreditCategory.HS))
        assertEquals(67, aarav.categoryTotals(CreditCategory.HS).percent)
        assertEquals(Totals(21, 25), aarav.categoryTotals(CreditCategory.BS))
        assertEquals(84, aarav.categoryTotals(CreditCategory.BS).percent)
        assertEquals(Totals(19, 23), aarav.categoryTotals(CreditCategory.ES))
        assertEquals(83, aarav.categoryTotals(CreditCategory.ES).percent)
    }

    @Test
    fun semesterStatesFollowTheCurrentSemester() {
        assertEquals(SemesterState.COMPLETED, aarav.semesterState(1))
        assertEquals(SemesterState.COMPLETED, aarav.semesterState(4))
        assertEquals(SemesterState.IN_PROGRESS, aarav.semesterState(5))
        assertEquals(SemesterState.UPCOMING, aarav.semesterState(8))
        assertEquals(Totals(22, 22), aarav.semesterTotals(1))
    }

    @Test
    fun diyaSummaryMatchesTheDesign() {
        assertEquals(165, diya.totalCredits)
        assertEquals(45, diya.obtainedCredits)
        assertEquals(120, diya.toBeObtained)
        assertEquals(27, diya.percent)
        assertEquals(2, diya.completedSemesters)
        assertEquals(22, diya.creditsThisSemester)
        assertEquals(50, diya.categoryTotals(CreditCategory.HS).percent)
        assertTrue(diya.pendingSemesters.isEmpty())
    }

    @Test
    fun percentRoundsToNearest() {
        assertEquals(67, roundedPercent(8, 12))
        assertEquals(0, roundedPercent(0, 0))
        assertEquals(100, roundedPercent(22, 22))
    }

    @Test
    fun categoryFilterNarrowsBothViews() {
        val all = SemCreditsUiState(record = aarav)
        assertEquals(CreditCategory.entries.toList(), all.categories)
        assertEquals((1..8).toList(), all.semesters)

        val es = all.copy(category = CreditCategory.ES)
        assertEquals(listOf(CreditCategory.ES), es.categories)
        // ES courses sit in semesters 1–4 only.
        assertEquals(listOf(1, 2, 3, 4), es.copy(view = CreditView.BY_SEMESTER).semesters)
    }

    @Test
    fun expandAllCoversOnlyTheCurrentView() {
        val state = SemCreditsUiState(record = aarav, view = CreditView.BY_SEMESTER)
        assertFalse(state.allExpanded)
        val expanded = state.copy(expanded = (1..8).map(::semesterKey).toSet())
        assertTrue(expanded.allExpanded)
        assertFalse(expanded.copy(view = CreditView.BY_CATEGORY).allExpanded)
    }
}
