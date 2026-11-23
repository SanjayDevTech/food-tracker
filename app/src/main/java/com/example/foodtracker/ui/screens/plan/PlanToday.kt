package com.example.foodtracker.ui.screens.plan

import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.data.db.PlanItemWithFood

/**
 * One line of the plan as today sees it: what was planned, and whether the
 * diary already holds it.
 *
 * [logged] is **derived, never stored.** The plan is permanent and the ticks
 * are not — they are a fact about one day — so a `checked` column would have to
 * be cleared at midnight by something, and would disagree with the diary the
 * moment an entry was deleted from Today. Reading it off the log instead means
 * the two cannot drift: delete the entry and the line un-ticks itself, which is
 * also the only way to un-tick one.
 */
data class PlannedRow(
    val planned: PlanItemWithFood,
    val logged: Boolean,
)

/**
 * Pair every planned line with today's diary, grouped by meal in [MealType]
 * order, dropping meals with nothing planned.
 *
 * **The match is on food *and* meal.** Matching on the food alone would tick
 * the dinner rice when you logged the lunch rice, which is both wrong and
 * silent. `(food_id, meal_type)` is unique on `plan_items`, so one diary entry
 * can satisfy at most one planned line.
 *
 * Shared by the Today summary row and the plan-for-today page so the count on
 * one and the ticks on the other cannot disagree — they are the same list,
 * counted and rendered.
 */
fun plannedGroups(
    plan: List<PlanItemWithFood>,
    logs: List<FoodLogWithFood>,
): Map<MealType, List<PlannedRow>> {
    if (plan.isEmpty()) return emptyMap()
    val loggedSlots = logs.mapTo(HashSet()) { it.log.foodId to it.log.mealType }
    return plan
        .map { PlannedRow(it, (it.item.foodId to it.item.mealType) in loggedSlots) }
        .groupBy { MealType.fromName(it.planned.item.mealType) }
        .toSortedMap(compareBy { it.ordinal })
}

/**
 * The planned lines that are not yet in today's diary, flattened out of their
 * meal groups but still in meal order — breakfast first.
 *
 * Today names the first few of these on its plan row. A count alone said how
 * much was left and nothing about what, which is the half that makes the row
 * worth reading: "3 foods" is a score, "Dosa · Sambar · Filter coffee" is the
 * plan.
 */
fun plannedRemainingRows(
    plan: List<PlanItemWithFood>,
    logs: List<FoodLogWithFood>,
): List<PlannedRow> = plannedGroups(plan, logs).values.flatten().filter { !it.logged }

/** How many planned lines are not yet in today's diary. */
fun plannedRemaining(
    plan: List<PlanItemWithFood>,
    logs: List<FoodLogWithFood>,
): Int = plannedGroups(plan, logs).values.sumOf { rows -> rows.count { !it.logged } }
