package com.example.foodtracker.ui.screens.trends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.screens.trends.TrendsRange
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Sizes
import com.example.foodtracker.ui.theme.Spacing

/** The rule under the selected range. Same 24dp as `DayStrip`'s, for the same reason. */
private val RULE_WIDTH = 24.dp
private val RULE_HEIGHT = 2.dp

/**
 * The window every section on Trends obeys: `7D · 30D · 90D · 1Y`.
 *
 * **The filled pills are gone.** This was an `ExclusiveButtonGroup` — four
 * `Radius.pill` segments, the selected one filled `primaryContainer`, the other
 * three outlined. That is three separate violations of house rule 3 in the
 * first control on the page: the accent is for data, not chrome; a selected
 * chip is not allowed to be a filled accent block; and a mint pill sitting
 * directly above a column of mint-free charts told you the control was the most
 * important thing on the screen, which it is not — it is the *frame* for the
 * numbers below it.
 *
 * What replaces it is `DayStrip`'s selection, verbatim: a label in the
 * foreground colour with a 2dp rule beneath it, and the unselected labels in
 * `onSurfaceVariant` with nothing beneath them. Trends and Today now mark "this
 * one" the same way, which is the whole point of having a house rule about it —
 * and the rule is `onSurface`, not `primary`, so the accent stays available to
 * mean "on track" when a chart uses it.
 *
 * **Re-tapping the selected range does nothing**, including no haptic. The
 * segmented control behaved that way and callers rely on it: [onSelect]
 * re-runs the whole range query, and a thumb resting on `30D` should not
 * re-issue it. The tick itself stays with the caller, beside the state change.
 */
@Composable
fun RangeBar(
    selected: TrendsRange,
    onSelect: (TrendsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth()) {
        TrendsRange.entries.forEach { range ->
            val isSelected = range == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Sizes.touchTarget)
                    .clickable(enabled = !isSelected) { onSelect(range) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = Spacing.m),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    // "7d" → "7D". The labels are set by the enum so the
                    // control and the ViewModel can never disagree about what
                    // "90d" means; the case is this control's business.
                    range.label.uppercase(),
                    style = AppText.label,
                    color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.s))
                Box(
                    Modifier
                        .width(RULE_WIDTH)
                        .height(RULE_HEIGHT)
                        .background(if (isSelected) scheme.onSurface else Color.Transparent)
                )
            }
        }
    }
}
