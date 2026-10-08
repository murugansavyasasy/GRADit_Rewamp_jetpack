package com.vsca.vsnapvoicecollege.ui.dashboard.help

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vsca.vsnapvoicecollege.R

/** The category a raised concern belongs to, shown as a selectable grid. */
enum class ConcernCategory(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    ACADEMIC(R.string.concern_cat_academic, R.drawable.ic_menu_book),
    ATTENDANCE(R.string.concern_cat_attendance, R.drawable.ic_calendar),
    FEES(R.string.concern_cat_fees, R.drawable.ic_credit_card),
    TRANSPORT(R.string.concern_cat_transport, R.drawable.ic_bus),
    HOSTEL(R.string.concern_cat_hostel, R.drawable.ic_home),
    OTHER(R.string.concern_cat_other, R.drawable.ic_help),
}
