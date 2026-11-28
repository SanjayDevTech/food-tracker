package com.example.foodtracker.data

import androidx.compose.runtime.Immutable
import com.example.foodtracker.data.db.FoodLogEntity
import kotlinx.serialization.Serializable

/**
 * How a seeded food was prepared, beyond which fat it was cooked in.
 *
 * A dosa is plain or ghee roast; puttu comes with coconut or without. These are
 * not serving sizes and not different foods — they are the same dish with
 * something added, and the something is usually the most calorie-dense part of
 * the plate. Four grams of ghee on a dosa is +36 kcal and more than doubles its
 * saturated fat.
 *
 * ## Two different mechanics, one currency
 *
 * [CookingOil] *substitutes*: the same grams of fat, priced differently. A
 * style option *adds*: extra grams of an ingredient that were not there before.
 * Both end up as a delta on [Nutrients], which is what lets a food carry an oil
 * axis and a style axis at once without the two needing to know about each
 * other.
 *
 * ## Why a recipe and not a stored delta
 *
 * An option holds an ingredient and a weight rather than thirty pre-computed
 * numbers. It keeps the seed legible — `StyleOption("Ghee roast", Ghee, 4.0)`
 * says what it means, where a frozen delta would just be digits nobody could
 * check — and it keeps every option consistent with every other: fix the ghee
 * row once and every ghee variant in the app moves with it.
 */
@Immutable
enum class VariantIngredient(val label: String, val per100g: Nutrients) {
    /**
     * Carries vitamin A and cholesterol as well as the fat, which plant
     * additions do not — same reason [CookingOil.Ghee] is the odd one out.
     */
    Ghee(
        "ghee",
        Nutrients(
            calories = 900.0, fat = 100.0, saturatedFat = 62.0, omega3 = 1.4,
            cholesterol = 256.0, vitaminA = 700.0, vitaminD = 1.8, vitaminE = 2.8, vitaminK = 8.6,
        ),
    ),

    /** Fresh grated coconut, as layered into puttu or sprinkled on idiyappam. */
    CoconutFresh(
        "grated coconut",
        Nutrients(
            calories = 354.0, carbs = 15.2, sugar = 6.2, fiber = 9.0, protein = 3.3,
            fat = 33.5, saturatedFat = 29.7, sodium = 20.0, calcium = 14.0, iron = 2.4,
            magnesium = 32.0, potassium = 356.0, zinc = 1.1, phosphorus = 113.0,
            selenium = 10.1, copper = 0.44, manganese = 1.5, choline = 7.0, vitaminC = 3.3, vitaminE = 0.24,
            vitaminB1 = 0.07, vitaminB2 = 0.02, vitaminB3 = 0.54, vitaminB6 = 0.05, folate = 26.0,
        ),
    ),

    /** Thick first-press coconut milk, as in an appam batter. */
    CoconutMilk(
        "coconut milk",
        Nutrients(
            calories = 230.0, carbs = 5.5, sugar = 3.3, fiber = 2.2, protein = 2.3,
            fat = 23.8, saturatedFat = 21.1, sodium = 15.0, calcium = 16.0, iron = 1.6,
            magnesium = 37.0, potassium = 263.0, zinc = 0.67, phosphorus = 100.0,
            selenium = 6.2, copper = 0.27, manganese = 0.92, choline = 4.0, vitaminC = 2.8, vitaminE = 0.15,
            vitaminB1 = 0.03, vitaminB2 = 0.01, vitaminB3 = 0.76, vitaminB6 = 0.03, folate = 16.0,
        ),
    ),
}

/**
 * One option on a food's style axis.
 *
 * [grams] is per 100 g of the finished dish, matching how every other figure in
 * the seed is expressed. A null [ingredient] is the "nothing added" option; its
 * delta is zero, and it is the anchor whenever the plain form is the one the
 * stored numbers describe.
 */
@Immutable
@Serializable
data class StyleOption(
    /**
     * Shown on the chip and — because it is also what [FoodLogEntity.style] and
     * `foods.last_style` persist — the identity of this option.
     *
     * So renaming one orphans every log row and remembered pick that referenced
     * it. Those fall back to the food's default rather than erroring, which
     * means a rename silently re-prices history instead of failing loudly. Add
     * an option freely; rename one only with a migration that rewrites the
     * stored labels too.
     *
     * Reads as the addition alone ("Coconut milk", not "With coconut milk"):
     * the sheet renders a binary axis as one toggle under a "Made with"
     * heading, so the preposition is already there.
     */
    val label: String,
    val ingredient: VariantIngredient? = null,
    val grams: Double = 0.0,
) {
    /** What this option adds to the food's stored per-100 g figures. */
    fun delta(): Nutrients =
        ingredient?.let { it.per100g * (grams / 100.0) } ?: Nutrients.Zero

    companion object {
        /** Matches by label, tolerating an option this build no longer offers. */
        fun find(options: List<StyleOption>, label: String?): StyleOption? =
            label?.let { l -> options.firstOrNull { it.label == l } }
    }
}

/**
 * Applies a style option's addition to a per-100 g figure.
 *
 * Only the fields the ingredient actually carries move, because [Nutrients.plus]
 * adds zeroes everywhere else. A null or plain [option] returns [this]
 * untouched, so callers need no branch.
 */
fun Nutrients.withStyle(option: StyleOption?): Nutrients =
    if (option == null || option.ingredient == null) this else this + option.delta()
