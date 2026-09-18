package com.example.foodtracker.ui.screens.profile

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.AllNutrientFields
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.Sex
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.WeightChangeRate
import com.example.foodtracker.domain.Bmi
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.BmiBar
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.FormLabel
import com.example.foodtracker.ui.components.FormSection
import com.example.foodtracker.ui.components.HapticFilterChip
import com.example.foodtracker.ui.components.HapticSlider
import com.example.foodtracker.ui.components.NumberStepper
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.Dimens

/**
 * Who the user is and what they are aiming at: the header card, the edit form,
 * and the daily targets those details produce.
 *
 * App behaviour — reminders, appearance, AI autofill, backups, export/import,
 * custom foods — moved to
 * [com.example.foodtracker.ui.screens.settings.SettingsScreen]. Keeping the two
 * apart is what stops this screen turning back into the ten-section wall it
 * used to be.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
) {
    val vm: ProfileViewModel = appViewModel { ProfileViewModel(it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & goals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
                    FormSection(title = "Edit profile") {
                        OutlinedTextField(
                            value = state.draft.name,
                            onValueChange = { v -> vm.updateDraft { it.copy(name = v) } },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        FormLabel("Sex")
                        ExclusiveButtonGroup(
                            options = Sex.entries,
                            selected = state.draft.sex,
                            onSelect = { sex -> vm.updateDraft { it.copy(sex = sex) } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { it.displayName },
                        )
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
                            range = 18f..200f,
                            steps = 0,
                            unit = "kg",
                            onChange = { vm.updateDraft { p -> p.copy(weightKg = ((it * 10).toInt() / 10.0)) } },
                            valueFormatter = { "%.1f".format(it) },
                            granularity = 1f,
                            stepBy = 0.1f,
                            onSliderChange = {
                                vm.updateDraft { p -> p.copy(weightKg = it.roundToInt().toDouble()) }
                            },
                        )

                        FormLabel("Activity level")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActivityLevel.entries.forEach { level ->
                                HapticFilterChip(
                                    selected = state.draft.activityLevel == level,
                                    onClick = { vm.updateDraft { it.copy(activityLevel = level) } },
                                    label = { Text(level.displayName) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FormLabel("Goal")
                        ExclusiveButtonGroup(
                            options = Goal.entries,
                            selected = state.draft.goal,
                            onSelect = vm::setDraftGoal,
                            modifier = Modifier.fillMaxWidth(),
                            label = { it.displayName },
                        )
                        if (state.draft.goal != Goal.Maintain) {
                            Spacer(Modifier.height(8.dp))
                            FormLabel(
                                if (state.draft.goal == Goal.Lose) "Rate of loss" else "Rate of gain",
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                WeightChangeRate.entries.forEach { rate ->
                                    HapticFilterChip(
                                        selected = state.draft.weightChangeRate == rate,
                                        onClick = { vm.updateDraft { it.copy(weightChangeRate = rate) } },
                                        label = { Text(rate.displayName) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            val goalWeight = state.draft.goalWeightKg ?: state.draft.weightKg
                            val goalRange = if (state.draft.goal == Goal.Lose) {
                                18f..state.draft.weightKg.toFloat().coerceAtLeast(18.1f)
                            } else {
                                state.draft.weightKg.toFloat().coerceAtMost(199.9f)..200f
                            }
                            SliderRow(
                                label = "Goal weight",
                                value = goalWeight.toFloat(),
                                range = goalRange,
                                steps = 0,
                                unit = "kg",
                                onChange = {
                                    vm.updateDraft { p -> p.copy(goalWeightKg = ((it * 10).toInt() / 10.0)) }
                                },
                                valueFormatter = { "%.1f".format(it) },
                                granularity = 1f,
                                stepBy = 0.1f,
                                onSliderChange = {
                                    vm.updateDraft { p -> p.copy(goalWeightKg = it.roundToInt().toDouble()) }
                                },
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        BmiBar(
                            currentBmi = Bmi.bmi(state.draft.weightKg, state.draft.heightCm),
                            goalBmi = state.draft.goalWeightKg?.let {
                                Bmi.bmi(it, state.draft.heightCm)
                            },
                        )
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
                FormSection(title = "Daily targets") {
                    val grouped = AllNutrientFields.groupBy { it.category }
                    NutrientCategory.entries.forEachIndexed { index, cat ->
                        if (index > 0) Spacer(Modifier.height(10.dp))
                        CategorySubheader(label = cat.displayLabel())
                        grouped[cat].orEmpty().forEach { field ->
                            TargetRow(
                                label = field.label,
                                value = "${field.goal(state.goals).toInt()} ${field.unit.symbol}",
                            )
                        }
                        if (cat == NutrientCategory.Macros) {
                            TargetRow(label = "Water", value = "${state.goals.waterMl} ml")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarEmoji(profile.sex, profile.ageYears)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name.ifBlank { "Welcome" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    buildString {
                        append(profile.goal.displayName)
                        if (profile.goal != Goal.Maintain) {
                            append(" · ")
                            append("%.2f".format(profile.weightChangeRate.kgPerWeek))
                            append(" kg/week")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatBlock(label = "Age", value = "${profile.ageYears}", unit = "yrs")
            StatBlock(label = "Height", value = "${profile.heightCm}", unit = "cm")
            StatBlock(label = "Weight", value = "%.1f".format(profile.weightKg), unit = "kg")
        }
        Spacer(Modifier.height(8.dp))
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

@Composable
private fun AvatarEmoji(sex: Sex, ageYears: Int) {
    val emoji = when (sex) {
        Sex.Male -> when {
            ageYears < 18 -> "👦"
            ageYears >= 60 -> "👴"
            else -> "👨"
        }
        Sex.Female -> when {
            ageYears < 18 -> "👧"
            ageYears >= 60 -> "👵"
            else -> "👩"
        }
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            emoji,
            fontSize = 30.sp,
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String, unit: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    unit: String,
    onChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
    granularity: Float = 1f,
    stepBy: Float = 1f,
    onSliderChange: (Float) -> Unit = onChange,
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
                value = "${valueFormatter(value)} $unit",
                onDecrement = { onChange((value - stepBy).coerceIn(range.start, range.endInclusive)) },
                onIncrement = { onChange((value + stepBy).coerceIn(range.start, range.endInclusive)) },
                decrementEnabled = value > range.start,
                incrementEnabled = value < range.endInclusive,
            )
        }
        HapticSlider(
            value = value,
            onValueChange = onSliderChange,
            valueRange = range,
            steps = steps,
            granularity = granularity,
        )
    }
}
