package com.example.foodtracker.data

import androidx.compose.runtime.Immutable

/**
 * When a planned food is meant to be eaten: a meal, or no particular time.
 *
 * ## Why "any time" had to exist
 *
 * The plan ticks a line off when today's diary holds the food, and the match
 * used to require the meal to agree as well. That is right for rice and wrong
 * for guava, and the two cases are the *same shape*:
 *
 *  - Guava planned as a snack, eaten at breakfast — should tick. The slot was
 *    never the point; it was somewhere to file it.
 *  - Rice planned at lunch, eaten at breakfast, with lunch still ahead —
 *    should not tick. The slot is exactly the point, because rice turns up at
 *    three meals and only one of them is the planned one.
 *
 * One plan line, one log in a different meal, opposite correct answers. No rule
 * over food, meal and clock can separate them, because the difference is not in
 * the data: it is what the user meant. So they declare it, and this is the
 * declaration — a fifth value of a field that already existed, rather than a
 * flag bolted beside it.
 *
 * ## Why it is not a fifth [MealType]
 *
 * A diary entry always happened at some real meal, so the log sheet's picker
 * must stay at four. This type is the plan's own vocabulary and only the plan
 * reads it; [MealType] is untouched, which also means `plan_items.meal_type`
 * needed no migration to hold [AnyTime] — it was always a String.
 *
 * Beware [MealType.fromName], which answers `Snack` for any name it does not
 * know: every plan read has to come through [fromName] here instead, or an
 * any-time line files itself under Snack with no error to show for it.
 */
@Immutable
sealed interface PlanSlot {

    /** What `plan_items.meal_type` holds for this slot. */
    val storedName: String

    /** The section heading in the editor, and the label on a chip. */
    val displayName: String

    /** Planned for one of the day's meals, matched to a log in that same meal. */
    @Immutable
    data class At(val meal: MealType) : PlanSlot {
        override val storedName: String get() = meal.name
        override val displayName: String get() = meal.displayName
    }

    /**
     * Planned for today, with no claim about when — matched by any log of that
     * food, whatever meal it was eaten at.
     */
    @Immutable
    data object AnyTime : PlanSlot {
        override val storedName: String = "Any"
        override val displayName: String = "Any time"
    }

    companion object {
        /**
         * Every slot, in reading order: the four meals as they fall, then
         * [AnyTime].
         *
         * Last rather than first on both screens. The meals are the spine of
         * the editor and a leading odd section would push breakfast below the
         * fold, and on Today's caption the timed foods are the ones with a
         * place in the day — "Dosa · Sambar · +2 more" says more about what is
         * coming than the pumpkin seeds do.
         */
        val entries: List<PlanSlot> = MealType.entries.map(::At) + AnyTime

        /**
         * Parses a stored name. Checked against [AnyTime] *first*, because
         * [MealType.fromName] would swallow it as `Snack`.
         */
        fun fromName(name: String): PlanSlot =
            if (name == AnyTime.storedName) AnyTime else At(MealType.fromName(name))
    }
}
