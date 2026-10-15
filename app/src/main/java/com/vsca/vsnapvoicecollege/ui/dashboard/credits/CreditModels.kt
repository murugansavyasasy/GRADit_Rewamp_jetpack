package com.vsca.vsnapvoicecollege.ui.dashboard.credits

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary

/** Curriculum categories (AICTE / Anna University regulation 2021). */
enum class CreditCategory(
    val code: String,
    @param:StringRes val label: Int,
    val background: Color,
    val content: Color,
) {
    HS("HS", R.string.credit_cat_hs, BadgeNeutralBackground, TextPrimary),
    BS("BS", R.string.credit_cat_bs, OnboardingGreenDisc, BrandGreen),
    ES("ES", R.string.credit_cat_es, SelectedRowBackground, BrandBlue),
    PC("PC", R.string.credit_cat_pc, Color(0xFFEDE9FE), Color(0xFF5B34B8)),
    PE("PE", R.string.credit_cat_pe, AmberChipBackground, OnboardingAmberCard),
    OE("OE", R.string.credit_cat_oe, Color(0xFFE0F2F1), Color(0xFF0F6E66)),
    EEC("EEC", R.string.credit_cat_eec, Color(0xFFFDE8EF), Color(0xFFA3204F)),
}

/** Where a course stands for the student. */
enum class CourseStatus {
    /** Cleared — its credits are obtained. */
    PASSED,

    /** Taken but not cleared; credits are pending until the arrear exam is passed. */
    ARREAR,

    /** Being studied this semester. */
    ONGOING,

    /** In a semester that hasn't started. */
    UPCOMING,
    ;

    val isObtained: Boolean get() = this == PASSED
}

data class Course(
    val code: String,
    val name: String,
    val semester: Int,
    val category: CreditCategory,
    val credits: Int,
    val status: CourseStatus,
    /** For arrears: when the next attempt is, e.g. "Nov 2026". */
    val arrearExam: String? = null,
)

/** One student's credit record; every total on the screen is derived from [courses]. */
data class CreditRecord(
    val studentId: String,
    val name: String,
    val initials: String,
    val programme: String,
    val currentSemester: Int,
    val section: String,
    val regulation: String,
    val totalSemesters: Int,
    val courses: List<Course>,
    /** "Nov 2024" per finished semester, when its results came out. */
    val resultMonths: Map<Int, String>,
) {
    val totalCredits: Int get() = courses.sumOf { it.credits }
    val obtainedCredits: Int get() = courses.filter { it.status.isObtained }.sumOf { it.credits }
    val toBeObtained: Int get() = totalCredits - obtainedCredits
    val percent: Int get() = roundedPercent(obtainedCredits, totalCredits)

    /** Semesters whose exams are over (arrears still count the semester as completed). */
    val completedSemesters: Int get() = currentSemester - 1

    val creditsThisSemester: Int get() = courses.filter { it.semester == currentSemester }.sumOf { it.credits }

    /** Finished semesters that still have uncleared courses, oldest first. */
    val pendingSemesters: List<PendingSemester>
        get() = courses.filter { it.status == CourseStatus.ARREAR }
            .groupBy { it.semester }
            .toSortedMap()
            .map { (semester, arrears) -> PendingSemester(semester, arrears.sumOf { it.credits }, arrears) }

    fun categoryTotals(category: CreditCategory): Totals = totals(courses.filter { it.category == category })

    fun semesterTotals(semester: Int, category: CreditCategory? = null): Totals =
        totals(courses.filter { it.semester == semester && (category == null || it.category == category) })

    fun semesterState(semester: Int): SemesterState = when {
        semester > currentSemester -> SemesterState.UPCOMING
        semester == currentSemester -> SemesterState.IN_PROGRESS
        courses.any { it.semester == semester && it.status == CourseStatus.ARREAR } -> SemesterState.PENDING
        else -> SemesterState.COMPLETED
    }

    private fun totals(subset: List<Course>) = Totals(
        obtained = subset.filter { it.status.isObtained }.sumOf { it.credits },
        total = subset.sumOf { it.credits },
    )
}

data class PendingSemester(val semester: Int, val credits: Int, val courses: List<Course>)

data class Totals(val obtained: Int, val total: Int) {
    val remaining: Int get() = total - obtained
    val percent: Int get() = roundedPercent(obtained, total)
    val fraction: Float get() = if (total == 0) 0f else obtained.toFloat() / total
}

enum class SemesterState { COMPLETED, PENDING, IN_PROGRESS, UPCOMING }

enum class CreditView { BY_CATEGORY, BY_SEMESTER }

/** Whole-number percentage, rounded to nearest (8 of 12 → 67%). */
fun roundedPercent(part: Int, whole: Int): Int = if (whole == 0) 0 else Math.round(part * 100f / whole)
