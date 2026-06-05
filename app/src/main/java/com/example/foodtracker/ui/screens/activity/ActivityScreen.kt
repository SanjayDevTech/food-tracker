package com.example.foodtracker.ui.screens.activity

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.ActivityType
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.components.DailyValue
import com.example.foodtracker.ui.components.SectionCard
import com.example.foodtracker.ui.components.WeeklyBarChart
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen() {
    val vm: ActivityViewModel = appViewModel { ActivityViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    // No navigation icon: Activity is a bottom-tab destination — system back
    // gesture handles deep entries from Home's "Burned" tile.
    Scaffold(
        topBar = { TopAppBar(title = { Text("Activity & exercise") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = Dimens.ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
        ) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${state.totalBurned.toInt()} kcal",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MacroBurned,
                        )
                        Text(
                            "burned today via exercise & activity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SectionCard(title = "Log activity") {
                    Text(
                        "Pick an activity and tell us how long.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    val rows = ActivityType.entries.chunked(3)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.forEach { type ->
                                FilterChip(
                                    selected = state.selectedType == type,
                                    onClick = { vm.setType(type) },
                                    label = { Text(type.displayName) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Duration: ${state.minutes} min",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = state.minutes.toFloat(),
                        onValueChange = { vm.setMinutes(it.toInt()) },
                        valueRange = 5f..180f,
                        steps = 34,
                    )
                    Text(
                        "Estimated burn: ${state.previewBurned.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            haptics.confirm()
                            vm.logCurrent()
                        },
                        modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
                    ) {
                        Text("Log ${state.selectedType.displayName.lowercase()}")
                    }
                }
            }
            item {
                SectionCard(title = "Last 7 days") {
                    WeeklyBarChart(
                        bars = state.weekTotals.map { (day, value) ->
                            DailyValue(DateUtil.shortDay(day), value)
                        },
                        goal = null,
                        barColor = MacroBurned,
                        unit = "kcal",
                    )
                }
            }
            item {
                Text(
                    "Today's activity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Nothing logged yet. Let's move 🏃",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    val type = ActivityType.fromName(entry.activityType)
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    type.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "${entry.minutes} min · ${entry.caloriesBurned.toInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.remove(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
