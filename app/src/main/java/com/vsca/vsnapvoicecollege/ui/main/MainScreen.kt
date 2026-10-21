package com.vsca.vsnapvoicecollege.ui.main

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.RoleSelectionScreen
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.dashboard.academics.AcademicsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.changepassword.ChangePasswordScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.chat.ChatScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.discussion.DiscussionBoardScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.discussion.DiscussionThreadScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.discussion.DiscussionThreadViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.discussion.NewPostScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faq.FaqScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.BaseFilesViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.files.FamilyFilesScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.FileDetailScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.FileTypeTab
import com.vsca.vsnapvoicecollege.ui.dashboard.files.FileViewerScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.FilesScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.FoldersScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.LessonVideosScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.UploadPreviewScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.UploadPublishedScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.UploadScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.files.UploadViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.help.RaiseConcernScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.home.StudentHomeScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.legal.PrivacyPolicyScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.legal.TermsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.ComposeMessageScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.InboxFilter
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.InboxScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.InboxViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.MessageDetailScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.MessageDetailViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.MessageSearchScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.MessageType
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.NewMessageScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.messages.NewMessageViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.credits.CreditView
import com.vsca.vsnapvoicecollege.ui.dashboard.credits.SemCreditsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.credits.SemCreditsViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyDirectoryScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyProfileScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyProfileViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FamilyFacultyScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FamilyTeacherScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FamilyTeacherViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FreePeriodsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.StudentFacultyScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.ComposeMode
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.FamilyNoticeBoardScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.FamilyNoticeScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.NoticeBoardScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.NoticeComposeScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.NoticeComposeViewModel
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.NoticeDetailScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.NoticePreviewScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.NoticePublishedScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notifications.NotificationsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementEventDetailScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementEventsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.StudentPlacementEventsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.profile.ProfileScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.settings.SettingsScreen
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Bottom-navigation destinations; [forRole] picks the five shown for the signed-in role. */
enum class MainTab(
    val route: String,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    HOME("tab/home", R.drawable.ic_home, R.string.nav_home),

    // Students & parents
    ACADEMICS("tab/academics", R.drawable.ic_menu_book, R.string.nav_academics),
    CAMPUS("tab/campus", R.drawable.ic_apartment, R.string.nav_campus),
    CHAT("tab/chat", R.drawable.ic_chat, R.string.nav_chat),
    ME("tab/me", R.drawable.ic_person, R.string.nav_me),

    // Management & faculty
    ATTENDANCE("tab/attendance", R.drawable.ic_calendar, R.string.nav_attendance),
    MESSAGES("tab/messages", R.drawable.ic_chat, R.string.nav_messages),
    NOTICES("tab/notices", R.drawable.ic_campaign, R.string.nav_notices),
    PROFILE("tab/profile", R.drawable.ic_person, R.string.nav_profile),
    ;

    /** The tab that hosts the profile / settings screens. */
    val isProfile: Boolean
        get() = this == ME || this == PROFILE

    companion object {
        fun forRole(role: Role): List<MainTab> = when (role) {
            Role.STUDENT, Role.PARENT -> listOf(HOME, ACADEMICS, CAMPUS, CHAT, ME)
            Role.FACULTY, Role.MANAGEMENT -> listOf(HOME, ATTENDANCE, MESSAGES, NOTICES, PROFILE)
        }
    }
}

private const val ROUTE_NOTIFICATIONS = "home/notifications"
private const val ROUTE_DISCUSSION = "home/discussion"
private const val ROUTE_DISCUSSION_NEW = "home/discussion/new"
private const val ROUTE_DISCUSSION_THREAD =
    "home/discussion/thread/{${DiscussionThreadViewModel.ARG_POST_ID}}"
private const val ROUTE_INBOX = "home/messages?${InboxViewModel.ARG_FILTER}={${InboxViewModel.ARG_FILTER}}"
private const val ROUTE_MESSAGE_SEARCH = "home/messages/search"
private const val ROUTE_MESSAGE_DETAIL = "home/messages/detail/{${MessageDetailViewModel.ARG_MESSAGE_ID}}"
private const val ROUTE_NEW_MESSAGE = "home/messages/new?${NewMessageViewModel.ARG_TYPE}={${NewMessageViewModel.ARG_TYPE}}"
private const val ROUTE_COMPOSE_MESSAGE = "home/messages/compose"
private const val ROUTE_FILES = "home/files?${BaseFilesViewModel.ARG_TAB}={${BaseFilesViewModel.ARG_TAB}}"
private const val ROUTE_FAMILY_FILES = "home/family-files"
private const val ROUTE_LESSON_VIDEOS = "home/videos"
private const val ROUTE_FOLDERS = "home/files/folders"
private const val ROUTE_UPLOAD = "home/files/upload"
private const val ROUTE_UPLOAD_PREVIEW = "home/files/upload/preview"
private const val ROUTE_UPLOAD_PUBLISHED = "home/files/upload/published"
private const val ARG_FILE_ID = "fileId"
private const val ROUTE_FILE_DETAIL = "home/files/detail/{$ARG_FILE_ID}"
private const val ROUTE_FILE_VIEWER = "home/files/view/{$ARG_FILE_ID}"
private val HOME_SUBROUTES = setOf(
    ROUTE_NOTIFICATIONS, ROUTE_DISCUSSION, ROUTE_INBOX, ROUTE_MESSAGE_SEARCH,
    ROUTE_FILES, ROUTE_FAMILY_FILES, ROUTE_LESSON_VIDEOS, ROUTE_FOLDERS, ROUTE_UPLOAD_PUBLISHED, ROUTE_FILE_DETAIL,
    ROUTE_STUDENT_FACULTY, ROUTE_STUDENT_PLACEMENT,
)

private fun discussionThreadRoute(postId: String) = "home/discussion/thread/$postId"
private fun inboxRoute(filter: InboxFilter) = "home/messages?${InboxViewModel.ARG_FILTER}=${filter.name}"
private fun messageDetailRoute(messageId: String) = "home/messages/detail/$messageId"
private fun newMessageRoute(type: MessageType) = "home/messages/new?${NewMessageViewModel.ARG_TYPE}=${type.name}"
private fun filesRoute(tab: FileTypeTab) = "home/files?${BaseFilesViewModel.ARG_TAB}=${tab.name}"
private fun fileDetailRoute(fileId: String) = "home/files/detail/$fileId"
private fun fileViewerRoute(fileId: String) = "home/files/view/$fileId"

private const val ARG_NOTICE_ID = "noticeId"
private const val ROUTE_NOTICE_COMPOSE =
    "notices/compose?${NoticeComposeViewModel.ARG_NOTICE_ID}={${NoticeComposeViewModel.ARG_NOTICE_ID}}" +
        "&${NoticeComposeViewModel.ARG_MODE}={${NoticeComposeViewModel.ARG_MODE}}"
private const val ROUTE_NOTICE_PREVIEW = "notices/preview"
private const val ROUTE_NOTICE_PUBLISHED = "notices/published"
private const val ROUTE_NOTICE_DETAIL = "notices/detail/{$ARG_NOTICE_ID}"
private const val ROUTE_FAMILY_NOTICE = "campus/notices/{$ARG_NOTICE_ID}"

private fun noticeComposeRoute(noticeId: String? = null, mode: ComposeMode = ComposeMode.NEW) =
    "notices/compose?${NoticeComposeViewModel.ARG_NOTICE_ID}=${noticeId.orEmpty()}&${NoticeComposeViewModel.ARG_MODE}=${mode.name}"
private fun noticeDetailRoute(noticeId: String) = "notices/detail/$noticeId"
private fun familyNoticeRoute(noticeId: String) = "campus/notices/$noticeId"

/** Keeps the bottom bar, with no tab highlighted, as in the design. */
private const val ROUTE_SEM_CREDITS = "credits?${SemCreditsViewModel.ARG_VIEW}={${SemCreditsViewModel.ARG_VIEW}}"
private fun semCreditsRoute(view: CreditView) = "credits?${SemCreditsViewModel.ARG_VIEW}=${view.name}"

private const val ROUTE_FACULTY = "faculty"
private const val ROUTE_FREE_PERIODS = "faculty/free-periods"
private const val ROUTE_FACULTY_PROFILE = "faculty/profile/{${FacultyProfileViewModel.ARG_FACULTY_ID}}"
private const val ROUTE_FAMILY_FACULTY = "academics/faculty"
private const val ROUTE_FAMILY_TEACHER =
    "academics/faculty/{${FamilyTeacherViewModel.ARG_CHILD}}/{${FamilyTeacherViewModel.ARG_SEMESTER}}/{${FamilyTeacherViewModel.ARG_TEACHER}}"
private const val ROUTE_STUDENT_FACULTY = "home/faculty"

private fun facultyProfileRoute(id: String) = "faculty/profile/$id"
private fun familyTeacherRoute(childId: String, semester: Int, teacherId: String) = "academics/faculty/$childId/$semester/$teacherId"

/**
 * Parents get the full placement list (no tab lit, as in the design); students a simple
 * list that keeps Home lit. Both open the same event detail.
 */
private const val ROUTE_PLACEMENT = "placement"
private const val ROUTE_STUDENT_PLACEMENT = "home/placement"
private const val ARG_EVENT_ID = "eventId"
private const val ROUTE_PLACEMENT_EVENT = "placement/event/{$ARG_EVENT_ID}"

private fun placementEventRoute(eventId: String) = "placement/event/$eventId"

/** Parents reach their children's teachers from Academics, so that tab stays lit. */
private val ACADEMICS_SUBROUTES = setOf(ROUTE_FAMILY_FACULTY, ROUTE_FAMILY_TEACHER)

/** Full-screen flows that hide the bottom bar (they have their own bottom composer / form). */
private val FULL_SCREEN_ROUTES = setOf(
    ROUTE_DISCUSSION_THREAD, ROUTE_DISCUSSION_NEW,
    ROUTE_MESSAGE_DETAIL, ROUTE_NEW_MESSAGE, ROUTE_COMPOSE_MESSAGE,
    ROUTE_UPLOAD, ROUTE_UPLOAD_PREVIEW, ROUTE_FILE_VIEWER,
    ROUTE_NOTICE_COMPOSE, ROUTE_NOTICE_PREVIEW, ROUTE_NOTICE_PUBLISHED, ROUTE_NOTICE_DETAIL, ROUTE_FAMILY_NOTICE,
)

/** Full-screen routes with a white page, so the status-bar strip matches. */
private val WHITE_ROUTES = setOf(ROUTE_DISCUSSION_NEW, ROUTE_NOTICE_PUBLISHED)

private const val ROUTE_SETTINGS = "me/settings"
private const val ROUTE_CHANGE_PASSWORD = "me/change_password"
private const val ROUTE_RAISE_CONCERN = "me/raise_concern"
private const val ROUTE_ROLE_SELECTION = "me/role_selection"
private const val ROUTE_FAQ = "me/faq"
private const val ROUTE_PRIVACY = "me/privacy"
private const val ROUTE_TERMS = "me/terms"
private val ME_SUBROUTES = setOf(
    ROUTE_SETTINGS, ROUTE_CHANGE_PASSWORD, ROUTE_RAISE_CONCERN, ROUTE_ROLE_SELECTION,
    ROUTE_FAQ, ROUTE_PRIVACY, ROUTE_TERMS,
)

/**
 * Authenticated app shell: a bottom-navigation scaffold with a nested NavHost so
 * tab sub-screens (e.g. Settings) push on top while the bottom bar stays visible.
 * The tabs depend on the signed-in role (see [MainTab.forRole]).
 */
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarIcons(darkIcons = true)
    val context = LocalContext.current
    var role by rememberSaveable { mutableStateOf(SessionPreferences.getRole(context)) }
    val tabs = MainTab.forRole(role)
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isFullScreen = currentRoute in FULL_SCREEN_ROUTES

    Scaffold(
        modifier = modifier,
        // The new-post form is white edge to edge, so its status-bar strip is too.
        containerColor = if (currentRoute in WHITE_ROUTES) White else ScreenBackground,
        bottomBar = {
            if (!isFullScreen) {
                NavigationBar(containerColor = White) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route ||
                            (tab.isProfile && currentRoute in ME_SUBROUTES) ||
                            (tab == MainTab.HOME && currentRoute in HOME_SUBROUTES) ||
                            (tab == MainTab.ACADEMICS && currentRoute in ACADEMICS_SUBROUTES)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.icon),
                                    contentDescription = stringResource(tab.label),
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = {
                                // Single line so longer translations (e.g. Tamil) don't wrap the bar.
                                Text(
                                    text = stringResource(tab.label),
                                    fontSize = AppTextSize.CaptionSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                selectedTextColor = BrandBlue,
                                indicatorColor = SelectedRowBackground,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Full-screen flows draw behind the navigation bar and pad their own bottom content.
        val contentPadding = if (isFullScreen) {
            PaddingValues(top = innerPadding.calculateTopPadding())
        } else {
            innerPadding
        }
        NavHost(
            navController = navController,
            startDestination = MainTab.HOME.route,
            // Consume the scaffold insets so screens using imePadding() don't double-count them.
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
        ) {
            composable(MainTab.HOME.route) {
                StudentHomeScreen(
                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                    onOpenDiscussionBoard = { navController.navigate(ROUTE_DISCUSSION) },
                    // Students & parents read messages; management writes them in two steps
                    // ("New message" → recipients) and faculty go straight to the composer.
                    onOpenTextMessages = {
                        navController.navigate(
                            when (role) {
                                Role.STUDENT, Role.PARENT -> inboxRoute(InboxFilter.TEXT)
                                Role.MANAGEMENT -> newMessageRoute(MessageType.TEXT)
                                Role.FACULTY -> ROUTE_COMPOSE_MESSAGE
                            },
                        )
                    },
                    onOpenVoiceMessages = {
                        navController.navigate(
                            when (role) {
                                Role.STUDENT, Role.PARENT -> inboxRoute(InboxFilter.VOICE)
                                Role.MANAGEMENT, Role.FACULTY -> newMessageRoute(MessageType.VOICE)
                            },
                        )
                    },
                    // Staff manage files; parents and students only see what was shared with them.
                    onOpenFiles = {
                        navController.navigate(if (isStaff(role)) filesRoute(FileTypeTab.ALL) else ROUTE_FAMILY_FILES)
                    },
                    onOpenVideos = {
                        navController.navigate(if (isStaff(role)) filesRoute(FileTypeTab.VIDEOS) else ROUTE_LESSON_VIDEOS)
                    },
                    // Staff manage notices from their Notices tab; families read them under Campus.
                    onOpenNotices = {
                        if (isStaff(role)) {
                            navController.navigate(MainTab.NOTICES.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate(MainTab.CAMPUS.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onOpenCredits = { view -> navController.navigate(semCreditsRoute(view)) },
                    // Staff get the directory, parents their children's teachers, students their own list.
                    onOpenFaculty = {
                        navController.navigate(
                            when (role) {
                                Role.MANAGEMENT, Role.FACULTY -> ROUTE_FACULTY
                                Role.PARENT -> ROUTE_FAMILY_FACULTY
                                Role.STUDENT -> ROUTE_STUDENT_FACULTY
                            },
                        )
                    },
                    onOpenPlacementEvents = {
                        navController.navigate(if (role == Role.PARENT) ROUTE_PLACEMENT else ROUTE_STUDENT_PLACEMENT)
                    },
                )
            }
            composable(ROUTE_PLACEMENT) {
                PlacementEventsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEvent = { id -> navController.navigate(placementEventRoute(id)) },
                )
            }
            composable(ROUTE_STUDENT_PLACEMENT) {
                StudentPlacementEventsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEvent = { id -> navController.navigate(placementEventRoute(id)) },
                )
            }
            composable(
                route = ROUTE_PLACEMENT_EVENT,
                arguments = listOf(navArgument(ARG_EVENT_ID) { type = NavType.StringType }),
            ) { entry ->
                PlacementEventDetailScreen(
                    eventId = entry.arguments?.getString(ARG_EVENT_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = ROUTE_INBOX,
                arguments = listOf(
                    navArgument(InboxViewModel.ARG_FILTER) {
                        type = NavType.StringType
                        defaultValue = InboxFilter.ALL.name
                    },
                ),
            ) {
                InboxScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSearch = { navController.navigate(ROUTE_MESSAGE_SEARCH) },
                    onOpenMessage = { id -> navController.navigate(messageDetailRoute(id)) },
                )
            }
            composable(ROUTE_MESSAGE_SEARCH) {
                MessageSearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMessage = { id -> navController.navigate(messageDetailRoute(id)) },
                )
            }
            composable(
                route = ROUTE_MESSAGE_DETAIL,
                arguments = listOf(
                    navArgument(MessageDetailViewModel.ARG_MESSAGE_ID) { type = NavType.StringType },
                ),
            ) {
                MessageDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = ROUTE_NEW_MESSAGE,
                arguments = listOf(
                    navArgument(NewMessageViewModel.ARG_TYPE) {
                        type = NavType.StringType
                        defaultValue = MessageType.TEXT.name
                    },
                ),
            ) {
                NewMessageScreen(
                    onBack = { navController.popBackStack() },
                    onContinueToRecipients = { navController.navigate(ROUTE_COMPOSE_MESSAGE) },
                )
            }
            composable(
                route = ROUTE_FILES,
                arguments = listOf(
                    navArgument(BaseFilesViewModel.ARG_TAB) {
                        type = NavType.StringType
                        defaultValue = FileTypeTab.ALL.name
                    },
                ),
            ) {
                FilesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFolders = { navController.navigate(ROUTE_FOLDERS) },
                    onUpload = { navController.navigate(ROUTE_UPLOAD) },
                    onOpenFile = { id -> navController.navigate(fileDetailRoute(id)) },
                )
            }
            composable(ROUTE_FOLDERS) { FoldersScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_UPLOAD) {
                UploadScreen(
                    viewModel = viewModel(),
                    onBack = { navController.popBackStack() },
                    onPreview = { navController.navigate(ROUTE_UPLOAD_PREVIEW) },
                    onPublished = { navController.navigate(ROUTE_UPLOAD_PUBLISHED) },
                )
            }
            // Preview and Published share the Upload screen's form state.
            composable(ROUTE_UPLOAD_PREVIEW) { entry ->
                val uploadEntry = remember(entry) { navController.getBackStackEntry(ROUTE_UPLOAD) }
                UploadPreviewScreen(
                    viewModel = viewModel(uploadEntry),
                    onEdit = { navController.popBackStack() },
                    onPublished = {
                        navController.navigate(ROUTE_UPLOAD_PUBLISHED) {
                            popUpTo(ROUTE_UPLOAD) { inclusive = false }
                        }
                    },
                )
            }
            composable(ROUTE_UPLOAD_PUBLISHED) { entry ->
                val uploadEntry = remember(entry) { navController.getBackStackEntry(ROUTE_UPLOAD) }
                val uploadViewModel: UploadViewModel = viewModel(uploadEntry)
                val backToFiles: () -> Unit = {
                    // Leave the upload flow; return to the file list it was opened from.
                    if (!navController.popBackStack(ROUTE_FILES, inclusive = false)) {
                        navController.popBackStack(MainTab.HOME.route, inclusive = false)
                        navController.navigate(filesRoute(FileTypeTab.ALL))
                    }
                }
                BackHandler(onBack = backToFiles)
                UploadPublishedScreen(
                    viewModel = uploadViewModel,
                    onUploadMore = {
                        uploadViewModel.onReset()
                        navController.popBackStack(ROUTE_UPLOAD, inclusive = false)
                    },
                    onViewFiles = backToFiles,
                )
            }
            composable(
                route = ROUTE_FILE_DETAIL,
                arguments = listOf(navArgument(ARG_FILE_ID) { type = NavType.StringType }),
            ) { entry ->
                FileDetailScreen(
                    fileId = entry.arguments?.getString(ARG_FILE_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_FAMILY_FILES) {
                FamilyFilesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFile = { id -> navController.navigate(fileViewerRoute(id)) },
                )
            }
            composable(
                route = ROUTE_FILE_VIEWER,
                arguments = listOf(navArgument(ARG_FILE_ID) { type = NavType.StringType }),
            ) { entry ->
                FileViewerScreen(
                    fileId = entry.arguments?.getString(ARG_FILE_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_LESSON_VIDEOS) { LessonVideosScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_COMPOSE_MESSAGE) {
                ComposeMessageScreen(
                    onBack = { navController.popBackStack() },
                    // Sent — leave the whole "New message" flow and return home.
                    onSent = { navController.popBackStack(MainTab.HOME.route, inclusive = false) },
                )
            }
            composable(ROUTE_NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_DISCUSSION) {
                DiscussionBoardScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { postId -> navController.navigate(discussionThreadRoute(postId)) },
                    onNewPost = { navController.navigate(ROUTE_DISCUSSION_NEW) },
                )
            }
            composable(
                route = ROUTE_DISCUSSION_THREAD,
                arguments = listOf(
                    navArgument(DiscussionThreadViewModel.ARG_POST_ID) { type = NavType.StringType },
                ),
            ) {
                DiscussionThreadScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_DISCUSSION_NEW) {
                NewPostScreen(
                    onClose = { navController.popBackStack() },
                    onPosted = { navController.popBackStack() },
                )
            }
            composable(MainTab.ACADEMICS.route) {
                AcademicsScreen(
                    onBack = {
                        navController.navigate(MainTab.HOME.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            // Campus currently holds the notice board for students and parents.
            composable(MainTab.CAMPUS.route) {
                FamilyNoticeBoardScreen(
                    onBack = {
                        navController.navigate(MainTab.HOME.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenNotice = { id -> navController.navigate(familyNoticeRoute(id)) },
                )
            }
            composable(MainTab.ATTENDANCE.route) { PlaceholderTab(MainTab.ATTENDANCE) }
            composable(MainTab.NOTICES.route) {
                NoticeBoardScreen(
                    onBack = {
                        navController.navigate(MainTab.HOME.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreate = { navController.navigate(noticeComposeRoute()) },
                    onOpenNotice = { id -> navController.navigate(noticeDetailRoute(id)) },
                )
            }
            composable(
                route = ROUTE_NOTICE_COMPOSE,
                arguments = listOf(
                    navArgument(NoticeComposeViewModel.ARG_NOTICE_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NoticeComposeViewModel.ARG_MODE) {
                        type = NavType.StringType
                        defaultValue = ComposeMode.NEW.name
                    },
                ),
            ) {
                NoticeComposeScreen(
                    viewModel = viewModel(),
                    onBack = { navController.popBackStack() },
                    onPreview = { navController.navigate(ROUTE_NOTICE_PREVIEW) },
                    onPublished = { navController.navigate(ROUTE_NOTICE_PUBLISHED) },
                    onDraftSaved = { navController.popBackStack() },
                )
            }
            // Preview and Published share the compose screen's form state.
            composable(ROUTE_NOTICE_PREVIEW) { entry ->
                val composeEntry = remember(entry) { navController.getBackStackEntry(ROUTE_NOTICE_COMPOSE) }
                NoticePreviewScreen(
                    viewModel = viewModel(composeEntry),
                    onEdit = { navController.popBackStack() },
                    onPublished = {
                        navController.navigate(ROUTE_NOTICE_PUBLISHED) {
                            popUpTo(ROUTE_NOTICE_COMPOSE) { inclusive = false }
                        }
                    },
                )
            }
            composable(ROUTE_NOTICE_PUBLISHED) { entry ->
                val composeEntry = remember(entry) { navController.getBackStackEntry(ROUTE_NOTICE_COMPOSE) }
                val composeViewModel: NoticeComposeViewModel = viewModel(composeEntry)
                // Leave the compose flow and land back on the notice board.
                val backToBoard: () -> Unit = {
                    if (!navController.popBackStack(MainTab.NOTICES.route, inclusive = false)) {
                        navController.navigate(MainTab.NOTICES.route)
                    }
                }
                BackHandler(onBack = backToBoard)
                NoticePublishedScreen(
                    viewModel = composeViewModel,
                    onClose = backToBoard,
                    onViewNotice = { id ->
                        backToBoard()
                        navController.navigate(noticeDetailRoute(id))
                    },
                    onCreateAnother = {
                        composeViewModel.onReset()
                        navController.popBackStack(ROUTE_NOTICE_COMPOSE, inclusive = false)
                    },
                )
            }
            composable(
                route = ROUTE_NOTICE_DETAIL,
                arguments = listOf(navArgument(ARG_NOTICE_ID) { type = NavType.StringType }),
            ) { entry ->
                NoticeDetailScreen(
                    noticeId = entry.arguments?.getString(ARG_NOTICE_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(noticeComposeRoute(id, ComposeMode.EDIT)) },
                    onDuplicate = { id -> navController.navigate(noticeComposeRoute(id, ComposeMode.DUPLICATE)) },
                )
            }
            composable(
                route = ROUTE_FAMILY_NOTICE,
                arguments = listOf(navArgument(ARG_NOTICE_ID) { type = NavType.StringType }),
            ) { entry ->
                FamilyNoticeScreen(
                    noticeId = entry.arguments?.getString(ARG_NOTICE_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = ROUTE_SEM_CREDITS,
                arguments = listOf(
                    navArgument(SemCreditsViewModel.ARG_VIEW) {
                        type = NavType.StringType
                        defaultValue = CreditView.BY_CATEGORY.name
                    },
                ),
            ) {
                SemCreditsScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_FACULTY) {
                FacultyDirectoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFaculty = { id -> navController.navigate(facultyProfileRoute(id)) },
                    onOpenFreePeriods = { navController.navigate(ROUTE_FREE_PERIODS) },
                )
            }
            composable(ROUTE_FREE_PERIODS) {
                FreePeriodsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFaculty = { id -> navController.navigate(facultyProfileRoute(id)) },
                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                    onOpenPunchIn = {
                        navController.navigate(MainTab.ATTENDANCE.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(
                route = ROUTE_FACULTY_PROFILE,
                arguments = listOf(navArgument(FacultyProfileViewModel.ARG_FACULTY_ID) { type = NavType.StringType }),
            ) {
                FacultyProfileScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFreePeriods = { navController.navigate(ROUTE_FREE_PERIODS) },
                )
            }
            composable(ROUTE_FAMILY_FACULTY) {
                FamilyFacultyScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTeacher = { child, semester, teacher -> navController.navigate(familyTeacherRoute(child, semester, teacher)) },
                )
            }
            composable(
                route = ROUTE_FAMILY_TEACHER,
                arguments = listOf(
                    navArgument(FamilyTeacherViewModel.ARG_CHILD) { type = NavType.StringType },
                    navArgument(FamilyTeacherViewModel.ARG_SEMESTER) { type = NavType.IntType },
                    navArgument(FamilyTeacherViewModel.ARG_TEACHER) { type = NavType.StringType },
                ),
            ) {
                FamilyTeacherScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTimetable = {
                        navController.navigate(MainTab.ACADEMICS.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(ROUTE_STUDENT_FACULTY) { StudentFacultyScreen(onBack = { navController.popBackStack() }) }
            listOf(MainTab.CHAT, MainTab.MESSAGES).forEach { tab ->
                composable(tab.route) { ChatScreen() }
            }
            listOf(MainTab.ME, MainTab.PROFILE).forEach { tab ->
                composable(tab.route) {
                    ProfileScreen(
                        onSignOut = onSignOut,
                        onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                        onAllRoles = { navController.navigate(ROUTE_ROLE_SELECTION) },
                    )
                }
            }
            composable(ROUTE_ROLE_SELECTION) {
                RoleSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { newRole ->
                        // Remove the role-selection screen from the profile tab's back stack
                        // first, so returning to the profile shows it (not this screen).
                        val profileTab = tabs.first { it.isProfile }
                        navController.popBackStack(profileTab.route, inclusive = false)
                        SessionPreferences.setRole(context, newRole)
                        role = newRole
                        // Switching role lands on the Home tab.
                        navController.navigate(MainTab.HOME.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChangePassword = { navController.navigate(ROUTE_CHANGE_PASSWORD) },
                    onOpenHelpSupport = { navController.navigate(ROUTE_RAISE_CONCERN) },
                    onOpenFaqs = { navController.navigate(ROUTE_FAQ) },
                    onOpenPrivacy = { navController.navigate(ROUTE_PRIVACY) },
                    onOpenTerms = { navController.navigate(ROUTE_TERMS) },
                )
            }
            composable(ROUTE_RAISE_CONCERN) {
                RaiseConcernScreen(
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() },
                )
            }
            composable(ROUTE_CHANGE_PASSWORD) {
                ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                    onUpdated = { navController.popBackStack() },
                )
            }
            composable(ROUTE_FAQ) {
                FaqScreen(
                    onBack = { navController.popBackStack() },
                    onRaiseConcern = { navController.navigate(ROUTE_RAISE_CONCERN) },
                )
            }
            composable(ROUTE_PRIVACY) {
                PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() },
                    onReadTerms = { navController.navigate(ROUTE_TERMS) },
                )
            }
            composable(ROUTE_TERMS) {
                TermsScreen(
                    onBack = { navController.popBackStack() },
                    onAccept = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderTab(tab: MainTab, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(tab.label),
            color = TextPrimary,
            fontSize = AppTextSize.H4,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Management and faculty manage content; parents and students receive it. */
private fun isStaff(role: Role): Boolean = role == Role.MANAGEMENT || role == Role.FACULTY
