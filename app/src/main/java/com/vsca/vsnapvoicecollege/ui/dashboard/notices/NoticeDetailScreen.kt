package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.widget.Toast
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.FormCard
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

private enum class NoticeAction(@param:DrawableRes val icon: Int) {
    EDIT(R.drawable.ic_edit),
    PUBLISH_NOW(R.drawable.ic_send),
    PIN(R.drawable.ic_place_outline),
    RESEND(R.drawable.ic_send),
    DUPLICATE(R.drawable.ic_content_copy),
    EXPIRE(R.drawable.ic_block),
    EXPORT(R.drawable.ic_download),
    DELETE(R.drawable.ic_delete),
}

/** Which actions make sense for a notice in each state. */
private fun actionsFor(status: NoticeStatus): List<NoticeAction> = when (status) {
    NoticeStatus.PUBLISHED -> listOf(NoticeAction.EDIT, NoticeAction.PIN, NoticeAction.RESEND, NoticeAction.DUPLICATE, NoticeAction.EXPIRE, NoticeAction.EXPORT, NoticeAction.DELETE)
    NoticeStatus.SCHEDULED -> listOf(NoticeAction.EDIT, NoticeAction.PUBLISH_NOW, NoticeAction.PIN, NoticeAction.DUPLICATE, NoticeAction.DELETE)
    NoticeStatus.DRAFT -> listOf(NoticeAction.EDIT, NoticeAction.DUPLICATE, NoticeAction.DELETE)
    NoticeStatus.EXPIRED -> listOf(NoticeAction.DUPLICATE, NoticeAction.EXPORT, NoticeAction.DELETE)
}

/**
 * A notice as management / faculty see it: status, audience, timings and the actions
 * that fit its state (edit, pin, resend, duplicate, expire, export, delete).
 */
@Composable
fun NoticeDetailScreen(
    noticeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notices by NoticeRepository.notices.collectAsStateWithLifecycle()
    val notice = notices.firstOrNull { it.id == noticeId }
    val context = LocalContext.current
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    if (notice == null) {
        Column(modifier.fillMaxSize().background(ScreenBackground)) {
            TitledTopBar(title = stringResource(R.string.notice_detail_title), onBack = onBack)
            Text(stringResource(R.string.notice_not_found), Modifier.padding(32.dp), color = TextSecondary)
        }
        return
    }
    val status = notice.status()
    val me = NoticeRepository.currentAuthor(SessionPreferences.getRole(context))

    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(title = stringResource(R.string.notice_detail_title), onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            FormCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NoticePills(notice, showPinned = notice.pinned && status != NoticeStatus.EXPIRED, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatusPill(status, withIcon = false)
                }
                Spacer(Modifier.height(14.dp))
                Text(notice.title, color = TextPrimary, fontSize = AppTextSize.H2, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
                Spacer(Modifier.height(16.dp))
                DetailLine(
                    R.drawable.ic_people_outline,
                    stringResource(R.string.notice_field_audience),
                    if (notice.classes.isEmpty()) stringResource(R.string.notice_no_audience)
                    else stringResource(R.string.notice_audience_full, recipientsText(notice), classesLabel(notice.classes), rolesLabel(notice.roles)),
                )
                val byWhom = if (notice.author.name == me.name) stringResource(R.string.notice_by_you) else stringResource(R.string.notice_by, notice.author.name)
                when (status) {
                    NoticeStatus.SCHEDULED -> DetailLine(R.drawable.ic_calendar, stringResource(R.string.notice_scheduled_for), "${longTimeLabel(notice.effectiveTime)} · $byWhom")
                    NoticeStatus.DRAFT -> DetailLine(R.drawable.ic_calendar, stringResource(R.string.notice_last_edited), "${longTimeLabel(notice.createdAt)} · $byWhom")
                    else -> DetailLine(R.drawable.ic_calendar, stringResource(R.string.notice_published), "${longTimeLabel(notice.effectiveTime)} · $byWhom")
                }
                DetailLine(
                    R.drawable.ic_clock,
                    stringResource(if (status == NoticeStatus.EXPIRED) R.string.notice_expired else R.string.notice_expires),
                    when {
                        notice.expiresAt == null -> stringResource(R.string.notice_no_expiry)
                        notice.pinned && status != NoticeStatus.EXPIRED -> stringResource(R.string.notice_pinned_until, dayLabel(notice.expiresAt))
                        else -> dayLabel(notice.expiresAt)
                    },
                )
                DetailLine(
                    R.drawable.ic_notifications,
                    stringResource(R.string.notice_notified_by),
                    stringResource(if (notice.smsEnabled) R.string.notice_push_and_sms else R.string.notice_push_only),
                )
                if (status == NoticeStatus.PUBLISHED || status == NoticeStatus.EXPIRED) {
                    Spacer(Modifier.height(16.dp))
                    ReadProgress(notice.readCount, notice.recipientCount)
                }
            }
            ActionGrid(actionsFor(status), pinned = notice.pinned) { action ->
                when (action) {
                    NoticeAction.EDIT -> onEdit(notice.id)
                    NoticeAction.DUPLICATE -> onDuplicate(notice.id)
                    NoticeAction.PIN -> {
                        NoticeRepository.setPinned(notice.id, !notice.pinned)
                        Toast.makeText(context, if (notice.pinned) R.string.notice_unpinned_toast else R.string.notice_pinned_toast, Toast.LENGTH_SHORT).show()
                    }
                    NoticeAction.RESEND -> {
                        val count = NoticeRepository.resend(notice.id)
                        Toast.makeText(
                            context,
                            if (count == 0) context.getString(R.string.notice_resend_none)
                            else context.resources.getQuantityString(R.plurals.notice_resend_done, count, count),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    NoticeAction.PUBLISH_NOW -> confirm = NoticeAction.PUBLISH_NOW.name
                    NoticeAction.EXPIRE -> confirm = NoticeAction.EXPIRE.name
                    NoticeAction.EXPORT -> exportReadReport(context, notice)
                    NoticeAction.DELETE -> confirm = NoticeAction.DELETE.name
                }
            }
            FormCard {
                Text(stringResource(R.string.notice_field_message), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                if (notice.body.isBlank()) {
                    Text(stringResource(R.string.notice_no_message), color = TextSecondary, fontSize = AppTextSize.Body)
                } else {
                    NoticeBody(notice.body)
                }
                notice.attachments.forEach {
                    Spacer(Modifier.height(14.dp))
                    AttachmentCard(it)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }

    when (confirm) {
        NoticeAction.DELETE.name -> ConfirmDialog(
            title = stringResource(R.string.notice_delete_title),
            body = if (status == NoticeStatus.DRAFT || status == NoticeStatus.SCHEDULED) {
                stringResource(R.string.notice_delete_unsent_body)
            } else {
                pluralStringResource(R.plurals.notice_delete_body, notice.recipientCount, notice.recipientCount)
            },
            confirm = stringResource(R.string.notice_delete),
            destructive = true,
            iconRes = R.drawable.ic_delete,
            iconTint = BadgeRed,
            iconBackground = AlertBackground,
            onDismiss = { confirm = null },
            onConfirm = {
                confirm = null
                NoticeRepository.delete(notice.id)
                Toast.makeText(context, R.string.notice_deleted_toast, Toast.LENGTH_SHORT).show()
                onBack()
            },
        )
        NoticeAction.EXPIRE.name -> ConfirmDialog(
            title = stringResource(R.string.notice_expire_title),
            body = stringResource(R.string.notice_expire_body),
            confirm = stringResource(R.string.notice_expire_now),
            destructive = true,
            iconRes = R.drawable.ic_block,
            iconTint = BadgeRed,
            iconBackground = AlertBackground,
            onDismiss = { confirm = null },
            onConfirm = {
                confirm = null
                NoticeRepository.expireNow(notice.id)
                Toast.makeText(context, R.string.notice_expired_toast, Toast.LENGTH_SHORT).show()
            },
        )
        NoticeAction.PUBLISH_NOW.name -> ConfirmDialog(
            title = stringResource(R.string.notice_publish_now_title),
            body = pluralStringResource(R.plurals.notice_publish_now_body, notice.recipientCount, notice.recipientCount),
            confirm = stringResource(R.string.notice_publish),
            destructive = false,
            onDismiss = { confirm = null },
            onConfirm = {
                confirm = null
                NoticeRepository.publishNow(notice.id)
                Toast.makeText(context, R.string.notice_published_toast, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun DetailLine(@DrawableRes iconRes: Int, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(painterResource(iconRes), contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(top = 2.dp).size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = AppTextSize.Body)
            Text(value, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
        }
    }
}

@Composable
private fun ActionGrid(actions: List<NoticeAction>, pinned: Boolean, onAction: (NoticeAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { action ->
                    val destructive = action == NoticeAction.DELETE
                    Surface(
                        onClick = { onAction(action) },
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = White,
                        border = BorderStroke(1.dp, if (destructive) BadgeRed.copy(alpha = 0.35f) else FieldBorder),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                painterResource(if (action == NoticeAction.PIN && pinned) R.drawable.ic_place_outline else action.icon),
                                contentDescription = null,
                                tint = if (destructive) BadgeRed else BrandBlue,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(actionLabel(action, pinned)),
                                color = if (destructive) BadgeRed else TextPrimary,
                                fontSize = AppTextSize.Body,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private fun actionLabel(action: NoticeAction, pinned: Boolean): Int = when (action) {
    NoticeAction.EDIT -> R.string.notice_edit
    NoticeAction.PUBLISH_NOW -> R.string.notice_publish_now_action
    NoticeAction.PIN -> if (pinned) R.string.notice_unpin else R.string.notice_pin
    NoticeAction.RESEND -> R.string.notice_resend
    NoticeAction.DUPLICATE -> R.string.notice_duplicate
    NoticeAction.EXPIRE -> R.string.notice_expire_now
    NoticeAction.EXPORT -> R.string.notice_export
    NoticeAction.DELETE -> R.string.notice_delete
}

