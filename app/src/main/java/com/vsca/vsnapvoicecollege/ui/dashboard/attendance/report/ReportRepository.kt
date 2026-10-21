package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.stableUnit
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyDirectory
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/** Everything the report reads: classes with their students, and staff groups. */
data class ReportData(
    val institution: String,
    val classes: List<ReportClass>,
    val staffGroups: List<ReportStaffGroup>,
    val directory: FacultyDirectory,
) {
    fun classById(id: String): ReportClass? = classes.firstOrNull { it.id == id }
    fun groupById(id: String): ReportStaffGroup? = staffGroups.firstOrNull { it.id == id }
    fun student(id: String): ReportStudent? = classes.firstNotNullOfOrNull { c -> c.students.firstOrNull { it.id == id } }
    fun staffMember(id: String): ReportStaffMember? = staffGroups.firstNotNullOfOrNull { g -> g.members.firstOrNull { it.id == id } }
}

/**
 * Student and staff attendance for the principal's report. Classes, teachers and teaching
 * staff come from the faculty directory; students and non-teaching staff are sample data.
 *
 * TODO: replace with the attendance report API.
 */
object ReportRepository {

    private var cache: Pair<FacultyDirectory, ReportData>? = null

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /** The report as it stands for the current directory. */
    fun data(): ReportData {
        val directory = FacultyRepository.directory.value
        cache?.let { (d, data) -> if (d === directory) return data }
        return build(directory).also { cache = directory to it }
    }

    /** Loads the report. Offline, the last loaded report stays; with nothing loaded yet this throws. */
    suspend fun load(): ReportData {
        delay(LOAD_DELAY_MS)
        if (NetworkMonitor.isOnline.value) {
            _lastSyncedAt.value = System.currentTimeMillis()
        } else if (_lastSyncedAt.value == 0L) {
            throw IOException("offline")
        }
        return data()
    }

    /** Addresses the report goes to by default: the principal, plus the HOD when one department is picked. */
    fun defaultRecipients(departmentId: String?): List<String> =
        listOf("principal@ucechennai.edu.in") + listOfNotNull(departmentId?.let { "hod.$it@ucechennai.edu.in" })

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L

    private fun build(directory: FacultyDirectory): ReportData {
        val classes = directory.divisions.map { division ->
            val department = directory.department(division.departmentId)
            val teachers = directory.faculty.flatMap { f ->
                f.teaching.filter { it.divisionId == division.id }.map { ClassSubject(it.subject, f.name, it.periodsPerWeek) }
            }
            ReportClass(
                division = division,
                departmentName = department?.name.orEmpty(),
                programme = division.programme,
                students = studentsFor(division.id, division.departmentId, division.semester),
                mentor = directory.faculty(division.classTeacherId)?.name,
                // One row per subject, even when two teachers share it.
                subjects = teachers.distinctBy { it.name }.ifEmpty { listOf(ClassSubject("General", "", 6)) },
            )
        }
        val teachingGroups = directory.departments.map { d ->
            ReportStaffGroup(
                id = "dept-" + d.id,
                name = d.name,
                head = directory.hodOf(d.id)?.name,
                members = directory.faculty.filter { it.departmentId == d.id }.map { f ->
                    ReportStaffMember(f.id, f.name, f.initials, f.designation, f.employeeId, "dept-" + d.id, faculty = f)
                },
                teaching = true,
                departmentId = d.id,
            )
        }
        return ReportData(directory.institution, classes, teachingGroups + nonTeachingGroups(), directory)
    }

    private val studentFirst = listOf(
        "Aakash", "Bhavya", "Charan", "Divya", "Eswar", "Farhana", "Gokul", "Harini", "Ishaan", "Janani", "Karthik", "Lavanya",
        "Manoj", "Nithya", "Pranav", "Priyanka", "Rohit", "Sanjana", "Tarun", "Varsha", "Vijay", "Yazhini", "Ajay", "Keerthana",
        "Naveen", "Pooja", "Sneha", "Surya", "Vishnu", "Anusha",
    )
    private val studentLast = listOf("M.", "R.", "S.", "K.", "A.", "P.", "V.", "N.", "T.", "G.", "B.", "D.")

    private fun studentsFor(classId: String, departmentId: String, semester: Int): List<ReportStudent> {
        val count = 40 + (stableUnit("size", classId) * 24).toInt()
        val base = 0.84 + stableUnit("base", classId) * 0.09
        val year = 2027 - (semester + 1) / 2
        val people = (0 until count).map { i ->
            val first = studentFirst[(i * 7 + classId.length) % studentFirst.size]
            val last = studentLast[(i * 5 + semester) % studentLast.size]
            // A few students in every class sit below the 75% minimum.
            val low = stableUnit("low", classId, i) < 0.08
            val rate = if (low) 0.62 + stableUnit("lr", classId, i) * 0.1 else (base + (stableUnit("sr", classId, i) - 0.5) * 0.12).coerceIn(0.7, 0.99)
            ReportStudent(
                id = "$classId-s$i",
                name = "$first $last",
                initials = "${first.first()}${last.first()}",
                registerNo = "$year${departmentId.take(3).uppercase()}${(100 + i).toString().padStart(3, '0')}",
                classId = classId,
                rate = rate,
            )
        }
        // The designed students: in CSE · Sem 5 A, Rahul Kumar (68%, below the minimum), Aarav R. and the
        // students with leave requests; in IT · Sem 3 B, Diya R. (Aarav's sister).
        val designed = when (classId) {
            "cse-5-A" -> listOf(
                ReportStudent("$classId-rahul", "Rahul Kumar", "RK", "2024503041", classId, 0.68),
                ReportStudent("$classId-aarav", "Aarav R.", "AR", "2024503017", classId, 0.86),
                ReportStudent("$classId-sneha", "Sneha Nair", "SN", "2024503052", classId, 0.9),
                ReportStudent("$classId-karthik", "Karthik S.", "KS", "2024503033", classId, 0.83),
                ReportStudent("$classId-janani", "Janani K.", "JK", "2024503029", classId, 0.93),
            )
            "it-3-B" -> listOf(ReportStudent("$classId-diya", "Diya R.", "DR", "2025504012", classId, 0.92))
            else -> emptyList()
        }
        // Keep names unique within the class (generated names can repeat or match a designed student).
        val names = designed.map { it.name }.toMutableSet()
        val generated = people.filter { names.add(it.name) }
        return generated.take(count - designed.size) + designed
    }

    private val staffFirst = listOf("Arun", "Bala", "Chitra", "Devi", "Ezhil", "Ganga", "Hema", "Jeeva", "Kumar", "Lakshmi", "Murali", "Nalini", "Prabhu", "Revathy", "Siva", "Tamil", "Usha", "Vel")
    private val staffInitial = listOf("P", "K", "S", "R", "M", "T", "V", "N")

    private fun nonTeachingGroups(): List<ReportStaffGroup> {
        fun group(id: String, name: String, head: String, size: Int, roles: List<String>, startId: Int): ReportStaffGroup {
            val members = (0 until size).map { i ->
                val first = staffFirst[(i * 5 + id.length) % staffFirst.size]
                val initial = staffInitial[(i * 3 + size) % staffInitial.size]
                ReportStaffMember(
                    id = "$id-$i",
                    name = "$first $initial",
                    initials = "${first.first()}$initial",
                    designation = roles[i % roles.size],
                    employeeId = "EMP-${startId + i}",
                    groupId = id,
                    rate = 0.86 + stableUnit("ntr", id, i) * 0.1,
                )
            }
            return ReportStaffGroup(id, name, head, members, teaching = false)
        }
        val admin = group("admin", "Admin office", "Mr. S. Balaji", 32, listOf("Office assistant", "Clerk", "Accountant", "Record clerk"), 2201)
        // Arun P, the office assistant on the design, is late often and sits in "watch".
        val arun = ReportStaffMember("admin-arun", "Arun P", "AP", "Office assistant", "EMP-2214", "admin", rate = 0.82)
        return listOf(
            admin.copy(members = admin.members.drop(1) + arun),
            group("library", "Library", "Ms. R. Kalaivani", 8, listOf("Librarian", "Library assistant"), 2301),
            group("labs", "Laboratory staff", "Mr. K. Ramesh", 24, listOf("Lab assistant", "Lab technician"), 2401),
            group("maintenance", "Maintenance", "Mr. V. Selvam", 18, listOf("Electrician", "Housekeeping", "Gardener", "Driver"), 2501),
        )
    }
}

/** What the export screen works from: the report's filter when "Export" was tapped, and the result. */
object ReportExportSession {
    var filter: ReportFilter = ReportFilter()
    var result: ReportExportResult? = null
}

data class ReportExportResult(
    val fileName: String,
    val sizeBytes: Long,
    val pages: Int,
    val uri: android.net.Uri,
    val mimeType: String,
    val recipients: List<String>,
)
