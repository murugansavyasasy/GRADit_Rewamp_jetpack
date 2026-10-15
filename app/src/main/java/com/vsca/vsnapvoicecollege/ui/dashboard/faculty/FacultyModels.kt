package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import java.util.Calendar

// ---- Staff directory (management & teachers) ----

data class Stream(val id: String, val name: String)

data class Department(val id: String, val name: String, val streamId: String)

/** One class section, e.g. B.Sc CS · Sem 1 · Division A. */
data class Division(
    val id: String,
    val programme: String,
    val semester: Int,
    val section: String,
    val departmentId: String,
    val classTeacherId: String?,
)

enum class PresenceStatus {
    PRESENT,

    /** Not punched in by the cut-off; counts as absent. */
    ABSENT,
    ON_LEAVE,

    /** Before the cut-off with no punch yet. */
    NOT_IN,
    ;

    val isAway: Boolean get() = this == ABSENT || this == ON_LEAVE
}

data class Presence(
    val status: PresenceStatus,
    /** Minute of the day the punch was recorded, e.g. 8 * 60 + 56. */
    val punchMinute: Int? = null,
    /** "fingerprint, Main block". */
    val punchMethod: String? = null,
    /** Leave reason shown on the profile, e.g. "sick leave". */
    val leaveReason: String? = null,
)

data class Teaching(val subject: String, val divisionId: String, val periodsPerWeek: Int)

data class Faculty(
    val id: String,
    val name: String,
    val initials: String,
    val designation: String,
    val departmentId: String,
    val employeeId: String,
    val qualification: String,
    val experienceYears: Int,
    val phone: String,
    /** Subjects / research areas, e.g. "Tamil Literature, Sangam Poetry". */
    val specialisation: String,
    val isHod: Boolean = false,
    /** Departments the teacher also teaches for ("Computer Science · also Mathematics"). */
    val alsoDepartmentIds: List<String> = emptyList(),
    val teaching: List<Teaching> = emptyList(),
    val presence: Presence = Presence(PresenceStatus.PRESENT, 9 * 60),
) {
    val periodsPerWeek: Int get() = teaching.sumOf { it.periodsPerWeek }

    fun inDepartment(id: String): Boolean = departmentId == id || id in alsoDepartmentIds
}

/** College period timings; P3 starts after the morning break. */
enum class PeriodSlot(val number: Int, val startMinute: Int, val endMinute: Int) {
    P1(1, 9 * 60, 9 * 60 + 50),
    P2(2, 9 * 60 + 50, 10 * 60 + 40),
    P3(3, 10 * 60 + 55, 11 * 60 + 45),
    P4(4, 11 * 60 + 45, 12 * 60 + 35),
    P5(5, 13 * 60 + 25, 14 * 60 + 15),
    P6(6, 14 * 60 + 15, 15 * 60 + 5),
    P7(7, 15 * 60 + 5, 15 * 60 + 55),
    ;

    companion object {
        fun of(number: Int): PeriodSlot = entries.first { it.number == number }
    }
}

enum class PeriodTiming { ENDED, IN_PROGRESS, UPCOMING }

/** A period left without its teacher today, and who (if anyone) is covering it. */
data class FreePeriod(
    val id: String,
    val period: Int,
    val subject: String,
    val divisionId: String,
    val room: String,
    val absentFacultyId: String,
    val allocatedToId: String? = null,
) {
    val slot: PeriodSlot get() = PeriodSlot.of(period)

    fun timing(nowMinute: Int): PeriodTiming = when {
        nowMinute >= slot.endMinute -> PeriodTiming.ENDED
        nowMinute >= slot.startMinute -> PeriodTiming.IN_PROGRESS
        else -> PeriodTiming.UPCOMING
    }

    /** Ended periods are history; anything else can still be (re)allocated. */
    fun canAllocate(nowMinute: Int): Boolean = timing(nowMinute) != PeriodTiming.ENDED
}

/** Everything the staff screens work from. */
data class FacultyDirectory(
    val institution: String,
    val streams: List<Stream>,
    val departments: List<Department>,
    val divisions: List<Division>,
    val faculty: List<Faculty>,
    val freePeriods: List<FreePeriod>,
) {
    fun stream(id: String?): Stream? = streams.firstOrNull { it.id == id }
    fun department(id: String?): Department? = departments.firstOrNull { it.id == id }
    fun division(id: String?): Division? = divisions.firstOrNull { it.id == id }
    fun faculty(id: String?): Faculty? = faculty.firstOrNull { it.id == id }

    fun departmentsIn(streamId: String?): List<Department> =
        if (streamId == null) departments else departments.filter { it.streamId == streamId }

    fun facultyIn(department: Department): List<Faculty> = faculty.filter { it.inDepartment(department.id) }

    fun hodOf(departmentId: String): Faculty? = faculty.firstOrNull { it.departmentId == departmentId && it.isHod }

    /** Divisions whose class teacher is [facultyId]. */
    fun classTeacherOf(facultyId: String): List<Division> = divisions.filter { it.classTeacherId == facultyId }

    /** Divisions taught by anyone in [departmentId], in curriculum order. */
    fun divisionsCovered(departmentId: String): List<Division> {
        val ids = facultyIn(department(departmentId) ?: return emptyList()).flatMap { f -> f.teaching.map { it.divisionId } }.toSet()
        return divisions.filter { it.id in ids && it.departmentId == departmentId }.sortedWith(compareBy({ it.programme }, { it.semester }, { it.section }))
    }

    fun freePeriodsCreatedBy(facultyId: String): List<FreePeriod> =
        freePeriods.filter { it.absentFacultyId == facultyId }.sortedBy { it.period }

    /** Staff whose absence left periods uncovered today, in the order they appear. */
    val absentWithPeriods: List<Faculty>
        get() = freePeriods.map { it.absentFacultyId }.distinct().mapNotNull(::faculty)
}

/** The directory filters, applied in order: stream → departments → year/sem & division → search. */
data class DirectoryFilter(
    val streamId: String? = null,
    val departmentIds: Set<String> = emptySet(),
    val semester: Int? = null,
    val section: String? = null,
    val query: String = "",
) {
    val isActive: Boolean get() = streamId != null || departmentIds.isNotEmpty() || semester != null || section != null || query.isNotBlank()
}

enum class DirectoryView { BY_DEPARTMENT, BY_DIVISION, ALL }

data class DepartmentGroup(val department: Department, val faculty: List<Faculty>)

data class DivisionGroup(val division: Division, val classTeacher: Faculty?, val faculty: List<Faculty>)

data class FacultyCounts(val total: Int, val present: Int, val away: Int)

/** Departments the filter covers before search and year/division narrow it. */
fun FacultyDirectory.departmentsInScope(filter: DirectoryFilter): List<Department> =
    departmentsIn(filter.streamId).filter { filter.departmentIds.isEmpty() || it.id in filter.departmentIds }

/** Divisions matching the stream / department / year / division choices. */
fun FacultyDirectory.divisionsInScope(filter: DirectoryFilter): List<Division> {
    val depts = departmentsInScope(filter).map { it.id }.toSet()
    return divisions.filter {
        it.departmentId in depts &&
            (filter.semester == null || it.semester == filter.semester) &&
            (filter.section == null || it.section == filter.section)
    }
}

/** Year / semester options for the current stream and departments. */
fun FacultyDirectory.semesterOptions(filter: DirectoryFilter): List<Int> =
    divisionsInScope(filter.copy(semester = null, section = null)).map { it.semester }.distinct().sorted()

fun FacultyDirectory.sectionOptions(filter: DirectoryFilter): List<String> =
    divisionsInScope(filter.copy(section = null)).map { it.section }.distinct().sorted()

private fun Faculty.matches(query: String, directory: FacultyDirectory): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return name.contains(q, ignoreCase = true) ||
        employeeId.contains(q, ignoreCase = true) ||
        specialisation.contains(q, ignoreCase = true) ||
        designation.contains(q, ignoreCase = true) ||
        directory.department(departmentId)?.name?.contains(q, ignoreCase = true) == true ||
        teaching.any { it.subject.contains(q, ignoreCase = true) }
}

/** Faculty matching every filter, in department order with heads first. */
fun FacultyDirectory.filtered(filter: DirectoryFilter): List<Faculty> {
    val depts = departmentsInScope(filter)
    val deptIds = depts.map { it.id }.toSet()
    val narrowByClass = filter.semester != null || filter.section != null
    val classIds = if (narrowByClass) divisionsInScope(filter).map { it.id }.toSet() else emptySet()
    val order = depts.withIndex().associate { (i, d) -> d.id to i }
    return faculty
        .filter { f ->
            deptIds.any(f::inDepartment) &&
                (!narrowByClass || f.teaching.any { it.divisionId in classIds } || classTeacherOf(f.id).any { it.id in classIds }) &&
                f.matches(filter.query, this)
        }
        .sortedWith(compareBy<Faculty>({ order[it.departmentId] ?: Int.MAX_VALUE }, { !it.isHod }, { classTeacherOf(it.id).isEmpty() }))
}

fun counts(faculty: List<Faculty>) = FacultyCounts(
    total = faculty.size,
    present = faculty.count { it.presence.status == PresenceStatus.PRESENT },
    away = faculty.count { it.presence.status.isAway },
)

fun FacultyDirectory.byDepartment(faculty: List<Faculty>, filter: DirectoryFilter): List<DepartmentGroup> =
    departmentsInScope(filter).mapNotNull { dept ->
        faculty.filter { it.inDepartment(dept.id) }.takeIf { it.isNotEmpty() }?.let { DepartmentGroup(dept, it) }
    }

fun FacultyDirectory.byDivision(faculty: List<Faculty>, filter: DirectoryFilter): List<DivisionGroup> {
    val ids = faculty.map { it.id }.toSet()
    return divisionsInScope(filter)
        .sortedWith(compareBy({ it.programme }, { it.semester }, { it.section }))
        .mapNotNull { division ->
            val teachers = this.faculty.filter { f -> f.id in ids && (f.teaching.any { it.divisionId == division.id } || division.classTeacherId == f.id) }
                .sortedBy { it.id != division.classTeacherId }
            if (teachers.isEmpty()) null else DivisionGroup(division, faculty(division.classTeacherId), teachers)
        }
}

/** "Arts-and-Science_Computer-Science_Faculty" — used for the export file name. */
fun FacultyDirectory.exportBaseName(filter: DirectoryFilter): String {
    fun slug(text: String) = text.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
    val parts = buildList {
        stream(filter.streamId)?.let { add(slug(it.name)) }
        filter.departmentIds.mapNotNull(::department).sortedBy { it.name }.forEach { add(slug(it.name)) }
        filter.semester?.let { add("Sem-$it") }
        filter.section?.let { add("Div-$it") }
        if (isEmpty()) add("All-streams")
        add("Faculty")
    }
    return parts.joinToString("_")
}

// ---- Family view (parents & students) ----

data class Child(
    val id: String,
    val name: String,
    /** First name, for "Aarav's teachers by semester". */
    val firstName: String,
    val initials: String,
    val programme: String,
    val currentSemester: Int,
    val section: String,
)

enum class SubjectKind { THEORY, LAB, ACTIVITY }

/** A teaching slot: [day] is a [Calendar] day-of-week constant, [period] is 1–7. */
data class Slot(val day: Int, val period: Int)

data class SubjectLoad(
    val name: String,
    val kind: SubjectKind,
    val periodsPerWeek: Int,
    val slots: List<Slot> = emptyList(),
)

/** Weekly hour when parents can meet the teacher. */
data class ParentHours(val day: Int, val startMinute: Int, val endMinute: Int)

data class Teacher(
    val id: String,
    val name: String,
    val initials: String,
    val designation: String,
    val department: String,
    val qualification: String,
    val experienceYears: Int,
    val cabin: String,
    val parentHours: ParentHours,
)

/** A period the teacher misses today, and who covers it (null → free period / self-study). */
data class AwayPeriod(val period: Int, val subject: String, val substitute: String? = null)

data class TeacherLoad(
    val teacherId: String,
    val subjects: List<SubjectLoad>,
    /** Periods this teacher misses today, when away. */
    val awayToday: List<AwayPeriod> = emptyList(),
) {
    val periodsPerWeek: Int get() = subjects.sumOf { it.periodsPerWeek }
    val isAwayToday: Boolean get() = awayToday.isNotEmpty()
}

enum class TeacherTab { ALL, THEORY, LABS }

enum class SemesterStatus { COMPLETED, CURRENT, UPCOMING }

/** A month as year * 12 + (month − 1), so ranges are easy to format and compare. */
data class MonthRange(val start: Int, val end: Int) {
    val startYear: Int get() = start / 12
    val endYear: Int get() = end / 12

    companion object {
        fun of(startYear: Int, startMonth: Int, endYear: Int, endMonth: Int) =
            MonthRange(startYear * 12 + startMonth - 1, endYear * 12 + endMonth - 1)
    }
}

data class SemesterClass(
    val semester: Int,
    val section: String,
    val months: MonthRange,
    val classTeacherId: String?,
    /** Empty until the semester's timetable is published. */
    val loads: List<TeacherLoad>,
) {
    val teacherCount: Int get() = loads.size
    val subjectCount: Int get() = loads.sumOf { it.subjects.size }
    val periodsPerWeek: Int get() = loads.sumOf { it.periodsPerWeek }

    /** Odd semesters start in July, so the academic year begins with their start year. */
    val isOdd: Boolean get() = semester % 2 == 1
    val academicYearStart: Int get() = if (isOdd) months.startYear else months.startYear - (if (months.start % 12 >= 6) 0 else 1)

    fun loadsFor(tab: TeacherTab): List<TeacherLoad> = when (tab) {
        TeacherTab.ALL -> loads
        TeacherTab.THEORY -> loads.filter { l -> l.subjects.any { it.kind == SubjectKind.THEORY } }
        TeacherTab.LABS -> loads.filter { l -> l.subjects.any { it.kind != SubjectKind.THEORY } }
    }

    /** The subjects shown on a card for [tab]: a teacher with a theory and a lab lists only the matching one. */
    fun subjectsFor(load: TeacherLoad, tab: TeacherTab): List<SubjectLoad> = when (tab) {
        TeacherTab.ALL -> load.subjects
        TeacherTab.THEORY -> load.subjects.filter { it.kind == SubjectKind.THEORY }
        TeacherTab.LABS -> load.subjects.filter { it.kind != SubjectKind.THEORY }
    }
}

data class ChildFaculty(val child: Child, val semesters: List<SemesterClass>, val teachers: List<Teacher>) {
    fun teacher(id: String?): Teacher? = teachers.firstOrNull { it.id == id }
    fun semester(number: Int): SemesterClass? = semesters.firstOrNull { it.semester == number }

    fun status(semester: Int): SemesterStatus = when {
        semester < child.currentSemester -> SemesterStatus.COMPLETED
        semester == child.currentSemester -> SemesterStatus.CURRENT
        else -> SemesterStatus.UPCOMING
    }

    /** Semester chips: everything so far plus the next one. */
    val semesterNumbers: List<Int> get() = (1..(child.currentSemester + 1).coerceAtMost(8)).toList()

    /** Other semesters in which [teacherId] taught this child. */
    fun alsoTaught(teacherId: String, except: Int): List<Int> =
        semesters.filter { s -> s.semester != except && s.loads.any { it.teacherId == teacherId } }.map { it.semester }
}

/** "Mon P5, P6 · Thu P5, P6": slots grouped by day, in week order. */
fun groupSlotsByDay(slots: List<Slot>): List<Pair<Int, List<Int>>> {
    val weekOrder = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
    return slots.groupBy { it.day }
        .toSortedMap(compareBy { weekOrder.indexOf(it) })
        .map { (day, list) -> day to list.map { it.period }.sorted() }
}

/** A short teacher list for students (name, role, subject, cabin). */
data class StudentFacultyEntry(val name: String, val designation: String, val subject: String, val note: String?, val cabin: String)
