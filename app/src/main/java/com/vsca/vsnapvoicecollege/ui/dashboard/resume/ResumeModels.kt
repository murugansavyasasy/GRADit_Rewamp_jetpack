package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vsca.vsnapvoicecollege.R

/** How a section's entries read on the resume: one per line, or joined with " · ". */
enum class EntryLayout { LINES, INLINE }

enum class ResumeSection(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    /** Filled in from college records; the student can't edit it. */
    val fromCollege: Boolean,
    val layout: EntryLayout,
    /** Entries have a detail line ("2023–2027 · CGPA 8.4"). */
    val hasDetail: Boolean,
    /** Titles shown after the count on the builder row ("Python, SQL, React" vs. just the first). */
    val previewTitles: Int,
) {
    PERSONAL(R.string.resume_sec_personal, R.drawable.ic_person, fromCollege = true, EntryLayout.LINES, hasDetail = false, previewTitles = 0),
    EDUCATION(R.string.resume_sec_education, R.drawable.ic_graduation_cap, fromCollege = true, EntryLayout.LINES, hasDetail = true, previewTitles = 0),
    SKILLS(R.string.resume_sec_skills, R.drawable.ic_build, fromCollege = false, EntryLayout.INLINE, hasDetail = false, previewTitles = 3),
    PROJECTS(R.string.resume_sec_projects, R.drawable.ic_work, fromCollege = false, EntryLayout.LINES, hasDetail = true, previewTitles = 1),
    CERTIFICATIONS(R.string.resume_sec_certifications, R.drawable.ic_shield_check, fromCollege = true, EntryLayout.INLINE, hasDetail = false, previewTitles = 1),
    ACHIEVEMENTS(R.string.resume_sec_achievements, R.drawable.ic_star, fromCollege = false, EntryLayout.LINES, hasDetail = false, previewTitles = 1),
    INTERNSHIPS(R.string.resume_sec_internships, R.drawable.ic_apartment, fromCollege = false, EntryLayout.LINES, hasDetail = true, previewTitles = 1),
    LANGUAGES(R.string.resume_sec_languages, R.drawable.ic_language, fromCollege = false, EntryLayout.INLINE, hasDetail = false, previewTitles = 3),
}

enum class ResumeTemplate(@param:StringRes val label: Int, @param:StringRes val description: Int) {
    CLASSIC(R.string.resume_tpl_classic, R.string.resume_tpl_classic_desc),
    MODERN(R.string.resume_tpl_modern, R.string.resume_tpl_modern_desc),
    COMPACT(R.string.resume_tpl_compact, R.string.resume_tpl_compact_desc),
}

/** One line on the resume: "Smart attendance kiosk" · "Python, OpenCV · Best project, Techfest ’26". */
data class ResumeEntry(
    val id: String,
    val title: String,
    val detail: String? = null,
    /** Shorter form for the builder row, e.g. "B.E. CSE · CGPA 8.4". */
    val short: String? = null,
)

data class ResumeContact(
    val degree: String,
    val college: String,
    /** "UCE Chennai" — named in the auto-fill note. */
    val collegeShort: String,
    val email: String,
    val phone: String,
    val city: String,
)

data class Resume(
    val studentId: String,
    val name: String,
    val firstName: String,
    val initials: String,
    val contact: ResumeContact,
    val template: ResumeTemplate,
    /** Entries per section; [ResumeSection.PERSONAL] is [contact], not entries. */
    val entries: Map<ResumeSection, List<ResumeEntry>>,
    val updatedAt: Long,
) {
    fun entries(section: ResumeSection): List<ResumeEntry> = entries[section].orEmpty()

    /** Personal details always count; other sections once they have an entry. */
    fun isComplete(section: ResumeSection): Boolean = section == ResumeSection.PERSONAL || entries(section).isNotEmpty()

    val completeCount: Int get() = ResumeSection.entries.count(::isComplete)

    val missing: List<ResumeSection> get() = ResumeSection.entries.filterNot(::isComplete)

    val strength: Int get() = completeCount * 100 / ResumeSection.entries.size

    /** Sections that print on the resume, in order. */
    val printedSections: List<ResumeSection>
        get() = ResumeSection.entries.filter { it != ResumeSection.PERSONAL && entries(it).isNotEmpty() }
}

enum class StrengthLevel(@param:StringRes val label: Int) {
    STARTED(R.string.resume_strength_started),
    FAIR(R.string.resume_strength_fair),
    GOOD(R.string.resume_strength_good),
    COMPLETE(R.string.resume_strength_complete),
    ;

    companion object {
        fun of(percent: Int): StrengthLevel = when {
            percent >= 100 -> COMPLETE
            percent >= 75 -> GOOD
            percent >= 50 -> FAIR
            else -> STARTED
        }
    }
}

/** One entry as a resume line: "title · detail". */
fun ResumeEntry.line(): String = if (detail.isNullOrBlank()) title else "$title · $detail"
