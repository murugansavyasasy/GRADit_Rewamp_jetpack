package com.vsca.vsnapvoicecollege.ui.auth.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.PageIndicator
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DecorativeStroke
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.White
import com.vsca.vsnapvoicecollege.ui.theme.WhiteMuted
import com.vsca.vsnapvoicecollege.ui.theme.WhiteTranslucent

/**
 * Splash / app launch screen. The user taps "Get started" to begin the app
 * introduction (onboarding). Already-authenticated users skip straight to home.
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Brand-blue background → light system-bar icons.
    SystemBarIcons(darkIcons = false)

    LaunchedEffect(uiState.isReady, uiState.isAuthenticated) {
        if (uiState.isReady && uiState.isAuthenticated) {
            onNavigateToHome()
        }
    }

    SplashContent(
        onGetStarted = onGetStarted,
        modifier = modifier,
    )
}

@Composable
private fun SplashContent(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBlue),
    ) {
        DecorativeCircles(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.9f))

            // White card with blue cap — the splash variant of the logo badge.
            LogoBadge(containerColor = White, iconTint = BrandBlue)

            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.splash_college_name),
                color = White,
                fontSize = AppTextSize.H2,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.splash_location),
                color = White,
                fontSize = AppTextSize.H6,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.splash_tagline),
                color = WhiteMuted,
                fontSize = AppTextSize.Label,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1.3f))

            PageIndicator(
                pageCount = 3,
                currentPage = 0,
                activeColor = White,
                inactiveColor = WhiteTranslucent,
            )

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = stringResource(R.string.splash_get_started),
                onClick = onGetStarted,
                // Inverted colors: white button on the brand-blue background.
                containerColor = White,
                contentColor = BrandBlue,
            )

            Spacer(Modifier.height(22.dp))

            PoweredByRow(textColor = White, logoBackground = White)

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Faint decorative circle outlines, matching the design's background motif.
 */
@Composable
private fun DecorativeCircles(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        // Top-right arc
        drawCircle(
            color = DecorativeStroke,
            radius = size.width * 0.62f,
            center = Offset(x = size.width * 0.9f, y = size.height * 0.02f),
            style = stroke,
        )
        // Bottom-left arc
        drawCircle(
            color = DecorativeStroke,
            radius = size.width * 0.72f,
            center = Offset(x = size.width * 0.05f, y = size.height * 0.78f),
            style = stroke,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashContentPreview() {
    GRADit_RewampTheme {
        SplashContent(onGetStarted = {})
    }
}
