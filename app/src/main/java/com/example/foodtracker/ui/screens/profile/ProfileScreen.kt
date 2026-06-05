package com.example.foodtracker.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.AllNutrientFields
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.Sex
import com.example.foodtracker.data.backup.LocalBackup
import java.text.DateFormat
import java.util.Date
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.SectionCard
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenReminders: () -> Unit = {},
    onOpenWeight: () -> Unit = {},
) {
    val vm: ProfileViewModel = appViewModel {
        ProfileViewModel(it.profileStore, it.backupManager, it.backupScheduler)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val snackbar = remember { SnackbarHostState() }

    // Surface one-shot backup messages as snackbars.
    LaunchedEffect(state.backupMessage) {
        val msg = state.backupMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        vm.consumeBackupMessage()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { vm.exportTo(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { vm.importFrom(it) } }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { vm.setBackupFolder(it) } }

    // Confirmation gate before a destructive import / restore.
    var pendingRestore by remember { mutableStateOf<LocalBackup?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    pendingRestore?.let { backup ->
        ConfirmDialog(
            title = "Restore this backup?",
            body = "All current data will be replaced with the contents of " +
                "${backup.displayName}. This can't be undone.",
            confirmLabel = "Restore",
            onConfirm = {
                vm.restoreBackup(backup)
                pendingRestore = null
            },
            onDismiss = { pendingRestore = null },
        )
    }
    if (confirmImport) {
        ConfirmDialog(
            title = "Import a snapshot?",
            body = "Choose a JSON file you previously exported. All current data " +
                "will be replaced — there is no undo.",
            confirmLabel = "Pick file",
            onConfirm = {
                confirmImport = false
                importLauncher.launch(arrayOf("application/json"))
            },
            onDismiss = { confirmImport = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Profile & goals") },
                actions = {
                    if (state.editing) {
                        IconButton(onClick = vm::cancelEdit) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = vm::save) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = vm::beginEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                        }
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = Dimens.ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
        ) {
            item { ProfileHeaderCard(profile = state.profile) }

            if (state.editing) {
                item {
                    SectionCard(title = "Edit profile") {
                        OutlinedTextField(
                            value = state.draft.name,
                            onValueChange = { v -> vm.updateDraft { it.copy(name = v) } },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
                        Text("Sex", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Sex.entries.forEach { sex ->
                                FilterChip(
                                    selected = state.draft.sex == sex,
                                    onClick = { vm.updateDraft { it.copy(sex = sex) } },
                                    label = {
                                        Text(
                                            sex.displayName,
                                            maxLines = 1,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SliderRow(
                            label = "Age",
                            value = state.draft.ageYears.toFloat(),
                            range = 10f..90f,
                            steps = 79,
                            unit = "years",
                            onChange = { vm.updateDraft { p -> p.copy(ageYears = it.toInt()) } },
                        )
                        SliderRow(
                            label = "Height",
                            value = state.draft.heightCm.toFloat(),
                            range = 120f..220f,
                            steps = 0,
                            unit = "cm",
                            onChange = { vm.updateDraft { p -> p.copy(heightCm = it.toInt()) } },
                        )
                        SliderRow(
                            label = "Weight",
                            value = state.draft.weightKg.toFloat(),
                            range = 35f..200f,
                            steps = 0,
                            unit = "kg",
                            onChange = { vm.updateDraft { p -> p.copy(weightKg = it.toDouble()) } },
                            valueFormatter = { "%.1f".format(it) },
                        )

                        Text("Activity level", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActivityLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = state.draft.activityLevel == level,
                                    onClick = { vm.updateDraft { it.copy(activityLevel = level) } },
                                    label = { Text(level.displayName) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Goal", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Goal.entries.forEach { g ->
                                FilterChip(
                                    selected = state.draft.goal == g,
                                    onClick = { vm.updateDraft { it.copy(goal = g) } },
                                    label = { Text(g.displayName, maxLines = 1) },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptics.confirm()
                                vm.save()
                            },
                            modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
                        ) {
                            Text("Save changes")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
                ) {
                    NavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MonitorWeight,
                        title = "Weight",
                        subtitle = "Log & track",
                        onClick = onOpenWeight,
                    )
                    NavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Notifications,
                        title = "Reminders",
                        subtitle = "Meals, water, weigh-in",
                        onClick = onOpenReminders,
                    )
                }
            }

            item {
                SectionCard(title = "Settings") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic feedback", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Subtle vibrations on save and toggle actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.hapticsEnabled,
                            onCheckedChange = { vm.setHapticsEnabled(it) },
                        )
                    }

                    Spacer(Modifier.height(Dimens.CardItemSpacingLoose))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Dynamic pulls accent colors from your wallpaper (Android 12+).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Dimens.CardItemSpacing))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = state.themeMode == mode,
                                    onClick = { vm.setThemeMode(mode) },
                                    label = {
                                        Text(
                                            mode.displayName,
                                            maxLines = 1,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Data") {
                    Text(
                        "Snapshots are plain JSON. Save them to Downloads/FoodTracker " +
                            "or pick any folder — other apps can read and chart the files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
                    BackupFolderRow(
                        folderLabel = state.backupFolderLabel,
                        onPick = {
                            haptics.light()
                            folderLauncher.launch(null)
                        },
                        onReset = { vm.clearBackupFolder() },
                    )

                    Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptics.light()
                                exportLauncher.launch(defaultExportFilename())
                            },
                            enabled = !state.backupBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export…")
                        }
                        FilledTonalButton(
                            onClick = {
                                haptics.light()
                                confirmImport = true
                            },
                            enabled = !state.backupBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Import…")
                        }
                    }

                    Spacer(Modifier.height(Dimens.CardItemSpacing))
                    OutlinedButton(
                        onClick = {
                            haptics.light()
                            vm.saveBackupNow()
                        },
                        enabled = !state.backupBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.backupFolderLabel != null) "Save backup to picked folder"
                            else "Save backup to Downloads"
                        )
                    }

                    Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
                    Text(
                        "Recurring backup",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "Runs in the background only when you turn it on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Dimens.CardItemSpacing))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BackupCadence.entries.forEach { cadence ->
                            FilterChip(
                                selected = state.backupCadence == cadence,
                                onClick = { vm.setBackupCadence(cadence) },
                                label = { Text(cadence.label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
                    Text(
                        if (state.backupFolderLabel != null) "Snapshots in your folder"
                        else "Snapshots in Downloads/FoodTracker",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Dimens.CardItemSpacing))
                    if (state.backups.isEmpty()) {
                        Text(
                            "Nothing saved yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.backups.forEach { backup ->
                            BackupRow(
                                backup = backup,
                                onRestore = { pendingRestore = backup },
                                onDelete = { vm.deleteBackup(backup) },
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.CardItemSpacing))
                    Text(
                        "Or pick any backup file the saved list doesn't show:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            haptics.light()
                            confirmImport = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restore from file…")
                    }
                }
            }

            item {
                SectionCard(title = "Daily targets") {
                    val grouped = AllNutrientFields.groupBy { it.category }
                    NutrientCategory.entries.forEachIndexed { index, cat ->
                        if (index > 0) Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
                        CategorySubheader(label = cat.displayLabel())
                        grouped[cat].orEmpty().forEach { field ->
                            TargetRow(
                                label = field.label,
                                value = "${field.goal(state.goals).toInt()} ${field.unit.symbol}",
                            )
                        }
                        // Tuck water into the Macros block — it's reported in ml, not a NutrientField.
                        if (cat == NutrientCategory.Macros) {
                            TargetRow(
                                label = "Water",
                                value = "${state.goals.waterMl} ml",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(profile: com.example.foodtracker.data.UserProfile) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarInitial(profile.name)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        profile.name.ifBlank { "Welcome" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        profile.goal.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(Dimens.CardItemSpacingLoose))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatBlock(label = "Age", value = "${profile.ageYears}", unit = "yrs")
                StatBlock(label = "Height", value = "${profile.heightCm}", unit = "cm")
                StatBlock(label = "Weight", value = "%.1f".format(profile.weightKg), unit = "kg")
            }
            Spacer(Modifier.height(Dimens.CardItemSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatBlock(label = "BMR", value = "${GoalCalculator.bmr(profile).toInt()}", unit = "kcal")
                StatBlock(label = "TDEE", value = "${GoalCalculator.tdee(profile).toInt()}", unit = "kcal")
                StatBlock(label = "Activity", value = profile.activityLevel.displayName, unit = "")
            }
        }
    }
}

@Composable
private fun AvatarInitial(name: String) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "👤"
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String, unit: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun CategorySubheader(label: String) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun TargetRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun NutrientCategory.displayLabel(): String = when (this) {
    NutrientCategory.Macros -> "Macros"
    NutrientCategory.Minerals -> "Minerals"
    NutrientCategory.Vitamins -> "Vitamins"
}

@Composable
private fun NavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    unit: String,
    onChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
) {
    Column {
        Text(
            "$label: ${valueFormatter(value)} $unit",
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun BackupFolderRow(
    folderLabel: String?,
    onPick: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Backup folder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                folderLabel ?: "Downloads/FoodTracker (default)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(onClick = onPick) {
            Text(if (folderLabel == null) "Choose…" else "Change")
        }
        if (folderLabel != null) {
            TextButton(onClick = onReset) { Text("Reset") }
        }
    }
}

@Composable
private fun BackupRow(
    backup: LocalBackup,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                backup.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            val when_ = DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(backup.createdAtMillis))
            val size = humanReadableSize(backup.sizeBytes)
            Text(
                "$when_ · $size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRestore) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Restore")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete backup")
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun defaultExportFilename(): String {
    val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(Date())
    return "foodtracker-$stamp.json"
}

private fun humanReadableSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
