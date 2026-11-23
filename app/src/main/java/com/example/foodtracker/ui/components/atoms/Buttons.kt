package com.example.foodtracker.ui.components.atoms

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Elevation
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Sizes

/**
 * Actions.
 *
 * Height is 48dp, down from the old 52, and the 56dp `ButtonHeightHero` is
 * gone: onboarding now uses the same button as everywhere else, because a CTA
 * that is bigger only on the first run teaches the wrong size for the button
 * the user will press a thousand times afterwards.
 *
 * All three are pill-shaped and carry **zero elevation**. The filled variant is
 * one of the five fills the house rules allow in the whole app.
 *
 * The 48dp is a **minimum, not a height**. These were the only three controls in
 * the app pinned to an exact `height()` — every other one (`HapticFilterChip`,
 * `ExclusiveButtonGroup`, `RangeBar`, the You and Onboarding rows) already said
 * `heightIn(min = ...)`. A fixed box is fine until the system font scale moves:
 * `AppText.label` is 13sp on an 18sp line, which at the 2.0 scale Android allows
 * becomes a 36sp line box that a 48dp button cannot hold, so the label is
 * squeezed and a wrapped one is cut off. A minimum keeps the tap target and the
 * rhythm at 1.0 — the content is smaller than 48dp there, so nothing moves — and
 * lets the button grow on the devices that need it.
 */

/** The primary action. Filled `primary`, pill, flat. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.none,
            pressedElevation = Elevation.none,
            focusedElevation = Elevation.none,
            hoveredElevation = Elevation.none,
        ),
    ) {
        Text(text, style = AppText.label)
    }
}

/** The secondary action. Hairline outline, no fill. */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
    ) {
        Text(text, style = AppText.label)
    }
}

/**
 * The tertiary action — a text link that still has a real tap target.
 *
 * [destructive] paints the label `error`. It is the one sanctioned exception to
 * house rule 3's "over-limit is the only thing coloured": a confirmation whose
 * two buttons look identical is a confirmation that does not confirm anything,
 * and the irreversible option has to be the one that looks different. It stays
 * a *text* button — a filled error button would make "erase everything" the
 * loudest thing in the dialog, which is the opposite of what a destructive
 * confirm wants.
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        colors = if (destructive) {
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.textButtonColors()
        },
    ) {
        Text(text, style = AppText.label)
    }
}

/**
 * The one spinner. Replaces the Expressive `LoadingIndicator`, which was a
 * shape-morphing animation — banned by the motion rules and, being
 * experimental, one of the APIs phase 1 had to remove anyway.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeWidth = 2.dp,
    )
}

