package com.vsca.vsnapvoicecollege.ui.auth.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.PageIndicator
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberDisc
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingCircle
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenRing
import com.vsca.vsnapvoicecollege.ui.theme.PositiveGreen
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.launch

/** A floating stat chip shown around the illustration. */
private data class OnboardingBadge(
    val text: String,
    val textColor: Color,
    val alignment: Alignment,
)

/** UI model for a single onboarding page. */
private data class OnboardingPageUi(
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val iconDescription: String,
    val cardColor: Color,
    val discColor: Color,
    val ringColor: Color,
    val badges: List<OnboardingBadge>,
)

/**
 * App introduction shown after the splash's "Get started". A 3-page pager; the
 * user can Skip or tap Next through to the end, both of which finish onboarding.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    SystemBarIcons(darkIcons = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: "x of N" + Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.onboarding_page_counter,
                    pagerState.currentPage + 1,
                    pages.size,
                ),
                color = TextMuted,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.onboarding_skip),
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onFinish),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        PageIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
        )

        Spacer(Modifier.height(28.dp))

        // Final page shows a green "Get started"; earlier pages a blue "Next".
        PrimaryButton(
            text = stringResource(
                if (isLastPage) R.string.splash_get_started else R.string.onboarding_next,
            ),
            onClick = {
                if (isLastPage) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            containerColor = if (isLastPage) BrandGreen else BrandBlue,
        )

        Spacer(Modifier.height(22.dp))

        PoweredByRow(textColor = TextSecondary, logoBackground = Color.Transparent)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPageUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.3f))

        OnboardingIllustration(page = page)

        Spacer(Modifier.height(20.dp))

        Text(
            text = page.title,
            color = TextPrimary,
            fontSize = AppTextSize.H3,
            lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.subtitle,
            color = TextSecondary,
            fontSize = AppTextSize.Body,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun OnboardingIllustration(
    page: OnboardingPageUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Light backing disc
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(page.discColor),
        )
        // Dashed ring
        Canvas(modifier = Modifier.size(230.dp)) {
            drawCircle(
                color = page.ringColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f),
                ),
            )
        }
        // Center themed card + icon
        LogoBadge(
            iconRes = page.iconRes,
            contentDescription = page.iconDescription,
            size = 112.dp,
            cornerRadius = 30.dp,
            iconSize = 52.dp,
            containerColor = page.cardColor,
            iconTint = White,
        )
        // Floating stat chips
        page.badges.forEach { badge ->
            StatChip(
                text = badge.text,
                textColor = badge.textColor,
                modifier = Modifier
                    .align(badge.alignment)
                    .padding(badge.alignment.toPadding()),
            )
        }
    }
}

@Composable
private fun StatChip(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = White,
        shadowElevation = 6.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = textColor,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Approximate offsets that place each chip around the illustration's ring. */
private fun Alignment.toPadding(): PaddingValues = when (this) {
    Alignment.TopStart -> PaddingValues(start = 4.dp, top = 52.dp)
    Alignment.TopEnd -> PaddingValues(end = 8.dp, top = 34.dp)
    Alignment.BottomEnd -> PaddingValues(end = 14.dp, bottom = 66.dp)
    Alignment.BottomStart -> PaddingValues(start = 8.dp, bottom = 60.dp)
    else -> PaddingValues(0.dp)
}

@Composable
private fun rememberOnboardingPages(): List<OnboardingPageUi> {
    val page1 = OnboardingPageUi(
        title = stringResource(R.string.onboarding_title_1),
        subtitle = stringResource(R.string.onboarding_subtitle_1),
        iconRes = R.drawable.ic_graduation_cap,
        iconDescription = stringResource(R.string.splash_logo_content_desc),
        cardColor = BrandBlue,
        discColor = OnboardingCircle,
        ringColor = DashedRing,
        badges = listOf(
            OnboardingBadge(stringResource(R.string.onboarding_badge_attendance), PositiveGreen, Alignment.TopStart),
            OnboardingBadge(stringResource(R.string.onboarding_badge_ia), TextPrimary, Alignment.TopEnd),
            OnboardingBadge(stringResource(R.string.onboarding_badge_cgpa), TextPrimary, Alignment.BottomEnd),
        ),
    )
    val page2 = OnboardingPageUi(
        title = stringResource(R.string.onboarding_title_2),
        subtitle = stringResource(R.string.onboarding_subtitle_2),
        iconRes = R.drawable.ic_chat,
        iconDescription = stringResource(R.string.onboarding_icon_chat_desc),
        cardColor = BrandGreen,
        discColor = OnboardingGreenDisc,
        ringColor = OnboardingGreenRing,
        badges = listOf(
            OnboardingBadge(stringResource(R.string.onboarding_badge_new_notice), BadgeRed, Alignment.TopStart),
            OnboardingBadge(stringResource(R.string.onboarding_badge_voice), PositiveGreen, Alignment.TopEnd),
            OnboardingBadge(stringResource(R.string.onboarding_badge_unread), BrandBlue, Alignment.BottomStart),
        ),
    )
    val page3 = OnboardingPageUi(
        title = stringResource(R.string.onboarding_title_3),
        subtitle = stringResource(R.string.onboarding_subtitle_3),
        iconRes = R.drawable.ic_bus,
        iconDescription = stringResource(R.string.onboarding_icon_bus_desc),
        cardColor = OnboardingAmberCard,
        discColor = OnboardingAmberDisc,
        ringColor = OnboardingAmberRing,
        badges = listOf(
            OnboardingBadge(stringResource(R.string.onboarding_badge_due), AmberText, Alignment.TopStart),
            OnboardingBadge(stringResource(R.string.onboarding_badge_bus), PositiveGreen, Alignment.TopEnd),
            OnboardingBadge(stringResource(R.string.onboarding_badge_receipt), BrandBlue, Alignment.BottomEnd),
        ),
    )
    return remember { listOf(page1, page2, page3) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingScreenPreview() {
    GRADit_RewampTheme {
        OnboardingScreen(onFinish = {})
    }
}
