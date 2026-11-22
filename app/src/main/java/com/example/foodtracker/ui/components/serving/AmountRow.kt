package com.example.foodtracker.ui.components.serving

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.ui.components.HapticFilterChip
import com.example.foodtracker.ui.components.HapticSlider
import com.example.foodtracker.ui.components.NumberStepper
import com.example.foodtracker.ui.components.atoms.ValueText
import com.example.foodtracker.ui.components.atoms.countText
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

// Servings mode: quarter-serving steps, 0.25–5. Custom mode: 5 g steps, 5–500
// on the slider but 1–2000 by stepper, because the slider is for the portion
// you are eating and the stepper is for the 1 200 g of biryani you cooked.
private const val SERVINGS_MIN = 0.25
private const val SERVINGS_MAX = 5.0
private const val SERVINGS_STEP = 0.25
private const val GRAMS_MIN = 1.0
private const val GRAMS_MAX = 2000.0
private const val GRAMS_STEP = 5.0
private const val GRAMS_SLIDER_MIN = 5f
private const val GRAMS_SLIDER_MAX = 500f

/**
 * *How much* — design-plan §4.5 and §6.3.
 *
 * Three controls, in the order the hand reaches for them: the variant pills
 * (which serving are we counting), a `[−] value [+]` row, and a slider.
 *
 * **Why the two modes now share one control.** The variant path used to be a
 * label plus a slider and the custom-grams path a label plus a stepper plus a
 * slider — two different shapes for one question, so switching modes rearranged
 * the sheet under the user's thumb. Both now render the same stepper-over-slider
 * pair and only the *text* changes form: `1.5 × dosa / 225 g` against `150 g`.
 *
 * **Why the stepper matters next to a slider.** A slider cannot hit 1.25
 * servings reliably on a 412dp screen, and a stepper is tedious past about
 * three taps. Coarse swing on the slider, last quarter on the stepper.
 *
 * The last chip is the free-form `Grams` toggle, and it seeds itself from the
 * gram total currently on screen — tapping it mid-adjustment should feel like
 * picking up where you were, not like being reset to a stock 100 g.
 *
 * The variant chips are no longer filled-accent when selected — see
 * `HapticFilterChip` for why, and note what it costs here: this is the one row
 * in the app where four chips sit side by side and exactly one is chosen, so it
 * is also where a monochrome selected state has to work hardest. It does,
 * because the gram readout directly underneath repeats the choice in words.
 */
@Composable
internal fun AmountRow(
    food: FoodEntity,
    variant: ServingVariant,
    servings: Double,
    onVariantChange: (ServingVariant) -> Unit,
    onServingsChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCustom = variant.isCustomGrams
    val grams = if (isCustom) variant.grams else variant.grams * servings

    Column(modifier.fillMaxWidth()) {
        // Horizontally scrollable so a long label like "1 masala dosa (150 g)"
        // is never clipped and never wraps the row into two lines.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            food.variantsOrDefault().forEach { v ->
                HapticFilterChip(
                    selected = !isCustom && v.label == variant.label && v.grams == variant.grams,
                    onClick = {
                        onVariantChange(v)
                        // A new serving means a new unit, so the count starts
                        // over: "2 × idli" becoming "2 × plate" is never what
                        // the user asked for.
                        onServingsChange(1.0)
                    },
                    label = { Text("${v.label} · ${v.grams.toInt()} g", maxLines = 1) },
                )
            }
            HapticFilterChip(
                selected = isCustom,
                onClick = {
                    val seed = grams.coerceIn(GRAMS_MIN, GRAMS_MAX)
                    onVariantChange(customGramsVariant(seed))
                    // Custom mode pins servings to 1.0 — see CUSTOM_GRAMS_PREFIX.
                    onServingsChange(1.0)
                },
                // "Grams", not "⌨ g": house rule 4 keeps glyphs to the picker's
                // category discs, and the keyboard one was carrying the chip's
                // meaning rather than decorating it — a user who did not read
                // it as "type a number" read it as nothing at all.
                label = { Text("Grams", maxLines = 1) },
            )
        }

        Spacer(Modifier.height(Spacing.l))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            NumberStepper(
                onDecrement = {
                    if (isCustom) {
                        onVariantChange(
                            customGramsVariant((variant.grams - GRAMS_STEP).coerceAtLeast(GRAMS_MIN))
                        )
                    } else {
                        onServingsChange((servings - SERVINGS_STEP).coerceAtLeast(SERVINGS_MIN))
                    }
                },
                onIncrement = {
                    if (isCustom) {
                        onVariantChange(
                            customGramsVariant((variant.grams + GRAMS_STEP).coerceAtMost(GRAMS_MAX))
                        )
                    } else {
                        onServingsChange((servings + SERVINGS_STEP).coerceAtMost(SERVINGS_MAX))
                    }
                },
                decrementEnabled = if (isCustom) variant.grams > GRAMS_MIN else servings > SERVINGS_MIN,
                incrementEnabled = if (isCustom) variant.grams < GRAMS_MAX else servings < SERVINGS_MAX,
            ) {
                AmountReadout(isCustom = isCustom, variant = variant, servings = servings, grams = grams)
            }
        }

        Spacer(Modifier.height(Spacing.s))

        if (isCustom) {
            HapticSlider(
                value = variant.grams.toFloat().coerceIn(GRAMS_SLIDER_MIN, GRAMS_SLIDER_MAX),
                onValueChange = { v ->
                    onVariantChange(customGramsVariant(v.toDouble().coerceIn(GRAMS_MIN, GRAMS_MAX)))
                },
                valueRange = GRAMS_SLIDER_MIN..GRAMS_SLIDER_MAX,
                steps = 98,
                granularity = GRAMS_STEP.toFloat(),
            )
        } else {
            HapticSlider(
                value = servings.toFloat().coerceIn(SERVINGS_MIN.toFloat(), SERVINGS_MAX.toFloat()),
                onValueChange = { onServingsChange(it.toDouble()) },
                valueRange = SERVINGS_MIN.toFloat()..SERVINGS_MAX.toFloat(),
                steps = 18,
                granularity = SERVINGS_STEP.toFloat(),
            )
        }
    }
}

/**
 * The two faces of the amount: a count of servings over its gram total, or the
 * gram total alone.
 *
 * The count uses [countText], not [ValueText] — a serving count is an integer
 * that happens to allow quarters, so it must read `2 ×` and `1.5 ×`, never the
 * `2.0 ×` a measurement formatter would print.
 */
@Composable
private fun AmountReadout(
    isCustom: Boolean,
    variant: ServingVariant,
    servings: Double,
    grams: Double,
) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isCustom) {
            ValueText(value = variant.grams, unit = "g")
        } else {
            Text(
                "${countText(servings)} × ${variant.label}",
                style = AppText.numberM,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                "${grams.toInt()} g",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
