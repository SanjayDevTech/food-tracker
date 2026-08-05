package com.example.foodtracker.ui.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
 */
@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    granularity: Float = 1f,
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
        modifier = modifier,
    )
}
