package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground

/** Small rounded numbered badge used in ordered content lists. */
@Composable
fun NumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SelectedRowBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = BrandBlue,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}
