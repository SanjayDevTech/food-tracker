package com.example.foodtracker.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.AiProvider
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.Sex
import com.example.foodtracker.data.WeightChangeRate
import com.example.foodtracker.reminder.BatteryOptHelper
import com.example.foodtracker.reminder.ReminderConfig
import com.example.foodtracker.reminder.ReminderKind
import com.example.foodtracker.reminder.rememberBatteryOptExempt
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.HapticSlider
import com.example.foodtracker.ui.components.NumberStepper
import com.example.foodtracker.ui.components.TimePickerDialog
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.screens.settings.BackupCadence
import kotlinx.coroutines.launch

private const val PageCount = 5

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm: OnboardingViewModel = appViewModel {
        OnboardingViewModel(
            it.profileStore,
            it.backupManager,
            it.backupScheduler,
            it.reminderStore,
            it.reminderScheduler,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val pagerState = rememberPagerState(pageCount = { PageCount })
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { vm.setBackupFolder(it, decodeFolderLabel(it.toString())) }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PageDots(
                count = PageCount,
                selected = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false,
            ) { page ->
                when (page) {
                    0 -> BasicsPage(state = state, vm = vm)
                    1 -> GoalPage(state = state, vm = vm)
                    2 -> BackupsPage(
                        state = state,
                        onPickFolder = { folderLauncher.launch(null) },
                        onClearFolder = vm::clearBackupFolder,
                        onCadence = vm::setBackupCadence,
                    )
                    3 -> RemindersPage(
                        configs = state.reminders,
                        onToggle = vm::setReminderEnabled,
                        onSetTime = vm::setReminderTime,
                    )
                    4 -> AiPage(state = state, vm = vm)
                }
            }

            BottomNav(
                pageIndex = pagerState.currentPage,
                pageCount = PageCount,
                onBack = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                onContinue = {
                    if (pagerState.currentPage < PageCount - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        haptics.confirm()
                        vm.finish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

// ───────────────────────── Page 1: About you ─────────────────────

@Composable
private fun BasicsPage(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("About you", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "A few quick details so we can calculate your targets accurately.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        PrivacyTagline()

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.profile.name,
            onValueChange = vm::setName,
            label = { Text("Your name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Sex")
        ExclusiveButtonGroup(
            options = Sex.entries,
            selected = state.profile.sex,
            onSelect = vm::setSex,
            modifier = Modifier.fillMaxWidth(),
            label = { it.displayName },
        )

        Spacer(Modifier.height(20.dp))
        StepperSliderRow(
            label = "Age",
            displayValue = "${state.profile.ageYears} yrs",
            sliderValue = state.profile.ageYears.toFloat(),
            sliderRange = 10f..90f,
            granularity = 1f,
            onSliderChange = { vm.setAge(it.toInt()) },
            onDecrement = { vm.setAge(state.profile.ageYears - 1) },
            onIncrement = { vm.setAge(state.profile.ageYears + 1) },
        )

        Spacer(Modifier.height(8.dp))
        StepperSliderRow(
            label = "Height",
            displayValue = "${state.profile.heightCm} cm",
            sliderValue = state.profile.heightCm.toFloat(),
            sliderRange = 120f..220f,
            granularity = 1f,
            onSliderChange = { vm.setHeight(it.toInt()) },
            onDecrement = { vm.setHeight(state.profile.heightCm - 1) },
            onIncrement = { vm.setHeight(state.profile.heightCm + 1) },
        )

        Spacer(Modifier.height(8.dp))
        StepperSliderRow(
            label = "Weight",
            displayValue = "%.1f kg".format(state.profile.weightKg),
            sliderValue = state.profile.weightKg.toFloat(),
            sliderRange = 18f..200f,
            granularity = 1f,
            onSliderChange = { vm.setWeight(it.roundToInt().toDouble()) },
            onDecrement = { vm.setWeight(state.profile.weightKg - 0.1) },
            onIncrement = { vm.setWeight(state.profile.weightKg + 0.1) },
        )

        Spacer(Modifier.height(20.dp))
        com.example.foodtracker.ui.components.BmiBar(
            currentBmi = com.example.foodtracker.domain.Bmi.bmi(
                state.profile.weightKg, state.profile.heightCm,
            ),
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ───────────────────────── Page 2: Your goal ─────────────────────

@Composable
private fun GoalPage(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("Your goal", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pick an activity level and where you want to land. We'll compute calorie and nutrient targets from this.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Activity level")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevel.entries.forEach { level ->
                SelectableRow(
                    label = level.displayName,
                    selected = state.profile.activityLevel == level,
                    onClick = { vm.setActivityLevel(level) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Goal")
        ExclusiveButtonGroup(
            options = Goal.entries,
            selected = state.profile.goal,
            onSelect = vm::setGoal,
            modifier = Modifier.fillMaxWidth(),
            label = { it.displayName },
        )

        if (state.profile.goal != Goal.Maintain) {
            Spacer(Modifier.height(16.dp))
            SectionLabel(
                if (state.profile.goal == Goal.Lose) "How fast to lose?" else "How fast to gain?"
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightChangeRate.entries.forEach { rate ->
                    SelectableRow(
                        label = rate.displayName,
                        selected = state.profile.weightChangeRate == rate,
                        onClick = { vm.setWeightChangeRate(rate) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            val goalWeight = state.profile.goalWeightKg ?: state.profile.weightKg
            val goalRange = if (state.profile.goal == Goal.Lose) {
                18f..state.profile.weightKg.toFloat().coerceAtLeast(18.1f)
            } else {
                state.profile.weightKg.toFloat().coerceAtMost(199.9f)..200f
            }
            StepperSliderRow(
                label = "Goal weight",
                displayValue = "%.1f kg".format(goalWeight),
                sliderValue = goalWeight.toFloat(),
                sliderRange = goalRange,
                granularity = 1f,
                onSliderChange = { vm.setGoalWeight(it.roundToInt().toDouble()) },
                onDecrement = { vm.setGoalWeight(goalWeight - 0.1) },
                onIncrement = { vm.setGoalWeight(goalWeight + 0.1) },
            )
        }

        Spacer(Modifier.height(20.dp))
        com.example.foodtracker.ui.components.BmiBar(
            currentBmi = com.example.foodtracker.domain.Bmi.bmi(
                state.profile.weightKg, state.profile.heightCm,
            ),
            goalBmi = state.profile.goalWeightKg?.let {
                com.example.foodtracker.domain.Bmi.bmi(it, state.profile.heightCm)
            },
        )

        Spacer(Modifier.height(24.dp))
        // Hero card anchoring the page — kept filled for emphasis (matches the
        // home GoalProgressBanner pattern). Symmetric 20dp corners now instead
        // of the asymmetric 28/8/28/8 shape so it sits flat with the rest of
        // the outlined-card design language.
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    "Your estimated daily targets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(10.dp))
                TargetRow("Calories", "${state.previewGoals.calories.toInt()} kcal")
                TargetRow("Protein", "${state.previewGoals.protein.toInt()} g")
                TargetRow("Carbs", "${state.previewGoals.carbs.toInt()} g")
                TargetRow("Fat", "${state.previewGoals.fat.toInt()} g")
                TargetRow("Fiber", "${state.previewGoals.fiber.toInt()} g")
                TargetRow("Water", "${state.previewGoals.waterMl} ml")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ───────────────────────── Page 3: Backups ─────────────────────────

@Composable
private fun BackupsPage(
    state: OnboardingUiState,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onCadence: (BackupCadence) -> Unit,
) {
    val haptics = rememberAppHaptics()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        PageHeader(title = "Keep your data safe", optional = true)
        Spacer(Modifier.height(4.dp))
        Text(
            "Snapshots are plain JSON. Pick a folder you sync (Drive, Dropbox, photos) — " +
                "or leave it on the default Downloads/FoodTracker. You can change this anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Backup folder")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.backupFolderLabel ?: "Downloads/FoodTracker (default)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    if (state.backupFolderLabel == null) "Tap Choose to pick any folder"
                    else "Selected — backups will land here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = {
                haptics.light()
                onPickFolder()
            }) {
                Text(if (state.backupFolderLabel == null) "Choose" else "Change")
            }
            if (state.backupFolderLabel != null) {
                TextButton(onClick = onClearFolder) { Text("Reset") }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Recurring backup")
        Text(
            "Runs in the background only when you turn it on. You can change this anytime in Profile › Data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ExclusiveButtonGroup(
            options = BackupCadence.entries,
            selected = state.backupCadence,
            onSelect = onCadence,
            modifier = Modifier.fillMaxWidth(),
            label = { it.label },
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ───────────────────────── Page 4: Reminders ─────────────────────

@Composable
private fun RemindersPage(
    configs: List<ReminderConfig>,
    onToggle: (ReminderKind, Boolean) -> Unit,
    onSetTime: (ReminderKind, Int, Int) -> Unit,
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        PageHeader(title = "Daily reminders", optional = true)
        Spacer(Modifier.height(4.dp))
        Text(
            "Turn on the ones you want — meals, water, weigh-in. Skip them all if you'd rather log when you remember. " +
                "Times use inexact alarms so they're battery-friendly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!notificationsGranted) {
            Spacer(Modifier.height(16.dp))
            NotificationsBlockedCard(
                onAllow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        // All reminder rows grouped inside one outlined-card section with
        // dividers between them — same pattern as the main Reminders screen
        // (no more N stacked individual cards).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            configs.forEachIndexed { index, config ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
                ReminderRow(
                    config = config,
                    onToggle = { onToggle(config.kind, it) },
                    onEditTime = { editingKind = config.kind },
                )
            }
        }

        val batteryExempt by rememberBatteryOptExempt()
        val anyReminderEnabled = configs.any { it.enabled }
        if (anyReminderEnabled && !batteryExempt) {
            Spacer(Modifier.height(16.dp))
            BatteryOptOutlinedCard(
                onOpenSettings = { BatteryOptHelper.openSettings(context) },
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    editingKind?.let { kind ->
        val current = configs.firstOrNull { it.kind == kind }
        if (current != null) {
            TimePickerDialog(
                initialHour = current.hour,
                initialMinute = current.minute,
                onDismiss = { editingKind = null },
                onConfirm = { h, m ->
                    onSetTime(kind, h, m)
                    editingKind = null
                },
            )
        }
    }
}

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
            "Allow notifications",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            "We need notification permission to fire the reminders you set.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onAllow) { Text("Allow notifications") }
    }
}

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
            "Android's battery saver can delay reminders by minutes or hours — especially on OEM " +
                "skins (Xiaomi / Oppo / Vivo / Realme / Samsung). Allow this app to ignore battery " +
                "optimisation so reminders fire on time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text("Open battery settings")
        }
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
            Spacer(Modifier.height(4.dp))
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

// ───────────────────────── Page 5: AI autofill ───────────────────

/**
 * Optional, and off unless the user turns it on. Mirrors the AI section in
 * Settings rather than adding a second way to configure the same thing — the
 * point of having it here is that someone who would use it finds out it exists
 * instead of discovering it months later.
 *
 * Nothing is written unless the toggle is on; see OnboardingViewModel.finish.
 */
@Composable
private fun AiPage(state: OnboardingUiState, vm: OnboardingViewModel) {
    val haptics = rememberAppHaptics()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        PageHeader(title = "AI nutrition autofill", optional = true)
        Spacer(Modifier.height(4.dp))
        Text(
            "Adding a food by hand means typing in every nutrient. An AI can fill them " +
                "from the name instead. You bring your own API key, only the food name is " +
                "sent, and you can set this up later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Enable AI autofill",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Needs an API key from your chosen provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.aiEnabled,
                onCheckedChange = {
                    haptics.light()
                    vm.setAiEnabled(it)
                },
            )
        }

        if (state.aiEnabled) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Provider")
            ExclusiveButtonGroup(
                options = AiProvider.entries,
                selected = state.aiProvider,
                onSelect = vm::setAiProvider,
                modifier = Modifier.fillMaxWidth(),
                label = { it.displayName },
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("Model")
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
                "${state.aiProvider.defaultModel.label} is the cheapest and accurate enough " +
                    "for most foods — leave it selected to save money.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
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
                "Your key stays on this device and is kept out of cloud backup.",
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

        Spacer(Modifier.height(24.dp))
    }
}

// ───────────────────────── Bottom nav + dots ─────────────────────

@Composable
private fun BottomNav(
    pageIndex: Int,
    pageCount: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLast = pageIndex == pageCount - 1
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pageIndex > 0) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(if (isLast) "Get started" else "Continue")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val isActive = i == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isActive) 10.dp else 8.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

// ───────────────────────── Shared row helpers ───────────────────

@Composable
private fun StepperSliderRow(
    label: String,
    displayValue: String,
    sliderValue: Float,
    sliderRange: ClosedFloatingPointRange<Float>,
    granularity: Float,
    onSliderChange: (Float) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            NumberStepper(
                value = displayValue,
                onDecrement = onDecrement,
                onIncrement = onIncrement,
                decrementEnabled = sliderValue > sliderRange.start,
                incrementEnabled = sliderValue < sliderRange.endInclusive,
            )
        }
        HapticSlider(
            value = sliderValue,
            onValueChange = onSliderChange,
            valueRange = sliderRange,
            granularity = granularity,
        )
    }
}

/**
 * First-screen reassurance: the app is local-first and only touches the network
 * for barcode lookups. Kept filled for hero emphasis (same pattern as the home
 * GoalProgressBanner). Corners normalized to a symmetric 20dp shape so it
 * lines up with the rest of the outlined-card layout language.
 */
@Composable
private fun PrivacyTagline() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Yours alone.\nOn this device.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            PrivacyLine(
                icon = Icons.Default.CloudOff,
                text = "Nothing is uploaded. Your profile, logs, and weight history stay in this app.",
            )
            PrivacyLine(
                icon = Icons.Default.QrCodeScanner,
                text = "The internet is used only when you scan a barcode — to fetch the product from OpenFoodFacts / USDA.",
            )
        }
    }
}

@Composable
private fun PrivacyLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PageHeader(title: String, optional: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (optional) {
            Spacer(Modifier.width(10.dp))
            OptionalBadge()
        }
    }
}

@Composable
private fun OptionalBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            "Optional",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/**
 * Full-width selectable row, outlined-card style. Unselected → 1dp
 * `outlineVariant` border. Selected → 1.5dp `primary` border + trailing
 * check + primary-toned label so the choice reads at a glance without
 * needing a filled background fight.
 */
@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 1.5.dp else 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TargetRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Best-effort human label for a SAF tree URI, e.g. `primary:Documents/Backups`. */
private fun decodeFolderLabel(uri: String): String? {
    val decoded = runCatching { java.net.URLDecoder.decode(uri, "UTF-8") }.getOrNull() ?: return null
    val docIdx = decoded.indexOf("/tree/")
    if (docIdx < 0) return decoded
    return decoded.substring(docIdx + "/tree/".length)
}
