package com.example.foodtracker.ui.screens.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.OilAmount
import com.example.foodtracker.data.PlanSlot
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.StyleOption
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.data.db.PlanItemWithFood
import com.example.foodtracker.ui.components.serving.ServingSheet
import com.example.foodtracker.ui.components.serving.isCustomGrams
import com.example.foodtracker.util.DateUtil

/**
 * Check a planned line off into today's diary.
 *
 * The serving sheet again, opened on the amount the plan already holds — so
 * the normal case is read the number, tap the button. The controls are live
 * because the day is not always the plan: you had three rotis, not two.
 *
 * An any-time line lands in the meal the clock is in — see the `meal` state
 * below. That is the completion of the any-time idea rather than a detail of
 * it: the reason guava is planned for no particular time is that it is eaten at
 * whatever meal you happen to be having, so the entry it becomes belongs there
 * and not in a slot the plan declined to name.
 *
 * **Nothing typed here touches the plan.** This writes a [FoodLogEntity] and
 * stops. A plan that quietly rewrote itself every time you ate slightly more
 * would drift a little each day until it described nothing in particular, and
 * the whole value of the list is that it is the amount you *decided* on.
 * Changing the plan is a deliberate act, in the editor.
 */
@Composable
fun PlanCheckSheet(
    row: PlanItemWithFood,
    onLog: (FoodLogEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val food = row.food
    val item = row.item

    // Pre-filled from the plan, matched back to a known variant so the right
    // pill starts selected — and keeping the custom-grams sentinel, which is
    // what tells the sheet to open in that mode.
    var variant by remember(item.id) {
        val known = food.variantsOrDefault().firstOrNull { it.label == item.servingLabel }
        mutableStateOf(
            known ?: ServingVariant(
                label = item.servingLabel.ifBlank { ServingVariant.Default.label },
                grams = if (item.servings > 0) item.grams / item.servings else item.grams,
            )
        )
    }
    var servings by remember(item.id) { mutableStateOf(item.servings) }
    // An any-time line has no meal to open on, so it opens on the one the
    // clock is in — which is the whole point of checking one off: you are
    // eating it now, and it should land where "now" is. A line planned for a
    // real meal still opens on that meal, and the footer chip edits either.
    var meal by remember(item.id) {
        mutableStateOf(
            when (val slot = PlanSlot.fromName(item.mealType)) {
                is PlanSlot.At -> slot.meal
                PlanSlot.AnyTime -> MealType.currentForTime()
            }
        )
    }
    // The preparation the plan decided on, not the food's default: the user
    // already answered "ghee roast?" when they built the list, and asking again
    // every morning is the friction the plan exists to remove.
    var oil by remember(item.id) { mutableStateOf(item.oil?.let { CookingOil.fromName(it) }) }
    var style by remember(item.id) { mutableStateOf(StyleOption.find(food.styleOptions, item.style)) }
    var oilAmount by remember(item.id) { mutableStateOf(OilAmount.fromName(item.oilAmount)) }

    ServingSheet(
        food = food,
        meal = meal,
        variant = variant,
        servings = servings,
        onVariantChange = { variant = it },
        onServingsChange = { servings = it },
        onMealChange = { meal = it },
        onConfirm = {
            val isCustom = variant.isCustomGrams
            onLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = meal.name,
                    servings = if (isCustom) 1.0 else servings,
                    grams = if (isCustom) variant.grams else variant.grams * servings,
                    servingLabel = variant.label,
                    oil = if (food.usesCookingOil()) (oil ?: food.preselectedOil()).name else null,
                    style = if (food.hasStyleAxis()) (style ?: food.preselectedStyle())?.label else null,
                    oilAmount = if (food.usesOilAmountAxis()) {
                        (oilAmount ?: food.preselectedOilAmount()).name
                    } else null,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            onDismiss()
        },
        onDismiss = onDismiss,
        confirmText = "Add to diary",
        subtitle = "From your plan",
        oil = oil,
        onOilChange = { oil = it },
        style = style,
        onStyleChange = { style = it },
        oilAmount = oilAmount,
        onOilAmountChange = { oilAmount = it },
    )
}
