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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.db.WeightLogEntity
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.LineChart
import com.example.foodtracker.ui.components.LinePoint
import com.example.foodtracker.ui.components.WeeklyBarChart
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.AppTopBar
import com.example.foodtracker.ui.components.atoms.grouped
import com.example.foodtracker.ui.components.sheets.kgText
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.screens.trends.components.ChartCard
import com.example.foodtracker.ui.screens.trends.components.ChartInsight
import com.example.foodtracker.ui.screens.trends.components.EmptyChartLine
import com.example.foodtracker.ui.screens.trends.components.InsightTone
import com.example.foodtracker.ui.screens.trends.components.MacroStackedBars
import com.example.foodtracker.ui.screens.trends.components.NutrientHeatmap
import com.example.foodtracker.ui.screens.trends.components.RangeBar
import com.example.foodtracker.ui.screens.trends.components.SectionLink
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.ui.theme.MacroWater
import com.example.foodtracker.ui.theme.MacroWeight
import com.example.foodtracker.ui.theme.Spacing
import com.example.foodtracker.util.DateUtil
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Trends — design-plan §4.2, re-cut against the UI house rules.
 *
 * **It is a destination, not a page.** Trends is one of the three bottom-bar
 * tabs (open question 3), so it renders no back arrow and no bar of its own:
 * the host `Scaffold` in `AppNav` owns both, and a second `Scaffold` here would
 * double the insets and put a back arrow on a peer destination, which is
 * exactly the thing a bottom bar means you no longer need.
 *
 * **The range control is pinned outside the list.** Changing the window is the
 * one control every section on the page obeys, and burying it in the scroll
 * means scrolling back up to use it — you notice you want 90 days *while*
 * reading the weight chart, which is three sections down.
 *
 * **The page absorbed two screens.** The weight section carries what was
 * `WeightScreen`'s chart and history, and the water section what was
 * `WaterScreen`'s week chart; what you *do* with either — a weigh-in, a glass —
 * is a sheet now ([onOpenWeight], [onOpenWater]). The split is decision 6: all
 * the history in one place, beside the other history, and entry wherever you
 * happen to be.
 *
 * **What the editorial pass changed here, and why.** The five charts used to
 * sit in five `AppCard`s 16dp apart — a stack of rounded hairline boxes, which
 * is the Direction A anatomy §0 rejected and the reason the rewrite still read
 * as the old app. They are `Section`s now: a standing head with its summary
 * figure on the baseline, a hairline under the head, then the reading and the
 * chart, open to the 16dp gutter. The charts got the 32dp of width the card
 * insets were taking, each section says what its window amounts to in the same
 * place, and the only thing left with a box around it on this screen is
 * nothing at all.
 *
 * @param onOpenRichFoods opens "Rich in…", optionally preselecting one nutrient
 *        — the heatmap's rows pass the one you are short of.
 */
@Composable
fun TrendsScreen(
    onOpenRichFoods: (String?) -> Unit,
    onOpenWater: () -> Unit,
    onOpenWeight: () -> Unit,
) {
    val vm: TrendsViewModel = appViewModel { TrendsViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    // Survives rotation and the bottom bar's save/restore of this destination,
    // so coming back from a sheet does not collapse what you had open.
    var showAllNutrients by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Trends")

        RangeBar(
            selected = state.range,
            onSelect = { haptics.light(); vm.setRange(it) },
            modifier = Modifier.padding(horizontal = Spacing.gutter),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                top = Spacing.l,
                bottom = Spacing.xxxl,
                start = Spacing.gutter,
                end = Spacing.gutter,
            ),
            // 24dp, not 32: every section now carries a rule under its standing
            // head, so the whitespace no longer has to do the separating on its
            // own — house rule 1, and the same value Today uses.
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
        ) {
            item("calories") {
                ChartCard(
                    title = "Calories",
                    summary = caloriesSummary(state),
                    insight = caloriesInsight(state),
                    empty = if (state.calories.hasTrend()) null else NEED_TWO_DAYS,
                    crossfadeKey = state.range,
                ) {
                    LineChart(
                        points = state.calories,
                        // `calories` is MacroBurned everywhere in the app
                        // (NutrientVisuals) — the progress screen used the carbs
                        // amber here, which made the calorie line and the carbs
                        // segment of the macro chart the same colour.
                        lineColor = MacroBurned,
                        unit = "kcal",
                        referenceLine = state.calorieGoal.takeIf { it > 0 },
                        referenceLabel = state.calorieGoal.takeIf { it > 0 }
                            ?.let { "Goal ${grouped(it)} kcal" },
                    )
                }
            }

            item("macros") {
                ChartCard(
                    title = "Macros",
                    summary = macrosSummary(state),
                    insight = macrosInsight(state),
                    empty = if (state.macroBars.any { it.total > 0.0 }) null else NEED_A_DAY,
                    crossfadeKey = state.range,
                ) {
                    MacroStackedBars(
                        bars = state.macroBars,
                        avgCarbs = state.macroAvgCarbs,
                        avgProtein = state.macroAvgProtein,
                        avgFat = state.macroAvgFat,
                    )
                }
            }

            item("nutrients") {
                ChartCard(
                    title = "Nutrient consistency",
                    summary = nutrientSummary(state),
                    insight = nutrientInsight(state),
                    empty = if (state.heatmapLoggedDays > 0) null else NEED_A_DAY,
                    actionLabel = "Find rich foods",
                    onAction = { onOpenRichFoods(null) },
                    crossfadeKey = state.range,
                    footer = if (state.heatmap.size > HEATMAP_COLLAPSED_ROWS) {
                        {
                            SectionLink(
                                text = if (showAllNutrients) "Show less"
                                else "Show all ${state.heatmap.size} nutrients",
                                onClick = {
                                    haptics.light()
                                    showAllNutrients = !showAllNutrients
                                },
                            )
                        }
                    } else null,
                ) {
                    NutrientHeatmap(
                        rows = state.heatmap,
                        firstDayLabel = state.heatmapFirstLabel,
                        lastDayLabel = state.heatmapLastLabel,
                        expanded = showAllNutrients,
                        collapsedRows = HEATMAP_COLLAPSED_ROWS,
                    )
                }
            }

            item("weight") {
                ChartCard(
                    title = "Weight",
                    summary = weightSummary(state),
                    insight = weightInsight(state),
                    empty = if (state.weight.size >= 2) null else NEED_TWO_WEIGH_INS,
                    actionLabel = "Log a weigh-in",
                    onAction = onOpenWeight,
                    crossfadeKey = state.range,
                    footer = if (state.weightEntries.isNotEmpty()) {
                        {
                            SectionLink(
                                text = if (showHistory) "Hide history"
                                else "History (${state.weightEntries.size})",
                                onClick = {
                                    haptics.light()
                                    showHistory = !showHistory
                                },
                            )
                            // Composed only once opened — which is the whole
                            // reason the history is a disclosure rather than a
                            // list: most visits to this section want the line.
                            if (showHistory) {
                                WeightHistory(
                                    entries = state.weightEntries,
                                    onDelete = { id ->
                                        haptics.light()
                                        vm.deleteWeight(id)
                                    },
                                )
                            }
                        }
                    } else null,
                ) {
                    LineChart(
                        points = state.weight,
                        lineColor = MacroWeight,
                        unit = "kg",
                        referenceLine = state.goalWeightKg,
                        referenceLabel = state.goalWeightKg?.let { "Goal ${kgText(it)} kg" },
                    )
                }
            }

            item("water") {
                ChartCard(
                    title = "Water",
                    summary = waterSummary(state),
                    insight = waterInsight(state),
                    empty = if (state.water.any { it.value > 0.0 }) null else NEED_A_DAY,
                    actionLabel = "Add water",
                    onAction = onOpenWater,
                    crossfadeKey = state.range,
                ) {
                    // A week is seven discrete days and reads as bars; ninety is
                    // a shape and reads as a line. Same data, and the chart that
                    // fits the question each range is asking.
                    if (state.range == TrendsRange.Week) {
                        WeeklyBarChart(
                            bars = state.waterBars,
                            goal = state.waterGoalMl.takeIf { it > 0 }?.toDouble(),
                            barColor = MacroWater,
                            unit = "ml",
                        )
                    } else {
                        LineChart(
                            points = state.water,
                            lineColor = MacroWater,
                            unit = "ml",
                            referenceLine = state.waterGoalMl.takeIf { it > 0 }?.toDouble(),
                            referenceLabel = state.waterGoalMl.takeIf { it > 0 }
                                ?.let { "Goal ${grouped(it.toDouble())} ml" },
                        )
                    }
                }
            }
        }
    }
}

/** Six rows is the top of the sorted list — the finding, without the appendix. */
private const val HEATMAP_COLLAPSED_ROWS = 6

/**
 * Within this many kcal of the goal, a daily average is *at* goal.
 *
 * Portion sizes are estimates and the database is a table; calling a 12 kcal
 * difference "over goal" would be reporting noise as a verdict.
 */
private const val CALORIE_DEADBAND = 25

private const val NEED_TWO_DAYS = "Log two or more days in this range to see the trend."
private const val NEED_A_DAY = "Log a day in this range to see this."
private const val NEED_TWO_WEIGH_INS = "Log two or more weigh-ins in this range to see the trend."

/** A series only says something once it has two real points to say it with. */
private fun List<LinePoint>.hasTrend(): Boolean =
    size >= 2 && any { it.value > 0.0 }

/*
 * The readings, two per section.
 *
 * Each section used to hand `ChartCard` one sentence — "2 981 kcal avg · 513
 * over goal" — which it set in `body`, the quietest register on the page,
 * directly under an eyebrow and directly above a chart. The one fact the
 * section existed to deliver was therefore the least audible thing in it.
 *
 * House rule 1 gives every section a trailing slot on its head for the figure
 * its window amounts to, and house rule 2 wants the finding as a number rather
 * than as prose. So the sentence split in two: a `*Summary` that states the
 * level in the head ("2 981 kcal avg"), and a `*Insight` that states the
 * verdict loudly under the rule ("VS GOAL · +513 kcal"). Nothing is printed
 * twice, and `ChartInsight` has no field prose would fit in.
 *
 * None of them touch the ViewModel: every value below is already in
 * `TrendsUiState`, and this is a presentation pass.
 */

/** `+513`, `−120`. U+2212 MINUS, which is the width of the plus — see `signedKgText`. */
private fun signedGrouped(delta: Int): String =
    (if (delta < 0) "−" else "+") + grouped(abs(delta).toDouble())

/** `+0.4`, `−1.2`. `signedKgText`'s sign without its unit, which here is `bodyS`. */
private fun signedKg(deltaKg: Double): String =
    (if (deltaKg < 0) "−" else "+") + kgText(abs(deltaKg))

private fun caloriesSummary(state: TrendsUiState): String? =
    if (state.calorieAvg <= 0) null else "${grouped(state.calorieAvg.toDouble())} kcal avg"

private fun caloriesInsight(state: TrendsUiState): ChartInsight? {
    if (state.calorieAvg <= 0 || state.calorieGoal <= 0) return null
    val delta = state.calorieAvg - state.calorieGoal.roundToInt()
    val onGoal = abs(delta) <= CALORIE_DEADBAND
    return ChartInsight(
        label = "Vs goal",
        value = signedGrouped(delta),
        unit = "kcal",
        caption = if (onGoal) "on goal" else "goal ${grouped(state.calorieGoal)} kcal",
        tone = when {
            onGoal -> InsightTone.OnTrack
            delta > 0 -> InsightTone.Over
            // Eating under the goal is not crossing a limit, and house rule 3
            // reserves `error` for exactly that. It reads in the foreground.
            else -> InsightTone.Neutral
        },
    )
}

private fun macrosSummary(state: TrendsUiState): String? {
    if (state.loggedDays == 0) return null
    return "${state.proteinMetDays} of ${state.loggedDays} days"
}

private fun macrosInsight(state: TrendsUiState): ChartInsight? {
    if (state.loggedDays == 0) return null
    // The head already carries the fraction; the loud line carries the rate,
    // which is the figure that survives a change of range.
    val pct = (state.proteinMetDays * 100.0 / state.loggedDays).roundToInt()
    return ChartInsight(
        label = "Protein on target",
        value = "$pct",
        unit = "%",
        caption = "of logged days",
    )
}

private fun nutrientSummary(state: TrendsUiState): String? {
    if (state.heatmapLoggedDays == 0) return null
    return "${state.chronicallyMissed} of ${state.heatmap.size} missed"
}

private fun nutrientInsight(state: TrendsUiState): ChartInsight? {
    if (state.heatmapLoggedDays == 0) return null
    return ChartInsight(
        label = "Days logged",
        value = "${state.heatmapLoggedDays}",
        unit = "of ${state.heatmapDays}",
        // Say so when the grid is narrower than the range: a heatmap that
        // silently meant "last month" while the control read "1Y" would be a
        // lie of omission, and the grid's own axis labels only name its ends.
        caption = if (state.heatmapDays < state.range.days) {
            "grid shows the last ${state.heatmapDays} days"
        } else null,
    )
}

private fun weightSummary(state: TrendsUiState): String? =
    state.latestWeightKg?.let { "${kgText(it)} kg now" }

private fun weightInsight(state: TrendsUiState): ChartInsight? {
    val change = state.weightChange ?: return null
    return ChartInsight(
        label = "Change over ${state.range.label}",
        value = signedKg(change),
        unit = "kg",
        caption = state.goalWeightKg?.let { "goal ${kgText(it)} kg" },
    )
}

private fun waterSummary(state: TrendsUiState): String? =
    if (state.waterAvgMl <= 0) null else "${grouped(state.waterAvgMl.toDouble())} ml / day"

private fun waterInsight(state: TrendsUiState): ChartInsight? {
    if (state.waterAvgMl <= 0 || state.waterGoalMl <= 0) return null
    return ChartInsight(
        label = "Vs goal",
        value = signedGrouped(state.waterAvgMl - state.waterGoalMl),
        unit = "ml",
        caption = "goal ${grouped(state.waterGoalMl.toDouble())} ml",
        // Water has a target, not a limit: drinking past it is the good
        // outcome, so the only verdict available here is "on track".
        tone = if (state.waterAvgMl >= state.waterGoalMl) {
            InsightTone.OnTrack
        } else InsightTone.Neutral,
    )
}

/**
 * The weigh-in list, under the weight section.
 *
 * **Capped, and honest about it.** `WeightScreen` kept history out of its card
 * precisely so `LazyColumn` could render it lazily, and that reasoning still
 * holds — but it was answering a different layout. Here the list belongs to one
 * section, and a section is one lazy item: everything in it composes at once.
 * So the list is bounded at [HISTORY_LIMIT] rows, which is far more weigh-ins
 * than anyone scrolls back through inline, and the rest is a line of text
 * rather than three hundred hidden nodes. The full series is still in the chart
 * above.
 *
 * **The row reads label-left, number-right** like every other row in the app
 * now. It used to lead with the weight and trail the date underneath it, which
 * put a `numberM` figure hard against the left gutter and left the column of
 * numbers unalignable — house rule 2's one rhythm, broken in the one place on
 * the screen that is an actual list.
 */
@Composable
private fun WeightHistory(
    entries: List<WeightLogEntity>,
    onDelete: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        entries.take(HISTORY_LIMIT).forEach { entry ->
            AppDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        DateUtil.formatHeader(entry.dateEpochDay).uppercase(),
                        style = AppText.eyebrow,
                        color = scheme.onSurfaceVariant,
                    )
                    entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Spacer(Modifier.height(Spacing.xs))
                        Text(note, style = AppText.bodyS, color = scheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(Spacing.m))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(kgText(entry.weightKg), style = AppText.numberM, color = scheme.onSurface)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        "kg",
                        style = AppText.bodyS,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                IconButton(onClick = { onDelete(entry.id) }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete weigh-in",
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (entries.size > HISTORY_LIMIT) {
            Spacer(Modifier.height(Spacing.s))
            EmptyChartLine("Showing the $HISTORY_LIMIT most recent of ${entries.size}.")
        }
    }
}

private const val HISTORY_LIMIT = 30
