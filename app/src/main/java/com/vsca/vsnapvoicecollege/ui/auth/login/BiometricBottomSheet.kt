package com.vsca.vsnapvoicecollege.ui.auth.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.AppDimens
import com.vsca.vsnapvoicecollege.ui.theme.BiometricHaloLight
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Bottom sheet that prompts the user to sign in with their fingerprint.
 *
 * UI only for now — [onUsePassword] and [onDismiss] close the sheet. Hook up a
 * real androidx.biometric BiometricPrompt where [onDismiss]/auth is handled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricBottomSheet(
    userName: String,
    maskedId: String,
    onCancel: () -> Unit,
    onUsePassword: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_biometric_title),
                color = TextPrimary,
                fontSize = AppTextSize.H5,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "$userName · $maskedId",
                color = TextSecondary,
                fontSize = AppTextSize.Label,
            )

            Spacer(Modifier.height(24.dp))

            FingerprintHalo()

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.login_biometric_touch),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.login_biometric_hint),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDimens.ButtonHeight),
                    shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
                    border = BorderStroke(1.dp, FieldBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                ) {
                    Text(
                        text = stringResource(R.string.login_biometric_cancel),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onUsePassword,
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDimens.ButtonHeight),
                    shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SelectedRowBackground,
                        contentColor = BrandBlue,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.login_biometric_use_password),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FingerprintHalo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(BiometricHaloLight),
        )
        Box(
            Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(OnboardingGreenDisc),
        )
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(BrandGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fingerprint),
                contentDescription = stringResource(R.string.login_biometric_icon_desc),
                tint = White,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}
