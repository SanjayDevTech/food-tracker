package com.example.foodtracker.ui.screens.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.DailyValue
import com.example.foodtracker.ui.components.SectionCard
import com.example.foodtracker.ui.components.WeeklyBarChart
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroFiber
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.ui.theme.MacroWater
import com.example.foodtracker.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen() {
    val vm: TrendsViewModel = appViewModel { TrendsViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trends") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = Dimens.ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
        ) {
            item {
                SectionCard(title = "Calories — last 7 days") {
                    WeeklyBarChart(
                        bars = state.days.map { DailyValue(DateUtil.shortDay(it.epochDay), it.nutrients.calories) },
                        goal = state.goals.calories,
                        barColor = MacroCarbs,
                        unit = "kcal",
                    )
                    Spacer(Modifier.height(8.dp))
                    AverageRow("Average", state.days.map { it.nutrients.calories }.average(), "kcal")
                }
            }
            item {
                SectionCard(title = "Protein — last 7 days") {
                    WeeklyBarChart(
                        bars = state.days.map { DailyValue(DateUtil.shortDay(it.epochDay), it.nutrients.protein) },
                        goal = state.goals.protein,
                        barColor = MacroProtein,
                        unit = "g",
                    )
                    Spacer(Modifier.height(8.dp))
                    AverageRow("Average", state.days.map { it.nutrients.protein }.average(), "g")
                }
            }
            item {
                SectionCard(title = "Fiber — last 7 days") {
                    WeeklyBarChart(
                        bars = state.days.map { DailyValue(DateUtil.shortDay(it.epochDay), it.nutrients.fiber) },
                        goal = state.goals.fiber,
                        barColor = MacroFiber,
                        unit = "g",
                    )
                    Spacer(Modifier.height(8.dp))
                    AverageRow("Average", state.days.map { it.nutrients.fiber }.average(), "g")
                }
            }
            item {
                SectionCard(title = "Water — last 7 days") {
                    WeeklyBarChart(
                        bars = state.days.map { DailyValue(DateUtil.shortDay(it.epochDay), it.waterMl.toDouble()) },
                        goal = state.goals.waterMl.toDouble(),
                        barColor = MacroWater,
                        unit = "ml",
                    )
                    Spacer(Modifier.height(8.dp))
                    AverageRow("Average", state.days.map { it.waterMl.toDouble() }.average(), "ml")
                }
            }
            item {
                SectionCard(title = "Calories burned — last 7 days") {
                    WeeklyBarChart(
                        bars = state.days.map { DailyValue(DateUtil.shortDay(it.epochDay), it.caloriesBurned) },
                        goal = null,
                        barColor = MacroBurned,
                        unit = "kcal",
                    )
                    Spacer(Modifier.height(8.dp))
                    AverageRow("Average", state.days.map { it.caloriesBurned }.average(), "kcal")
                }
            }
        }
    }
}

@Composable
private fun AverageRow(label: String, value: Double, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${value.toInt()} $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
