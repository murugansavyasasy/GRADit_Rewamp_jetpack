package com.vsca.vsnapvoicecollege.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.RoleSelectionScreen
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.dashboard.academics.AcademicsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.changepassword.ChangePasswordScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.chat.ChatScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.faq.FaqScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.help.RaiseConcernScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.home.StudentHomeScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.legal.PrivacyPolicyScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.legal.TermsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.notifications.NotificationsScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.profile.ProfileScreen
import com.vsca.vsnapvoicecollege.ui.dashboard.settings.SettingsScreen
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** The five bottom-navigation destinations. */
enum class MainTab(
    val route: String,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    HOME("tab/home", R.drawable.ic_home, R.string.nav_home),
    ACADEMICS("tab/academics", R.drawable.ic_menu_book, R.string.nav_academics),
    CAMPUS("tab/campus", R.drawable.ic_apartment, R.string.nav_campus),
    CHAT("tab/chat", R.drawable.ic_chat, R.string.nav_chat),
    ME("tab/me", R.drawable.ic_person, R.string.nav_me),
}

private const val ROUTE_NOTIFICATIONS = "home/notifications"
private val HOME_SUBROUTES = setOf(ROUTE_NOTIFICATIONS)

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
 */
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarIcons(darkIcons = true)
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        modifier = modifier,
        containerColor = ScreenBackground,
        bottomBar = {
            NavigationBar(containerColor = White) {
                MainTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route ||
                        (tab == MainTab.ME && currentRoute in ME_SUBROUTES) ||
                        (tab == MainTab.HOME && currentRoute in HOME_SUBROUTES)
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
                        label = { Text(stringResource(tab.label), fontSize = AppTextSize.CaptionSmall) },
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainTab.HOME.route) {
                StudentHomeScreen(
                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                )
            }
            composable(ROUTE_NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
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
            composable(MainTab.CAMPUS.route) { PlaceholderTab(MainTab.CAMPUS) }
            composable(MainTab.CHAT.route) { ChatScreen() }
            composable(MainTab.ME.route) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onAllRoles = { navController.navigate(ROUTE_ROLE_SELECTION) },
                )
            }
            composable(ROUTE_ROLE_SELECTION) {
                RoleSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = {
                        // Remove the role-selection screen from the Me tab's back stack
                        // first, so returning to Me shows the profile (not this screen).
                        navController.popBackStack(MainTab.ME.route, inclusive = false)
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
