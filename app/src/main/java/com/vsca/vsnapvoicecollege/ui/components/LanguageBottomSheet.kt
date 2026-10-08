package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Bottom sheet for changing the app language. Shows a 2-column grid of languages;
 * the user picks one and taps "Apply". Reused by login and settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageBottomSheet(
    languages: List<Language>,
    selectedCode: String,
    onApply: (Language) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var tempSelectedCode by remember(selectedCode) { mutableStateOf(selectedCode) }
    val tempSelected = languages.firstOrNull { it.code == tempSelectedCode } ?: languages.first()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.language_title),
                        color = TextPrimary,
                        fontSize = AppTextSize.H4,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.language_subtitle),
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                    )
                }
                CloseButton(onClick = onDismiss)
            }

            Spacer(Modifier.height(20.dp))

            languages.chunked(2).forEach { rowLanguages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowLanguages.forEach { language ->
                        LanguageCard(
                            language = language,
                            selected = language.code == tempSelectedCode,
                            onClick = { tempSelectedCode = language.code },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowLanguages.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = stringResource(R.string.language_apply, tempSelected.englishName),
                onClick = { onApply(tempSelected) },
            )
        }
    }
}

@Composable
private fun LanguageCard(
    language: Language,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandBlue else FieldBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    color = if (selected) BrandBlue else TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = language.englishName,
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                )
            }
            Spacer(Modifier.size(8.dp))
            SelectionRadio(selected = selected)
        }
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.close_content_desc),
            tint = TextPrimary,
            modifier = Modifier.padding(8.dp),
        )
    }
}
