package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AttachmentIcon
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.IMAGE_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.PDF_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.readAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * "New post" form: subject, required title, optional description, an image / PDF
 * attachment and a toggle to alert the class teacher. "Post" stays disabled until a
 * title is entered.
 */
@Composable
fun NewPostScreen(
    onClose: () -> Unit,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewPostViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NewPostContent(
        uiState = uiState,
        onClose = onClose,
        onPost = { if (viewModel.onPost()) onPosted() },
        onSubjectSelected = viewModel::onSubjectSelected,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAttachmentPicked = viewModel::onAttachmentPicked,
        onRemoveAttachment = viewModel::onRemoveAttachment,
        onNotifyTeacherChange = viewModel::onNotifyTeacherChange,
        modifier = modifier,
    )
}

@Composable
private fun NewPostContent(
    uiState: NewPostUiState,
    onClose: () -> Unit,
    onPost: () -> Unit,
    onSubjectSelected: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAttachmentPicked: (FileAttachment) -> Unit,
    onRemoveAttachment: () -> Unit,
    onNotifyTeacherChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAttachment(context, it) }?.let(onAttachmentPicked)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(
                onClick = onClose,
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.discussion_close_desc),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.discussion_new_post),
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
            Button(
                onClick = onPost,
                enabled = uiState.canPost,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = White,
                    disabledContainerColor = FieldBorder,
                    disabledContentColor = TextSecondary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.discussion_post),
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider(color = FieldBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.discussion_subject_label))
            Spacer(Modifier.height(10.dp))
            DropdownField(
                value = uiState.subject,
                options = uiState.subjects,
                onOptionSelected = onSubjectSelected,
            )

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.discussion_title_label), required = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.discussion_title_hint), color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = appTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            HelperText(
                text = if (uiState.title.isEmpty()) {
                    stringResource(R.string.discussion_title_helper)
                } else {
                    stringResource(R.string.discussion_char_count, uiState.title.length, NewPostUiState.MAX_TITLE_LENGTH)
                },
            )

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.discussion_description_label))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                placeholder = { Text(stringResource(R.string.discussion_description_hint), color = TextMuted) },
                shape = RoundedCornerShape(14.dp),
                colors = appTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            HelperText(
                text = stringResource(
                    R.string.discussion_char_count,
                    uiState.description.length,
                    NewPostUiState.MAX_DESCRIPTION_LENGTH,
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.discussion_attach_label))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AttachButton(
                    iconRes = R.drawable.ic_image_outline,
                    label = stringResource(R.string.discussion_attach_image),
                    onClick = { picker.launch(IMAGE_MIME_TYPES) },
                    modifier = Modifier.weight(1f),
                )
                AttachButton(
                    iconRes = R.drawable.ic_file,
                    label = stringResource(R.string.discussion_attach_pdf),
                    onClick = { picker.launch(PDF_MIME_TYPES) },
                    modifier = Modifier.weight(1f),
                )
            }
            uiState.attachment?.let { attachment ->
                Spacer(Modifier.height(12.dp))
                AttachedFileCard(attachment = attachment, onRemove = onRemoveAttachment)
            }
            Spacer(Modifier.height(10.dp))
            HelperText(stringResource(R.string.discussion_attach_helper))

            Spacer(Modifier.height(20.dp))
            NotifyTeacherCard(
                teacher = uiState.classTeacher,
                checked = uiState.notifyTeacher,
                onCheckedChange = onNotifyTeacherChange,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormLabel(text: String, required: Boolean = false) {
    Text(
        text = buildAnnotatedString {
            append(text)
            if (required) {
                append(" ")
                withStyle(SpanStyle(color = BadgeRed)) {
                    append(stringResource(R.string.discussion_required))
                }
            }
        },
        color = TextPrimary,
        fontSize = AppTextSize.Body,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun HelperText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = TextSecondary,
        fontSize = AppTextSize.Caption,
        textAlign = textAlign,
    )
}

@Composable
private fun AttachButton(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ScreenBackground)
            .dashedBorder(color = DashedRing, cornerRadius = 14.dp, strokeWidth = 1.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = BrandBlue,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AttachedFileCard(attachment: FileAttachment, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentIcon(attachment.type, size = 26.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    color = TextPrimary,
                    fontSize = AppTextSize.Body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = fileSizeLabel(attachment.sizeBytes),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.discussion_remove_attachment_desc),
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun NotifyTeacherCard(
    teacher: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ScreenBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.discussion_notify_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.Body,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.discussion_notify_subtitle, teacher),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = BrandGreen,
                    checkedBorderColor = BrandGreen,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NewPostEmptyPreview() {
    GRADit_RewampTheme {
        NewPostContent(
            uiState = NewPostUiState(),
            onClose = {},
            onPost = {},
            onSubjectSelected = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onAttachmentPicked = {},
            onRemoveAttachment = {},
            onNotifyTeacherChange = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NewPostFilledPreview() {
    GRADit_RewampTheme {
        NewPostContent(
            uiState = NewPostUiState(
                title = "Inserting a node in the middle of a linked list",
                description = "I understand insertion at the head and tail, but I get stuck when the new node " +
                    "has to go in the middle. Which pointer should be updated first?",
                attachment = FileAttachment("Notes.pdf", 420 * 1024L, AttachmentType.PDF),
            ),
            onClose = {},
            onPost = {},
            onSubjectSelected = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onAttachmentPicked = {},
            onRemoveAttachment = {},
            onNotifyTeacherChange = {},
        )
    }
}
