package com.vsca.vsnapvoicecollege.ui.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.vsca.vsnapvoicecollege.ui.navigation.AppRoute
import com.vsca.vsnapvoicecollege.ui.auth.forgotpassword.ForgotPasswordScreen
import com.vsca.vsnapvoicecollege.ui.auth.login.LoginScreen
import com.vsca.vsnapvoicecollege.ui.auth.otp.OtpVerificationScreen
import com.vsca.vsnapvoicecollege.ui.auth.resetpassword.ResetPasswordScreen
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.RoleSelectionScreen

/**
 * Registers the authentication flow as a nested navigation graph.
 *
 * Call from the app's root NavHost:
 * ```
 * NavHost(navController, startDestination = AuthRoute.GRAPH) {
 *     authNavGraph(navController)
 *     // ...other graphs
 * }
 * ```
 */
fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        route = AuthRoute.GRAPH,
        startDestination = AuthRoute.LOGIN,
    ) {
        composable(AuthRoute.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(AuthRoute.REGISTER) },
                onNavigateToForgotPassword = { mobile ->
                    navController.navigate(AuthRoute.forgotPassword(mobile))
                },
                onLoginSuccess = {
                    navController.navigate(AuthRoute.ROLE_SELECTION)
                },
            )
        }

        composable(AuthRoute.ROLE_SELECTION) {
            RoleSelectionScreen(
                onContinue = {
                    navController.navigate(AppRoute.HOME) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = AuthRoute.FORGOT_PASSWORD_PATTERN,
            arguments = listOf(
                navArgument(AuthRoute.ARG_MOBILE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val mobile = backStackEntry.arguments?.getString(AuthRoute.ARG_MOBILE).orEmpty()
            ForgotPasswordScreen(
                enteredMobile = mobile,
                onNavigateBack = { navController.popBackStack() },
                onOtpSent = { target -> navController.navigate(AuthRoute.otpVerification(target)) },
            )
        }
        composable(
            route = AuthRoute.OTP_VERIFICATION_PATTERN,
            arguments = listOf(
                navArgument(AuthRoute.ARG_TARGET) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val target = backStackEntry.arguments?.getString(AuthRoute.ARG_TARGET).orEmpty()
            OtpVerificationScreen(
                maskedTarget = target,
                onNavigateBack = { navController.popBackStack() },
                onVerified = { navController.navigate(AuthRoute.RESET_PASSWORD) },
            )
        }
        composable(AuthRoute.RESET_PASSWORD) {
            ResetPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onPasswordReset = { navController.navigate(AuthRoute.LOGIN) },
            )
        }
    }
}
