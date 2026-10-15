package com.vsca.vsnapvoicecollege.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DestructiveRed
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Title + body + Cancel / confirm; [destructive] makes the confirm button red. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    @DrawableRes iconRes: Int? = null,
    iconTint: Color = BrandBlue,
    iconBackground: Color = Color.Transparent,
    note: String? = null,
    dismissLabel: String? = null,
) {
    DialogCard(onDismiss = onDismiss) {
        if (iconRes != null) {
            Box(Modifier.size(64.dp).background(iconBackground, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(title, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text(body, color = TextSecondary, fontSize = AppTextSize.BodyLarge)
        if (note != null) {
            Spacer(Modifier.height(12.dp))
            Text(note, color = TextSecondary, fontSize = AppTextSize.Label)
        }
        Spacer(Modifier.height(20.dp))
        DialogButtons(
            confirm = confirm,
            confirmEnabled = true,
            destructive = destructive,
            dismissLabel = dismissLabel,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

/** Rounded white card shown as a dialog. */
@Composable
fun DialogCard(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = White) {
            Column(modifier = Modifier.padding(24.dp)) { content() }
        }
    }
}

/** Side-by-side Cancel (outlined) and confirm (filled) buttons for dialogs. */
@Composable
fun DialogButtons(
    confirm: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Text(
                dismissLabel ?: stringResource(R.string.common_cancel),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (destructive) DestructiveRed else BrandBlue,
                contentColor = White,
                disabledContainerColor = BrandBlue.copy(alpha = 0.4f),
                disabledContentColor = White,
            ),
        ) {
            Text(confirm, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
