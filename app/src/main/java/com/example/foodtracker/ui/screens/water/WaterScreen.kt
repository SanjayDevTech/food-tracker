package com.example.foodtracker.ui.screens.water

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.components.DailyValue
import com.example.foodtracker.ui.components.NutrientBar
import com.example.foodtracker.ui.components.WeeklyBarChart
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroWater
import com.example.foodtracker.util.DateUtil
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(onBack: () -> Unit) {
    val vm: WaterViewModel = appViewModel { WaterViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    val haptics = rememberAppHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water tracker") },
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
                WaterHeadlineCard(
                    totalMl = state.totalMl,
                    goalMl = state.goals.waterMl,
                )
            }
            item {
                WaterSection(title = "Quick add") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(100, 200, 250, 500).forEach { ml ->
                            OutlinedButton(
                                onClick = {
                                    haptics.confirm()
                                    vm.addWater(ml)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("$ml ml") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            haptics.confirm()
                            vm.addWater(1000)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("+ 1 L bottle") }
                }
            }
            item {
                WaterSection(title = "Last 7 days") {
                    WeeklyBarChart(
                        bars = state.weekTotals.map {
                            DailyValue(DateUtil.shortDay(it.first), it.second.toDouble())
                        },
                        goal = state.goals.waterMl.toDouble(),
                        barColor = MacroWater,
                        unit = "ml",
                    )
                }
            }
            // "Today's entries" stays a flat heading + lazy rows (matching the
            // History pattern on WeightScreen). Wrapping every entry in its own
            // card stacked vertically reads heavy; a single section item would
            // defeat LazyColumn's lazy rendering once a day's log gets long.
            item {
                Text(
                    "Today's entries",
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
                            "No water logged yet. Stay hydrated 💧",
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
                                    "${entry.amountMl} ml",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    java.time.Instant.ofEpochMilli(entry.loggedAtMillis)
                                        .atZone(DateUtil.zoneId()).toLocalTime().format(timeFormatter),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.removeWater(entry.id) }) {
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
private fun WaterSection(
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
private fun WaterHeadlineCard(totalMl: Int, goalMl: Int) {
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
            "$totalMl ml",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "of $goalMl ml goal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        NutrientBar(
            label = "Today",
            consumed = totalMl.toDouble(),
            goal = goalMl.toDouble(),
            unit = "ml",
            color = MacroWater,
        )
    }
}
