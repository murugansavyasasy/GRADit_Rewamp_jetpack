package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * "Powered by [SAVYASASY logo]" footer, reused across splash and onboarding.
 *
 * [textColor] adapts the "Powered by" label to the background; [logoBackground]
 * is the chip behind the logo (White on brand bg, Transparent on light bg).
 */
@Composable
fun PoweredByRow(
    modifier: Modifier = Modifier,
    textColor: Color = White,
    logoBackground: Color = White,
    logoHeight: Dp = 28.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.splash_powered_by),
            color = textColor,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = logoBackground,
        ) {
            Image(
                painter = painterResource(R.drawable.savyasasy_logo),
                contentDescription = stringResource(R.string.splash_partner_logo_content_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .height(logoHeight),
            )
        }
    }
}
