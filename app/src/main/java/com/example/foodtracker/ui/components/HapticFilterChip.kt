package com.example.foodtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Elevation
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Spacing

/**
 * A pill's minimum height — see the note at the end of [HapticFilterChip]'s
 * documentation for why it is not the 48dp touch target.
 */
private val MinHeight = 40.dp

/**
 * The chip, revised by the editorial pass.
 *
 * **Why it is hand-rolled.** It used to wrap Material's `FilterChip`, which
 * brings a 32dp height, an 8dp corner, a tonal `secondaryContainer` fill when
 * selected and a border whose colour is derived from the chip's own token set.
 * That is four fights with the house rules for a control that is a rounded rectangle
 * with two states, and on `#000000` the tonal fill in particular reads as a
 * lift — which the house rules ban.
 *
 * **Why the selected state is monochrome now.** It was a filled
 * `primaryContainer` block, and house rule 3 is that the accent is for data,
 * not chrome: `primary` has to keep meaning *on track*, which it cannot do
 * while it is also the colour of "this chip is the one you tapped". A filled
 * mint block is also the single loudest shape on a screen whose structure is
 * otherwise hairlines — four of them in a scrolling variant row read as the
 * Direction A pill stack this pass exists to remove.
 *
 * So selection is carried by weight instead of hue: the hairline brightens from
 * `outlineVariant` to `onSurface` and the label goes from `onSurfaceVariant` to
 * full `onSurface`. Both states are the same geometry and the same (absent)
 * fill, so a row of chips reads as one object with one member emphasised rather
 * than as a block and some outlines. It is the same move `DayStrip` makes with
 * its 2dp rule — a chip cannot wear a rule under it inside a scrolling row, but
 * a brightened edge says the same thing in the same language.
 *
 * **The corner comes down to [Radius.md].** House rule 6 reserves `Radius.pill` for the
 * FAB and primary buttons; a chip is neither, and a 999dp corner is most of
 * what made these read as the old app's filter pills. Callers that genuinely
 * need another shape still pass one.
 *
 * **The haptic stays, and the name with it.** The tick is a good idea and
 * keeps it; the name survives because this is called from six places and a
 * rename would be a diff across files other hands are in.
 *
 * Unlike a segmented control, a chip ticks on *every* tap, not only on a change
 * of selection: its callers include toggles where tapping the selected chip
 * turns it off, and a dead tap on a live control feels broken.
 *
 * [MinHeight] is 40dp rather than the 48dp touch target, deliberately. These
 * sit in dense horizontally-scrolling rows — four serving variants, four oils —
 * where 48dp each would cost a third of the serving sheet's scrollable middle,
 * and 40dp is already well above the 32dp Material gives a chip.
 */
@Composable
fun HapticFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.md),
) {
    val haptics = rememberAppHaptics()
    val scheme = MaterialTheme.colorScheme
    val content = when {
        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.38f)
        selected -> scheme.onSurface
        else -> scheme.onSurfaceVariant
    }
    // The edge is the whole control on a black canvas, so it is also where the
    // selection lives: `onSurface` for the chosen one, `outlineVariant` for the
    // rest. Disabled drops to the same 38% the label does, so the chip fades as
    // one thing rather than losing its text inside a live-looking outline.
    val edge = when {
        !enabled -> scheme.outlineVariant.copy(alpha = 0.38f)
        selected -> scheme.onSurface
        else -> scheme.outlineVariant
    }
    Surface(
        onClick = {
            haptics.light()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        contentColor = content,
        border = BorderStroke(Elevation.hairline, edge),
        tonalElevation = Elevation.none,
        shadowElevation = Elevation.none,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = MinHeight)
                .padding(horizontal = Spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(Spacing.s))
                }
                ProvideTextStyle(AppText.label) { label() }
                if (trailingIcon != null) {
                    Spacer(Modifier.width(Spacing.s))
                    trailingIcon()
                }
            }
        }
    }
}
