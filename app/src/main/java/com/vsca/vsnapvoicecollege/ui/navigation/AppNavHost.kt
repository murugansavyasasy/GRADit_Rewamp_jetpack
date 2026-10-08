package com.vsca.vsnapvoicecollege.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vsca.vsnapvoicecollege.data.OnboardingPreferences
import com.vsca.vsnapvoicecollege.data.RegionPreferences
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.navigation.AuthRoute
import com.vsca.vsnapvoicecollege.ui.auth.navigation.authNavGraph
import com.vsca.vsnapvoicecollege.ui.auth.country.CountrySelectionScreen
import com.vsca.vsnapvoicecollege.ui.auth.onboarding.OnboardingScreen
import com.vsca.vsnapvoicecollege.ui.auth.splash.SplashScreen
import com.vsca.vsnapvoicecollege.ui.main.MainScreen

/**
 * Top-level, type-safe route keys for the app's root navigation graph.
 */
object AppRoute {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val COUNTRY = "country"
    const val HOME = "home"
}

/**
 * Root navigation host: Splash decides whether to enter the auth flow or the
 * main app. The auth flow is registered as a nested graph.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.SPLASH,
        modifier = modifier,
    ) {
        composable(AppRoute.SPLASH) {
            SplashScreen(
                onGetStarted = {
                    navController.navigate(AppRoute.ONBOARDING)
                },
                onNavigateToHome = {
                    navController.navigate(AppRoute.HOME) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToAuth = {
                    navController.navigate(AuthRoute.GRAPH) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToCountry = {
                    navController.navigate(AppRoute.COUNTRY) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoute.ONBOARDING) {
            val context = LocalContext.current
            OnboardingScreen(
                onFinish = {
                    // Mark the one-time intro as done so later launches skip it.
                    OnboardingPreferences.setCompleted(context)
                    navController.navigate(AppRoute.COUNTRY)
                },
            )
        }

        composable(AppRoute.COUNTRY) {
            val context = LocalContext.current
            CountrySelectionScreen(
                onBack = { navController.popBackStack() },
                onContinue = { _ ->
                    // Region confirmed — later launches can skip straight to sign in.
                    // TODO: also persist which country was chosen for the auth flow.
                    RegionPreferences.setSelected(context, true)
                    navController.navigate(AuthRoute.GRAPH) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        authNavGraph(navController)

        composable(AppRoute.HOME) {
            val context = LocalContext.current
            MainScreen(
                onSignOut = {
                    // Clear the session so the app no longer auto-logins.
                    SessionPreferences.setLoggedIn(context, false)
                    navController.navigate(AuthRoute.GRAPH) {
                        popUpTo(AppRoute.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
