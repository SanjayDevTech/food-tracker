package com.example.foodtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One line of a sub-breakdown: a component name and its formatted amount. */
data class NutrientSubRow(val label: String, val value: String)

/**
 * The quiet detail inside a nutrient's bottom sheet — soluble/insoluble under
 * Fiber, essential amino acids under Protein.
 *
 * Deliberately plain: no progress bar, no goal, no percentage of a target, no
 * tap target. These are detail you open when curious, not numbers to hit. They
 * sit behind a tap rather than beside the bar on the home screen, so the day's
 * scoreboard stays the same length for someone who never looks at them.
 * Indentation and [labelMedium] on [onSurfaceVariant] is the whole design.
 *
 * [note] carries the honest caveat about coverage. A day's sub-total is only
 * summed from the foods that happen to have a breakdown, so without it the
 * number would read as a complete figure when it is usually a partial one.
 */
@Composable
fun NutrientSubRows(
    rows: List<NutrientSubRow>,
    note: String? = null,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Indented under the sheet's heading, with air above so it reads
            // as its own block rather than a second line of the total.
            .padding(start = 14.dp, top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    row.value,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (note != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
