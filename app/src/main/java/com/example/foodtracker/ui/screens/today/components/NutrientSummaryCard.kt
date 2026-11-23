package com.example.foodtracker.ui.screens.today.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.foodtracker.data.MET_FRACTION
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.onTrack
import com.example.foodtracker.ui.components.scored
import com.example.foodtracker.ui.components.NutrientRow
import com.example.foodtracker.ui.components.NutrientStatusStrip
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.components.readingsFor
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/** How many shortfalls and overshoots get named before the list becomes a wall. */
private const val MAX_SHORT = 4
private const val MAX_OVER = 2

/**
 * The micronutrient card on Today — design-plan §4.1.
 *
 * Answers two questions in that order, which is the whole design:
 *
 *  1. *Is today broadly fine?* — one wrapped band of status cells, zero reading.
 *  2. *What specifically is wrong?* — at most six named rows, worst first.
 *
 * Everything else is behind "See all". Tracking 29 micronutrients is only
 * defensible if the common case costs a glance, and a screen of 29 labelled
 * bars is not a glance.
 */
@Composable
fun NutrientSummaryCard(
    totals: Nutrients,
    goals: NutrientGoals,
    hasLogs: Boolean,
    onSeeAll: () -> Unit,
    onPickNutrient: (NutrientField) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val readings = remember(totals, goals) { readingsFor(totals, goals) }
    val scored = readings.scored()
    val onTrackCount = scored.count { it.status.onTrack }

    // A limit is never "short", and a goal within 80% of target is counted as
    // met by the tally above — listing it here would contradict the count.
    val shortOn = readings
        .filter { !it.limit && it.goal > 0 && it.fraction < MET_FRACTION }
        .sortedBy { it.fraction }
        .take(MAX_SHORT)
    val over = readings
        .filter { it.limit && it.fraction > 1.0 }
        .sortedByDescending { it.fraction }
        .take(MAX_OVER)

    Section(
        title = "Nutrients",
        modifier = modifier,
        trailing = {
            Text(
                if (hasLogs) "$onTrackCount of ${scored.size} on track" else "—",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        },
    ) {
        if (!hasLogs) {
            Text(
                "Log something and your ${scored.size} nutrient targets appear here.",
                style = AppText.body,
                color = scheme.onSurfaceVariant,
            )
            return@Section
        }

        NutrientStatusStrip(readings, onCellClick = onSeeAll)

        if (shortOn.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.l))
            Text("SHORT ON", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.s))
            shortOn.forEach { r ->
                NutrientRow(r, Modifier.clickable { onPickNutrient(r.field) })
            }
        }
        if (over.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.inCard))
            Text("OVER", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.s))
            over.forEach { r ->
                NutrientRow(r, Modifier.clickable { onPickNutrient(r.field) })
            }
        }

        Spacer(Modifier.height(Spacing.inCard))
        AppDivider()
        Text(
            // No count here. The screen behind this link shows all 29 fields,
            // but the tally directly above says "of 28" — both numbers are
            // right and they mean different things (shown vs. scored), which
            // side by side just reads as an off-by-one. The link does not need
            // a number to say where it goes.
            "See all nutrients",
            style = AppText.label,
            color = scheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSeeAll)
                .padding(vertical = Spacing.inCard),
        )
    }
}
