package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * A rounded-square badge holding a single icon (the app's graduation-cap logo by
 * default, or any icon for the onboarding illustrations).
 *
 * Colors and icon are configurable so it can render white-on-brand (splash),
 * brand-on-white, or themed card variants while staying one reusable component.
 */
@Composable
fun LogoBadge(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_graduation_cap,
    contentDescription: String? = stringResource(R.string.splash_logo_content_desc),
    size: Dp = 96.dp,
    cornerRadius: Dp = 28.dp,
    iconSize: Dp = 44.dp,
    containerColor: Color = White,
    iconTint: Color = BrandBlue,
    shadowElevation: Dp = 12.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        shadowElevation = shadowElevation,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
