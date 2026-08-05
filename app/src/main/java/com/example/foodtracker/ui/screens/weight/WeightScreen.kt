package com.example.foodtracker.ui.screens.weight

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.components.LineChart
import com.example.foodtracker.ui.components.LinePoint
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.util.DateUtil
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(onBack: () -> Unit) {
    val vm: WeightViewModel = appViewModel { WeightViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    val haptics = rememberAppHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight tracking") },
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
            item {
                WeightHeadlineCard(
                    latest = state.latest,
                    sevenDayAvg = state.sevenDayAvg,
                    change30d = state.change30d,
                )
            }

            item {
                WeightSection(title = "Trend") {
                    val sorted = state.entries.sortedBy { it.dateEpochDay }
                    if (sorted.size < 2) {
                        Text(
                            if (sorted.isEmpty()) "Log two or more weigh-ins to see your trend over time."
                            else "Log one more weigh-in to see your trend over time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val points = sorted
                            .takeLast(30)
                            .map { LinePoint(DateUtil.shortDay(it.dateEpochDay), it.weightKg) }
                        LineChart(
                            points = points,
                            lineColor = MacroProtein,
                            unit = "kg",
                            referenceLine = state.profile.goalWeightKg,
                            referenceLabel = state.profile.goalWeightKg?.let { "Goal %.1f kg".format(it) },
                        )
                    }
                }
            }

            item {
                WeightSection(title = "Log today's weight") {
                    OutlinedTextField(
                        value = state.draftKg,
                        onValueChange = vm::setDraft,
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = vm::setNote,
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Update profile weight", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Keep your calorie & nutrient targets in sync.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.syncToProfile, onCheckedChange = vm::setSyncToProfile)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            haptics.confirm()
                            vm.saveEntry()
                        },
                        modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
                        enabled = state.draftKg.toDoubleOrNull()?.let { it in 20.0..400.0 } == true,
                    ) { Text("Save weight") }
                }
            }

            // "History" stays a standalone heading rather than wrapping every
            // row in an outlined section card — history can run to hundreds of
            // entries, and a single eagerly-rendered section would defeat
            // LazyColumn's lazy item rendering. Entries below scan as one list
            // under this heading thanks to consistent padding + dividers.
            item {
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            if (state.entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            "No entries yet. Log your first weight above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${"%.1f".format(entry.weightKg)} kg",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    listOfNotNull(
                                        DateUtil.epochDayToDate(entry.dateEpochDay).format(dateFmt),
                                        entry.note,
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.deleteEntry(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightSection(
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
private fun WeightHeadlineCard(
    latest: com.example.foodtracker.data.db.WeightLogEntity?,
    sevenDayAvg: Double?,
    change30d: Double?,
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
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            if (latest != null) "${"%.1f".format(latest.weightKg)} kg" else "—",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (latest != null) "as of ${DateUtil.formatHeader(latest.dateEpochDay)}" else "Log your first weight to start tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (sevenDayAvg != null || change30d != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                sevenDayAvg?.let {
                    StatBlock("7-day avg", "${"%.1f".format(it)} kg")
                }
                change30d?.let {
                    val sign = if (it > 0) "+" else ""
                    StatBlock("30-day change", "$sign${"%.1f".format(it)} kg")
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
