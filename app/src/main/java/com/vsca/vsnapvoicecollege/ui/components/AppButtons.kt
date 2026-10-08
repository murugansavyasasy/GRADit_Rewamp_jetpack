package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.vsca.vsnapvoicecollege.ui.theme.AppDimens
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * The app's standard full-width primary button.
 *
 * Shape, height and label typography are fixed so every primary action looks the
 * same across the app. Only the colors are configurable, for use on different
 * backgrounds (e.g. white-on-brand on the splash, brand-on-white on light screens).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = BrandBlue,
    contentColor: Color = White,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.ButtonHeight),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(
            text = text,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
