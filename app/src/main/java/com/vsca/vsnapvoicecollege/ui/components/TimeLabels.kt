package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Date

/** Time of day in the device's 12/24-hour format, e.g. "10:42 AM". */
@Composable
fun clockLabel(millis: Long): String =
    android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(Date(millis))

/** True when [millis] falls on the calendar day [daysAgo] days before today. */
fun isDaysAgo(millis: Long, daysAgo: Int): Boolean {
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    val target = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    return day.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        day.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
