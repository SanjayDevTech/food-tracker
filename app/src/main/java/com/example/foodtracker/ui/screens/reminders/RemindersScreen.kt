package com.example.foodtracker.ui.screens.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.reminder.BatteryOptHelper
import com.example.foodtracker.reminder.ReminderConfig
import com.example.foodtracker.reminder.ReminderKind
import com.example.foodtracker.reminder.rememberBatteryOptExempt
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.TimePickerDialog
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val vm: RemindersViewModel = appViewModel { RemindersViewModel(it.reminderStore, it.reminderScheduler) }
    val configs by vm.configs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    var editingKind by remember { mutableStateOf<ReminderKind?>(null) }
    val batteryExempt by rememberBatteryOptExempt()
    val anyReminderEnabled = configs.any { it.enabled }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = Dimens.ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
        ) {
            if (!notificationsGranted) {
                item {
                    NotificationsBlockedCard(
                        onAllow = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
            }

            item {
                RemindersSection(title = "Daily reminders") {
                    configs.forEachIndexed { index, config ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            )
                        }
                        ReminderRow(
                            config = config,
                            onToggle = { vm.setEnabled(config.kind, it) },
                            onEditTime = { editingKind = config.kind },
                        )
                    }
                }
            }

            if (anyReminderEnabled && !batteryExempt) {
                item {
                    BatteryOptOutlinedCard(
                        onOpenSettings = { BatteryOptHelper.openSettings(context) },
                    )
                }
            }

            item {
                Text(
                    "Reminders use Doze-aware alarms — they respect battery saver but may " +
                        "fire a few minutes after the set time while the device is asleep.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }

    editingKind?.let { kind ->
        val current = configs.firstOrNull { it.kind == kind }
        if (current != null) {
            TimePickerDialog(
                initialHour = current.hour,
                initialMinute = current.minute,
                onDismiss = { editingKind = null },
                onConfirm = { h, m ->
                    vm.setTime(kind, h, m)
                    editingKind = null
                },
            )
        }
    }
}

/**
 * Outlined-card section, same container language as Home (`HomeSection`):
 * transparent fill + 1dp `outlineVariant` border, 18dp corners, 14h/12v
 * padding, `titleMedium` SemiBold heading with an 8dp gap.
 */
@Composable
private fun RemindersSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun ReminderRow(
    config: ReminderConfig,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                config.kind.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                config.kind.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            AssistChip(
                onClick = onEditTime,
                label = { Text(config.timeLabel()) },
            )
        }
        Switch(
            checked = config.enabled,
            onCheckedChange = {
                haptics.light()
                onToggle(it)
            },
        )
    }
}

/**
 * Outlined-card variant of the notifications-blocked alert. Uses an error-tinted
 * border instead of a filled errorContainer so it follows the same minimal
 * outlined idiom as the rest of the page; the error tone still reads as a
 * blocking warning.
 */
@Composable
private fun NotificationsBlockedCard(onAllow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            "Notifications are blocked",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            "Allow notifications so we can remind you at the times you set.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onAllow) { Text("Allow notifications") }
    }
}

/**
 * Outlined-card variant of the battery-optimisation nudge. Tertiary border
 * tone marks it as a soft warning (less severe than the error-bordered
 * notifications card above).
 */
@Composable
private fun BatteryOptOutlinedCard(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            "Make reminders reliable",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Android's battery saver can delay reminders by minutes or hours — especially on " +
                "OEM skins (Xiaomi / Oppo / Vivo / Realme / Samsung). Allow this app to ignore " +
                "battery optimisation so reminders fire on time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text("Open battery settings")
        }
    }
}
