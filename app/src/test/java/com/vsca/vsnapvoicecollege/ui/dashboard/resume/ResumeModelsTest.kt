package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeModelsTest {

    private fun aarav() = ResumeRepository.resume("aarav")!!

    @Test
    fun aaravMatchesTheDesign() {
        val resume = aarav()
        assertEquals(6, resume.completeCount)
        assertEquals(75, resume.strength)
        assertEquals(StrengthLevel.GOOD, StrengthLevel.of(resume.strength))
        assertEquals(listOf(ResumeSection.INTERNSHIPS, ResumeSection.LANGUAGES), resume.missing)
        assertEquals(ResumeTemplate.CLASSIC, resume.template)
        assertEquals(listOf("Python", "SQL", "React"), resume.entries(ResumeSection.SKILLS).take(3).map { it.title })
    }

    @Test
    fun strengthLevels() {
        assertEquals(StrengthLevel.STARTED, StrengthLevel.of(37))
        assertEquals(StrengthLevel.FAIR, StrengthLevel.of(62))
        assertEquals(StrengthLevel.GOOD, StrengthLevel.of(87))
        assertEquals(StrengthLevel.COMPLETE, StrengthLevel.of(100))
    }

    @Test
    fun emptySectionsDontPrint() {
        val printed = aarav().printedSections
        assertFalse(ResumeSection.PERSONAL in printed)
        assertFalse(ResumeSection.INTERNSHIPS in printed)
        assertEquals(ResumeSection.EDUCATION, printed.first())
    }

    @Test
    fun entryLines() {
        assertEquals("Smart attendance kiosk · Python, OpenCV · Best project, Techfest ’26", aarav().entries(ResumeSection.PROJECTS)[0].line())
        assertEquals("Java", ResumeEntry("x", "Java").line())
    }

    @Test
    fun addingAndRemovingAnEntryUpdatesStrength() {
        ResumeRepository.addEntry("aarav", ResumeSection.LANGUAGES, "  English ", "")
        val added = aarav().entries(ResumeSection.LANGUAGES).single()
        assertEquals("English", added.title)
        assertEquals(null, added.detail)
        assertEquals(87, aarav().strength)

        ResumeRepository.updateEntry("aarav", ResumeSection.LANGUAGES, added.id, "Tamil", null)
        assertEquals("Tamil", aarav().entries(ResumeSection.LANGUAGES).single().title)

        ResumeRepository.removeEntry("aarav", ResumeSection.LANGUAGES, added.id)
        assertTrue(aarav().entries(ResumeSection.LANGUAGES).isEmpty())
        assertEquals(75, aarav().strength)
    }

    @Test(expected = IllegalArgumentException::class)
    fun collegeSectionsCantBeEdited() {
        ResumeRepository.addEntry("aarav", ResumeSection.EDUCATION, "Fake degree", null)
    }
}
