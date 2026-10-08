package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Row
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Fixed-length numeric OTP input rendered as a row of individual cells. A single
 * hidden text field captures input; the active (next-to-fill) cell is highlighted.
 */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= length && new.all(Char::isDigit)) onValueChange(new)
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = { _ ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(length) { index ->
                    OtpCell(
                        char = value.getOrNull(index)?.toString().orEmpty(),
                        active = index == value.length,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun OtpCell(
    char: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasChar = char.isNotEmpty()
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (hasChar || active) BrandBlue else FieldBorder
    val borderWidth = if (hasChar || active) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(if (hasChar) SelectedRowBackground else White)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (hasChar) {
            Text(
                text = char,
                color = TextPrimary,
                fontSize = AppTextSize.H5,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
