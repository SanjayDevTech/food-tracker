package com.example.foodtracker.ui.screens.plan

import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.PlanSlot
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
 * Pair every planned line with today's diary, grouped by slot in
 * [PlanSlot.entries] order, dropping slots with nothing planned.
 *
 * ## The match, and why it takes two passes
 *
 * A planned line for a real meal is satisfied only by a log of that food *in
 * that meal*. An any-time line is satisfied by any log of that food today. The
 * two cannot be resolved in one sweep, because an any-time line would otherwise
 * be free to claim a log that the exact line it was sitting beside had a better
 * title to:
 *
 *     planned: rice @ Any time, rice @ Lunch     logged: rice @ Lunch
 *
 * Settle the exact matches first and the lunch line ticks while the any-time
 * line waits, which is right — two rices were planned and one was eaten.
 * Resolve them in the other order and the lunch line reads as unlogged with a
 * bowl of lunch rice sitting right there in the diary.
 *
 * ## Entries are consumed, not merely matched
 *
 * The pool is a **multiset** that each satisfied line draws one entry out of,
 * so two planned lines can never answer to the same bowl. That is what makes
 * rice at lunch *and* dinner honest: it takes two entries to clear two lines.
 * The old implementation could afford a plain set, because `(food_id,
 * meal_type)` is unique on `plan_items` and no two lines could want the same
 * key; any-time lines compete with the meal lines for the same food, so the
 * counting is now load-bearing.
 *
 * Shared by the Today summary row and the plan-for-today page so the count on
 * one and the ticks on the other cannot disagree — they are the same list,
 * counted and rendered.
 */
fun plannedGroups(
    plan: List<PlanItemWithFood>,
    logs: List<FoodLogWithFood>,
): Map<PlanSlot, List<PlannedRow>> {
    if (plan.isEmpty()) return emptyMap()

    // Today's entries, counted per (food, meal). Rows are drawn out of here as
    // lines claim them.
    val pool = HashMap<Pair<Long, String>, Int>()
    logs.forEach { pool.merge(it.log.foodId to it.log.mealType, 1, Int::plus) }

    fun take(key: Pair<Long, String>): Boolean {
        val left = pool[key] ?: 0
        if (left <= 0) return false
        pool[key] = left - 1
        return true
    }

    val satisfied = HashSet<Long>()
    val (anyTime, atMeal) = plan.partition {
        PlanSlot.fromName(it.item.mealType) == PlanSlot.AnyTime
    }

    // Pass one: a line for a real meal takes an entry from that meal only.
    atMeal.forEach { row ->
        if (take(row.item.foodId to row.item.mealType)) satisfied += row.item.id
    }

    // Pass two: an any-time line takes whatever entry for that food is left,
    // earliest meal first. Sorted rather than taken off the map's iteration
    // order, which is unspecified — two runs over the same day must not tick
    // different lines.
    anyTime.forEach { row ->
        val key = pool.keys
            .filter { it.first == row.item.foodId && (pool[it] ?: 0) > 0 }
            .minByOrNull { mealOrder(it.second) }
        if (key != null && take(key)) satisfied += row.item.id
    }

    val grouped = plan
        .map { PlannedRow(it, it.item.id in satisfied) }
        .groupBy { PlanSlot.fromName(it.planned.item.mealType) }

    // Built in PlanSlot order rather than sorted, so "any time last" is stated
    // once, where the order is defined, instead of re-derived by a comparator
    // on every screen.
    return PlanSlot.entries
        .mapNotNull { slot -> grouped[slot]?.let { slot to it } }
        .toMap(LinkedHashMap())
}

/** Meal order for a stored name, so an unknown one sorts last rather than first. */
private fun mealOrder(mealName: String): Int =
    MealType.entries.indexOfFirst { it.name == mealName }
        .let { if (it < 0) Int.MAX_VALUE else it }

/**
 * The planned lines that are not yet in today's diary, flattened out of their
 * groups but still in slot order — breakfast first, any-time last.
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
