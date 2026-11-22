package com.example.foodtracker.ui.screens.today.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * Water and weight — design-plan §3, rebuilt as rows rather than tiles.
 *
 * They used to be two outlined cards side by side. Two boxes holding one number
 * each is the Direction A anatomy the rewrite is supposed to be leaving: the
 * border does no work a label and a rule cannot do, and splitting the row in
 * half caps the number at a third of the screen's width.
 *
 * As a row the label sits left in an eyebrow, the number sits right at full
 * weight, and the caption trails it — the same structure as every other line on
 * the screen, which is what gives the page one rhythm instead of three.
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    unit: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                style = AppText.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                caption,
                style = AppText.bodyS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.m))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppText.numberL, color = MaterialTheme.colorScheme.onSurface)
            if (unit.isNotEmpty()) {
                Text(
                    " $unit",
                    style = AppText.bodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
        }
    }
}
