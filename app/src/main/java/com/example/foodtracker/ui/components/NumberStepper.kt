package com.example.foodtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.rememberAppHaptics

/**
 * Compact `-` value `+` row for nudging a numeric value by one step at a time
 * without the user having to drag a slider. Each tap fires a light haptic so
 * the change is felt even when the buttons are out of the focal area.
 *
 * Pair with a continuous Slider on the surrounding screen — slider for coarse
 * swings, stepper for precision.
 */
@Composable
fun NumberStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilledTonalIconButton(
            onClick = {
                haptics.light()
                onDecrement()
            },
            enabled = decrementEnabled,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minWidth = 76.dp),
        )
        FilledTonalIconButton(
            onClick = {
                haptics.light()
                onIncrement()
            },
            enabled = incrementEnabled,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}
