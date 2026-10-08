package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted

/**
 * Small, muted, bold section header (e.g. "NOTIFICATIONS", "LINKED STUDENTS").
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = TextMuted,
        fontSize = AppTextSize.CaptionSmall,
        fontWeight = FontWeight.Bold,
    )
}
