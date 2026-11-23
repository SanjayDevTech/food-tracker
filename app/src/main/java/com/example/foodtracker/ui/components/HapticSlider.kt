package com.example.foodtracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.foodtracker.ui.AppHaptics
import com.example.foodtracker.ui.rememberAppHaptics
import kotlin.math.roundToInt

/**
 * Slider variant that emits a [AppHaptics.light] tick whenever the dragged
 * value crosses a [granularity]-sized bucket. Lets us keep the slider track
 * fully continuous (no visual notches) while still giving the finger a sense
 * of integer ticks.
 *
 * `granularity = 1f` ticks per integer unit (age, height). Use a smaller
 * value (e.g. `0.1f`) for finer feedback on weight-style sliders.
 *
 * The editorial pass kept this component as it is and changed only the track
 * colours: the inactive half becomes the same `outlineVariant` hairline colour
 * every meter in the app uses for its track, so an unfilled slider and an
 * unfilled meter are the same grey. Material's default inactive track is a
 * tint of `surfaceVariant`, which on `#000000` is a pale bar brighter than the
 * value it is meant to sit behind.
 */
@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    granularity: Float = 1f,
    enabled: Boolean = true,
) {
    val haptics = rememberAppHaptics()
    val lastBucket = remember { mutableFloatStateOf(Float.NaN) }
    Slider(
        value = value,
        onValueChange = { v ->
            val bucket = (v / granularity).roundToInt().toFloat()
            if (lastBucket.floatValue.isNaN()) {
                lastBucket.floatValue = bucket
            } else if (bucket != lastBucket.floatValue) {
                haptics.light()
                lastBucket.floatValue = bucket
            }
            onValueChange(v)
        },
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            // The step ticks are drawn by `steps`, and on a continuous-feeling
            // control they should not be visible — the finger is told where the
            // buckets are, the eye is not. Both tick colours go transparent.
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
}
