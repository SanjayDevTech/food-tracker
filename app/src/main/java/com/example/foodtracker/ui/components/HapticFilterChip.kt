package com.example.foodtracker.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.foodtracker.ui.rememberAppHaptics

/**
 * `FilterChip` that fires an [com.example.foodtracker.ui.AppHaptics.light] tick
 * when toggled. Material's stock `FilterChip` has no haptic, which means raw
 * chips feel "dead" next to the rest of the app (sliders, segmented buttons,
 * stepper buttons — all ticking). Use this for any standalone chip outside an
 * `ExclusiveButtonGroup`.
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
    shape: androidx.compose.ui.graphics.Shape = FilterChipDefaults.shape,
) {
    val haptics = rememberAppHaptics()
    FilterChip(
        selected = selected,
        onClick = {
            haptics.light()
            onClick()
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
    )
}
