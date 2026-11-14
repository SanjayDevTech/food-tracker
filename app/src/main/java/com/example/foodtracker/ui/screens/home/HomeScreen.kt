package com.example.foodtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.NutrientFieldsByCategory
import com.example.foodtracker.data.subFieldsFor
import com.example.foodtracker.ui.components.NutrientSubRow
import com.example.foodtracker.ui.components.color
import com.example.foodtracker.ui.components.NutrientSubRows
import com.example.foodtracker.data.NutrientTarget
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.components.CalorieHero
import com.example.foodtracker.ui.components.DonutSlice
import com.example.foodtracker.ui.components.NutrientBar
import com.example.foodtracker.ui.components.smartRound
import com.example.foodtracker.ui.theme.Dimens
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroFat
import com.example.foodtracker.ui.theme.MacroFiber
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.ui.theme.MacroWater
import com.example.foodtracker.ui.theme.MacroWeight
import com.example.foodtracker.util.DateUtil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddFoodToMeal: (MealType) -> Unit,
    onOpenWater: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenRichFoods: () -> Unit,
) {
    val vm: HomeViewModel = appViewModel { HomeViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val hour = java.time.LocalTime.now().hour
    val salutation = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    // First token only — full names look heavy in the greeting and get
    // truncated on narrow screens. Fall back to "there" if name is blank.
    val displayName = state.profile.name
        .trim()
        .substringBefore(' ')
        .ifBlank { "there" }

    // Quick-log → serving sheet flow. Tapping a Quick log row opens the same
    // ServingSheet the picker uses, so users confirm grams/servings/meal
    // before the row is written. Widget taps are deliberately one-tap and
    // don't go through this flow.
    var quickLogFood by remember { mutableStateOf<FoodEntity?>(null) }
    var quickLogVariant by remember { mutableStateOf<ServingVariant?>(null) }
    var quickLogServings by remember { mutableStateOf(1.0) }
    var quickLogMeal by remember { mutableStateOf(currentMeal()) }

    // No topBar — the greeting is rendered as the first scrollable item so it
    // scrolls off with the rest of the content (no sticky chrome, no elevation
    // tint when the user scrolls).
    Scaffold(
        floatingActionButton = {
            // Expressive FAB: medium size + the new larger corner shape and a
            // pronounced container color. The shape morphs to a tighter pill
            // on press under MaterialExpressiveTheme.
            ExtendedFloatingActionButton(
                text = {
                    Text(
                        "Log food",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = { onAddFoodToMeal(currentMeal()) },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp,
                ),
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
            item("greeting") {
                GreetingHeader(
                    salutation = salutation,
                    displayName = displayName,
                    epochDay = state.epochDay,
                    onPrevDay = { vm.shiftDay(-1) },
                    onNextDay = { vm.shiftDay(1) },
                    onToday = { vm.goToToday() },
                    onOpenProfile = onOpenProfile,
                    onOpenSettings = onOpenSettings,
                    onOpenProgress = onOpenProgress,
                    onOpenRichFoods = onOpenRichFoods,
                )
            }
            com.example.foodtracker.domain.goalProgress(state.profile)?.let { progress ->
                item("goalBanner") {
                    GoalProgressBanner(
                        profile = state.profile,
                        progress = progress,
                    )
                }
            }
            item("calorieHero") {
                // Single hero block — ring + macro legend together, with the
                // Goal/Food/Exercise stat strip below. Replaces what used to
                // be two stacked items (calorie ring + Macros section card).
                val n = state.totalNutrients
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    CalorieHero(
                        consumed = n.calories,
                        goal = state.goals.calories,
                        macroSlices = listOf(
                            DonutSlice("Carbs", n.carbs, MacroCarbs),
                            DonutSlice("Protein", n.protein, MacroProtein),
                            DonutSlice("Fat", n.fat, MacroFat),
                            DonutSlice("Fiber", n.fiber, MacroFiber),
                        ),
                    )
                }
            }
            // Action-first ordering: after the hero, the "log something" cluster
            // (Quick log shortcuts → Meals history) comes BEFORE passive viz
            // (stat tiles, nutrient breakdown). Most users open Home to log a
            // food they eat often, so the next-action paths sit at the fold.
            val quickList = (state.favourites + state.recent.filterNot { r -> state.favourites.any { it.id == r.id } }).take(10)
            if (quickList.isNotEmpty()) {
                item("quickLog") {
                    QuickLogSection(
                        items = quickList,
                        onLog = { food ->
                            // Seed the sheet with the food's first variant at
                            // 1 serving, and the meal that matches the wall
                            // clock — same defaults as the picker flow.
                            quickLogFood = food
                            quickLogVariant = food.variantsOrDefault().first()
                            quickLogServings = 1.0
                            quickLogMeal = currentMeal()
                        },
                    )
                }
            }
            item("quickStats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    val latest = state.latestWeight
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MonitorWeight,
                        title = "Weight",
                        value = latest?.let { "%.1f kg".format(it.weightKg) } ?: "—",
                        sub = if (latest != null) "tap to log today" else "log your first weigh-in",
                        tint = MacroWeight,
                        onClick = onOpenWeight,
                    )
                }
            }
            item("mealsCombined") {
                MealsSection(
                    groups = state.mealGroups,
                    activeMeal = currentMeal(),
                    onAdd = onAddFoodToMeal,
                    onRemove = { vm.removeLog(it) },
                )
            }
            item("nutrientBreakdown") {
                NutrientBreakdown(state.totalNutrients, state.goals, state.logs)
            }
        }
    }

    val pendingFood = quickLogFood
    val pendingVariant = quickLogVariant
    if (pendingFood != null && pendingVariant != null) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { quickLogFood = null },
            sheetState = sheetState,
        ) {
            com.example.foodtracker.ui.components.ServingSheet(
                food = pendingFood,
                meal = quickLogMeal,
                variant = pendingVariant,
                servings = quickLogServings,
                onVariantChange = { quickLogVariant = it },
                onServingsChange = { quickLogServings = it },
                onMealChange = { quickLogMeal = it },
                onConfirm = {
                    haptics.confirm()
                    // Read the latest sheet state through the delegates so any
                    // last-moment variant / servings / meal change is included.
                    val food = quickLogFood ?: return@ServingSheet
                    val variant = quickLogVariant ?: return@ServingSheet
                    vm.quickLog(food, variant, quickLogServings, quickLogMeal)
                    quickLogFood = null
                },
            )
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

/**
 * Top-of-list row that merges the greeting, day selector, and chrome icons
 * into a single line. Scrolls with the list (not sticky, no elevation tint).
 *
 * Layout: greeting on the left, compact day-picker pill in the middle, and
 * Trends + Profile icons on the right. The pill is the same control the old
 * standalone DateSelector card surfaced — chevrons step the day, tapping the
 * label snaps back to today.
 */
@Composable
private fun GreetingHeader(
    salutation: String,
    displayName: String,
    epochDay: Long,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenRichFoods: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "$salutation, $displayName 👋",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        InlineDayPicker(
            epochDay = epochDay,
            onPrev = onPrevDay,
            onNext = onNextDay,
            onToday = onToday,
        )
        OverflowMenu(
            onOpenProgress = onOpenProgress,
            onOpenRichFoods = onOpenRichFoods,
            onOpenProfile = onOpenProfile,
            onOpenSettings = onOpenSettings,
        )
    }
}

/**
 * The greeting row only has space for one action, so Stats, Profile and
 * Settings share a single overflow. They used to be two bare icons with no
 * labels, and Settings had nowhere to go at all - it was buried inside Profile.
 */
@Composable
private fun OverflowMenu(
    onOpenProgress: () -> Unit,
    onOpenRichFoods: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    Box {
        // The icon is 24dp centred in a 48dp touch target, so on its own it
        // reads as 12dp further from the edge than the 16dp screen padding
        // every other element lines up with. Shift it back out by that 12dp so
        // the dots align optically; the target then overhangs into the padding,
        // which is what Material's own app-bar actions do.
        IconButton(
            onClick = {
                haptics.light()
                expanded = true
            },
            modifier = Modifier.offset(x = 12.dp),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuAction("Stats", Icons.Default.Insights) {
                expanded = false
                onOpenProgress()
            }
            MenuAction("Rich in…", Icons.Default.Search) {
                expanded = false
                onOpenRichFoods()
            }
            MenuAction("Profile", Icons.Default.Person) {
                expanded = false
                onOpenProfile()
            }
            MenuAction("Settings", Icons.Default.Settings) {
                expanded = false
                onOpenSettings()
            }
        }
    }
}

/**
 * One overflow entry. Exists so all three share a single padding value rather
 * than inheriting the 12dp menu default, which sat oddly against the 16dp
 * padding the rest of the app aligns to.
 */
@Composable
private fun MenuAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
    )
}

/**
 * Compact rounded pill — `[◀ Today ▶]` — used inline in the greeting row.
 * The label is a tap target that resets to today; the chevrons step ±1 day.
 * Selected (today) state uses primary tint so the user can tell at a glance
 * which day the rest of the screen is showing.
 */
@Composable
private fun InlineDayPicker(
    epochDay: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val isToday = epochDay == DateUtil.todayEpochDay()
    val labelColor =
        if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isToday) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        IconButton(
            onClick = { haptics.light(); onPrev() },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Previous day",
                tint = labelColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            DateUtil.formatHeader(epochDay),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { haptics.light(); onToday() }
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
        IconButton(
            onClick = { haptics.light(); onNext() },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next day",
                tint = labelColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GoalProgressBanner(
    profile: com.example.foodtracker.data.UserProfile,
    progress: com.example.foodtracker.domain.GoalProgress,
) {
    val verb = if (profile.goal == com.example.foodtracker.data.Goal.Lose) "lose" else "gain"
    val isComplete = progress.deltaKg <= 0.01
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                if (isComplete) "You hit your target"
                else "${"%.1f".format(progress.deltaKg)} kg to $verb",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (!isComplete) {
                Text(
                    "${"%.1f".format(profile.weightKg)} → ${"%.1f".format(profile.goalWeightKg ?: profile.weightKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                val rate = profile.weightChangeRate.kgPerWeek
                if (rate > 0.0 && progress.weeksRemaining > 0.0) {
                    Text(
                        "~${weeksLabel(progress.weeksRemaining)} at ${"%.2f".format(rate)} kg/week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

private fun weeksLabel(weeks: Double): String {
    val rounded = kotlin.math.round(weeks * 10) / 10.0
    if (rounded < 1.0) return "less than a week"
    if (rounded < 6.0) return "${"%.0f".format(rounded)} week${if (rounded < 1.5) "" else "s"}"
    val months = rounded / 4.345
    return "${"%.0f".format(kotlin.math.round(months))} month${if (months < 1.5) "" else "s"}"
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
    // Minimal text-forward tile with a 1dp outline (no fill) — same shape
    // language as the section containers below. The outline signals
    // tappability without the heaviness of a filled card.
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NutrientBreakdown(
    consumed: Nutrients,
    goals: NutrientGoals,
    logs: List<FoodLogWithFood>,
) {
    val grouped = NutrientFieldsByCategory
    var selectedCategory by rememberSaveable { mutableStateOf(NutrientCategory.Macros) }
    var detailField by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.example.foodtracker.data.NutrientField?>(null)
    }
    val haptics = rememberAppHaptics()
    HomeSection(title = "Nutrient breakdown") {
        com.example.foodtracker.ui.components.ExclusiveButtonGroup(
            options = NutrientCategory.entries,
            selected = selectedCategory,
            onSelect = {
                haptics.light()
                selectedCategory = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { it.name },
        )
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

/**
 * The optional detail for Fiber and Protein — soluble/insoluble, and essential
 * amino acids with leucine called out — shown inside the nutrient's bottom
 * sheet, not next to its bar.
 *
 * Renders nothing unless at least one food logged today carries the
 * breakdown, which is the normal state for a diary full of custom and
 * AI-filled foods. When it does render it says what share of the parent it
 * covers, because summing only the foods that happen to have data would
 * otherwise present a partial figure as a complete one.
 */
@Composable
private fun SubBreakdown(
    fieldKey: String,
    consumed: Nutrients,
    logs: List<FoodLogWithFood>,
) {
    val subs = subFieldsFor(fieldKey)
    if (subs.isEmpty()) return
    val known = subs.any { it.read(consumed) > 0.0 }
    if (!known) return

    // What fraction of today's fiber (or protein) came from foods that have a
    // breakdown at all. Walks the day's logs, so it is remembered on them.
    val coverage = remember(fieldKey, logs) {
        var total = 0.0
        var covered = 0.0
        logs.forEach { row ->
            val n = row.food.nutrientsPer100g() * (row.log.grams / 100.0)
            val parent = if (fieldKey == "fiber") n.fiber else n.protein
            val hasDetail = if (fieldKey == "fiber") n.hasFiberSplit() else n.hasAminoAcids()
            total += parent
            if (hasDetail) covered += parent
        }
        if (total <= 0.0) 1.0 else covered / total
    }

    val parentLabel = if (fieldKey == "fiber") "fiber" else "protein"
    NutrientSubRows(
        rows = subs.map { sub ->
            NutrientSubRow(sub.label, "${sub.read(consumed).smartRound()} g")
        },
        note = if (coverage >= 0.995) null else
            // Not "published": seeded foods carry figures from composition
            // tables, but an AI-filled food's amino split may be a typical
            // figure for its family rather than a measurement of that food.
            // "Carry a breakdown" is true of both; the old wording claimed a
            // provenance the newer half of these numbers does not have.
            "From the ${(coverage * 100).roundToInt()}% of today's $parentLabel " +
                "whose foods carry a breakdown.",
    )
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

    // Combine repeat logs of the same food into one row, then rank by
    // contribution. Remembered because it walks every log row and does nutrient
    // maths per food — it depends on the logs and the field, not on how many
    // times the sheet recomposes while it animates in.
    val contributions = remember(field, logs, total) {
        logs
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
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // LazyColumn so long contribution lists scroll within the sheet
        // instead of overflowing off-screen. ModalBottomSheet provides the
        // nested-scroll connection so dragging the list past its top also
        // drags the sheet down.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item("header") {
                // A capped nutrient says so; a target says nothing, since
                // "work towards this number" is the reader's default reading
                // and only the exceptions need calling out.
                val isLimit = field.target == NutrientTarget.Limit
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            field.label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isLimit) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Limit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    val targetWord = if (isLimit) "limit" else "goal"
                    val goalSuffix = if (goal > 0) " · ${((total / goal) * 100).toInt()}% of ${goal.smartRound()} ${field.unit.symbol} $targetWord" else ""
                    Text(
                        "${total.smartRound()} ${field.unit.symbol} today$goalSuffix",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isLimit) {
                        Text(
                            "Staying under this is the win — there is no shortfall to make up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Fiber and protein break down further. This lives here
                    // rather than beside the bar on the home screen: the bars
                    // are the day's scoreboard and adding rows to them makes
                    // the scoreboard longer for everyone, whether or not they
                    // care. Behind a tap it costs nothing to ignore.
                    SubBreakdown(field.key, consumed, logs)
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (contributions.isEmpty()) {
                item("empty") {
                    Text(
                        if (field.target == NutrientTarget.Limit) {
                            "Nothing logged today contributes ${field.label.lowercase()}. " +
                                "That is exactly where you want to be."
                        } else {
                            "Nothing logged today contributes ${field.label.lowercase()}. " +
                                "Log foods rich in it to close the gap."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val barColor = field.color()
                items(contributions, key = { it.name }) { c ->
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

/**
 * Shared section wrapper used by Quick log and Meals on Home. Same shape +
 * 1dp outline as [StatTile] so the three blocks share one container
 * language. Transparent fill keeps AMOLED-friendly black pixels everywhere
 * except the outline.
 */
@Composable
private fun HomeSection(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (trailing != null) trailing()
        }
        content()
    }
}

/**
 * Combined meals section — replaces four mostly-empty per-meal cards with a
 * single dense card where each meal is one row. The current meal (by time of
 * day) gets a tonal background so the user's eye lands on the row they're
 * most likely to add to next.
 */
@Composable
private fun MealsSection(
    groups: Map<MealType, List<FoodLogWithFood>>,
    activeMeal: MealType,
    onAdd: (MealType) -> Unit,
    onRemove: (Long) -> Unit,
) {
    HomeSection(title = "Meals") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MealType.entries.forEachIndexed { i, meal ->
                if (i > 0) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
                MealRow(
                    meal = meal,
                    rows = groups[meal].orEmpty(),
                    isActive = meal == activeMeal,
                    onAdd = { onAdd(meal) },
                    onRemove = onRemove,
                )
            }
        }
    }
}

@Composable
private fun MealRow(
    meal: MealType,
    rows: List<FoodLogWithFood>,
    isActive: Boolean,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val hasItems = rows.isNotEmpty()
    // The active meal is always expanded (the user wants their next add target
    // to read fully without a tap). Other meals start collapsed; user can open
    // them via the chevron when they want to inspect or edit history.
    var userExpanded by rememberSaveable(meal) { mutableStateOf(false) }
    val expanded = isActive || userExpanded
    val kcal = rows.sumOf { it.food.calories * (it.log.grams / 100.0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
        ) {
            Text(
                meal.emoji,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        meal.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isActive) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Now",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = if (hasItems) {
                        "${kcal.toInt()} kcal · ${rows.size} item${if (rows.size == 1) "" else "s"}"
                    } else "Nothing logged yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasItems && !isActive) {
                IconButton(onClick = {
                    haptics.light()
                    userExpanded = !userExpanded
                }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add to ${meal.displayName}")
            }
        }
        if (hasItems && expanded) {
            Column(
                modifier = Modifier.padding(start = 36.dp, end = 4.dp, bottom = 4.dp),
            ) {
                rows.forEach { row ->
                    MealEntryRow(row, onRemove = { onRemove(row.log.id) })
                }
            }
        }
    }
}

@Composable
private fun QuickLogSection(
    items: List<FoodEntity>,
    onLog: (FoodEntity) -> Unit,
) {
    val haptics = rememberAppHaptics()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visibleItems = if (expanded) items else items.take(2)

    HomeSection(
        title = "Quick log",
        trailing = if (items.size > 1) {
            {
                TextButton(onClick = {
                    haptics.light()
                    expanded = !expanded
                }) {
                    Text(if (expanded) "Show less" else "Show all (${items.size})")
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            visibleItems.forEach { food ->
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
                            "${food.servingCalories().roundToInt()} kcal · ${food.servingLabel}",
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
