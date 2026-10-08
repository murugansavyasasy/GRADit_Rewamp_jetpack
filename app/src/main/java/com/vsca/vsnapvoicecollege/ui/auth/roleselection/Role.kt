package com.vsca.vsnapvoicecollege.ui.auth.roleselection

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vsca.vsnapvoicecollege.R

/**
 * A role the signed-in account can act as. Each carries its own copy
 * (title / description), leading icon and the lowercase word used in the
 * "Continue as …" CTA. Badge colors are resolved on the screen.
 */
enum class Role(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:StringRes val continueLabel: Int,
    @param:StringRes val badgeContentDesc: Int,
    @param:DrawableRes val icon: Int,
) {
    PARENT(
        title = R.string.role_parent_title,
        description = R.string.role_parent_desc,
        continueLabel = R.string.role_continue_parent,
        badgeContentDesc = R.string.role_parent_badge_desc,
        icon = R.drawable.ic_people,
    ),
    STUDENT(
        title = R.string.role_student_title,
        description = R.string.role_student_desc,
        continueLabel = R.string.role_continue_student,
        badgeContentDesc = R.string.role_student_badge_desc,
        icon = R.drawable.ic_graduation_cap,
    ),
    FACULTY(
        title = R.string.role_faculty_title,
        description = R.string.role_faculty_desc,
        continueLabel = R.string.role_continue_faculty,
        badgeContentDesc = R.string.role_faculty_badge_desc,
        icon = R.drawable.ic_book,
    ),
    MANAGEMENT(
        title = R.string.role_management_title,
        description = R.string.role_management_desc,
        continueLabel = R.string.role_continue_management,
        badgeContentDesc = R.string.role_management_badge_desc,
        icon = R.drawable.ic_apartment,
    ),
}
