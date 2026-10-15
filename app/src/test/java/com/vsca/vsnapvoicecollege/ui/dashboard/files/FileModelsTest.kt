package com.vsca.vsnapvoicecollege.ui.dashboard.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileModelsTest {

    private val files = FileRepository.files.value

    @Test
    fun staffSeeEveryFile_withTheDesignedTypeCounts() {
        assertEquals(13, files.size)
        assertEquals(2, files.count(FileTypeTab.IMAGES::matches))
        assertEquals(6, files.count(FileTypeTab.PDFS::matches))
        assertEquals(3, files.count(FileTypeTab.DOCUMENTS::matches))
        assertEquals(2, files.count(FileTypeTab.VIDEOS::matches))
    }

    @Test
    fun familiesSeeOnlyPublishedFilesForTheirClass() {
        val family = files.filter { it.isVisibleToFamily }
        assertEquals(10, family.size)
        assertEquals(2, family.count(FileTypeTab.IMAGES::matches))
        assertEquals(4, family.count(FileTypeTab.PDFS::matches))
        assertEquals(3, family.count(FileTypeTab.DOCUMENTS::matches))
        assertEquals(1, family.count(FileTypeTab.VIDEOS::matches))
        // Scheduled, staff-only and other-semester files stay hidden.
        assertFalse(family.any { it.id in setOf("hostel-rules", "staff-minutes", "lab-safety") })
    }

    @Test
    fun foldersHoldTheDesignedNumberOfFiles() {
        val counts = files.groupingBy { it.folderId }.eachCount()
        assertEquals(2, counts["exams"])
        assertEquals(4, counts["events"])
        assertEquals(2, counts["forms"])
        assertEquals(1, counts["staff"])
    }

    @Test
    fun audienceCountAddsTheChosenRoles() {
        val group = AudienceGroup("g", "B.E. CSE · Sem 5 · Division A", students = 60, parents = 57, staff = 6, includesMyClass = true)
        assertEquals(117, group.count(setOf(AudienceRole.STUDENTS, AudienceRole.PARENTS)))
        assertEquals(123, group.count(AudienceRole.entries.toSet()))
        assertEquals(0, group.count(emptySet()))
    }

    @Test
    fun sortOrders() {
        val newest = FileSort.NEWEST.apply(files)
        assertTrue(newest.zipWithNext().all { (a, b) -> (a.scheduledAt ?: a.publishedAt) >= (b.scheduledAt ?: b.publishedAt) })
        val largest = FileSort.LARGEST.apply(files)
        assertEquals("principal-address", largest.first().id)
        val byName = FileSort.NAME.apply(files).map { it.title.lowercase() }
        assertEquals(byName.sorted(), byName)
    }

    @Test
    fun formatsAreRecognisedByExtension_withTheirSizeLimits() {
        assertEquals(FileFormat.JPG, FileFormat.fromFileName("Seating_plan.JPG"))
        assertEquals(FileFormat.PPTX, FileFormat.fromFileName("orientation.pptx"))
        assertEquals(FileFormat.MOV, FileFormat.fromFileName("Convocation_full.mov"))
        assertNull(FileFormat.fromFileName("notes.heic"))
        assertEquals("JPG", FileFormat.JPEG.label)
        assertTrue(412L * 1024 * 1024 > FileKind.VIDEO.maxBytes)
        assertTrue(9L * 1024 * 1024 < FileKind.IMAGE.maxBytes)
    }

    @Test
    fun titleIsSuggestedFromTheFileName() {
        assertEquals("Exam rules IA2", UploadViewModel.titleFromFileName("Exam_rules_IA2.docx"))
        assertEquals("IA2 timetable Sem5A", UploadViewModel.titleFromFileName("IA2_timetable__Sem5A.pdf"))
    }

    @Test
    fun deletingAFolderMovesItsFilesToUnsorted() {
        val folder = FileRepository.createFolder("Temp test folder")
        FileRepository.moveFile("sports-day", folder.id)
        FileRepository.deleteFolder(folder.id)
        assertNull(FileRepository.fileById("sports-day")?.folderId)
        assertFalse(FileRepository.folders.value.any { it.id == folder.id })
        FileRepository.moveFile("sports-day", "events")
    }

    @Test
    fun durationLabels() {
        assertEquals("04:32", durationLabel((4 * 60 + 32) * 1000L))
        assertEquals("1:02:10", durationLabel((3600 + 2 * 60 + 10) * 1000L))
    }
}
