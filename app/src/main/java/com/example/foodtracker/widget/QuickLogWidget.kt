package com.example.foodtracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.foodtracker.DeepLink
import com.example.foodtracker.FoodTrackerApp
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Quick-log widget — three sections stacked vertically:
 *  1. Header: title + "Add" FAB.
 *  2. Meal pills: Breakfast / Lunch / Snack / Dinner. Tap → opens
 *     LogFoodScreen at that meal.
 *  3. Recent foods grid: as many of the user's most-recently-logged foods
 *     as fit in the remaining height — 3 chips per row, rows added as the
 *     user grows the widget. Tap → instantly logs that food (1 serving,
 *     default grams) for the current meal-of-day.
 *
 * Pre-onboarding: every tap routes to MainActivity without extras so the
 * user lands in Onboarding naturally.
 */
class QuickLogWidget : GlanceAppWidget() {

    // Exact so LocalSize.current reflects the user's actual resize choice —
    // needed to dynamically pack rows of recent foods into the available
    // vertical space.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // One-shot read — no long-lived collect, so the session ends and
        // WidgetUpdater.updateAll can re-run this. The success-flash is driven
        // externally by QuickLogActionCallback via update(), not by a collect.
        val snapshot = WidgetData.snapshot(context)
        provideContent {
            GlanceTheme {
                QuickLogContent(context, snapshot, QuickLogFeedback.current())
            }
        }
    }

    companion object {
        /** Key for [QuickLogActionCallback] — matches the param put into the action. */
        val FoodIdKey = ActionParameters.Key<Long>("food_id")

        /** Columns in the recents grid; fixed across all widget sizes. */
        const val COLUMNS = 3

        // Layout constants used to compute how many recent-food rows fit.
        const val HEADER_HEIGHT_DP = 50f
        const val MEAL_ROW_HEIGHT_DP = 44f
        const val CHIP_ROW_HEIGHT_DP = 40f
        const val ROW_GAP_DP = 6f
        const val SECTION_GAP_DP = 10f
        const val OUTER_PADDING_DP = 14f
    }
}

class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}

/**
 * Process-singleton feedback flag. The action callback marks the foodId of the
 * chip the user just tapped, then pushes two RemoteViews frames via the widget's
 * `update()`: one with the success styling (tertiary tint + ✓), one reverted
 * after the dwell time. The widget reads [current] one-shot inside
 * `provideGlance` — we can't use a collected flow here because that would pin
 * the session open and stop `updateAll` from re-running `provideGlance`.
 *
 * Glance has no real animation primitive — RemoteViews are discrete frames, so
 * the "animation" is just those two driven re-renders.
 */
object QuickLogFeedback {
    @Volatile
    private var tappedFoodId: Long? = null

    const val DWELL_MS = 650L

    /** The chip currently flashing success, or null. Read at composition time. */
    fun current(): Long? = tappedFoodId

    fun set(foodId: Long) {
        tappedFoodId = foodId
    }

    /**
     * Clear the flash — but only if [foodId] is still the active one. A rapid
     * second tap should replace the first flash, not have the first's delayed
     * clear race it away.
     */
    fun clear(foodId: Long) {
        if (tappedFoodId == foodId) tappedFoodId = null
    }
}

/**
 * Handles a tap on a recent-food chip. Inserts a [FoodLogEntity] for today
 * at the current meal-of-day with one serving at the food's default grams.
 *
 * Refresh is explicit: [FoodRepository.addFoodLog] pushes a [WidgetUpdater]
 * refresh (re-running every widget's `provideGlance` with fresh data), which
 * also picks up the success flag set below to render the ✓ frame. We then
 * await the dwell and push the reverted frame via [GlanceAppWidget.update].
 * Awaiting keeps the broadcast process alive long enough for both frames to
 * reach the launcher.
 */
class QuickLogActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val foodId = parameters[QuickLogWidget.FoodIdKey] ?: return
        val app = context.applicationContext as FoodTrackerApp
        val food = app.repository.foodById(foodId) ?: return
        val variant = food.variantsOrDefault().first()
        // Mark success BEFORE the insert so the refresh the insert triggers
        // already renders the ✓ frame.
        QuickLogFeedback.set(food.id)
        app.repository.addFoodLog(
            FoodLogEntity(
                foodId = food.id,
                dateEpochDay = DateUtil.todayEpochDay(),
                mealType = currentMealForTime().name,
                servings = 1.0,
                grams = variant.grams,
                servingLabel = variant.label,
                loggedAtMillis = System.currentTimeMillis(),
            )
        )
        // Dwell on the ✓, then clear and re-render this widget reverted.
        delay(QuickLogFeedback.DWELL_MS)
        QuickLogFeedback.clear(food.id)
        QuickLogWidget().update(context, glanceId)
    }
}

private fun currentMealForTime(): MealType {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> MealType.Breakfast
        hour < 15 -> MealType.Lunch
        hour < 19 -> MealType.Snack
        else -> MealType.Dinner
    }
}

@Composable
private fun QuickLogContent(context: Context, snapshot: WidgetSnapshot, tappedId: Long?) {
    val size = LocalSize.current
    // tappedId: the just-logged chip flashing ✓ (or null). Driven externally by
    // QuickLogActionCallback's two update() frames, read one-shot above.
    // Height left over after the fixed sections (header, meals, paddings,
    // section gaps). Negative is possible at the smallest sizes — clamp.
    val availableForRecentsDp = (
        size.height.value
            - QuickLogWidget.HEADER_HEIGHT_DP
            - QuickLogWidget.MEAL_ROW_HEIGHT_DP
            - 2f * QuickLogWidget.OUTER_PADDING_DP
            - 2f * QuickLogWidget.SECTION_GAP_DP
        ).coerceAtLeast(0f)
    // Each chip row consumes its own height plus the gap above it (except
    // the first row, which the section gap covers). Round down so partial
    // rows don't show a half-clipped chip.
    val rowSpanDp = QuickLogWidget.CHIP_ROW_HEIGHT_DP + QuickLogWidget.ROW_GAP_DP
    val recentRows = if (rowSpanDp > 0f) {
        ((availableForRecentsDp + QuickLogWidget.ROW_GAP_DP) / rowSpanDp).toInt()
            .coerceAtLeast(0)
    } else 0
    val visibleRecent = snapshot.recent.take(recentRows * QuickLogWidget.COLUMNS)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.Surface)
            .cornerRadius(com.example.foodtracker.R.dimen.widget_corner_radius)
            .padding(QuickLogWidget.OUTER_PADDING_DP.dp),
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (snapshot.onboarded) "QUICK LOG" else "FINISH SETUP",
                    style = TextStyle(
                        color = WidgetColors.OnSurfaceMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = if (snapshot.onboarded) "What did you just eat?" else "Open the app to continue",
                    style = TextStyle(
                        color = WidgetColors.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            PlusButton(action = openLogScreen(context, mealName = null))
        }
        Spacer(GlanceModifier.height(QuickLogWidget.SECTION_GAP_DP.dp))
        // Meal pills
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MealType.entries.forEachIndexed { i, meal ->
                if (i > 0) Spacer(GlanceModifier.width(QuickLogWidget.ROW_GAP_DP.dp))
                MealPill(
                    meal = meal,
                    action = openLogScreen(context, mealName = meal.name),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        // Recents grid — N rows × 3 cols, where N is whatever fits. Pad
        // partial rows with invisible placeholders so columns stay aligned.
        if (visibleRecent.isNotEmpty()) {
            Spacer(GlanceModifier.height(QuickLogWidget.SECTION_GAP_DP.dp))
            RecentFoodsGrid(
                foods = visibleRecent,
                rows = recentRows,
                onboarded = snapshot.onboarded,
                tappedId = tappedId,
                context = context,
            )
        }
    }
}

/**
 * Renders [foods] as a fixed-column grid. Glance has no native LazyVerticalGrid,
 * so we lay it out as `rows` × [QuickLogWidget.COLUMNS] with manual Row/Column.
 * Empty cells (partial last row) keep their column slot via `defaultWeight`
 * so column widths stay even.
 */
@Composable
private fun RecentFoodsGrid(
    foods: List<com.example.foodtracker.data.db.FoodEntity>,
    rows: Int,
    onboarded: Boolean,
    tappedId: Long?,
    context: Context,
) {
    val cols = QuickLogWidget.COLUMNS
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        for (r in 0 until rows) {
            if (r > 0) Spacer(GlanceModifier.height(QuickLogWidget.ROW_GAP_DP.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                for (c in 0 until cols) {
                    if (c > 0) Spacer(GlanceModifier.width(QuickLogWidget.ROW_GAP_DP.dp))
                    val index = r * cols + c
                    val food = foods.getOrNull(index)
                    if (food != null) {
                        RecentFoodChip(
                            name = food.name,
                            action = directLogAction(food.id, onboarded, context),
                            justTapped = food.id == tappedId,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                    } else {
                        // Empty cell — keeps the column even when this row
                        // is partially filled.
                        Box(modifier = GlanceModifier.defaultWeight()) {}
                    }
                }
            }
        }
    }
}

/** Open MainActivity at LogFoodScreen — used by header FAB and meal pills. */
private fun openLogScreen(context: Context, mealName: String?): Action =
    actionStartActivity(DeepLink.logMealIntent(context, mealName))

/**
 * Direct-log when onboarded, open-app when not. Pre-onboarding the user has
 * no profile so we can't pick a meal automatically — route to onboarding
 * instead and let them finish setup.
 */
private fun directLogAction(foodId: Long, onboarded: Boolean, context: Context): Action =
    if (onboarded) {
        actionRunCallback<QuickLogActionCallback>(
            actionParametersOf(QuickLogWidget.FoodIdKey to foodId)
        )
    } else {
        actionStartActivity(DeepLink.logMealIntent(context, mealName = null))
    }

@Composable
private fun MealPill(
    meal: MealType,
    action: Action,
    modifier: GlanceModifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(WidgetColors.MealPill)
            .cornerRadius(20.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = meal.displayName,
            style = TextStyle(
                color = WidgetColors.OnMealPill,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun RecentFoodChip(
    name: String,
    action: Action,
    justTapped: Boolean,
    modifier: GlanceModifier,
) {
    val bg = if (justTapped) WidgetColors.Success else WidgetColors.Chip
    val fg = if (justTapped) WidgetColors.OnSuccess else WidgetColors.OnChip
    Box(
        modifier = modifier
            .height(40.dp)
            .background(bg)
            .cornerRadius(16.dp)
            .clickable(action)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (justTapped) {
            // Just-logged success: bold ✓ instead of the food name. Renders
            // for ~650ms (see [QuickLogActionCallback]) then reverts.
            Text(
                text = "✓ Logged",
                maxLines = 1,
                style = TextStyle(
                    color = fg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        } else {
            Text(
                text = name,
                // Truncation handled by Glance via ellipsize-end when bounded.
                maxLines = 1,
                style = TextStyle(
                    color = fg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun PlusButton(action: Action) {
    Box(
        modifier = GlanceModifier
            .height(44.dp)
            .width(44.dp)
            .background(WidgetColors.Primary)
            .cornerRadius(22.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            style = TextStyle(
                color = WidgetColors.OnPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
