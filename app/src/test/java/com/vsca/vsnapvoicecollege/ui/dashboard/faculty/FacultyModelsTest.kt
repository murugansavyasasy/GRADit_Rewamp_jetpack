package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacultyModelsTest {

    private val d = FacultyRepository.directory.value

    @Test
    fun wholeCollegeMatchesTheDesign() {
        val all = d.filtered(DirectoryFilter())
        assertEquals(FacultyCounts(102, 94, 6), counts(all))
        assertEquals(18, d.departments.size)
        assertEquals(6, d.freePeriods.size)
    }

    @Test
    fun streamsMatchTheStreamSheet() {
        val expected = mapOf("as" to (42 to 8), "en" to (38 to 6), "mg" to (13 to 2), "ed" to (9 to 2))
        expected.forEach { (stream, pair) ->
            assertEquals(stream, pair.first, d.filtered(DirectoryFilter(streamId = stream)).size)
            assertEquals(stream, pair.second, d.departmentsIn(stream).size)
        }
        assertEquals(FacultyCounts(42, 39, 2), counts(d.filtered(DirectoryFilter(streamId = "as"))))
    }

    @Test
    fun departmentsCountTeachersWhoAlsoTeachThere() {
        fun size(vararg depts: String) = d.filtered(DirectoryFilter(streamId = "as", departmentIds = depts.toSet())).size
        assertEquals(7, size("cs"))
        assertEquals(7, size("maths"))
        // Dr. Anitha Rajan belongs to both, so she is counted once.
        assertEquals(13, size("cs", "maths"))
        assertEquals(FacultyCounts(5, 5, 0), counts(d.filtered(DirectoryFilter(streamId = "as", departmentIds = setOf("tamil")))))
    }

    @Test
    fun headsComeFirstInEachDepartment() {
        val groups = d.byDepartment(d.filtered(DirectoryFilter(streamId = "as")), DirectoryFilter(streamId = "as"))
        assertEquals("Tamil", groups.first().department.name)
        assertEquals("Dr. Sivakumar Raju", groups.first().faculty.first().name)
        d.departments.forEach { dept -> assertEquals(dept.name, 1, d.faculty.count { it.departmentId == dept.id && it.isHod }) }
    }

    @Test
    fun computerScienceSummary() {
        assertEquals("Dr. Senthil Kumar Ganesan", d.hodOf("cs")?.name)
        val covered = d.divisionsCovered("cs").map { "${it.programme} ${it.semester}${it.section}" }
        assertEquals(listOf("B.Sc CS 1A", "B.Sc CS 1B", "B.Sc CS 3A", "B.Sc CS 3B", "B.Sc CS 5A", "B.Sc CS 5B"), covered)
        val firstDivision = d.byDivision(d.filtered(DirectoryFilter(streamId = "as", departmentIds = setOf("cs"))), DirectoryFilter(streamId = "as", departmentIds = setOf("cs"))).first()
        assertEquals("cs-1-A", firstDivision.division.id)
        assertEquals("Dr. Geetha Arumugam", firstDivision.classTeacher?.name)
    }

    @Test
    fun anithaRajanProfile() {
        val anitha = d.faculty("anitha")!!
        assertEquals(14, anitha.periodsPerWeek)
        assertEquals(listOf("cs-5-A"), d.classTeacherOf("anitha").map { it.id })
    }

    @Test
    fun searchWithNoMatchIsEmpty() {
        assertTrue(d.filtered(DirectoryFilter(streamId = "as", departmentIds = setOf("cs"), query = "Thermodynamics")).isEmpty())
        assertEquals("Dr. Anitha Rajan", d.filtered(DirectoryFilter(query = "UCE-AS-1148")).single().name)
    }

    @Test
    fun exportNameFollowsTheFilters() {
        assertEquals("Arts-and-Science_Computer-Science_Faculty", d.exportBaseName(DirectoryFilter(streamId = "as", departmentIds = setOf("cs"))))
        assertEquals("All-streams_Faculty", d.exportBaseName(DirectoryFilter()))
    }

    @Test
    fun freePeriodsMatchTheDesign() {
        val now = FacultyRepository.NOW_MINUTE
        assertEquals(3, d.freePeriods.count { it.allocatedToId == null })
        assertEquals(listOf("priya", "vignesh").sorted(), d.absentWithPeriods.map { it.id }.sorted())
        assertEquals(4, d.freePeriodsCreatedBy("vignesh").size)
        val p1 = d.freePeriods.first { it.period == 1 }
        assertEquals(PeriodTiming.ENDED, p1.timing(now))
        assertFalse(p1.canAllocate(now))
        assertEquals(PeriodTiming.IN_PROGRESS, d.freePeriods.first { it.period == 3 }.timing(now))
    }

    @Test
    fun yearAndDivisionNarrowTheList() {
        val f = DirectoryFilter(streamId = "as", departmentIds = setOf("cs"))
        assertEquals(listOf(1, 3, 5), d.semesterOptions(f))
        assertEquals(listOf("A", "B"), d.sectionOptions(f))
        val sem5 = d.filtered(f.copy(semester = 5, section = "A"))
        assertTrue(sem5.any { it.id == "anitha" })
    }

    @Test
    fun familySemesters() {
        val aarav = kotlinx.coroutines.runBlocking { FacultyRepository.loadChild("aarav") }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), aarav.semesterNumbers)
        val sem2 = aarav.semester(2)!!
        assertEquals(Triple(4, 5, 22), Triple(sem2.teacherCount, sem2.subjectCount, sem2.periodsPerWeek))
        val sem3 = aarav.semester(3)!!
        assertEquals(Triple(5, 6, 25), Triple(sem3.teacherCount, sem3.subjectCount, sem3.periodsPerWeek))
        val sem5 = aarav.semester(5)!!
        assertEquals(Triple(7, 9, 39), Triple(sem5.teacherCount, sem5.subjectCount, sem5.periodsPerWeek))
        assertEquals(SemesterStatus.UPCOMING, aarav.status(6))
        assertTrue(aarav.semester(6)!!.loads.isEmpty())
        assertEquals(listOf(4), aarav.alsoTaught("vignesh", except = 5))
        assertTrue(aarav.alsoTaught("meena", except = 5).isEmpty())
        assertEquals(2026, sem5.academicYearStart)
        assertEquals(2025, aarav.semester(4)!!.academicYearStart)

        val diya = kotlinx.coroutines.runBlocking { FacultyRepository.loadChild("diya") }
        val d3 = diya.semester(3)!!
        assertEquals(Triple(5, 6, 38), Triple(d3.teacherCount, d3.subjectCount, d3.periodsPerWeek))
        assertNull(diya.semester(4)!!.classTeacherId)
    }

    @Test
    fun tabsSplitTheoryAndLabs() {
        val sem5 = kotlinx.coroutines.runBlocking { FacultyRepository.loadChild("aarav") }.semester(5)!!
        assertEquals(5, sem5.loadsFor(TeacherTab.THEORY).size)
        assertEquals(listOf("meena", "anbu", "deepa"), sem5.loadsFor(TeacherTab.LABS).map { it.teacherId })
        val meena = sem5.loads.first { it.teacherId == "meena" }
        assertEquals(listOf("DBMS Lab", "Mentoring hour"), sem5.subjectsFor(meena, TeacherTab.LABS).map { it.name })
        assertEquals(
            listOf(java.util.Calendar.MONDAY to listOf(5, 6), java.util.Calendar.THURSDAY to listOf(5, 6)),
            groupSlotsByDay(meena.subjects[1].slots),
        )
    }

    @Test
    fun principalActions() {
        val before = FacultyRepository.directory.value
        FacultyRepository.assignClassTeacher("anitha", "cs-3-B")
        assertEquals("anitha", FacultyRepository.directory.value.division("cs-3-B")?.classTeacherId)
        FacultyRepository.makeHeadOfDepartment("anitha")
        val after = FacultyRepository.directory.value
        assertEquals("anitha", after.hodOf("cs")?.id)
        assertEquals("Professor", after.faculty("senthil")?.designation)
        assertEquals("Professor and Head", after.faculty("anitha")?.designation)
        assertFalse(after.faculty("senthil")!!.isHod)
        FacultyRepository.restore(before)
    }
}
