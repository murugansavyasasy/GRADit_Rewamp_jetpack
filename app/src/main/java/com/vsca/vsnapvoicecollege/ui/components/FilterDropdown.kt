package com.vsca.vsnapvoicecollege.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * "Category: All ▾" chip that opens a menu of [options] (label to action). Pass
 * [selected] to highlight the current option, and [iconRes] to replace the chevron
 * (e.g. a sort icon on "Newest first").
 */
@Composable
fun FilterDropdown(
    label: String?,
    value: String,
    options: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    selected: String? = null,
    @DrawableRes iconRes: Int = R.drawable.ic_expand_more,
    /** Blue outline, for when the filter is narrowing the list. */
    highlighted: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = White,
            border = if (highlighted) BorderStroke(1.5.dp, BrandBlue) else BorderStroke(1.dp, FieldBorder),
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (label != null) stringResource(R.string.common_filter_value, label, value) else value,
                    color = TextPrimary,
                    fontSize = AppTextSize.Body,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(10.dp))
                Icon(painterResource(iconRes), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(
                    text = { Text(text, color = if (text == selected) BrandBlue else TextPrimary) },
                    onClick = {
                        expanded = false
                        action()
                    },
                )
            }
        }
    }
}
