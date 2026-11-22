package com.example.foodtracker.data

import androidx.compose.runtime.Immutable

/**
 * The cooking fat a dish was made with.
 *
 * Why this exists: for anything fried or tempered, the oil is not a detail —
 * it is most of the fat on the plate, and the oils people actually use differ
 * from each other far more than the dishes do. One vada is 13 g of absorbed
 * oil. In palmolein that is 5.9 g of saturated fat; in coconut oil it is
 * 10.7 g; in gingelly oil it is 1.8 g. A single number baked into the seed
 * cannot be right for all three kitchens, so the seed stores each dish at one
 * documented default and this enum re-derives the difference.
 *
 * ## Only six numbers move
 *
 * Swapping the oil changes saturated fat, omega-3, vitamin E and vitamin K —
 * and, for [Ghee] alone, vitamin A and cholesterol. Nothing else moves, and
 * that is a property of fats rather than a simplification:
 *
 *  - **Calories and total fat stay put.** Every fat is ~9 kcal/g, and the
 *    quantity the food absorbs is set by the frying, not by which oil is in
 *    the pan. Choosing coconut oil must not make a vada "lighter".
 *  - **Protein, carbs, fiber, minerals and the B-complex stay put.** Refined
 *    oils carry none of them in any amount worth recording.
 *
 * So the adjustment is a delta on six fields, never a re-derivation of the
 * food.
 *
 * ## Vitamin E is alpha-tocopherol, and that matters most for palm
 *
 * Palm oil is marketed as a vitamin E powerhouse on a figure near 100 mg per
 * 100 g. That total is mostly **tocotrienols**. RDAs — including the 15 mg
 * this app targets — count only alpha-tocopherol, because the 2000 IOM review
 * found the other vitamers do not maintain plasma concentrations in humans.
 * Palmolein's alpha-tocopherol is about 16 mg/100 g, and that is the number
 * below. Please do not "correct" it upward against a label or a supplement
 * brochure; the larger figure is real but not what the goal is measured in.
 *
 * The same distinction, in the other direction, is why [Gingelly] is so low.
 * Sesame oil is genuinely rich in tocopherols, but almost all of it is the
 * gamma form, leaving only ~1.4 mg of alpha.
 *
 * Values are per 100 g of oil, USDA SR Legacy for the single oils, with
 * palmolein taken as RBD palm olein rather than whole palm oil (the liquid
 * fraction Indian retail actually sells, and a few points less saturated than
 * crude palm).
 */
@Immutable
enum class CookingOil(
    val label: String,
    /** g per 100 g oil. */
    val saturatedFat: Double,
    /** g ALA per 100 g oil. No oil here carries EPA or DHA. */
    val omega3: Double,
    /** mg alpha-tocopherol per 100 g oil — see the note above. */
    val vitaminE: Double,
    /** µg phylloquinone (K1) per 100 g oil. */
    val vitaminK: Double,
    /** µg RAE per 100 g oil. Zero for every plant oil; refined palmolein is
     *  bleached, so even red palm's carotenes are gone by the time it is sold. */
    val vitaminA: Double = 0.0,
    /** mg per 100 g oil. Zero for every plant oil — cholesterol is animal-only. */
    val cholesterol: Double = 0.0,
) {
    /**
     * The default almost everywhere, because it is what is actually in the
     * pan — India is the world's largest palm oil importer and palmolein
     * dominates both commercial frying and the blended "vegetable oil" sold in
     * pouches. Choosing it as the baseline is the single biggest correction in
     * this model: it is roughly two and a half times as saturated as the
     * groundnut oil the seed's older figures had implied.
     */
    Palmolein("Palmolein", saturatedFat = 45.0, omega3 = 0.20, vitaminE = 16.0, vitaminK = 8.0),

    /** Kerala, and the coastal belt. By far the most saturated option. */
    Coconut("Coconut", saturatedFat = 82.5, omega3 = 0.0, vitaminE = 0.09, vitaminK = 0.5),

    /** The traditional frying oil inland — Tamil Nadu, Andhra, Karnataka. */
    Groundnut("Groundnut", saturatedFat = 16.9, omega3 = 0.0, vitaminE = 15.7, vitaminK = 0.7),

    /**
     * Nallennai. Traditional for tempering, kuzhambu, poriyal and pickles
     * rather than for deep frying. The best vitamin K of the plant oils here
     * and the least saturated, but almost no alpha-tocopherol.
     */
    Gingelly("Gingelly (sesame)", saturatedFat = 14.2, omega3 = 0.30, vitaminE = 1.4, vitaminK = 13.6),

    /**
     * The only animal fat in the list, and so the only one that moves vitamin A
     * and cholesterol as well. Used for pongal, kesari and the payasams.
     */
    Ghee(
        "Ghee",
        saturatedFat = 62.0,
        omega3 = 1.4,
        vitaminE = 2.8,
        vitaminK = 8.6,
        vitaminA = 700.0,
        cholesterol = 256.0,
    );

    companion object {
        /** What a food falls back to when nothing has been chosen for it. */
        val Default = Palmolein

        /** Parses a stored name, tolerating rows written before this existed. */
        fun fromName(name: String?): CookingOil? =
            name?.let { n -> entries.firstOrNull { it.name == n } }

        /**
         * The oils to offer for a dish whose own default is [default].
         *
         * Deliberately not the full list every time. Nobody deep-fries murukku
         * in ghee, and offering it invites a tap that produces a number no
         * kitchen would ever cook — so ghee's slot is reserved for the dishes
         * that are actually made with it, and those in turn only offer the fats
         * a sweet is plausibly made in. The food's own default always leads, so
         * the pre-selected chip is the first one.
         */
        fun optionsFor(default: CookingOil): List<CookingOil> {
            val set = if (default == Ghee) {
                listOf(Ghee, Palmolein, Coconut)
            } else {
                listOf(Palmolein, Coconut, Groundnut, Gingelly)
            }
            return listOf(default) + set.filter { it != default }
        }

    }
}

/**
 * Re-states [this] — a per-100 g-of-food figure made with [from] — as though
 * the same dish had been cooked in [to].
 *
 * [oilGrams] is the added cooking fat per 100 g of the finished dish, so the
 * whole adjustment is `oilGrams/100 × (to − from)` on the six fields that the
 * fat decides. A food with no added oil ([oilGrams] of 0 — a fruit, a dal
 * boiled plain, milk) is returned untouched, which is why this is safe to call
 * unconditionally.
 *
 * Results are floored at zero. The arithmetic can go slightly negative when a
 * dish's stored figure was rounded down and the swap subtracts more than the
 * rounding left — a vitamin K of -0.04 µg is an artefact, not a finding.
 */
fun Nutrients.withCookingOil(
    oilGrams: Double,
    from: CookingOil,
    to: CookingOil,
): Nutrients {
    if (oilGrams <= 0.0 || from == to) return this
    val f = oilGrams / 100.0
    return copy(
        saturatedFat = (saturatedFat + f * (to.saturatedFat - from.saturatedFat)).coerceAtLeast(0.0),
        omega3 = (omega3 + f * (to.omega3 - from.omega3)).coerceAtLeast(0.0),
        vitaminE = (vitaminE + f * (to.vitaminE - from.vitaminE)).coerceAtLeast(0.0),
        vitaminK = (vitaminK + f * (to.vitaminK - from.vitaminK)).coerceAtLeast(0.0),
        vitaminA = (vitaminA + f * (to.vitaminA - from.vitaminA)).coerceAtLeast(0.0),
        cholesterol = (cholesterol + f * (to.cholesterol - from.cholesterol)).coerceAtLeast(0.0),
    )
}
