package com.vsca.vsnapvoicecollege.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder

/** Password strength buckets, each mapping to a number of filled meter segments. */
enum class PasswordStrength(val segments: Int) {
    NONE(0),
    WEAK(1),
    FAIR(2),
    STRONG(3),
    VERY_STRONG(4),
}

private const val STRENGTH_SEGMENTS = 4

/** Length-driven strength: 12+ chars needed for "Strong". */
fun passwordStrengthByLength(password: String): PasswordStrength = when {
    password.isEmpty() -> PasswordStrength.NONE
    password.length < 8 -> PasswordStrength.WEAK
    password.length < 12 -> PasswordStrength.FAIR
    password.length < 16 -> PasswordStrength.STRONG
    else -> PasswordStrength.VERY_STRONG
}

/** The accent color for a given strength (red → amber → green). */
fun passwordStrengthColor(strength: PasswordStrength): Color = when (strength) {
    PasswordStrength.WEAK -> BadgeRed
    PasswordStrength.FAIR -> AmberText
    PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> BrandGreen
    PasswordStrength.NONE -> FieldBorder
}

/** The 4-segment strength bar. */
@Composable
fun PasswordStrengthBar(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val color = passwordStrengthColor(strength)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(STRENGTH_SEGMENTS) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index < strength.segments) color else FieldBorder),
            )
        }
    }
}
