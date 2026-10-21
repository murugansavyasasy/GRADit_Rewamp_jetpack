package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The resume as it will print, with Share and Download PDF. */
@Composable
fun ResumePreviewScreen(studentId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val resumes by ResumeRepository.resumes.collectAsStateWithLifecycle()
    val resume = resumes.firstOrNull { it.studentId == studentId }
    val context = LocalContext.current
    val isStudent = remember { SessionPreferences.getRole(context) == Role.STUDENT }
    // Page count from the real PDF layout, so the subtitle and footer match what downloads.
    val pages by produceState(1, resume) {
        value = resume?.let { r -> withContext(Dispatchers.Default) { ResumePdf.render(context, r).second } } ?: 1
    }
    ResumePreviewContent(
        resume = resume,
        pages = pages,
        isStudent = isStudent,
        onBack = onBack,
        onShare = { resume?.let { ResumePdf.share(context, it) } },
        onDownload = { resume?.let { ResumePdf.download(context, it) } },
        modifier = modifier,
    )
}

@Composable
private fun ResumePreviewContent(
    resume: Resume?,
    pages: Int,
    isStudent: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.resume_preview),
            subtitle = resume?.let {
                stringResource(R.string.resume_preview_subtitle, stringResource(it.template.label), pluralStringResource(R.plurals.resume_pages, pages, pages))
            },
            onBack = onBack,
        )
        if (resume == null) {
            Text(stringResource(R.string.resume_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(4.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = White, shadowElevation = 6.dp) {
                ResumeDocument(resume, Modifier.padding(horizontal = 18.dp, vertical = 22.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (isStudent) {
                    stringResource(R.string.resume_preview_footer_student, pages)
                } else {
                    stringResource(R.string.resume_preview_footer_parent, pages, resume.firstName)
                },
                Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
        ActionBar(
            onPreview = onShare,
            onDownload = onDownload,
            previewIcon = R.drawable.ic_share_nodes,
            previewLabel = stringResource(R.string.resume_share),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResumePreviewPreview() {
    GRADit_RewampTheme {
        ResumePreviewContent(ResumeRepository.resumes.value[0], pages = 1, isStudent = false, onBack = {}, onShare = {}, onDownload = {})
    }
}
