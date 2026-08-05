package com.example.foodtracker.ui.screens.progress

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.LineChart
import com.example.foodtracker.ui.components.LinePoint
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroWater
import com.example.foodtracker.ui.theme.MacroWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val vm: ProgressViewModel = appViewModel { ProgressViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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
            item("range") {
                ExclusiveButtonGroup(
                    options = ProgressRange.entries,
                    selected = state.range,
                    onSelect = vm::setRange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { it.label },
                )
            }

            item("calories") {
                ProgressSection(
                    title = "Calories eaten",
                    subtitle = if (state.caloriesInAvg > 0) "${state.caloriesInAvg} kcal / logged day" else null,
                ) {
                    ProgressChart(state.caloriesIn, MacroCarbs, "kcal")
                }
            }

            item("weight") {
                ProgressSection(
                    title = "Weight",
                    subtitle = state.weightChange?.let {
                        val sign = if (it > 0) "+" else ""
                        "$sign${"%.1f".format(it)} kg over ${state.range.label.lowercase()}"
                    },
                ) {
                    if (state.weight.size < 2) {
                        EmptyChart("Log two or more weigh-ins in this range to see the trend.")
                    } else {
                        LineChart(
                            points = state.weight,
                            lineColor = MacroWeight,
                            unit = "kg",
                            referenceLine = state.goalWeightKg,
                            referenceLabel = state.goalWeightKg?.let { "Goal %.1f kg".format(it) },
                        )
                    }
                }
            }

            item("water") {
                ProgressSection(
                    title = "Water",
                    subtitle = if (state.waterAvgMl > 0) "${state.waterAvgMl} ml / day avg" else null,
                ) {
                    ProgressChart(state.water, MacroWater, "ml")
                }
            }

            item("burned") {
                ProgressSection(
                    title = "Active calories",
                    subtitle = if (state.burnedAvg > 0) "${state.burnedAvg} kcal / active day" else null,
                ) {
                    ProgressChart(state.burned, MacroBurned, "kcal")
                }
            }
        }
    }
}

@Composable
private fun ProgressChart(points: List<LinePoint>, color: Color, unit: String) {
    // A flat all-zero series carries no signal — show a hint instead of a line
    // pinned to the axis.
    if (points.size < 2 || points.all { it.value == 0.0 }) {
        EmptyChart("No data logged in this range yet.")
    } else {
        LineChart(points = points, lineColor = color, unit = unit)
    }
}

@Composable
private fun EmptyChart(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProgressSection(
    title: String,
    subtitle: String? = null,
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
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}
