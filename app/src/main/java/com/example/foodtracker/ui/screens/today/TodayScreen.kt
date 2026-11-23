package com.example.foodtracker.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.AppTopBar
import com.example.foodtracker.ui.components.atoms.grouped
import com.example.foodtracker.ui.components.NutrientContributionSheet
import com.example.foodtracker.ui.components.sheets.EntryEditSheet
import com.example.foodtracker.ui.components.sheets.WaterSheet
import com.example.foodtracker.ui.components.sheets.WeighInSheet
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.screens.today.components.CalorieHero
import com.example.foodtracker.ui.screens.today.components.DayStrip
import com.example.foodtracker.ui.screens.today.components.MealsSection
import com.example.foodtracker.ui.screens.today.components.NutrientSummarySection
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.screens.today.components.StatRow
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Spacing
import com.example.foodtracker.util.DateUtil
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A modal this screen can be asked to open on arrival — deep links land here. */
enum class TodaySheet { Water, Weight }

/**
 * Today — docs/design-plan.md §4.1. The first bottom-bar destination.
 *
 * The screen itself is deliberately thin: a scaffold, a `LazyColumn` of five
 * blocks, and the modals those blocks open. Everything with an opinion lives in
 * `today/components/`. What it replaced was a 1 176-line `HomeScreen.kt` that
 * held its own cards, its own meters, its own nutrient sheet and a second copy
 * of the serving-sheet host.
 *
 * **No bottom bar and no `Today` back arrow here** — the nav host owns both, so
 * switching destinations keeps each one's scroll position instead of rebuilding
 * the bar inside every screen.
 *
 * **No loading state, by design.** `TodayViewModel.state` has a non-null initial
 * value built from a default profile, so the first frame is real layout with
 * placeholder numbers; the ring's 450ms value animation absorbs the transition
 * when the database arrives. A skeleton would be a worse version of that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddFoodToMeal: (MealType) -> Unit,
    onOpenRichFoods: (String?) -> Unit,
    onOpenAllNutrients: (Long) -> Unit,
    openSheet: TodaySheet? = null,
    onSheetConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: TodayViewModel = appViewModel { TodayViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    var detailField by remember { mutableStateOf<NutrientField?>(null) }
    var editing by remember { mutableStateOf<FoodLogWithFood?>(null) }
    var sheet by remember { mutableStateOf<TodaySheet?>(null) }

    // A deep link (widget or reminder tap) asks for a sheet rather than a
    // route: water and weight stopped being pages in phase 4. Consumed on
    // arrival so a configuration change does not reopen it.
    LaunchedEffect(openSheet) {
        if (openSheet != null) {
            sheet = openSheet
            onSheetConsumed()
        }
    }

    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            AppTopBar(
                title = DateUtil.formatHeader(state.epochDay),
                subtitle = DateUtil.epochDayToDate(state.epochDay)
                    .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())),
            )
        },
        snackbarHost = {
            // Material's default snackbar uses the *inverse* scheme, which on a
            // black canvas is a cream slab — the loudest surface in the app, for
            // its least important message. §1 allows five fills and this is not
            // one of them, so it gets the same hairline-on-near-black treatment
            // as every other container.
            SnackbarHost(snackbars) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(Radius.md),
                    containerColor = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurface,
                    actionColor = scheme.primary,
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.light(); onAddFoodToMeal(currentMeal()) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log", style = AppText.label) },
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                shape = RoundedCornerShape(Radius.pill),
                // §1 bans elevation everywhere, the FAB included. All four
                // states, not just the default — leaving focused/hovered at
                // their 6dp defaults keeps a lift behind on a plugged-in phone.
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            )
        },
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + Spacing.s,
                // Clears the FAB: the plan keeps it visible on scroll, so the
                // last card has to be able to get out from under it.
                bottom = inner.calculateBottomPadding() + 96.dp,
                start = Spacing.gutter,
                end = Spacing.gutter,
            ),
            // 24dp, not 32: each section carries a rule under its standing
            // head (see Section.kt), so the whitespace no longer has to do the
            // separating on its own.
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
        ) {
            item("dayStrip") {
                DayStrip(selected = state.epochDay, onSelect = { haptics.light(); vm.setDay(it) })
            }
            item("hero") {
                CalorieHero(
                    state.totalNutrients,
                    state.goals,
                    loaded = state.loaded,
                    // The shortcut the Trends heatmap used to own. Same
                    // destination, on a row you can actually hit.
                    onMacroClick = { key -> haptics.light(); onOpenRichFoods(key) },
                )
            }
            item("tiles") {
                Section("Intake") {
                    StatRow(
                        label = "Water",
                        value = if (state.waterMl > 0) grouped(state.waterMl.toDouble()) else "\u2014",
                        unit = "ml",
                        caption = "of ${grouped(state.goals.waterMl.toDouble())} ml",
                        onClick = { sheet = TodaySheet.Water },
                    )
                    AppDivider()
                    StatRow(
                        label = "Weight",
                        value = state.latestWeight?.let { "%.1f".format(it.weightKg) } ?: "\u2014",
                        unit = if (state.latestWeight != null) "kg" else "",
                        caption = state.latestWeight?.let { agoLabel(it.dateEpochDay) } ?: "Tap to log",
                        onClick = { sheet = TodaySheet.Weight },
                    )
                }
            }
            item("meals") {
                MealsSection(
                    groups = state.mealGroups,
                    activeMeal = currentMeal(),
                    totalKcal = state.totalNutrients.calories,
                    onAdd = { haptics.light(); onAddFoodToMeal(it) },
                    onEditEntry = { editing = it },
                    onDeleteEntry = { row ->
                        haptics.light()
                        vm.deleteWithUndo(row.log)
                        scope.launch {
                            val result = snackbars.showSnackbar(
                                message = "${row.food.name} deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                            else vm.clearUndo()
                        }
                    },
                )
            }
            item("nutrients") {
                NutrientSummarySection(
                    totals = state.totalNutrients,
                    goals = state.goals,
                    hasLogs = state.logs.isNotEmpty(),
                    // The full list is a page, not a sheet — see
                    // AllNutrientsScreen's docstring for why.
                    onSeeAll = { onOpenAllNutrients(state.epochDay) },
                    onPickNutrient = { detailField = it },
                )
            }
        }
    }

    // Transient detail about one row, raised over this page. The only sheet in
    // the nutrient flow now, so there is no sheet-over-sheet transition left.
    detailField?.let { field ->
        NutrientContributionSheet(
            field = field,
            consumed = state.totalNutrients,
            goals = state.goals,
            logs = state.logs,
            onDismiss = { detailField = null },
            onFindRichFoods = { key -> detailField = null; onOpenRichFoods(key) },
        )
    }

    editing?.let { row ->
        EntryEditSheet(
            row = row,
            onDismiss = { editing = null },
            onSave = { vm.editLog(it) },
            onDelete = { vm.removeLog(row.log.id) },
        )
    }

    when (sheet) {
        TodaySheet.Water -> WaterSheet(onDismiss = { sheet = null })
        TodaySheet.Weight -> WeighInSheet(onDismiss = { sheet = null })
        null -> Unit
    }
}

/** The meal the wall clock says you are in. Drives the auto-expanded row and the FAB. */
internal fun currentMeal(): MealType {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> MealType.Breakfast
        hour < 15 -> MealType.Lunch
        hour < 18 -> MealType.Snack
        else -> MealType.Dinner
    }
}

private fun agoLabel(epochDay: Long): String {
    val days = DateUtil.todayEpochDay() - epochDay
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}
