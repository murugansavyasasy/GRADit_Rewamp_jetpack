package com.vsca.vsnapvoicecollege.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
        // TEMP (testing): launch into the auth flow (Login) to exercise Sign in → Role selection.
        // Revert to AppRoute.SPLASH for the real flow.
        startDestination = AuthRoute.GRAPH,
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
            )
        }

        composable(AppRoute.ONBOARDING) {
            OnboardingScreen(
                onFinish = { navController.navigate(AppRoute.COUNTRY) },
            )
        }

        composable(AppRoute.COUNTRY) {
            CountrySelectionScreen(
                onBack = { navController.popBackStack() },
                onContinue = { _ ->
                    // TODO: persist the selected country for the auth flow.
                    navController.navigate(AuthRoute.GRAPH) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        authNavGraph(navController)

        composable(AppRoute.HOME) {
            MainScreen(
                onSignOut = {
                    navController.navigate(AuthRoute.GRAPH) {
                        popUpTo(AppRoute.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
