package com.example.foodtracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.AiProvider
import com.example.foodtracker.data.backup.AnalysisRange
import com.example.foodtracker.data.backup.LocalBackup
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.FormLabel
import com.example.foodtracker.ui.components.FormSection
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.ColorPalette
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.ThemeMode
import java.text.DateFormat
import java.util.Date

/**
 * Everything that configures the app rather than describing the user: reminders,
 * appearance, AI autofill, backup/restore, export/import and custom foods.
 *
 * Split out of the old "Profile & goals" screen, which had grown to ten stacked
 * sections mixing "who am I" with "how does the app behave". Profile keeps the
 * former; this keeps the latter. Both hang off the Home overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
) {
    val vm: SettingsViewModel = appViewModel {
        SettingsViewModel(it.profileStore, it.backupManager, it.backupScheduler, it.repository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val snackbar = remember { SnackbarHostState() }

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

    var analysisRange by remember { mutableStateOf(AnalysisRange.Week) }
    val analysisExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { vm.exportAnalysisLogs(it, analysisRange) } }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { vm.setBackupFolder(it) } }

    var pendingRestore by remember { mutableStateOf<LocalBackup?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var deleteCustomMode by remember { mutableStateOf(false) }
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
    if (deleteCustomMode) {
        DeleteAllCustomFoodsDialog(
            onKeepHistory = {
                vm.deleteAllCustomFoods(cascadeLogs = false)
                deleteCustomMode = false
            },
            onCascade = {
                vm.deleteAllCustomFoods(cascadeLogs = true)
                deleteCustomMode = false
            },
            onDismiss = { deleteCustomMode = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            item {
                NavTile(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Notifications,
                    title = "Reminders",
                    subtitle = "Meals, water, weigh-in",
                    onClick = onOpenReminders,
                )
            }

            item {
                FormSection(title = "Appearance & feedback") {
                    SettingsToggleRow(
                        title = "Haptic feedback",
                        subtitle = "Subtle vibrations on toggles and saves",
                        checked = state.hapticsEnabled,
                        onChange = vm::setHapticsEnabled,
                    )
                    Spacer(Modifier.height(12.dp))
                    FormLabel("Theme")
                    ExclusiveButtonGroup(
                        options = ThemeMode.entries,
                        selected = state.themeMode,
                        onSelect = vm::setThemeMode,
                        modifier = Modifier.fillMaxWidth(),
                        label = { it.displayName },
                    )
                    Spacer(Modifier.height(10.dp))
                    FormLabel("Accent")
                    ExclusiveButtonGroup(
                        options = ColorPalette.entries,
                        selected = state.colorPalette,
                        onSelect = vm::setColorPalette,
                        modifier = Modifier.fillMaxWidth(),
                        label = { it.displayName },
                    )
                }
            }

            item {
                FormSection(title = "AI nutrition autofill") {
                    SettingsToggleRow(
                        title = "Enable AI autofill",
                        subtitle = "Off by default. When on, the add-food screen can ask an AI to fill nutrition — the food name is sent to your chosen provider.",
                        checked = state.aiEnabled,
                        onChange = vm::setAiEnabled,
                    )
                    if (state.aiEnabled) {
                        Spacer(Modifier.height(12.dp))
                        FormLabel("Provider")
                        ExclusiveButtonGroup(
                            options = AiProvider.entries,
                            selected = state.aiProvider,
                            onSelect = vm::setAiProvider,
                            modifier = Modifier.fillMaxWidth(),
                            label = { it.displayName },
                        )
                        Spacer(Modifier.height(12.dp))
                        FormLabel("Model")
                        ExclusiveButtonGroup(
                            options = state.aiProvider.models,
                            selected = state.aiModel,
                            onSelect = vm::setAiModel,
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = false,
                            label = { it.label },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${state.aiProvider.defaultModel.label} is the cheapest and accurate enough for most " +
                                "foods — leave it selected to save money. Bigger models are for unusual dishes and " +
                                "skip the extra verify step (accurate on the first pass).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.aiApiKey,
                            onValueChange = vm::setAiApiKey,
                            label = { Text("${state.aiProvider.displayName} API key") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your key stays on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val uriHandler = LocalUriHandler.current
                        Text(
                            "Get a ${state.aiProvider.displayName} API key at ${state.aiProvider.keyHint} ↗",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { uriHandler.openUri(state.aiProvider.keyUrl) },
                        )
                    }
                }
            }

            item {
                FormSection(title = "Backup & restore") {
                    BackupFolderRow(
                        folderLabel = state.backupFolderLabel,
                        onPick = {
                            haptics.light()
                            folderLauncher.launch(null)
                        },
                        onReset = { vm.clearBackupFolder() },
                    )
                    Spacer(Modifier.height(10.dp))
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
                            if (state.backupFolderLabel != null) "Save backup now"
                            else "Save backup to Downloads"
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    FormLabel("Recurring backup")
                    ExclusiveButtonGroup(
                        options = BackupCadence.entries,
                        selected = state.backupCadence,
                        onSelect = vm::setBackupCadence,
                        modifier = Modifier.fillMaxWidth(),
                        label = { it.label },
                    )

                    Spacer(Modifier.height(12.dp))
                    FormLabel(
                        if (state.backupFolderLabel != null) "Snapshots"
                        else "Snapshots in Downloads/FoodTracker",
                    )
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
                }
            }

            item {
                FormSection(title = "Export & import") {
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
                            Text("Export")
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
                            Text("Import")
                        }
                    }
                }
            }

            item {
                FormSection(title = "Export logs for AI") {
                    Text(
                        "A compact JSON of just your food log — foods are de-duplicated and " +
                            "empty nutrients dropped to keep it small. Hand it to an AI to review " +
                            "your eating habits and suggest what to improve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FormLabel("Range")
                    ExclusiveButtonGroup(
                        options = AnalysisRange.entries,
                        selected = analysisRange,
                        onSelect = { analysisRange = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { it.label },
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = {
                            haptics.light()
                            analysisExportLauncher.launch(analysisExportFilename(analysisRange))
                        },
                        enabled = !state.backupBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export logs (${analysisRange.label.lowercase()})")
                    }
                }
            }

            item {
                FormSection(title = "Custom foods") {
                    OutlinedButton(
                        onClick = {
                            haptics.light()
                            deleteCustomMode = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete all custom foods")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            Text("Folder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                folderLabel ?: "Downloads/FoodTracker",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(onClick = onPick) {
            Text(if (folderLabel == null) "Choose" else "Change")
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

@Composable
private fun DeleteAllCustomFoodsDialog(
    onKeepHistory: () -> Unit,
    onCascade: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete all custom foods?") },
        text = {
            Column {
                Text(
                    "This removes every food you added by hand or saved from a barcode " +
                        "scan. Seeded foods stay untouched. Choose how to handle log history:",
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Keep history — past diary entries still show the food name and nutrients, but it's gone from the picker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "• Delete with history — wipes every past log entry that used a custom food. Daily totals will change.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepHistory) { Text("Keep history") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onCascade,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete with history") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun defaultExportFilename(): String {
    val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(Date())
    return "foodtracker-$stamp.json"
}

private fun analysisExportFilename(range: AnalysisRange): String {
    val stamp = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(Date())
    return "foodtracker-log-${range.fileTag}-$stamp.json"
}

private fun humanReadableSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
