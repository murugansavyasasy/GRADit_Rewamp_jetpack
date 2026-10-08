package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.RadioBorder
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Circular selection indicator: a filled blue check when selected, an empty
 * outlined circle otherwise. Reused by any single-select list/grid row.
 */
@Composable
fun SelectionRadio(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(BrandBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.selected_content_desc),
                tint = White,
                modifier = Modifier.size(16.dp),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.5.dp, RadioBorder, CircleShape),
        )
    }
}
