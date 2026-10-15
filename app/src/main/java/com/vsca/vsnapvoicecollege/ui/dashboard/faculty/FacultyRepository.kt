package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.Calendar

/**
 * Faculty data for staff (the directory, today's presence and free periods) and for
 * families (each child's teachers by semester). Offline, the last loaded data stays
 * available; with nothing loaded yet, loading fails.
 *
 * TODO: load from the staff / timetable APIs; the sample data mirrors the designs.
 */
object FacultyRepository {

    private val _directory by lazy { MutableStateFlow(sampleDirectory()) }
    val directory: StateFlow<FacultyDirectory> get() = _directory

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    private val _familySyncedAt = MutableStateFlow(0L)
    val familySyncedAt: StateFlow<Long> = _familySyncedAt.asStateFlow()

    /** Messages written offline, sent once the connection is back. */
    private val _pendingMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pendingMessages: StateFlow<List<Pair<String, String>>> = _pendingMessages.asStateFlow()

    /**
     * Minute of the day used for "ended / in progress" on free periods.
     * TODO: use the device clock once timetables come from the API; fixed at 11:00 AM so the
     * sample day always has past, current and upcoming periods.
     */
    const val NOW_MINUTE = 11 * 60

    suspend fun loadDirectory(): FacultyDirectory {
        delay(LOAD_DELAY_MS)
        checkConnection(_lastSyncedAt)
        return _directory.value
    }

    /** A child already loaded this session (profiles open from the loaded list). */
    fun cachedChild(childId: String): ChildFaculty? = family.firstOrNull { it.child.id == childId }

    suspend fun loadChild(childId: String): ChildFaculty {
        delay(LOAD_DELAY_MS)
        checkConnection(_familySyncedAt)
        return family.firstOrNull { it.child.id == childId } ?: throw IOException("unknown child")
    }

    private fun checkConnection(syncedAt: MutableStateFlow<Long>) {
        if (NetworkMonitor.isOnline.value) {
            syncedAt.value = System.currentTimeMillis()
        } else if (syncedAt.value == 0L) {
            throw IOException("offline")
        }
    }

    // ---- Staff actions (principal) ----

    /** Makes [facultyId] class teacher of [divisionId]; the previous one is replaced. */
    fun assignClassTeacher(facultyId: String, divisionId: String) = _directory.update { d ->
        d.copy(divisions = d.divisions.map { if (it.id == divisionId) it.copy(classTeacherId = facultyId) else it })
    }

    /** Makes [facultyId] head of their department; the current head stays on as Professor. */
    fun makeHeadOfDepartment(facultyId: String) = _directory.update { d ->
        val dept = d.faculty(facultyId)?.departmentId ?: return@update d
        d.copy(
            faculty = d.faculty.map { f ->
                when {
                    f.id == facultyId -> f.copy(isHod = true, designation = HOD_DESIGNATION)
                    f.departmentId == dept && f.isHod -> f.copy(isHod = false, designation = DEMOTED_DESIGNATION)
                    else -> f
                }
            },
        )
    }

    fun allocate(freePeriodId: String, facultyId: String?) = _directory.update { d ->
        d.copy(freePeriods = d.freePeriods.map { if (it.id == freePeriodId) it.copy(allocatedToId = facultyId) else it })
    }

    /** Present teachers with no class in [period] today, best matches (same department) first. */
    fun freeTeachersFor(freePeriod: FreePeriod): List<Faculty> {
        val d = _directory.value
        val busy = d.freePeriods.filter { it.period == freePeriod.period && it.id != freePeriod.id }.mapNotNull { it.allocatedToId }.toSet()
        val dept = d.faculty(freePeriod.absentFacultyId)?.departmentId
        val stream = d.department(dept)?.streamId
        return d.faculty
            .filter { it.presence.status == PresenceStatus.PRESENT && it.id !in busy && d.department(it.departmentId)?.streamId == stream }
            .sortedWith(compareBy({ it.departmentId != dept }, { it.periodsPerWeek }))
            .take(6)
    }

    /** Puts back a snapshot (tests undo the principal actions with it). */
    internal fun restore(snapshot: FacultyDirectory) {
        _directory.value = snapshot
    }

    // ---- Family messages ----

    /** Sends a message to a teacher; returns false when it was queued to send later. */
    suspend fun sendMessage(teacherId: String, text: String): Boolean {
        if (!NetworkMonitor.isOnline.value) {
            _pendingMessages.update { it + (teacherId to text) }
            return false
        }
        delay(SEND_DELAY_MS)
        // TODO: post to the messages API.
        return true
    }

    /** Sends whatever was written offline. */
    suspend fun flushPending(): Int = flushLock.withLock {
        val queued = _pendingMessages.value
        if (queued.isEmpty() || !NetworkMonitor.isOnline.value) return@withLock 0
        delay(SEND_DELAY_MS)
        _pendingMessages.update { it - queued.toSet() }
        queued.size
    }

    private val flushLock = Mutex()

    private const val LOAD_DELAY_MS = 700L
    private const val SEND_DELAY_MS = 400L
    private const val DEMOTED_DESIGNATION = "Professor"
    private const val HOD_DESIGNATION = "Professor and Head"

    // ===================================================================
    // Sample staff directory: UCE Chennai, 102 teaching staff, 18 departments.
    // ===================================================================

    private class DeptPlan(
        val id: String,
        val name: String,
        val stream: String,
        val count: Int,
        val programme: String,
        val semesters: List<Int>,
        val sections: List<String>,
        val subjects: List<String>,
        val away: List<PresenceStatus> = emptyList(),
        val notIn: Int = 0,
    )

    private val plans = listOf(
        // Arts and Science · 42 (Mathematics also counts Dr. Anitha Rajan from Computer Science)
        DeptPlan("tamil", "Tamil", "as", 5, "B.A. Tamil", listOf(1, 3, 5), listOf("A"), listOf("Tamil Literature", "Sangam Poetry", "Modern Tamil Prose", "Tamil Grammar")),
        DeptPlan("english", "English", "as", 6, "B.A. English", listOf(1, 3, 5), listOf("A"), listOf("British Poetry", "Indian Writing in English", "Linguistics", "Communicative English"), away = listOf(PresenceStatus.ON_LEAVE)),
        DeptPlan("maths", "Mathematics", "as", 6, "B.Sc Maths", listOf(1, 3, 5), listOf("A"), listOf("Real Analysis", "Linear Algebra", "Discrete Mathematics", "Statistics")),
        DeptPlan("physics", "Physics", "as", 5, "B.Sc Physics", listOf(1, 3, 5), listOf("A"), listOf("Thermodynamics", "Quantum Mechanics", "Optics", "Electronics"), away = listOf(PresenceStatus.ABSENT)),
        DeptPlan("chemistry", "Chemistry", "as", 5, "B.Sc Chemistry", listOf(1, 3, 5), listOf("A"), listOf("Organic Chemistry", "Physical Chemistry", "Inorganic Chemistry", "Analytical Chemistry"), notIn = 1),
        DeptPlan("cs", "Computer Science", "as", 7, "B.Sc CS", listOf(1, 3, 5), listOf("A", "B"), listOf("Database Systems", "Python Programming", "Data Structures", "Operating Systems", "Web Technology", "Computer Networks")),
        DeptPlan("commerce", "Commerce", "as", 4, "B.Com", listOf(1, 3, 5), listOf("A"), listOf("Financial Accounting", "Business Law", "Cost Accounting")),
        DeptPlan("economics", "Economics", "as", 4, "B.A. Economics", listOf(1, 3, 5), listOf("A"), listOf("Microeconomics", "Macroeconomics", "Indian Economy")),
        // Engineering · 38
        DeptPlan("cse", "CSE", "en", 8, "B.E. CSE", listOf(3, 5, 7), listOf("A", "B"), listOf("Theory of Computation", "Compiler Design", "Database Management Systems", "Computer Networks", "Cloud Computing", "Algorithms")),
        DeptPlan("it", "IT", "en", 7, "B.Tech IT", listOf(3, 5, 7), listOf("A", "B"), listOf("Data Structures", "Software Engineering", "Web Programming", "Information Security")),
        DeptPlan("ece", "ECE", "en", 7, "B.E. ECE", listOf(3, 5, 7), listOf("A"), listOf("Signals and Systems", "Digital Electronics", "VLSI Design", "Communication Systems")),
        DeptPlan("eee", "EEE", "en", 6, "B.E. EEE", listOf(3, 5, 7), listOf("A"), listOf("Electrical Machines", "Power Systems", "Control Systems")),
        DeptPlan("mech", "Mechanical", "en", 5, "B.E. Mechanical", listOf(3, 5, 7), listOf("A"), listOf("Thermodynamics", "Fluid Mechanics", "Machine Design"), away = listOf(PresenceStatus.ABSENT)),
        DeptPlan("civil", "Civil", "en", 5, "B.E. Civil", listOf(3, 5, 7), listOf("A"), listOf("Structural Analysis", "Surveying", "Concrete Technology"), notIn = 1),
        // Management · 13
        DeptPlan("mba", "MBA", "mg", 8, "MBA", listOf(1, 3), listOf("A", "B"), listOf("Marketing Management", "Financial Management", "Organisational Behaviour", "Business Analytics"), away = listOf(PresenceStatus.ON_LEAVE)),
        DeptPlan("bba", "BBA", "mg", 5, "BBA", listOf(1, 3, 5), listOf("A"), listOf("Principles of Management", "Business Economics", "Human Resource Management")),
        // Education · 9
        DeptPlan("bed", "B.Ed", "ed", 5, "B.Ed", listOf(1, 3), listOf("A"), listOf("Pedagogy of Science", "Educational Psychology", "Assessment for Learning")),
        DeptPlan("med", "M.Ed", "ed", 4, "M.Ed", listOf(1, 3), listOf("A"), listOf("Curriculum Studies", "Research Methodology", "Philosophy of Education")),
    )

    private val streamCodes = mapOf("as" to "AS", "en" to "EN", "mg" to "MG", "ed" to "ED")

    /** Named staff from the designs; everyone else is generated per department. */
    private fun namedStaff(): Map<String, List<Faculty>> {
        fun f(
            id: String, name: String, initials: String, designation: String, dept: String, empId: String,
            specialisation: String, isHod: Boolean = false, punch: Int = 9 * 60, presence: Presence? = null,
            qualification: String = "Ph.D.", years: Int = 10, phone: String = "+91 94440 1${id.hashCode().toUInt().toString().takeLast(4)}",
            also: List<String> = emptyList(), teaching: List<Teaching> = emptyList(),
        ) = Faculty(
            id, name, initials, designation, dept, empId, qualification, years, phone, specialisation, isHod, also, teaching,
            presence ?: Presence(PresenceStatus.PRESENT, punch, "fingerprint, Main block"),
        )
        return mapOf(
            "tamil" to listOf(
                f("sivakumar", "Dr. Sivakumar Raju", "SR", "Professor and Head", "tamil", "UCE-AS-1101", "Tamil Literature, Sangam Poetry", isHod = true, punch = 8 * 60 + 56, years = 22),
                f("jayanthi", "Dr. Jayanthi Ramesh", "JR", "Associate Professor", "tamil", "UCE-AS-1102", "Modern Tamil Prose, Tamil Grammar", punch = 8 * 60 + 49, years = 15),
            ),
            "maths" to listOf(
                f("abirami", "Dr. Abirami Nagarajan", "AN", "Professor and Head", "maths", "UCE-AS-1131", "Real Analysis, Linear Algebra", isHod = true, punch = 8 * 60 + 41, years = 20),
                f("gayathri", "Dr. Gayathri Pandian", "GP", "Associate Professor", "maths", "UCE-AS-1132", "Discrete Mathematics, Graph Theory", punch = 8 * 60 + 52, years = 12),
            ),
            "cs" to listOf(
                f("senthil", "Dr. Senthil Kumar Ganesan", "SG", "Professor and Head", "cs", "UCE-AS-1141", "Machine Learning, Data Mining", isHod = true, punch = 8 * 60 + 58, years = 21),
                f(
                    "anitha", "Dr. Anitha Rajan", "AR", "Associate Professor", "cs", "UCE-AS-1148", "Database Systems, Python Programming",
                    punch = 8 * 60 + 47, qualification = "MCA, M.Phil., Ph.D. (Anna University)", years = 14, phone = "+91 94440 21587",
                    also = listOf("maths"),
                    teaching = listOf(
                        Teaching("Database Systems", "cs-5-A", 5),
                        Teaching("Python Programming", "cs-3-B", 5),
                        Teaching("Discrete Mathematics", "maths-3-A", 4),
                    ),
                ),
                f("geetha", "Dr. Geetha Arumugam", "GA", "Assistant Professor", "cs", "UCE-AS-1149", "Python Programming, Web Technology", punch = 8 * 60 + 50, years = 8),
                f("lakshmipriya", "Ms. Lakshmi Priya Srinivasan", "LS", "Assistant Professor", "cs", "UCE-AS-1150", "Data Structures, Operating Systems", punch = 8 * 60 + 54, years = 6),
            ),
            "cse" to listOf(
                f(
                    "vignesh", "Prof. Vignesh Prabhu", "VP", "Assistant Professor", "cse", "UCE-EN-2116", "Theory of Computation, Compiler Design",
                    qualification = "Ph.D., M.Tech", years = 9, phone = "+91 9178523907",
                    presence = Presence(PresenceStatus.ON_LEAVE, leaveReason = "sick leave"),
                    teaching = listOf(Teaching("Theory of Computation", "cse-5-B", 6), Teaching("Theory of Computation", "cse-5-A", 6), Teaching("Compiler Design", "cse-3-A", 4), Teaching("Cloud Computing", "cse-7-A", 4)),
                ),
                f("aruldoss", "Mr. Arul Doss", "AD", "Assistant Professor", "cse", "UCE-EN-2121", "Algorithms, Computer Networks", punch = 8 * 60 + 44, years = 7),
                f("kavitha", "Dr. Kavitha M.", "KM", "Assistant Professor", "cse", "UCE-EN-2124", "Database Management Systems, Programming in C", punch = 8 * 60 + 51, years = 11),
                f("meena", "Dr. Meena Krishnan", "MK", "Professor", "cse", "UCE-EN-2102", "Database Management Systems, Data Mining", punch = 8 * 60 + 39, years = 18),
            ),
            "it" to listOf(
                f(
                    "priya", "Dr. Priya Natarajan", "PN", "Associate Professor", "it", "UCE-EN-2208", "Data Structures, Algorithms",
                    presence = Presence(PresenceStatus.ABSENT),
                    teaching = listOf(Teaching("Data Structures", "it-3-B", 8), Teaching("Data Structures Lab", "it-3-B", 4)),
                ),
                f("revathi", "Dr. Revathi S.", "RS", "Assistant Professor", "it", "UCE-EN-2214", "Software Engineering, Web Programming", punch = 10 * 60 + 20, years = 9),
            ),
        )
    }

    private val firstNames = listOf(
        "Arun", "Bharathi", "Chitra", "Dinesh", "Elango", "Fathima", "Ganesh", "Hari", "Indira", "Jagan", "Kalai", "Latha",
        "Mohan", "Nandini", "Prakash", "Radha", "Saravanan", "Thenmozhi", "Uma", "Vasanth", "Yamuna", "Anand", "Bala", "Deepa",
        "Gowri", "Kannan", "Malathi", "Nirmal", "Pavithra", "Raghu", "Selvi", "Thilak", "Vani", "Ashok", "Charulatha", "Dhanush",
    )
    private val lastNames = listOf(
        "Kumar", "Subramanian", "Murugan", "Rajendran", "Velu", "Krishnan", "Pandian", "Natarajan", "Shankar", "Iyer",
        "Sundaram", "Mani", "Raman", "Selvam", "Venkatesh", "Balaji", "Chandran", "Durai",
    )
    private val designations = listOf("Associate Professor", "Assistant Professor", "Assistant Professor", "Associate Professor", "Assistant Professor")

    private fun sampleDirectory(): FacultyDirectory {
        val named = namedStaff()
        val departments = plans.map { Department(it.id, it.name, it.stream) }
        val divisions = mutableListOf<Division>()
        val faculty = mutableListOf<Faculty>()
        var seq = 0
        plans.forEach { plan ->
            plan.semesters.forEach { sem -> plan.sections.forEach { sec -> divisions += Division("${plan.id}-$sem-$sec", plan.programme, sem, sec, plan.id, null) } }
            val deptDivisions = divisions.filter { it.departmentId == plan.id }
            val people = named[plan.id].orEmpty().toMutableList()
            while (people.size < plan.count) {
                val i = seq++
                val first = firstNames[(i * 7) % firstNames.size]
                val last = lastNames[(i * 5) % lastNames.size]
                val title = if (i % 3 == 0) "Prof." else "Dr."
                people += Faculty(
                    id = "${plan.id}-$i",
                    name = "$title $first $last",
                    initials = "${first.first()}${last.first()}",
                    designation = if (people.none { it.isHod }) "Professor and Head" else designations[i % designations.size],
                    departmentId = plan.id,
                    employeeId = "UCE-${streamCodes[plan.stream]}-${1200 + i}",
                    qualification = listOf("Ph.D.", "M.Phil., Ph.D.", "M.E., Ph.D.", "M.Sc., M.Phil.")[i % 4],
                    experienceYears = 4 + (i * 3) % 19,
                    phone = "+91 9${(840000000 + i * 7919L) % 1_000_000_000}".let { it.substring(0, 9) + " " + it.substring(9) },
                    specialisation = plan.subjects.drop(i % plan.subjects.size).plus(plan.subjects).take(2).joinToString(", "),
                    isHod = people.none { it.isHod },
                    presence = Presence(PresenceStatus.PRESENT, 8 * 60 + 35 + (i * 11) % 25, "fingerprint, Main block"),
                )
            }
            // Away / not-yet-in staff are the last generated people, so named staff keep their designed status.
            val generated = people.filter { p -> named[plan.id].orEmpty().none { it.id == p.id } }.reversed().toMutableList()
            plan.away.forEach { status ->
                val p = generated.removeAt(0)
                people[people.indexOf(p)] = p.copy(presence = Presence(status, leaveReason = if (status == PresenceStatus.ON_LEAVE) "casual leave" else null))
            }
            repeat(plan.notIn) {
                val p = generated.removeAt(0)
                people[people.indexOf(p)] = p.copy(presence = Presence(PresenceStatus.NOT_IN))
            }
            // Spread the department's subjects over its divisions; designed teachers keep their own load.
            people.forEachIndexed { index, p ->
                if (p.teaching.isEmpty() && deptDivisions.isNotEmpty()) {
                    val load = (0 until 2).map { k ->
                        val division = deptDivisions[(index + k * 2) % deptDivisions.size]
                        Teaching(plan.subjects[(index + k) % plan.subjects.size], division.id, 4 + (index + k) % 3)
                    }.distinctBy { it.divisionId }
                    people[index] = p.copy(teaching = load)
                }
            }
            faculty += people
        }
        // Class teachers: the designed ones, then the first teacher of each remaining division.
        val designedClassTeachers = mapOf("cs-1-A" to "geetha", "cs-5-A" to "anitha", "cs-3-B" to "lakshmipriya", "maths-3-A" to "gayathri", "tamil-1-A" to "jayanthi")
        val assigned = mutableSetOf<String>()
        val withTeachers = divisions.map { division ->
            val teacher = designedClassTeachers[division.id]
                ?: faculty.firstOrNull { f -> !f.isHod && f.id !in assigned && f.id !in designedClassTeachers.values && f.teaching.any { it.divisionId == division.id } }?.id
            teacher?.let(assigned::add)
            division.copy(classTeacherId = teacher)
        }
        // Make sure the designed class teachers teach their division.
        val withLoads = faculty.map { f ->
            val missing = designedClassTeachers.filterValues { it == f.id }.keys.filter { id -> f.teaching.none { it.divisionId == id } }
            if (missing.isEmpty()) f else f.copy(teaching = f.teaching + missing.map { Teaching(plans.first { p -> it.startsWith(p.id + "-") }.subjects.first(), it, 4) })
        }
        return FacultyDirectory(
            institution = "UCE Chennai",
            streams = listOf(Stream("as", "Arts and Science"), Stream("en", "Engineering"), Stream("mg", "Management"), Stream("ed", "Education")),
            departments = departments,
            divisions = withTeachers,
            faculty = withLoads,
            freePeriods = listOf(
                FreePeriod("fp1", 1, "Theory of Computation", "cse-5-B", "B-206", "vignesh", allocatedToId = "aruldoss"),
                FreePeriod("fp2", 3, "Data Structures", "it-3-B", "C-108", "priya"),
                FreePeriod("fp3", 4, "Theory of Computation", "cse-5-A", "B-204", "vignesh", allocatedToId = "kavitha"),
                FreePeriod("fp4", 5, "Data Structures Lab", "it-3-B", "Lab 2", "priya"),
                FreePeriod("fp5", 6, "Compiler Design", "cse-3-A", "B-112", "vignesh"),
                FreePeriod("fp6", 7, "Cloud Computing", "cse-7-A", "B-301", "vignesh", allocatedToId = "meena"),
            ),
        )
    }

    /** "Dr. Revathi S. punched in late…" — the handover after a late arrival. */
    val lateArrival = LateArrival(facultyId = "revathi", returnedPeriod = 5, keptPeriod = 2, keptWithId = "aruldoss")

    // ===================================================================
    // Sample family data: Aarav R. (B.E. CSE · Sem 5 A) and Diya R. (B.Tech IT · Sem 3 B).
    // ===================================================================

    private const val MON = Calendar.MONDAY
    private const val TUE = Calendar.TUESDAY
    private const val WED = Calendar.WEDNESDAY
    private const val THU = Calendar.THURSDAY
    private const val FRI = Calendar.FRIDAY
    private const val SAT = Calendar.SATURDAY

    private fun t(id: String, name: String, initials: String, designation: String, qualification: String, years: Int, cabin: String, hours: ParentHours, dept: String = "CSE") =
        Teacher(id, name, initials, designation, dept, qualification, years, cabin, hours)

    private fun hours(day: Int, start: Int) = ParentHours(day, start, start + 50)

    private val familyTeachers = listOf(
        t("kavitha", "Dr. Kavitha M.", "KM", "Assistant Professor", "Ph.D., M.E. (CSE)", 11, "Cabin B-205", hours(WED, 14 * 60 + 15)),
        t("uma", "Dr. Uma Maheswari Ganesan", "UG", "Professor", "Ph.D. (Mathematics)", 19, "Cabin A-112", hours(FRI, 14 * 60 + 15), dept = "Mathematics"),
        t("hemalatha", "Dr. Hemalatha Sekar", "HS", "Associate Professor", "Ph.D. (Physics)", 13, "Cabin A-118", hours(TUE, 14 * 60 + 15), dept = "Physics"),
        t("suresh", "Prof. Suresh Kannan", "SK", "Assistant Professor", "M.A., M.Phil. (English)", 8, "Cabin A-104", hours(MON, 15 * 60 + 5), dept = "English"),
        t("karthik", "Prof. Karthik Raman", "KR", "Associate Professor", "Ph.D., M.E. (CSE)", 12, "Cabin B-214", hours(TUE, 15 * 60 + 5)),
        t("priya", "Dr. Priya Natarajan", "PN", "Associate Professor", "Ph.D., M.Tech (IT)", 14, "Cabin C-106", hours(FRI, 15 * 60 + 5), dept = "IT"),
        t("rajan", "Prof. Rajan K.", "RK", "Assistant Professor", "M.E. (CSE)", 7, "Cabin B-220", hours(THU, 14 * 60 + 15)),
        t("bala", "Prof. Bala Murugan", "BM", "Assistant Professor", "M.E. (ECE)", 9, "Cabin D-102", hours(WED, 15 * 60 + 5), dept = "ECE"),
        t("meena", "Dr. Meena Krishnan", "MK", "Professor", "Ph.D., M.E. (CSE)", 18, "Cabin B-210", hours(THU, 15 * 60 + 5)),
        t("vignesh", "Prof. Vignesh Prabhu", "VP", "Assistant Professor", "M.E. (CSE)", 9, "Cabin B-218", hours(MON, 15 * 60 + 5)),
        t("anbu", "Prof. Anbu Selvan", "AS", "Assistant Professor", "M.E. (CSE)", 6, "Cabin B-224", hours(FRI, 14 * 60 + 15)),
        t("deepa", "Dr. Deepa Venkat", "DV", "Associate Professor", "Ph.D., M.E. (CSE)", 13, "Cabin B-208", hours(WED, 15 * 60 + 5)),
        t("anand", "Prof. Anand V.", "AV", "Assistant Professor", "M.Tech (IT)", 8, "Cabin C-110", hours(MON, 14 * 60 + 15), dept = "IT"),
        t("lakshmin", "Dr. Lakshmi Narayanan", "LN", "Associate Professor", "Ph.D. (Mathematics)", 15, "Cabin A-114", hours(THU, 14 * 60 + 15), dept = "Mathematics"),
    )

    private fun s(name: String, kind: SubjectKind, periods: Int, slots: List<Slot> = emptyList()) =
        SubjectLoad(name, kind, periods, slots.ifEmpty { spreadSlots(periods, name.hashCode()) })

    /** A plausible weekly spread when the timetable doesn't say: one period a day, Monday on. */
    private fun spreadSlots(periods: Int, seed: Int): List<Slot> {
        val days = listOf(MON, TUE, WED, THU, FRI, SAT)
        val start = (seed and 0x7fffffff) % 6
        return (0 until periods).map { i -> Slot(days[(start + i) % days.size], 1 + ((seed and 0x7fffffff) / 7 + i * 3) % 7) }
    }

    private val TH = SubjectKind.THEORY
    private val LAB = SubjectKind.LAB
    private val ACT = SubjectKind.ACTIVITY

    private val family: List<ChildFaculty> by lazy {
        listOf(
            ChildFaculty(
                child = Child("aarav", "Aarav R.", "Aarav", "AR", "B.E. CSE", 5, "A"),
                teachers = familyTeachers,
                semesters = listOf(
                    SemesterClass(
                        1, "A", MonthRange.of(2024, 7, 2024, 11), "suresh",
                        listOf(
                            TeacherLoad("suresh", listOf(s("Professional English I", TH, 4))),
                            TeacherLoad("uma", listOf(s("Matrices and Calculus", TH, 5))),
                            TeacherLoad("hemalatha", listOf(s("Engineering Physics", TH, 4))),
                            TeacherLoad("kavitha", listOf(s("Python Programming", TH, 4), s("Python Programming Lab", LAB, 4))),
                        ),
                    ),
                    SemesterClass(
                        2, "A", MonthRange.of(2024, 12, 2025, 4), "kavitha",
                        listOf(
                            TeacherLoad("kavitha", listOf(s("Programming in C", TH, 5), s("C Programming Lab", LAB, 4))),
                            TeacherLoad("uma", listOf(s("Engineering Mathematics II", TH, 5))),
                            TeacherLoad("hemalatha", listOf(s("Physics for Information Science", TH, 4))),
                            TeacherLoad("suresh", listOf(s("Professional English II", TH, 4))),
                        ),
                    ),
                    SemesterClass(
                        3, "A", MonthRange.of(2025, 7, 2025, 11), "karthik",
                        listOf(
                            TeacherLoad("karthik", listOf(s("Object Oriented Programming", TH, 4), s("OOP Lab", LAB, 4))),
                            TeacherLoad("priya", listOf(s("Data Structures", TH, 5))),
                            TeacherLoad("rajan", listOf(s("Discrete Mathematics", TH, 4))),
                            TeacherLoad("bala", listOf(s("Digital Principles and Computer Organization", TH, 4))),
                            TeacherLoad("hemalatha", listOf(s("Environmental Sciences", TH, 4))),
                        ),
                    ),
                    SemesterClass(
                        4, "A", MonthRange.of(2025, 12, 2026, 4), "uma",
                        listOf(
                            TeacherLoad("uma", listOf(s("Probability and Queueing Theory", TH, 5))),
                            TeacherLoad("vignesh", listOf(s("Operating Systems", TH, 5), s("Operating Systems Lab", LAB, 4))),
                            TeacherLoad("karthik", listOf(s("Design and Analysis of Algorithms", TH, 5))),
                            TeacherLoad("rajan", listOf(s("Algorithms Lab", LAB, 4))),
                        ),
                    ),
                    SemesterClass(
                        5, "A", MonthRange.of(2026, 7, 2026, 11), "meena",
                        listOf(
                            TeacherLoad(
                                "meena",
                                listOf(
                                    s("Database Management Systems", TH, 6, listOf(Slot(MON, 1), Slot(TUE, 3), Slot(WED, 1), Slot(THU, 2), Slot(FRI, 3), Slot(SAT, 3))),
                                    s("DBMS Lab", LAB, 4, listOf(Slot(MON, 5), Slot(MON, 6), Slot(THU, 5), Slot(THU, 6))),
                                    s("Mentoring hour", ACT, 2, listOf(Slot(MON, 7), Slot(WED, 6))),
                                ),
                            ),
                            TeacherLoad("karthik", listOf(s("Computer Networks", TH, 5))),
                            TeacherLoad(
                                "vignesh",
                                listOf(s("Theory of Computation", TH, 6, listOf(Slot(MON, 4), Slot(TUE, 4), Slot(WED, 4), Slot(THU, 1), Slot(FRI, 2), Slot(SAT, 4)))),
                                awayToday = listOf(AwayPeriod(4, "Theory of Computation", substitute = "Dr. Kavitha M.")),
                            ),
                            TeacherLoad("kavitha", listOf(s("Compiler Design", TH, 5))),
                            TeacherLoad("anbu", listOf(s("Computer Networks Lab", LAB, 4))),
                            TeacherLoad("rajan", listOf(s("Cloud Computing (Elective)", TH, 4))),
                            TeacherLoad("deepa", listOf(s("Mini Project", ACT, 3))),
                        ),
                    ),
                    SemesterClass(6, "A", MonthRange.of(2026, 12, 2027, 4), null, emptyList()),
                ),
            ),
            ChildFaculty(
                child = Child("diya", "Diya R.", "Diya", "DR", "B.Tech IT", 3, "B"),
                teachers = familyTeachers,
                semesters = listOf(
                    SemesterClass(
                        1, "B", MonthRange.of(2025, 7, 2025, 11), "suresh",
                        listOf(
                            TeacherLoad("suresh", listOf(s("Professional English I", TH, 4))),
                            TeacherLoad("lakshmin", listOf(s("Matrices and Calculus", TH, 5))),
                            TeacherLoad("hemalatha", listOf(s("Engineering Physics", TH, 4), s("Physics Lab", LAB, 3))),
                            TeacherLoad("anand", listOf(s("Problem Solving and Python", TH, 4), s("Python Lab", LAB, 4))),
                        ),
                    ),
                    SemesterClass(
                        2, "B", MonthRange.of(2025, 12, 2026, 4), "anand",
                        listOf(
                            TeacherLoad("anand", listOf(s("Programming in C", TH, 5), s("C Programming Lab", LAB, 4))),
                            TeacherLoad("lakshmin", listOf(s("Statistics and Numerical Methods", TH, 5))),
                            TeacherLoad("bala", listOf(s("Basic Electronics", TH, 4))),
                            TeacherLoad("suresh", listOf(s("Professional English II", TH, 4))),
                        ),
                    ),
                    SemesterClass(
                        3, "B", MonthRange.of(2026, 7, 2026, 11), "anand",
                        listOf(
                            TeacherLoad("anand", listOf(s("Object Oriented Programming", TH, 7))),
                            TeacherLoad(
                                "priya",
                                listOf(s("Data Structures", TH, 8), s("Data Structures Lab", LAB, 4)),
                                awayToday = listOf(AwayPeriod(3, "Data Structures"), AwayPeriod(5, "Data Structures Lab")),
                            ),
                            TeacherLoad("lakshmin", listOf(s("Discrete Mathematics", TH, 7))),
                            TeacherLoad("bala", listOf(s("Digital Principles and Computer Organization", TH, 6))),
                            TeacherLoad("hemalatha", listOf(s("Environmental Sciences", TH, 6))),
                        ),
                    ),
                    SemesterClass(4, "B", MonthRange.of(2026, 12, 2027, 4), null, emptyList()),
                ),
            ),
        )
    }

    val children: List<Child> get() = family.map { it.child }

    /** The signed-in student's teachers. TODO: take from the session. */
    val studentFaculty = listOf(
        StudentFacultyEntry("Dr. Meena Iyer", "Associate Professor", "DBMS", "Class mentor", "Cabin B-210"),
        StudentFacultyEntry("Prof. K. Rao", "Assistant Professor", "Operating Systems", null, "Cabin B-214"),
        StudentFacultyEntry("Dr. S. Nair", "Associate Professor", "Computer Networks", null, "Cabin C-105"),
        StudentFacultyEntry("Prof. A. Das", "Assistant Professor", "Software Engineering", null, "Cabin B-218"),
        StudentFacultyEntry("Dr. R. Lakshmi", "Professor & HOD", "Computer Science", null, "Cabin B-201"),
    )
}

/** A late punch-in: [returnedPeriod] went back to the teacher, [keptPeriod] stayed with its substitute. */
data class LateArrival(val facultyId: String, val returnedPeriod: Int, val keptPeriod: Int, val keptWithId: String)
