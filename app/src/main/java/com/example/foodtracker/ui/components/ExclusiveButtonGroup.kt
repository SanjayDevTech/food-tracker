package com.example.foodtracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.rememberAppHaptics

/**
 * Single-select row of M3 Expressive [ToggleButton]s wrapped in a [ButtonGroup].
 * Replaces the equivalent `Row { FilterChip(...) }` exclusive-picker pattern
 * with the new "connected" look — chips visually fuse into one control and
 * the selected button morphs to the connected-corner shape.
 *
 * [fillWidth] (default true) makes every button share the row's width equally —
 * use this when labels are short and you want the picker to feel like a
 * segmented control. Set it false when labels are long: each button sizes to
 * its label and the whole row becomes horizontally scrollable so text stays
 * single-line and complete.
 *
 * Generic over the option type so it works for enums and arbitrary lists.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val groupModifier =
        if (fillWidth) modifier else modifier.horizontalScroll(rememberScrollState())
    ButtonGroup(
        modifier = groupModifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { wasChecked ->
                    if (wasChecked) {
                        haptics.light()
                        onSelect(option)
                    }
                },
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
            ) {
                Text(label(option), maxLines = 1, softWrap = false)
            }
        }
    }
}
