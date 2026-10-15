package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlueDark
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** "Exactly what receivers will see", with Edit and Publish. */
@Composable
fun NoticePreviewScreen(
    viewModel: NoticeComposeViewModel,
    onEdit: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notice = viewModel.previewNotice()
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.notice_preview),
            onBack = onEdit,
            trailing = {
                SquareIconButton(R.drawable.ic_share_nodes, stringResource(R.string.notice_share), onClick = { shareNotice(context, notice) }, tint = BrandBlue)
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = BrandBlueDark) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_visibility_outline), contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.notice_preview_banner, recipientWho(uiState)),
                        color = White,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 21.sp,
                    )
                }
            }
            NoticeCard(
                notice = notice,
                time = longTimeLabel(uiState.scheduledAt ?: System.currentTimeMillis()),
                showPinned = notice.pinned,
            )
            AttachmentsSection(notice)
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.background(White)) {
            HorizontalDivider(color = FieldBorder)
            Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.notice_edit), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { if (viewModel.onPublish()) onPublished() else onEdit() },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(publishLabel(uiState), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** "198 students and parents" — who the preview banner says will see it. */
@Composable
private fun recipientWho(state: NoticeComposeUiState): String {
    val roles = NoticeRole.entries.filter { it in state.roles }.map { stringResource(it.label).lowercase() }
    val who = when (roles.size) {
        0 -> ""
        1 -> roles[0]
        else -> stringResource(R.string.notice_and, roles.dropLast(1).joinToString(", "), roles.last())
    }
    return "${state.recipientCount} $who".trim()
}

/** Pills, title, author, and the formatted message — as receivers see it. */
@Composable
internal fun NoticeCard(notice: Notice, time: String, showPinned: Boolean, modifier: Modifier = Modifier, extraPill: @Composable () -> Unit = {}) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(20.dp)) {
            NoticePills(notice, showPinned = showPinned, trailing = extraPill)
            Spacer(Modifier.height(14.dp))
            Text(notice.title, color = TextPrimary, fontSize = AppTextSize.H2, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
            Spacer(Modifier.height(16.dp))
            AuthorRow(notice.author, time)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = FieldBorder)
            Spacer(Modifier.height(14.dp))
            NoticeBody(notice.body)
        }
    }
}

@Composable
internal fun AttachmentsSection(notice: Notice) {
    if (notice.attachments.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.notice_attachments_header, notice.attachments.size),
            color = TextPrimary,
            fontSize = AppTextSize.H6,
            fontWeight = FontWeight.ExtraBold,
        )
        notice.attachments.forEach { AttachmentCard(it) }
    }
}

/** "Notice published / scheduled / updated" with who was reached and how. */
@Composable
fun NoticePublishedScreen(
    viewModel: NoticeComposeViewModel,
    onClose: () -> Unit,
    onViewNotice: (String) -> Unit,
    onCreateAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice = uiState.published ?: return
    val scheduled = notice.scheduledAt != null && notice.scheduledAt > System.currentTimeMillis()
    Column(modifier.fillMaxSize().background(White).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.CenterEnd) {
            Surface(onClick = onClose, shape = CircleShape, color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.notice_close), tint = TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.align(Alignment.CenterHorizontally).size(150.dp).clip(CircleShape).background(OnboardingGreenDisc.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(118.dp).clip(CircleShape).background(OnboardingGreenDisc), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = BrandGreen, modifier = Modifier.size(60.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(
                when {
                    scheduled -> R.string.notice_scheduled_title
                    uiState.editingLive -> R.string.notice_updated_title
                    else -> R.string.notice_published_title
                },
            ),
            Modifier.fillMaxWidth(),
            color = TextPrimary,
            fontSize = AppTextSize.H2,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(
                when {
                    scheduled -> R.string.notice_scheduled_body
                    notice.pinned -> R.string.notice_live_pinned_body
                    else -> R.string.notice_live_body
                },
                notice.title,
                notice.scheduledAt?.let { longTimeLabel(it) }.orEmpty(),
            ),
            Modifier.fillMaxWidth(),
            color = TextSecondary,
            fontSize = AppTextSize.BodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(20.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = ScreenBackground) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                SummaryRow(stringResource(R.string.notice_field_audience), recipientBreakdown(uiState))
                HorizontalDivider(color = FieldBorder)
                SummaryRow(stringResource(R.string.notice_classes), classesLabel(notice.classes))
                HorizontalDivider(color = FieldBorder)
                SummaryRow(
                    stringResource(R.string.notice_notified),
                    stringResource(
                        when {
                            scheduled && notice.smsEnabled -> R.string.notice_push_sms_scheduled
                            scheduled -> R.string.notice_push_scheduled
                            notice.smsEnabled -> R.string.notice_push_sms_sent
                            else -> R.string.notice_push_sent
                        },
                    ),
                )
                HorizontalDivider(color = FieldBorder)
                SummaryRow(stringResource(R.string.notice_expires), notice.expiresAt?.let { dayLabel(it) } ?: stringResource(R.string.notice_no_expiry))
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = { onViewNotice(notice.id) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            Text(stringResource(R.string.notice_view), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCreateAnother,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.notice_create_another), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        }
        PoweredByRow(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 24.dp), textColor = TextSecondary)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(118.dp), color = TextSecondary, fontSize = AppTextSize.BodyLarge)
        Text(value, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
    }
}

/** Used by the recipient summary on detail screens. */
@Composable
internal fun recipientsText(notice: Notice): String =
    pluralStringResource(R.plurals.notice_recipients, notice.recipientCount, notice.recipientCount)
