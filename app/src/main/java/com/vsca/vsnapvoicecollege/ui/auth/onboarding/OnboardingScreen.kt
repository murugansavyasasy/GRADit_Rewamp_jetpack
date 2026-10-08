package com.vsca.vsnapvoicecollege.ui.auth.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.IconBadge
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.IndicatorInactive
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberDisc
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingCircle
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

/**
 * App introduction shown after the splash's "Get started". A four-page pager with
 * a themed hero illustration per page; Skip or the final "Get started" finishes.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    SystemBarIcons(darkIcons = true)

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(White),
    ) { index ->
        OnboardingPageView(
            index = index,
            onBack = { scope.launch { pagerState.animateScrollToPage(index - 1) } },
            onSkip = onFinish,
            onNext = {
                if (index == PAGE_COUNT - 1) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(index + 1) }
                }
            },
        )
    }
}

@Composable
private fun OnboardingPageView(
    index: Int,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val isLast = index == PAGE_COUNT - 1
    val accent = if (isLast) BrandGreen else BrandBlue

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero: themed tinted area with decorative corner bubbles, the top bar
        // and the illustration.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(heroColor(index)),
        ) {
            // Soft darker bubbles bleeding from the top-right and bottom-left.
            Canvas(modifier = Modifier.matchParentSize()) {
                val bubble = accent.copy(alpha = 0.16f)
                drawCircle(
                    color = bubble,
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.96f, size.height * 0.04f),
                )
                drawCircle(
                    color = bubble,
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.04f, size.height * 0.98f),
                )
            }

            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (index == 0) BrandRow() else BackButton(onClick = onBack)
                    if (!isLast) SkipButton(onClick = onSkip)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OnboardingIllustration(index)
                }
            }
        }

        // Content: progress, pill, title, subtitle, CTA.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(22.dp))
            ProgressRow(current = index)

            Spacer(Modifier.height(16.dp))
            Pill(text = stringResource(pillRes(index)), background = pillBg(index), textColor = pillFg(index))

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(titleRes(index)),
                color = TextPrimary,
                fontSize = AppTextSize.H2,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(subtitleRes(index)),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(if (isLast) R.string.onboarding_get_started else R.string.onboarding_next),
                onClick = onNext,
                containerColor = accent,
                trailingIconRes = R.drawable.ic_arrow_forward,
            )

            Spacer(Modifier.height(16.dp))
            PoweredByRow(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textColor = TextSecondary,
                logoBackground = Color.Transparent,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ----------------------------- Top bar pieces ----------------------------- */

@Composable
private fun BrandRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoBadge(
            size = 38.dp,
            cornerRadius = 11.dp,
            iconSize = 22.dp,
            containerColor = BrandBlue,
            iconTint = White,
            shadowElevation = 0.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.app_name),
            color = TextPrimary,
            fontSize = AppTextSize.H6,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = White,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = stringResource(R.string.onboarding_skip),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = TextPrimary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ----------------------------- Content pieces ----------------------------- */

@Composable
private fun ProgressRow(current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(PAGE_COUNT) { i ->
                val color = when {
                    i == current -> BrandBlue
                    i < current -> BrandBlue.copy(alpha = 0.35f)
                    else -> IndicatorInactive
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.onboarding_page_counter, current + 1, PAGE_COUNT),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Pill(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = textColor,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ------------------------------ Illustrations ----------------------------- */

@Composable
private fun OnboardingIllustration(index: Int) {
    when (index) {
        0 -> VoiceIllustration()
        1 -> GeoIllustration()
        2 -> ResumeIllustration()
        else -> GridIllustration()
    }
}

@Composable
private fun HeroCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = White,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun VoiceIllustration() {
    Column {
        HeroCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BadgeRed),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_rec_recording),
                        color = BadgeRed,
                        fontSize = AppTextSize.Caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_rec_to_parents),
                    color = BrandBlue,
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))
            Waveform()
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(R.drawable.ic_delete, stringResource(R.string.onboarding_rec_delete_desc), White, TextSecondary, 48.dp, border = true)
                Spacer(Modifier.width(20.dp))
                CircleButton(R.drawable.ic_mic, stringResource(R.string.onboarding_rec_mic_desc), BrandBlue, White, 64.dp)
                Spacer(Modifier.width(20.dp))
                CircleButton(R.drawable.ic_send, stringResource(R.string.onboarding_rec_send_desc), BrandGreen, White, 48.dp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = White,
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OnboardingGreenDisc),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_rec_heard_title),
                        color = TextPrimary,
                        fontSize = AppTextSize.Label,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_rec_heard_sub),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
            }
        }
    }
}

@Composable
private fun Waveform() {
    // Heights (in dp) for the bars; the trailing ones are faded.
    val heights = listOf(10, 18, 26, 14, 30, 22, 34, 20, 28, 16, 24, 12, 20, 14, 10, 16, 12, 8)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEachIndexed { i, h ->
            val color = if (i < 12) BrandBlue else BrandBlue.copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Composable
private fun CircleButton(
    @DrawableRes iconRes: Int,
    description: String,
    background: Color,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    border: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border) Modifier.border(1.dp, FieldBorder, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

@Composable
private fun GeoIllustration() {
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = White,
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(OnboardingGreenDisc),
            ) {
                // Faint grid + dashed geo-fence ring.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = size.width / 7f
                    var x = step
                    while (x < size.width) {
                        drawLine(White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(White, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                    drawCircle(
                        color = BrandGreen,
                        radius = size.minDimension * 0.38f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                        ),
                    )
                }
                Pill(
                    text = stringResource(R.string.onboarding_geo_campus),
                    background = White,
                    textColor = BrandGreen,
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(BrandGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_on),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(50),
                    color = BrandGreen,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_geo_inside),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = White,
                        fontSize = AppTextSize.CaptionSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = White,
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(
                    iconRes = R.drawable.ic_location_on,
                    contentDescription = null,
                    size = 36.dp,
                    cornerRadius = 10.dp,
                    iconSize = 18.dp,
                    containerColor = OnboardingGreenDisc,
                    iconTint = BrandGreen,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_geo_checkin_title),
                        color = TextPrimary,
                        fontSize = AppTextSize.Label,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_geo_checkin_sub),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Pill(
                    text = stringResource(R.string.onboarding_geo_ontime),
                    background = OnboardingGreenDisc,
                    textColor = BrandGreen,
                )
            }
        }
    }
}

@Composable
private fun ResumeIllustration() {
    Column {
        HeroCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OnboardingAmberCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_resume_initials),
                        color = White,
                        fontSize = AppTextSize.Label,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_resume_name),
                        color = TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_resume_detail),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FieldBorder),
            )
            Spacer(Modifier.height(12.dp))

            MiniLabel(stringResource(R.string.onboarding_resume_skills))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkillChip(stringResource(R.string.onboarding_resume_skill_1))
                SkillChip(stringResource(R.string.onboarding_resume_skill_2))
                SkillChip(stringResource(R.string.onboarding_resume_skill_3))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                SkillChip(stringResource(R.string.onboarding_resume_skill_4))
            }

            Spacer(Modifier.height(14.dp))
            MiniLabel(stringResource(R.string.onboarding_resume_project_label))
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_resume_project),
                color = TextPrimary,
                fontSize = AppTextSize.Caption,
            )
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = White,
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_resume_progress),
                        color = TextPrimary,
                        fontSize = AppTextSize.Caption,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AmberChipBackground),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(OnboardingAmberCard),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Surface(shape = RoundedCornerShape(50), color = BrandBlue) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.onboarding_resume_pdf),
                            color = White,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = AppTextSize.CaptionSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun SkillChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = AmberChipBackground) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = OnboardingAmberCard,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GridIllustration() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(
                iconRes = R.drawable.ic_calendar,
                iconBg = SelectedRowBackground,
                iconTint = BrandBlue,
                label = stringResource(R.string.onboarding_grid_attendance_label),
                value = stringResource(R.string.onboarding_grid_attendance_value),
                modifier = Modifier.weight(1f),
            )
            GridCard(
                iconRes = R.drawable.ic_credit_card,
                iconBg = AmberChipBackground,
                iconTint = AmberText,
                label = stringResource(R.string.onboarding_grid_fee_label),
                value = stringResource(R.string.onboarding_grid_fee_value),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(
                iconRes = R.drawable.ic_folder,
                iconBg = AlertBackground,
                iconTint = BadgeRed,
                label = stringResource(R.string.onboarding_grid_files_label),
                value = stringResource(R.string.onboarding_grid_files_value),
                modifier = Modifier.weight(1f),
            )
            GridCard(
                iconRes = R.drawable.ic_calendar,
                iconBg = OnboardingGreenDisc,
                iconTint = BrandGreen,
                label = stringResource(R.string.onboarding_grid_events_label),
                value = stringResource(R.string.onboarding_grid_events_value),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GridCard(
    @DrawableRes iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = White,
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            IconBadge(
                iconRes = iconRes,
                contentDescription = null,
                size = 40.dp,
                cornerRadius = 12.dp,
                iconSize = 20.dp,
                containerColor = iconBg,
                iconTint = iconTint,
            )
            Spacer(Modifier.height(12.dp))
            Text(text = label, color = TextSecondary, fontSize = AppTextSize.Caption)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
            )
        }
    }
}

/* ------------------------------- Page config ------------------------------ */

private fun heroColor(index: Int): Color = when (index) {
    1 -> OnboardingGreenDisc
    2 -> OnboardingAmberDisc
    else -> OnboardingCircle
}

private fun pillBg(index: Int): Color = when (index) {
    1 -> OnboardingGreenDisc
    2 -> AmberChipBackground
    else -> SelectedRowBackground
}

private fun pillFg(index: Int): Color = when (index) {
    1 -> BrandGreen
    2 -> OnboardingAmberCard
    else -> BrandBlue
}

private fun pillRes(index: Int): Int = when (index) {
    0 -> R.string.onboarding_pill_1
    1 -> R.string.onboarding_pill_2
    2 -> R.string.onboarding_pill_3
    else -> R.string.onboarding_pill_4
}

private fun titleRes(index: Int): Int = when (index) {
    0 -> R.string.onboarding_title_1
    1 -> R.string.onboarding_title_2
    2 -> R.string.onboarding_title_3
    else -> R.string.onboarding_title_4
}

private fun subtitleRes(index: Int): Int = when (index) {
    0 -> R.string.onboarding_subtitle_1
    1 -> R.string.onboarding_subtitle_2
    2 -> R.string.onboarding_subtitle_3
    else -> R.string.onboarding_subtitle_4
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingScreenPreview() {
    GRADit_RewampTheme {
        OnboardingScreen(onFinish = {})
    }
}
