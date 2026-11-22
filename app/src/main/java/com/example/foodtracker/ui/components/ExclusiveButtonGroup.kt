package com.example.foodtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Elevation
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Sizes
import com.example.foodtracker.ui.theme.Spacing

/**
 * Single-select segmented control — a row of mutually exclusive segments.
 *
 * **Why this is hand-rolled.** It used to be `ButtonGroup { toggleableItem(…) }`
 * from Material 3 Expressive. Expressive is gone from the app (design-plan §0
 * hard constraints, §1 motion table), and with it that pair: `ButtonGroup`
 * brought the connected-corner shape-morph on press, which §1 bans outright,
 * and both APIs are behind `ExperimentalMaterial3ExpressiveApi`. The stock M3
 * alternative, `SingleChoiceSegmentedButtonRow`, is not a drop-in either — it
 * hard-codes a check icon in the selected segment, its own 40dp height and a
 * border on *every* segment, which is three fights with §1 and §2 for a control
 * that is a `Row` of five properties. So: `Row` + `Surface`, per §2's
 * `SegmentedControl` row.
 *
 * The look, straight off §1/§2:
 *  - [Radius.md] on every segment, and **zero elevation** — neither tonal nor
 *    shadow. Structure comes from the hairline, not from a lift.
 *  - Selected: transparent still, with the edge and the label at `onSurface`.
 *  - Unselected: transparent, `onSurfaceVariant` label, `outlineVariant` edge.
 *
 * Neither state is filled and neither uses the accent. That is house rule 3
 * (`docs/ui-house-rules.md`): `primary` means "on track" and belongs to data, so
 * a segmented control — which is chrome — may not borrow it.
 *  - [AppText.label] for the text and a [Sizes.touchTarget] minimum height, so
 *    the segment is a legal tap target however short its label is.
 *
 * **The haptic stays.** §2 calls the tick out as worth keeping: this is a
 * discrete tap on a tap target, so it fires [rememberAppHaptics]`.light()`, and
 * only on an actual change of selection. Re-tapping the selected segment is a
 * no-op — no tick, no [onSelect] — which is what the Expressive version did
 * (its `onCheckedChange(false)` was dropped) and what callers assume: several
 * of them persist on every `onSelect`.
 *
 * [fillWidth] (default true) makes every segment share the row's width equally —
 * use this when labels are short and you want the picker to feel like a
 * segmented control. Set it false when labels are long: each segment sizes to
 * its label and the whole row becomes horizontally scrollable so text stays
 * single-line and complete.
 *
 * Generic over the option type so it works for enums and arbitrary lists.
 * Equality is `==`, so `T` must have sane `equals` — every caller passes an
 * enum entry or a data class.
 */
@Composable
fun <T> ExclusiveButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    label: (T) -> String,
) {
    val haptics = rememberAppHaptics()
    val rowModifier =
        if (fillWidth) modifier else modifier.horizontalScroll(rememberScrollState())
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val colors = MaterialTheme.colorScheme
            Surface(
                onClick = {
                    if (!isSelected) {
                        haptics.light()
                        onSelect(option)
                    }
                },
                // Equal-weight segments when filling; wrap-to-label otherwise,
                // which is what makes the scrolling variant scroll.
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
                // Radius.md, not pill: house rule 6 reserves the pill for the
                // FAB and primary buttons, and a row of pills is the soft
                // geometry the editorial pass is removing.
                shape = RoundedCornerShape(Radius.md),
                // Monochrome selection. The accent means "on track" (house rule
                // 3) and a segmented control is chrome, not data — a filled
                // primaryContainer segment was the app's most repeated misuse
                // of it, on seven screens at once. Selection now reads as the
                // edge and the label going to full strength, which is the same
                // state HapticFilterChip uses, so chips and segments finally
                // agree about what "selected" looks like.
                color = Color.Transparent,
                contentColor = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                border = BorderStroke(
                    Elevation.hairline,
                    if (isSelected) colors.onSurface else colors.outlineVariant,
                ),
                tonalElevation = Elevation.none,
                shadowElevation = Elevation.none,
            ) {
                // Surface propagates its min constraints, so under a weight this
                // Box is already the segment's full width and Center does the
                // rest; unweighted it shrinks to the label plus padding.
                Box(
                    modifier = Modifier
                        .heightIn(min = Sizes.touchTarget)
                        .padding(horizontal = Spacing.m, vertical = Spacing.s),
                    contentAlignment = Alignment.Center,
                ) {
                    // Deliberately unconstrained lines. Under [fillWidth] a long
                    // label wraps and centres rather than ellipsing — AddFood's
                    // category rows need that ("South Indian" does not fit a
                    // third of a card at 13sp) and the segment's height grows
                    // with it. Unweighted the row is measured unbounded, so
                    // there is nothing to wrap against and it stays one line.
                    Text(
                        text = label(option),
                        style = AppText.label,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
