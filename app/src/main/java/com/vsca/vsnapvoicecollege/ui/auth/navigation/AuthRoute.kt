package com.vsca.vsnapvoicecollege.ui.auth.navigation

import android.net.Uri

/**
 * Type-safe route keys for the authentication flow.
 */
object AuthRoute {
    const val GRAPH = "auth_graph"

    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT_PASSWORD = "auth/forgot_password"
    const val OTP_VERIFICATION = "auth/otp_verification"
    const val RESET_PASSWORD = "auth/reset_password"
    const val ROLE_SELECTION = "auth/role_selection"
    const val TERMS = "auth/terms"
    const val PRIVACY = "auth/privacy"

    /** Optional mobile/identifier carried into the forgot-password screen. */
    const val ARG_MOBILE = "mobile"

    /** Optional masked contact (mobile/email) carried into the OTP screen. */
    const val ARG_TARGET = "target"

    /** Route pattern registered by the graph (mobile is an optional query arg). */
    const val FORGOT_PASSWORD_PATTERN = "$FORGOT_PASSWORD?$ARG_MOBILE={$ARG_MOBILE}"

    /** Route pattern for the OTP screen (masked target is an optional query arg). */
    const val OTP_VERIFICATION_PATTERN = "$OTP_VERIFICATION?$ARG_TARGET={$ARG_TARGET}"

    /** Route pattern for the role-selection screen (typed mobile is an optional query arg). */
    const val ROLE_SELECTION_PATTERN = "$ROLE_SELECTION?$ARG_MOBILE={$ARG_MOBILE}"

    /** Builds the role-selection route carrying the [mobile] the user signed in with. */
    fun roleSelection(mobile: String): String =
        "$ROLE_SELECTION?$ARG_MOBILE=${Uri.encode(mobile)}"

    /** Builds the forgot-password route carrying the entered [mobile]. */
    fun forgotPassword(mobile: String): String =
        "$FORGOT_PASSWORD?$ARG_MOBILE=${Uri.encode(mobile)}"

    /** Builds the OTP route carrying the masked [target] the code was sent to. */
    fun otpVerification(target: String): String =
        "$OTP_VERIFICATION?$ARG_TARGET=${Uri.encode(target)}"
}
