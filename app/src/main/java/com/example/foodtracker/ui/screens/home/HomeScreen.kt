package com.example.foodtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.AllNutrientFields
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.components.CalorieRing
import com.example.foodtracker.ui.components.DonutSlice
import com.example.foodtracker.ui.components.MacroDonut
import com.example.foodtracker.ui.components.NutrientBar
import com.example.foodtracker.ui.components.SectionCard
import com.example.foodtracker.ui.components.smartRound
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroFat
import com.example.foodtracker.ui.theme.MacroFiber
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.ui.theme.MacroWater
import com.example.foodtracker.util.DateUtil

@Composable
fun HomeScreen(
    onAddFoodToMeal: (MealType) -> Unit,
    onOpenWater: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    val vm: HomeViewModel = appViewModel { HomeViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()

    // Scaffold here consumes the status-bar inset via default contentWindowInsets,
    // so we don't double-pad with the outer Scaffold (which sets contentWindowInsets
    // to zero). FAB lives in the Scaffold so it sits above the bottom NavigationBar.
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Log food") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { onAddFoodToMeal(currentMeal()) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = Dimens.ScreenPadding + innerPadding.calculateTopPadding(),
                // Extra bottom space so the FAB doesn't sit on top of the last row.
                bottom = 96.dp + innerPadding.calculateBottomPadding(),
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
        ) {
            item("greeting") { Greeting(state.profile.name) }
            item("dateSelector") {
                DateSelector(
                    epochDay = state.epochDay,
                    onPrev = { vm.shiftDay(-1) },
                    onNext = { vm.shiftDay(1) },
                    onToday = { vm.goToToday() },
                )
            }
            item("calorieRing") {
                // The hero ring sits directly on the AMOLED black background —
                // no card around it, so almost the whole row is pure #000000.
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    CalorieRing(
                        consumed = state.totalNutrients.calories,
                        burned = state.caloriesBurned,
                        goal = state.goals.calories,
                    )
                }
            }
            item("macroDonut") {
                SectionCard(title = "Macros") {
                    val n = state.totalNutrients
                    MacroDonut(
                        slices = listOf(
                            DonutSlice("Carbs", n.carbs, MacroCarbs),
                            DonutSlice("Protein", n.protein, MacroProtein),
                            DonutSlice("Fat", n.fat, MacroFat),
                            DonutSlice("Fiber", n.fiber, MacroFiber),
                        ),
                        centerCaption = "kcal from food",
                        centerValue = "${n.calories.toInt()}",
                    )
                }
            }
            item("quickStats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalDrink,
                        title = "Water",
                        value = "${state.waterMl} ml",
                        sub = "of ${state.goals.waterMl} ml",
                        tint = MacroWater,
                        onClick = onOpenWater,
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        title = "Burned",
                        value = "${state.caloriesBurned.toInt()} kcal",
                        sub = "exercise & activity",
                        tint = MacroBurned,
                        onClick = onOpenActivity,
                    )
                }
            }
            val quickList = (state.favourites + state.recent.filterNot { r -> state.favourites.any { it.id == r.id } }).take(8)
            if (quickList.isNotEmpty() || state.canCopyYesterday) {
                item("quickLog") {
                    QuickLogSection(
                        items = quickList,
                        canCopyYesterday = state.canCopyYesterday,
                        onLog = { vm.quickLog(it) },
                        onCopyYesterday = { vm.copyYesterday() },
                    )
                }
            }
            item("nutrientBreakdown") {
                NutrientBreakdown(state.totalNutrients, state.goals, state.logs)
            }
            MealType.entries.forEach { meal ->
                val items = state.mealGroups[meal].orEmpty()
                item("meal_${meal.name}") {
                    MealSection(
                        meal = meal,
                        rows = items,
                        onAdd = { onAddFoodToMeal(meal) },
                        onRemove = { vm.removeLog(it) },
                    )
                }
            }
        }
    }
}

private fun currentMeal(): MealType {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 11 -> MealType.Breakfast
        hour < 15 -> MealType.Lunch
        hour < 19 -> MealType.Snack
        else -> MealType.Dinner
    }
}

@Composable
private fun Greeting(name: String) {
    val displayName = name.ifBlank { "there" }
    val hour = java.time.LocalTime.now().hour
    val salutation = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column {
        Text("$salutation, $displayName 👋", style = MaterialTheme.typography.titleLarge)
        Text(
            "Here's how today is shaping up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DateSelector(epochDay: Long, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { haptics.light(); onPrev() }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            FilterChip(
                selected = epochDay == DateUtil.todayEpochDay(),
                onClick = { haptics.light(); onToday() },
                label = { Text(DateUtil.formatHeader(epochDay)) },
            )
        }
        IconButton(onClick = { haptics.light(); onNext() }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    sub: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NutrientBreakdown(
    consumed: Nutrients,
    goals: NutrientGoals,
    logs: List<FoodLogWithFood>,
) {
    val grouped = AllNutrientFields.groupBy { it.category }
    var selectedCategory by rememberSaveable { mutableStateOf(NutrientCategory.Macros) }
    var detailField by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.example.foodtracker.data.NutrientField?>(null)
    }
    val haptics = rememberAppHaptics()
    SectionCard(title = "Nutrient breakdown") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NutrientCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        haptics.light()
                        selectedCategory = cat
                    },
                    label = { Text(cat.name) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap any nutrient to see which foods contributed it today.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped[selectedCategory].orEmpty()
                .filter { it.key != "calories" || selectedCategory == NutrientCategory.Macros }
                .forEach { field ->
                    NutrientBar(
                        label = field.label,
                        consumed = field.read(consumed),
                        goal = field.goal(goals),
                        unit = field.unit.symbol,
                        color = field.color(),
                        onClick = {
                            haptics.light()
                            detailField = field
                        },
                    )
                }
        }
    }

    detailField?.let { field ->
        NutrientContributionSheet(
            field = field,
            consumed = consumed,
            goals = goals,
            logs = logs,
            onDismiss = { detailField = null },
        )
    }
}

private data class NutrientContribution(
    val name: String,
    val detail: String,
    val amount: Double,
    val shareOfTotal: Float,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NutrientContributionSheet(
    field: com.example.foodtracker.data.NutrientField,
    consumed: Nutrients,
    goals: NutrientGoals,
    logs: List<FoodLogWithFood>,
    onDismiss: () -> Unit,
) {
    val total = field.read(consumed)
    val goal = field.goal(goals)

    // Combine repeat logs of the same food into one row, then rank by contribution.
    val contributions = logs
        .groupBy { it.food.id }
        .mapNotNull { (_, rows) ->
            val food = rows.first().food
            val grams = rows.sumOf { it.log.grams }
            val amount = field.read(food.nutrientsPer100g() * (grams / 100.0))
            if (amount <= 0.0) return@mapNotNull null
            val entryNote = if (rows.size > 1) " · ${rows.size} entries" else ""
            NutrientContribution(
                name = food.name,
                detail = "${grams.toInt()} g$entryNote",
                amount = amount,
                shareOfTotal = if (total > 0) (amount / total).toFloat() else 0f,
            )
        }
        .sortedByDescending { it.amount }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(field.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            val goalSuffix = if (goal > 0) " · ${((total / goal) * 100).toInt()}% of ${goal.smartRound()} ${field.unit.symbol} goal" else ""
            Text(
                "${total.smartRound()} ${field.unit.symbol} today$goalSuffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (contributions.isEmpty()) {
                Text(
                    "Nothing logged today contributes ${field.label.lowercase()}. " +
                        "Log foods rich in it to close the gap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val barColor = field.color()
                contributions.forEach { c ->
                    ContributionRow(c, field.unit.symbol, barColor)
                }
            }
        }
    }
}

@Composable
private fun ContributionRow(
    c: NutrientContribution,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = c.shareOfTotal.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(450),
        label = "share",
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(c.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${c.amount.smartRound()} $unit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("${(c.shareOfTotal * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

private fun com.example.foodtracker.data.NutrientField.color(): androidx.compose.ui.graphics.Color = when (key) {
    "calories" -> MacroBurned
    "carbs" -> MacroCarbs
    "protein" -> MacroProtein
    "fat", "saturatedFat" -> MacroFat
    "fiber" -> MacroFiber
    else -> when (category) {
        NutrientCategory.Macros -> MacroBurned
        NutrientCategory.Minerals -> MacroFiber
        NutrientCategory.Vitamins -> MacroWater
    }
}

@Composable
private fun MealSection(
    meal: MealType,
    rows: List<FoodLogWithFood>,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    val mealCalories = rows.sumOf { it.food.calories * (it.log.grams / 100.0) }
    SectionCard(
        title = "${meal.emoji} ${meal.displayName}",
        trailing = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add to ${meal.displayName}")
            }
        },
    ) {
        if (rows.isEmpty()) {
            Text(
                "Nothing logged yet — tap + to add.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row ->
                MealEntryRow(row, onRemove = { onRemove(row.log.id) })
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${mealCalories.toInt()} kcal · ${rows.size} item${if (rows.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickLogSection(
    items: List<FoodEntity>,
    canCopyYesterday: Boolean,
    onLog: (FoodEntity) -> Unit,
    onCopyYesterday: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    SectionCard(
        title = "Quick log",
        trailing = if (canCopyYesterday) {
            {
                AssistChip(
                    onClick = {
                        haptics.confirm()
                        onCopyYesterday()
                    },
                    label = { Text("Copy yesterday") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        } else null,
    ) {
        if (items.isEmpty()) {
            Text(
                "Star foods or log a few meals — they'll appear here for one-tap logging.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (food.isFavourite) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    food.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                "${food.calories.toInt()} kcal · ${food.servingLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        IconButton(onClick = {
                            haptics.confirm()
                            onLog(food)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Log ${food.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(row: FoodLogWithFood, onRemove: () -> Unit) {
    val haptics = rememberAppHaptics()
    val cals = row.food.calories * (row.log.grams / 100.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val label = row.log.servingLabel.ifBlank { row.food.servingLabel }
            Text(
                "${"%.1f".format(row.log.servings)} × $label · ${cals.toInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = {
            haptics.confirm()
            onRemove()
        }) {
            Icon(Icons.Default.Delete, contentDescription = "Remove ${row.food.name}")
        }
    }
}
