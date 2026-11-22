package com.example.foodtracker.ui.components.serving

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.foodtracker.data.AllNutrientFields
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.ui.components.atoms.NBSP
import com.example.foodtracker.ui.components.atoms.ValueText
import com.example.foodtracker.ui.components.atoms.formatValue
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing
import kotlin.math.abs

/**
 * The nutrients as they stood the instant a preparation chip was tapped.
 *
 * Captured in the chip's own click handler, where the pre-change figures are
 * still the composed ones. [token] exists so two taps in a row restart the
 * two-second window instead of the first tap's timer cancelling the second.
 */
internal data class PrepFlash(val before: Nutrients, val token: Int)

/** Always shown. Six fields is what fits in three rows without scrolling. */
private val BaseKeys = listOf("calories", "carbs", "protein", "fat", "fiber", "sodium")

/**
 * The two fields a preparation change actually moves. Shown only for foods that
 * have such a choice — on a banana they would be two rows of noise.
 */
private val PrepKeys = listOf("saturatedFat", "vitaminE")

/**
 * *What's in it* — design-plan §4.5, plus the behavioural addition in §6.3.
 *
 * A two-column grid rather than a list, for the same reason §4.7's all-nutrient
 * sheet is: eight half-width cells are four rows, and eight full-width rows
 * would push the footer off a 412×915 screen on a food with a preparation axis.
 *
 * **The change-highlighting is the point of this component.** Switching from
 * palmolein to coconut oil moves saturated fat and vitamin E and leaves
 * calories and total fat *exactly* where they were — that is not a bug, it is
 * the whole claim the oil model makes. Without a signal the control reads as
 * dead, and the single most domain-specific feature in the app looks broken.
 * So for two seconds after a prep tap the fields that moved carry a ↑/↓ delta
 * and their value pulses to the accent colour.
 *
 * The delta says *what moved*, never whether that is good: more vitamin E is
 * good and more saturated fat is not, and this component has no goals in hand
 * to tell them apart. Colouring it as a verdict would be a guess printed as a
 * fact — the status palette in §1 is reserved for readings measured against a
 * real target.
 *
 * @param previous the pre-tap figures, or null when nothing is being flagged.
 */
@Composable
internal fun LivePreview(
    nutrients: Nutrients,
    previous: Nutrients?,
    showFatDetail: Boolean,
    modifier: Modifier = Modifier,
) {
    val fields = remember(showFatDetail) { previewFields(showFatDetail) }
    Column(modifier.fillMaxWidth()) {
        fields.chunked(2).forEachIndexed { index, pair ->
            if (index > 0) Spacer(Modifier.height(Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.inCard),
            ) {
                pair.forEach { field ->
                    PreviewCell(
                        field = field,
                        value = field.read(nutrients),
                        delta = previous?.let { before ->
                            val d = field.read(nutrients) - field.read(before)
                            d.takeIf { moved(it, field.read(nutrients)) }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a lone trailing cell at half width instead of letting
                // it stretch across the row.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Whether a difference is worth flagging.
 *
 * Both tests are needed: the absolute floor stops 0.02 g of fat earning a
 * badge, and the relative one stops a 1 mg wobble on a 2 740 mg sodium figure
 * doing the same. Oil and style deltas are real arithmetic on doubles, so
 * exact-zero comparisons would flash on rounding dust.
 */
private fun moved(delta: Double, now: Double): Boolean =
    abs(delta) >= 0.05 && abs(delta) >= abs(now) * 0.005

@Composable
private fun PreviewCell(
    field: NutrientField,
    value: Double,
    delta: Double?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val valueColor by animateColorAsState(
        targetValue = if (delta != null) scheme.primary else scheme.onSurface,
        animationSpec = tween(450),
        label = "previewPulse",
    )
    Column(modifier) {
        // Eyebrow over the number — house rule 2's one rhythm. These were
        // `bodyS` labels, which at 13sp sit close enough to the 18dp value to
        // read as a two-line sentence; uppercased at 11sp with tracking, the
        // label recedes to being a caption and the figure carries the cell,
        // which is the whole claim this grid is making.
        Text(
            field.label.uppercase(),
            style = AppText.eyebrow,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            ValueText(value = value, unit = field.unit.symbol, color = valueColor)
            if (delta != null) {
                Spacer(Modifier.width(Spacing.s))
                // An accent-coloured label, not a filled badge: rule 3 allows
                // the accent on data and refuses it as a block, and the value
                // beside it is already pulsing to the same colour — the two
                // now read as one thing moving rather than as a number and a
                // chip that happen to be adjacent.
                Text(
                    (if (delta > 0) "↑" else "↓") + NBSP + formatValue(abs(delta)),
                    style = AppText.bodyS,
                    color = scheme.primary,
                )
            }
        }
    }
}

/**
 * The preview's fields, read off [AllNutrientFields] rather than retyped, so a
 * label or unit fixed in the nutrient model is fixed here too.
 */
private fun previewFields(showFatDetail: Boolean): List<NutrientField> {
    val keys = if (showFatDetail) {
        // Interleaved so the two prep-sensitive fields sit in the same row as
        // each other, which is where the eye lands when they both flash.
        listOf("calories", "carbs", "protein", "fat") + PrepKeys + listOf("fiber", "sodium")
    } else {
        BaseKeys
    }
    return keys.mapNotNull { key -> AllNutrientFields.firstOrNull { it.key == key } }
}
