package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.FormCard
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.RadioBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the "Saved to …" confirmation stays up. */
private const val SAVED_BANNER_MS = 5_000L

private sealed interface DownloadUi {
    data object Idle : DownloadUi
    data object Saving : DownloadUi
    data class Saved(val result: DownloadResult.Saved) : DownloadUi
    data class Failed(val offline: Boolean) : DownloadUi
}

/**
 * A shared file as a parent / student opens it: online preview, description, Share and
 * Download (when the sender allows it).
 */
@Composable
fun FileViewerScreen(fileId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val files by FileRepository.files.collectAsStateWithLifecycle()
    val online by NetworkMonitor.isOnline.collectAsStateWithLifecycle()
    val file = files.firstOrNull { it.id == fileId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var download by remember { mutableStateOf<DownloadUi>(DownloadUi.Idle) }
    var storageFull by remember { mutableStateOf<DownloadResult.StorageFull?>(null) }
    var askStorage by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    LaunchedEffect(fileId) {
        NetworkMonitor.start(context)
        FileRepository.markSeen(fileId)
    }
    LaunchedEffect(download) {
        if (download is DownloadUi.Saved) {
            delay(SAVED_BANNER_MS)
            download = DownloadUi.Idle
        }
    }

    if (file == null) {
        Column(modifier.fillMaxSize().background(ScreenBackground)) {
            TitledTopBar(title = stringResource(R.string.files_title), onBack = onBack)
            Text(stringResource(R.string.files_not_found), modifier = Modifier.padding(32.dp), color = TextSecondary)
        }
        return
    }

    fun startDownload() {
        scope.launch {
            download = DownloadUi.Saving
            download = when (val result = saveToDevice(context, file)) {
                is DownloadResult.Saved -> DownloadUi.Saved(result)
                is DownloadResult.StorageFull -> {
                    storageFull = result
                    DownloadUi.Idle
                }
                DownloadResult.Offline -> DownloadUi.Failed(offline = true)
                DownloadResult.Failed -> DownloadUi.Failed(offline = false)
                DownloadResult.NotAvailable -> {
                    Toast.makeText(context, R.string.files_not_bundled, Toast.LENGTH_SHORT).show()
                    DownloadUi.Idle
                }
            }
        }
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startDownload() else Toast.makeText(context, R.string.files_storage_denied, Toast.LENGTH_SHORT).show()
    }
    val onDownload = {
        val needsPermission = needsLegacyStoragePermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) askStorage = true else startDownload()
    }

    Box(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        Column(Modifier.fillMaxSize()) {
            TitledTopBar(
                title = displayName(file),
                subtitle = stringResource(R.string.files_meta, file.sender, viewerDate(file.publishedAt)),
                onBack = onBack,
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                if (online) {
                    FilePreview(file)
                } else {
                    OfflinePreview(onRetry = {
                        if (!NetworkMonitor.isOnline.value) Toast.makeText(context, R.string.files_still_offline, Toast.LENGTH_SHORT).show()
                    })
                }
                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypeChip(file.format)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.files_meta, fileSizeLabel(file.sizeBytes), stringResource(file.category.label)),
                            color = TextSecondary,
                            fontSize = AppTextSize.Body,
                        )
                    }
                    if (file.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(file.description, color = TextPrimary, fontSize = AppTextSize.BodyLarge, lineHeight = 24.sp)
                    }
                }
                (download as? DownloadUi.Failed)?.let { failed -> DownloadFailedBanner(failed.offline, onRetry = onDownload) }
                PoweredByRow(Modifier.fillMaxWidth().padding(vertical = 12.dp), textColor = TextSecondary)
            }
            Column(Modifier.background(White)) {
                HorizontalDivider(color = FieldBorder)
                Row(modifier = Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showShare = true },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FieldBorder),
                    ) {
                        Icon(painterResource(R.drawable.ic_share), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.files_share), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                    }
                    if (file.downloadAllowed) {
                        Button(
                            onClick = onDownload,
                            enabled = download != DownloadUi.Saving,
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                        ) {
                            if (download == DownloadUi.Saving) {
                                CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.files_download), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        DownloadNotAllowed(Modifier.weight(1f))
                    }
                }
            }
        }

        (download as? DownloadUi.Saved)?.let { saved ->
            SavedBanner(
                toGallery = saved.result.toGallery,
                onOpen = { openSaved(context, saved.result) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
            )
        }
    }

    if (showShare) {
        ShareSheet(file = file, onDismiss = { showShare = false })
    }
    if (askStorage) {
        ConfirmDialog(
            title = stringResource(R.string.files_storage_title),
            body = stringResource(if (file.kind == FileKind.IMAGE) R.string.files_storage_body_photo else R.string.files_storage_body_file),
            note = stringResource(R.string.files_storage_note),
            confirm = stringResource(R.string.files_allow),
            dismissLabel = stringResource(R.string.files_not_now),
            destructive = false,
            iconRes = R.drawable.ic_folder_outline,
            iconTint = BrandBlue,
            iconBackground = SelectedRowBackground,
            onDismiss = { askStorage = false },
            onConfirm = {
                askStorage = false
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            },
        )
    }
    storageFull?.let { full ->
        ConfirmDialog(
            title = stringResource(R.string.files_storage_full_title),
            body = stringResource(
                if (file.kind == FileKind.VIDEO) R.string.files_storage_full_body_video else R.string.files_storage_full_body_file,
                fileSizeLabel(full.neededBytes),
                fileSizeLabel(full.freeBytes),
            ),
            confirm = stringResource(R.string.files_try_again),
            destructive = false,
            iconRes = R.drawable.ic_storage,
            iconTint = BadgeRed,
            iconBackground = AlertBackground,
            onDismiss = { storageFull = null },
            onConfirm = {
                storageFull = null
                onDownload()
            },
        )
    }
}

/** "Today, 09:40 AM" or "05 Sep 2026" for the viewer header. */
@Composable
private fun viewerDate(millis: Long): String =
    if (isDaysAgo(millis, 0)) stringResource(R.string.files_today_at, clockLabel(millis)) else shortDateLabel(millis)

@Composable
private fun DownloadNotAllowed(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ScreenBackground)
            .dashedBorder(color = RadioBorder, cornerRadius = 16.dp, strokeWidth = 1.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.files_download_not_allowed),
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.Body,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun DownloadFailedBanner(offline: Boolean, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AlertBackground) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = BadgeRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.files_download_failed), color = BadgeRed, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    stringResource(if (offline) R.string.files_download_failed_offline else R.string.files_download_failed_body),
                    color = TextPrimary,
                    fontSize = AppTextSize.Label,
                    lineHeight = 19.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, BadgeRed)) {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = BadgeRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.files_retry), color = BadgeRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SavedBanner(toGallery: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = FilesDark, shadowElevation = 8.dp) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (toGallery) R.string.files_saved_gallery else R.string.files_saved_downloads),
                    color = White,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(if (toGallery) R.string.files_saved_gallery_body else R.string.files_saved_downloads_body),
                    color = White.copy(alpha = 0.85f),
                    fontSize = AppTextSize.Label,
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = BrandBlue),
            ) {
                Text(stringResource(R.string.files_open), fontWeight = FontWeight.Bold, fontSize = AppTextSize.BodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(file: SharedFile, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shareFile by remember { mutableStateOf(false) }
    // A copy of the file can only be shared when the sender allows downloads.
    val canShareFile = file.downloadAllowed
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = White) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileTile(file.format, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName(file), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.files_share_sheet), color = TextSecondary, fontSize = AppTextSize.Body)
                }
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.files_close), tint = TextPrimary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = BadgeNeutralBackground) {
                Row(Modifier.padding(4.dp)) {
                    ShareModeTab(stringResource(R.string.files_share_link), selected = !shareFile, Modifier.weight(1f)) { shareFile = false }
                    ShareModeTab(stringResource(R.string.files_share_file), selected = shareFile, Modifier.weight(1f), enabled = canShareFile) { shareFile = true }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(if (shareFile) R.string.files_share_file_note else R.string.files_share_link_note),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val share: (ShareTarget) -> Unit = { target ->
                    if (shareFile) scope.launch { shareFileCopy(context, file, target) } else shareLink(context, file, target)
                }
                ShareTargetButton(R.drawable.ic_chat_bubble_outline, stringResource(R.string.files_share_messages), OnboardingGreenDisc, BrandGreen, Modifier.weight(1f)) {
                    share(ShareTarget.MESSAGES)
                }
                ShareTargetButton(R.drawable.ic_mail_outline, stringResource(R.string.files_share_mail), SelectedRowBackground, BrandBlue, Modifier.weight(1f)) {
                    share(ShareTarget.MAIL)
                }
                ShareTargetButton(R.drawable.ic_link, stringResource(R.string.files_share_copy_link), BadgeNeutralBackground, TextPrimary, Modifier.weight(1f)) {
                    shareLink(context, file, ShareTarget.COPY_LINK)
                }
                ShareTargetButton(R.drawable.ic_more_horiz, stringResource(R.string.files_share_more), BadgeNeutralBackground, TextPrimary, Modifier.weight(1f)) {
                    share(ShareTarget.MORE)
                }
            }
        }
    }
}

@Composable
private fun ShareModeTab(text: String, selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) White else BadgeNeutralBackground,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = when {
                    !enabled -> TextSecondary.copy(alpha = 0.5f)
                    selected -> TextPrimary
                    else -> TextSecondary
                },
                fontSize = AppTextSize.BodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ShareTargetButton(
    @DrawableRes iconRes: Int,
    label: String,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = ScreenBackground) {
        Column(modifier = Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(iconBackground), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
