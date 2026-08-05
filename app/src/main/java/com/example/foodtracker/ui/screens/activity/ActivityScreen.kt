package com.example.foodtracker.ui.screens.activity

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.ActivityType
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.DailyValue
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.HapticSlider
import com.example.foodtracker.ui.components.WeeklyBarChart
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(onBack: () -> Unit) {
    val vm: ActivityViewModel = appViewModel { ActivityViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity & exercise") },
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
                ActivityHeadlineCard(totalBurned = state.totalBurned)
            }
            item {
                ActivitySection(title = "Log activity") {
                    val rows = ActivityType.entries.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { row ->
                            ExclusiveButtonGroup(
                                options = row,
                                selected = state.selectedType,
                                onSelect = vm::setType,
                                modifier = Modifier.fillMaxWidth(),
                                label = { it.displayName },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Duration: ${state.minutes} min",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    HapticSlider(
                        value = state.minutes.toFloat(),
                        onValueChange = { vm.setMinutes(it.toInt()) },
                        valueRange = 5f..180f,
                        steps = 34,
                        granularity = 5f,
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
                ActivitySection(title = "Last 7 days") {
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
            // "Today's activity" stays a flat heading + lazy rows (same pattern
            // as History on Weight and Today's entries on Water). One section
            // item would defeat LazyColumn's lazy rendering once entries pile up.
            item {
                Text(
                    "Today's activity",
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
                            "Nothing logged yet. Let's move 🏃",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    val type = ActivityType.fromName(entry.activityType)
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
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
private fun ActivitySection(
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
private fun ActivityHeadlineCard(totalBurned: Double) {
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
            "${totalBurned.toInt()} kcal",
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
