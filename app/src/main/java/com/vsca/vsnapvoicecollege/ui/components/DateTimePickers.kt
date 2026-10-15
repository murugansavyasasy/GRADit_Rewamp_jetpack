package com.vsca.vsnapvoicecollege.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import com.vsca.vsnapvoicecollege.R
import java.util.Calendar

/** System date then time picker; only future times are accepted. */
fun pickDateTime(context: Context, onPicked: (Long) -> Unit) {
    pickDate(context) { day ->
        val now = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val picked = Calendar.getInstance().apply {
                    timeInMillis = day
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }.timeInMillis
                if (picked > System.currentTimeMillis()) {
                    onPicked(picked)
                } else {
                    Toast.makeText(context, R.string.common_pick_future, Toast.LENGTH_SHORT).show()
                }
            },
            now.get(Calendar.HOUR_OF_DAY) + 1,
            0,
            DateFormat.is24HourFormat(context),
        ).show()
    }
}

/** System date picker from today onwards; reports the end of the picked day. */
fun pickDate(context: Context, onPicked: (Long) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onPicked(
                Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis,
            )
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH),
    ).apply { datePicker.minDate = now.timeInMillis }.show()
}
