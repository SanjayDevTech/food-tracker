package com.example.foodtracker.data

import androidx.compose.runtime.Immutable

/**
 * How much of the pan's oil actually ended up in the dish.
 *
 * ## Why this is a second axis and not more chips on the first
 *
 * [CookingOil] answers *which fat*, holding *how much* fixed, and it is
 * emphatic that calories and total fat must not move when the answer changes:
 * every fat is ~9 kcal/g, so 2.7 g of palmolein and 2.7 g of coconut oil are
 * the same energy and the same grams of fat. Making the chip move them would
 * invent a difference that does not exist and bury the one that does — a vada
 * carries 1.8 g of saturated fat in gingelly oil and 10.7 g in coconut, at
 * identical calories.
 *
 * This axis answers the other question, and it *must* move calories and total
 * fat, because it changes the number of grams. One row is "what was in the
 * pan"; the other is "how much of it ended up in the dosa", and only the second
 * has any business touching the energy figure.
 *
 * ## Named steps rather than a gram field
 *
 * The honest reason: nobody knows their own figure. "A smear to keep the cast
 * iron from sticking, skipped on some" is a real and useful thing to say; "0.54
 * g per 100 g" is the same claim wearing a precision that was never measured.
 * A number field would invite the false one. [factor] converts the step into
 * the seed's own units, so the per-dish arithmetic stays grounded in the
 * documented figure rather than in a second guess.
 *
 * Stored by [name], the same way [CookingOil] and [StyleOption] are, so the
 * steps can be re-tuned later without rewriting a single row.
 */
@Immutable
enum class OilAmount(val label: String, val factor: Double) {
    /** A dry non-stick or well-seasoned pan. Removes the cooking fat entirely. */
    None("None", 0.0),

    /**
     * A wiped or smeared pan — enough to stop it sticking, and skipped on some
     * of the batch. A fifth of the seed's figure, which on a dosa is 0.54 g per
     * 100 g against 2.7.
     */
    Light("Light", 0.2),

    /** The seed's own figure: drizzled as a restaurant or a tiffin shop would. */
    Normal("Normal", 1.0);

    companion object {
        /** What a food falls back to when nothing has been chosen for it. */
        val Default = Normal

        /** Parses a stored name, tolerating a step this build no longer offers. */
        fun fromName(name: String?): OilAmount? =
            name?.let { n -> entries.firstOrNull { it.name == n } }

        /**
         * The foods that offer this axis, by pinned `clientUuid`.
         *
         * **Deliberately short.** The mechanic is general — any dish with added
         * cooking fat could carry it — but a chip row is a claim that this is a
         * choice *you* make, and that is only true where you are the one at the
         * pan. So the list is the home griddle and the home fry: the two dosas,
         * the chapati, the maththi and the dry chicken fry. It is not on a
         * samosa or a vada pav, where the oil was someone else's decision
         * before you bought it, and not on a curry, where the tempering is the
         * dish rather than a dressing you can skip.
         *
         * Widening it is adding uuids here and nothing else.
         *
         * Keyed on the pinned uuid rather than the name so a rename cannot
         * silently drop the axis — that is what the pinned identities are for.
         */
        val AXIS_FOOD_UUIDS: Set<String> = setOf(
            // Dosa (plain)
            "dbed1a1c-dc61-4849-b572-e885480d7c5a",
            // Wheat Dosa
            "e450728c-21d1-447f-b378-fa1c4ad0a6f8",
            // Chapati / Roti
            "3e420f59-d232-48ef-b878-021ea688cef5",
            // Kerala Maththi (Sardine, fried)
            "9faa12b5-23b4-4c1e-b7d6-0c9ac3e348f6",
            // Chicken Boneless (Fried)
            "5988db31-52f9-41b3-a8b4-108700363af4",
        )
    }
}

/**
 * Energy per gram of any cooking fat.
 *
 * The oils in [CookingOil] span 876–900 kcal/100 g, a spread of under 3% that
 * is an order of magnitude below the precision of the gram figure it would
 * multiply — a dosa's 2.7 g is itself a considered estimate. Carrying a
 * per-oil calorie column would therefore buy a rounding artefact and a column
 * to keep correct.
 */
private const val KCAL_PER_G_FAT = 9.0

/**
 * Re-states [this] — a per-100 g-of-food figure that includes [oilGrams] of
 * [oil] — as though only [amount] of that oil had been used.
 *
 * Unlike [withCookingOil], this moves **calories and total fat**, because it is
 * removing grams rather than substituting them. The same six fat-decided fields
 * move with them, in the same proportion.
 *
 * [OilAmount.Normal] and a food with no added fat both return [this] untouched,
 * so callers need no branch. Results are floored at zero: a dish whose stored
 * figures were rounded down can otherwise land a hair below it, and a vitamin K
 * of -0.04 µg is an artefact rather than a finding.
 *
 * Call it **after** [withCookingOil] and **before** [withStyle]: by then the
 * figures describe the oil the user actually picked, which is the one being
 * taken back out, and a style addition is fat the pan never supplied.
 */
fun Nutrients.withOilAmount(
    oilGrams: Double,
    oil: CookingOil,
    amount: OilAmount,
): Nutrients {
    if (oilGrams <= 0.0 || amount == OilAmount.Normal) return this
    val removed = oilGrams * (1.0 - amount.factor)
    val f = removed / 100.0
    return copy(
        calories = (calories - removed * KCAL_PER_G_FAT).coerceAtLeast(0.0),
        fat = (fat - removed).coerceAtLeast(0.0),
        saturatedFat = (saturatedFat - f * oil.saturatedFat).coerceAtLeast(0.0),
        omega3 = (omega3 - f * oil.omega3).coerceAtLeast(0.0),
        vitaminE = (vitaminE - f * oil.vitaminE).coerceAtLeast(0.0),
        vitaminK = (vitaminK - f * oil.vitaminK).coerceAtLeast(0.0),
        vitaminA = (vitaminA - f * oil.vitaminA).coerceAtLeast(0.0),
        cholesterol = (cholesterol - f * oil.cholesterol).coerceAtLeast(0.0),
    )
}
