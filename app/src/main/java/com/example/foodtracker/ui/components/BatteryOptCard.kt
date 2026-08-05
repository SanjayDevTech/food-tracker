package com.example.foodtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Optional nudge shown when reminders are enabled but the system can still
 * delay alarms under aggressive battery savers (OEM "app standby", App Sleep,
 * etc.). Tapping deeplinks to the per-app battery setting — the user decides
 * whether to allow the exemption. The card auto-hides once the app is exempt.
 */
@Composable
fun BatteryOptCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Make reminders reliable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Android's battery saver can delay reminders by minutes or hours, especially on OEM " +
                    "skins (Xiaomi / Oppo / Vivo / Realme / Samsung). Allow this app to ignore " +
                    "battery optimisation so its reminders fire on time. Optional — not needed if " +
                    "you don't use reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings) {
                Text("Open battery settings")
            }
        }
    }
}
