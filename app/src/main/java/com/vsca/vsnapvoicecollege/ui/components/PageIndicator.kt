package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.IndicatorInactive

/**
 * Horizontal page indicator: the active page is an elongated pill, the rest are
 * dots. Colors are configurable so it works on both brand and light backgrounds.
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = BrandBlue,
    inactiveColor: Color = IndicatorInactive,
    dotSize: Dp = 6.dp,
    activeWidth: Dp = 22.dp,
    spacing: Dp = 6.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .height(dotSize)
                    .width(if (selected) activeWidth else dotSize)
                    .clip(CircleShape)
                    .background(if (selected) activeColor else inactiveColor),
            )
        }
    }
}
