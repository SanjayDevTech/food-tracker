package com.example.foodtracker.data.db

import com.example.foodtracker.data.FoodCategory
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ServingVariant
import kotlin.math.roundToInt

/**
 * Curated seed list of Indian foods. Nutrient values are per 100 g unless noted.
 * Values are reasonable approximations drawn from common food composition tables
 * (IFCT, USDA equivalents). Not a substitute for clinical accuracy.
 */
object IndianFoodSeed {

    /**
     * Bump this whenever any seed entry's nutrient values change. On the next
     * cold start the repository will run `refreshSeedIfStale()`, which updates
     * every seeded food in place by name — preserving the user's log history,
     * favourites, weight entries, and custom foods.
     */
    const val SEED_VERSION = 10

    private fun food(
        name: String,
        category: FoodCategory,
        servingGrams: Double,
        servingLabel: String,
        n: Nutrients,
        servingVariants: List<ServingVariant> = emptyList(),
    ) = FoodEntity.fromNutrientsPer100g(
        name = name,
        category = category.name,
        n = n,
        servingGrams = servingGrams,
        servingLabel = servingLabel,
        // Explicit variants win; otherwise attach Small/Medium/Large presets to
        // foods whose portion size genuinely varies (see [sizeVariantsFor]).
        servingVariants = servingVariants.ifEmpty { sizeVariantsFor(servingGrams, servingLabel) },
    )

    /** Rounds a derived gram amount to a clean, human-friendly step. */
    private fun roundServing(g: Double): Double = when {
        g < 50 -> (g / 5.0).roundToInt() * 5.0
        g < 200 -> (g / 10.0).roundToInt() * 10.0
        else -> (g / 25.0).roundToInt() * 25.0
    }

    /**
     * Small / Medium / Large presets for foods whose portion size genuinely
     * varies (fruits, breads, tiffin, bowls, plates, glasses). Medium is the
     * canonical serving, so the default logged amount is unchanged; Small/Large
     * are derived from it (×0.7 / ×1.4) and rounded to clean steps.
     *
     * Tiny fixed condiments measured by the spoon (tbsp/tsp — pickles, podis,
     * chutneys, ghee, sugar) and sub-25 g crisp items get NO size presets: the
     * servings slider and Custom-g chip already cover those, and "large ghee"
     * is nonsense. Returns [] when not applicable, so
     * [FoodEntity.fromNutrientsPer100g] falls back to its default gram-reference
     * variants (canonical + 100 g / 50 g).
     */
    fun sizeVariantsFor(grams: Double, label: String): List<ServingVariant> {
        val l = label.lowercase()
        val applicable = grams >= 25.0 && !l.contains("tbsp") && !l.contains("tsp")
        if (!applicable) return emptyList()
        return listOf(
            ServingVariant("Medium", grams),
            ServingVariant("Small", roundServing(grams * 0.7)),
            ServingVariant("Large", roundServing(grams * 1.4)),
        )
    }


    /**
     * The protein quality of one food, held as grams per gram of protein
     * rather than as absolute grams: [eaa] is the share of its protein made up
     * of the nine essential amino acids, [leucine] the share that is leucine.
     *
     * Ratios, not absolutes, because our per-100 g protein figures are our own
     * (IFCT-leaning, rounded) and rarely match the USDA entry these profiles
     * come from. Scaling a published ratio by OUR protein keeps the parts
     * consistent with the whole shown right above them, and makes it
     * arithmetically impossible for them to exceed it.
     *
     * Each pair is that food's own USDA SR profile summed, not a food-group
     * average — which is why only the ~25 single-ingredient staples below
     * carry one. Every other seeded food, and every custom or AI food, keeps 0
     * (unknown) and shows no breakdown at all. A mixed dish like sambar or
     * biryani gets none: its protein comes from several sources in proportions
     * that vary by cook.
     */
    private class AminoPattern(val eaa: Double, val leucine: Double)

    // Animal protein and dairy — the complete profiles, 41-47% EAA.
    private val AA_EGG = AminoPattern(eaa = 0.445, leucine = 0.087)
    private val AA_CHICKEN = AminoPattern(eaa = 0.415, leucine = 0.075)
    // One pattern for milk, curd and paneer: all three are the same casein +
    // whey protein. Curd ferments the lactose, not the amino acids, and paneer
    // concentrates the casein rather than changing its profile.
    private val AA_DAIRY = AminoPattern(eaa = 0.465, leucine = 0.099)

    // Cereals — 29-37% EAA, and the lowest leucine yields per gram of food,
    // because there is so little protein to take a share of.
    private val AA_RICE_WHITE = AminoPattern(eaa = 0.373, leucine = 0.083)
    private val AA_RICE_BROWN = AminoPattern(eaa = 0.370, leucine = 0.081)
    private val AA_OATS = AminoPattern(eaa = 0.348, leucine = 0.074)
    private val AA_WHEAT = AminoPattern(eaa = 0.292, leucine = 0.065)

    // Pulses — 36-39% EAA, between the cereals and animal protein, which is
    // why a rice-and-dal meal scores better than either grain alone.
    private val AA_LENTIL = AminoPattern(eaa = 0.365, leucine = 0.073)
    private val AA_CHICKPEA = AminoPattern(eaa = 0.364, leucine = 0.071)
    private val AA_MUNG = AminoPattern(eaa = 0.393, leucine = 0.080)
    private val AA_COWPEA = AminoPattern(eaa = 0.386, leucine = 0.077)
    private val AA_URAD = AminoPattern(eaa = 0.393, leucine = 0.079)
    private val AA_TOOR = AminoPattern(eaa = 0.369, leucine = 0.071)
    private val AA_SOY = AminoPattern(eaa = 0.383, leucine = 0.079)

    /**
     * Attach the optional detail — the protein profile and/or the soluble
     * share of fiber — to an otherwise plain [Nutrients].
     *
     * [solubleFiberFraction] is the published soluble share of this food's
     * total dietary fiber, applied to OUR fiber figure for the same reason the
     * amino acids are ratios: the two parts then add up to the total on screen
     * instead of contradicting it.
     *
     * Leaving either argument null leaves those fields at 0, which the app
     * reads as unknown and hides.
     */
    private fun Nutrients.detail(
        amino: AminoPattern? = null,
        solubleFiberFraction: Double? = null,
    ): Nutrients {
        val soluble = solubleFiberFraction?.let { fiber * it }
        return copy(
            solubleFiber = soluble ?: solubleFiber,
            insolubleFiber = soluble?.let { fiber - it } ?: insolubleFiber,
            essentialAminoAcids = amino?.let { protein * it.eaa } ?: essentialAminoAcids,
            leucine = amino?.let { protein * it.leucine } ?: leucine,
        )
    }

    val ALL: List<FoodEntity> = listOf(
        // ---------- SOUTH INDIAN ----------
        food("Idli", FoodCategory.SouthIndian, 35.0, "1 idli (35 g)",
            Nutrients(calories = 145.0, carbs = 30.0, protein = 4.0, fat = 1.0, fiber = 1.2, sodium = 220.0,
                calcium = 18.0, iron = 1.0, magnesium = 25.0, potassium = 60.0, zinc = 0.4, vitaminB12 = 0.0, folate = 14.0, phosphorus = 50.0, selenium = 3.5, copper = 0.1, manganese = 0.3, vitaminB1 = 0.05, vitaminB2 = 0.03, vitaminB3 = 0.7, vitaminB6 = 0.04)),
        food("Dosa (plain)", FoodCategory.SouthIndian, 80.0, "1 dosa (80 g)",
            Nutrients(calories = 168.0, carbs = 30.0, protein = 4.0, fat = 3.7, fiber = 1.5, sodium = 230.0,
                calcium = 14.0, iron = 1.2, magnesium = 23.0, potassium = 70.0, zinc = 0.5, folate = 16.0, phosphorus = 75.0, selenium = 4.5, copper = 0.14, manganese = 0.4, vitaminB1 = 0.07, vitaminB2 = 0.04, vitaminB3 = 1.0, vitaminB6 = 0.06)),
        food("Masala Dosa", FoodCategory.SouthIndian, 150.0, "1 masala dosa (150 g)",
            Nutrients(calories = 168.0, carbs = 27.0, protein = 3.6, fat = 5.4, fiber = 2.2, sodium = 280.0,
                calcium = 17.0, iron = 1.4, magnesium = 26.0, potassium = 195.0, zinc = 0.6, vitaminC = 6.0, folate = 18.0, phosphorus = 70.0, selenium = 3.5, copper = 0.13, manganese = 0.35, vitaminB1 = 0.08, vitaminB2 = 0.04, vitaminB3 = 1.1, vitaminB6 = 0.12)),
        food("Uttapam", FoodCategory.SouthIndian, 100.0, "1 uttapam (100 g)",
            Nutrients(calories = 155.0, carbs = 28.0, protein = 4.5, fat = 3.0, fiber = 1.8, sodium = 240.0,
                calcium = 20.0, iron = 1.2, magnesium = 24.0, potassium = 110.0, vitaminC = 3.0, folate = 18.0, phosphorus = 70.0, selenium = 4.0, copper = 0.13, manganese = 0.38, vitaminB1 = 0.07, vitaminB2 = 0.04, vitaminB3 = 1.0, vitaminB6 = 0.08)),
        food("Medu Vada", FoodCategory.SouthIndian, 40.0, "1 vada (40 g)",
            Nutrients(calories = 290.0, carbs = 30.0, protein = 9.0, fat = 14.0, fiber = 3.0, sodium = 340.0,
                calcium = 30.0, iron = 1.6, magnesium = 50.0, potassium = 250.0, zinc = 1.4, folate = 60.0, phosphorus = 110.0, selenium = 3.0, copper = 0.3, manganese = 0.55, vitaminB1 = 0.12, vitaminB2 = 0.05, vitaminB3 = 1.0, vitaminB6 = 0.1)),
        food("Sambar", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 70.0, carbs = 9.0, protein = 4.0, fat = 2.0, fiber = 3.0, sodium = 350.0,
                calcium = 28.0, iron = 1.4, magnesium = 35.0, potassium = 290.0, zinc = 0.7, vitaminA = 60.0, vitaminC = 8.0, folate = 35.0, phosphorus = 60.0, selenium = 2.0, copper = 0.18, manganese = 0.3, vitaminB1 = 0.08, vitaminB2 = 0.04, vitaminB3 = 0.7, vitaminB6 = 0.12)),
        food("Rasam", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 35.0, carbs = 6.0, protein = 1.4, fat = 0.8, fiber = 0.7, sodium = 300.0,
                calcium = 14.0, iron = 0.8, magnesium = 15.0, potassium = 180.0, vitaminC = 14.0, folate = 10.0, phosphorus = 25.0, selenium = 1.0, copper = 0.08, manganese = 0.15, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.5, vitaminB6 = 0.08)),
        food("Coconut Chutney", FoodCategory.SouthIndian, 30.0, "2 tbsp (30 g)",
            Nutrients(calories = 220.0, carbs = 7.0, protein = 3.0, fat = 21.0, saturatedFat = 18.0, fiber = 5.0,
                sodium = 240.0, calcium = 14.0, iron = 1.2, magnesium = 28.0, potassium = 270.0, zinc = 1.1, phosphorus = 80.0, selenium = 4.0, copper = 0.3, manganese = 0.9, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.5, vitaminB6 = 0.05)),
        food("Tomato Chutney", FoodCategory.SouthIndian, 30.0, "2 tbsp (30 g)",
            Nutrients(calories = 70.0, carbs = 8.0, protein = 1.5, fat = 3.5, fiber = 1.6, sodium = 320.0,
                calcium = 12.0, iron = 0.6, magnesium = 14.0, potassium = 230.0, vitaminA = 80.0, vitaminC = 16.0, phosphorus = 35.0, selenium = 0.6, copper = 0.12, manganese = 0.2, vitaminB1 = 0.04, vitaminB2 = 0.03, vitaminB3 = 0.7, vitaminB6 = 0.1)),
        food("Curd Rice", FoodCategory.SouthIndian, 250.0, "1 plate (250 g)",
            Nutrients(calories = 115.0, carbs = 19.0, protein = 4.0, fat = 2.5, fiber = 0.5, sodium = 210.0,
                calcium = 60.0, iron = 0.5, magnesium = 15.0, potassium = 90.0, zinc = 0.5, vitaminB12 = 0.3, phosphorus = 75.0, selenium = 3.0, copper = 0.08, manganese = 0.25, vitaminB1 = 0.04, vitaminB2 = 0.08, vitaminB3 = 0.5, vitaminB6 = 0.05)),
        food("Lemon Rice", FoodCategory.SouthIndian, 200.0, "1 plate (200 g)",
            Nutrients(calories = 175.0, carbs = 30.0, protein = 3.5, fat = 4.5, fiber = 1.0, sodium = 230.0,
                calcium = 10.0, iron = 0.7, magnesium = 14.0, potassium = 70.0, vitaminC = 6.0, phosphorus = 50.0, selenium = 4.0, copper = 0.12, manganese = 0.4, vitaminB1 = 0.05, vitaminB2 = 0.02, vitaminB3 = 0.8, vitaminB6 = 0.06)),
        food("Tamarind Rice (Pulihora)", FoodCategory.SouthIndian, 200.0, "1 plate (200 g)",
            Nutrients(calories = 185.0, carbs = 32.0, protein = 3.0, fat = 5.0, fiber = 1.3, sodium = 280.0,
                calcium = 22.0, iron = 1.0, magnesium = 18.0, potassium = 130.0, vitaminC = 2.0, phosphorus = 55.0, selenium = 4.0, copper = 0.16, manganese = 0.45, vitaminB1 = 0.05, vitaminB2 = 0.02, vitaminB3 = 0.8, vitaminB6 = 0.08)),
        food("Ven Pongal", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 195.0, carbs = 27.0, protein = 6.0, fat = 7.0, fiber = 2.5, sodium = 240.0,
                calcium = 22.0, iron = 1.3, magnesium = 38.0, potassium = 180.0, zinc = 1.0, folate = 22.0, phosphorus = 70.0, selenium = 4.0, copper = 0.18, manganese = 0.4, vitaminB1 = 0.08, vitaminB2 = 0.03, vitaminB3 = 0.7, vitaminB6 = 0.07)),
        food("Rava Upma", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 175.0, carbs = 30.0, protein = 5.0, fat = 4.0, fiber = 1.8, sodium = 280.0,
                calcium = 15.0, iron = 1.4, magnesium = 24.0, potassium = 90.0, folate = 30.0, phosphorus = 55.0, selenium = 8.0, copper = 0.12, manganese = 0.3, vitaminB1 = 0.07, vitaminB2 = 0.03, vitaminB3 = 0.9, vitaminB6 = 0.06)),
        food("Appam", FoodCategory.SouthIndian, 60.0, "1 appam (60 g)",
            Nutrients(calories = 145.0, carbs = 28.0, protein = 2.4, fat = 2.5, fiber = 0.8, sodium = 180.0,
                calcium = 10.0, iron = 0.8, magnesium = 18.0, potassium = 70.0, phosphorus = 40.0, selenium = 2.5, copper = 0.1, manganese = 0.3, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.6, vitaminB6 = 0.04)),
        // One string-hopper nest is ~35–40 g; a serving is 2 nests. Count in nests,
        // not a scaled "1 nest" that the auto-Large would inflate past a real nest.
        food("Idiyappam", FoodCategory.SouthIndian, 80.0, "2 nests (80 g)",
            Nutrients(calories = 155.0, carbs = 33.0, protein = 2.8, fat = 0.8, fiber = 0.6, sodium = 90.0,
                calcium = 8.0, iron = 0.5, magnesium = 14.0, potassium = 50.0, phosphorus = 35.0, selenium = 3.0, copper = 0.08, manganese = 0.25, vitaminB1 = 0.03, vitaminB2 = 0.01, vitaminB3 = 0.5, vitaminB6 = 0.04),
            servingVariants = listOf(
                ServingVariant("2 nests", 80.0),
                ServingVariant("1 nest", 40.0),
                ServingVariant("3 nests", 120.0),
            )),
        food("Puttu", FoodCategory.SouthIndian, 100.0, "1 cup (100 g)",
            Nutrients(calories = 170.0, carbs = 33.0, protein = 3.5, fat = 2.5, fiber = 1.7, sodium = 80.0,
                calcium = 14.0, iron = 1.0, magnesium = 22.0, potassium = 105.0, phosphorus = 45.0, selenium = 3.0, copper = 0.1, manganese = 0.3, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.6, vitaminB6 = 0.05)),
        food("Avial", FoodCategory.SouthIndian, 150.0, "1 serving (150 g)",
            Nutrients(calories = 110.0, carbs = 9.0, protein = 2.5, fat = 7.5, saturatedFat = 6.0, fiber = 3.2,
                sodium = 280.0, calcium = 32.0, iron = 1.0, magnesium = 24.0, potassium = 280.0, vitaminA = 220.0, vitaminC = 22.0, phosphorus = 55.0, selenium = 2.0, copper = 0.18, manganese = 0.35, vitaminB1 = 0.07, vitaminB2 = 0.05, vitaminB3 = 0.7, vitaminB6 = 0.14)),
        food("Kerala Parotta", FoodCategory.SouthIndian, 90.0, "1 parotta (90 g)",
            Nutrients(calories = 320.0, carbs = 45.0, protein = 7.0, fat = 12.0, saturatedFat = 5.5, fiber = 2.0,
                sodium = 380.0, calcium = 25.0, iron = 2.0, magnesium = 22.0, potassium = 95.0, phosphorus = 75.0, selenium = 18.0, copper = 0.13, manganese = 0.45, vitaminB1 = 0.12, vitaminB2 = 0.05, vitaminB3 = 1.5, vitaminB6 = 0.06)),
        food("Bisi Bele Bath", FoodCategory.SouthIndian, 250.0, "1 plate (250 g)",
            Nutrients(calories = 165.0, carbs = 26.0, protein = 5.5, fat = 4.5, fiber = 2.4, sodium = 320.0,
                calcium = 24.0, iron = 1.4, magnesium = 30.0, potassium = 210.0, vitaminA = 50.0, folate = 28.0, phosphorus = 75.0, selenium = 4.0, copper = 0.2, manganese = 0.45, vitaminB1 = 0.09, vitaminB2 = 0.04, vitaminB3 = 0.9, vitaminB6 = 0.12)),
        food("Coconut Rice", FoodCategory.SouthIndian, 200.0, "1 plate (200 g)",
            Nutrients(calories = 210.0, carbs = 30.0, protein = 3.5, fat = 9.0, saturatedFat = 7.0, fiber = 1.5,
                sodium = 200.0, calcium = 16.0, iron = 0.9, magnesium = 24.0, potassium = 130.0, phosphorus = 60.0, selenium = 4.5, copper = 0.18, manganese = 0.55, vitaminB1 = 0.05, vitaminB2 = 0.02, vitaminB3 = 0.8, vitaminB6 = 0.06)),
        food("Sambar Vada", FoodCategory.SouthIndian, 220.0, "1 plate (220 g)",
            Nutrients(calories = 175.0, carbs = 20.0, protein = 6.0, fat = 8.0, fiber = 3.0, sodium = 360.0,
                calcium = 30.0, iron = 1.5, magnesium = 36.0, potassium = 260.0, folate = 42.0, phosphorus = 95.0, selenium = 3.0, copper = 0.26, manganese = 0.5, vitaminB1 = 0.11, vitaminB2 = 0.05, vitaminB3 = 0.9, vitaminB6 = 0.11)),
        food("Filter Coffee", FoodCategory.Beverage, 150.0, "1 tumbler (150 ml)",
            Nutrients(calories = 45.0, carbs = 6.0, protein = 1.8, fat = 1.6, sugar = 6.0, sodium = 25.0, calcium = 60.0,
                potassium = 90.0, phosphorus = 55.0, selenium = 1.5, copper = 0.02, manganese = 0.04, vitaminB1 = 0.03, vitaminB2 = 0.18, vitaminB3 = 0.3, vitaminB6 = 0.04)),

        // South Indian — additions (seed v3): breakfast, lunch/dinner, accompaniments.
        food("Poori", FoodCategory.SouthIndian, 30.0, "1 poori (30 g)",
            Nutrients(calories = 270.0, carbs = 38.0, protein = 5.0, fat = 11.0, saturatedFat = 2.5, fiber = 4.0,
                sodium = 220.0, calcium = 20.0, iron = 2.4, magnesium = 60.0, potassium = 130.0, folate = 20.0, phosphorus = 75.0, selenium = 20.0, copper = 0.14, manganese = 0.5, vitaminB1 = 0.15, vitaminB2 = 0.05, vitaminB3 = 1.8, vitaminB6 = 0.08)),
        food("Set Dosa", FoodCategory.SouthIndian, 50.0, "1 dosa (50 g)",
            Nutrients(calories = 175.0, carbs = 28.0, protein = 5.0, fat = 5.0, fiber = 1.4, sodium = 230.0,
                calcium = 18.0, iron = 1.1, magnesium = 22.0, potassium = 80.0, folate = 16.0, phosphorus = 75.0, selenium = 4.5, copper = 0.12, manganese = 0.38, vitaminB1 = 0.06, vitaminB2 = 0.04, vitaminB3 = 1.0, vitaminB6 = 0.05)),
        food("Onion Dosa", FoodCategory.SouthIndian, 100.0, "1 dosa (100 g)",
            Nutrients(calories = 180.0, carbs = 28.0, protein = 5.0, fat = 5.5, fiber = 2.0, sodium = 260.0,
                calcium = 22.0, iron = 1.3, magnesium = 24.0, potassium = 140.0, vitaminC = 4.0, folate = 18.0, phosphorus = 80.0, selenium = 4.0, copper = 0.13, manganese = 0.36, vitaminB1 = 0.07, vitaminB2 = 0.04, vitaminB3 = 1.1, vitaminB6 = 0.08)),
        food("Rava Dosa", FoodCategory.SouthIndian, 80.0, "1 dosa (80 g)",
            Nutrients(calories = 185.0, carbs = 32.0, protein = 4.0, fat = 5.0, fiber = 1.0, sodium = 280.0,
                calcium = 12.0, iron = 1.4, magnesium = 22.0, potassium = 90.0, folate = 24.0, phosphorus = 70.0, selenium = 8.0, copper = 0.12, manganese = 0.35, vitaminB1 = 0.08, vitaminB2 = 0.04, vitaminB3 = 1.2, vitaminB6 = 0.05)),
        food("Egg Dosa (Muttai Dosa)", FoodCategory.SouthIndian, 120.0, "1 dosa (120 g)",
            Nutrients(calories = 195.0, carbs = 24.0, protein = 7.5, fat = 8.0, saturatedFat = 2.2, fiber = 1.2,
                cholesterol = 95.0, sodium = 260.0, calcium = 22.0, iron = 1.5, magnesium = 22.0, potassium = 120.0,
                vitaminB12 = 0.3, folate = 24.0, phosphorus = 110.0, selenium = 11.0, copper = 0.11, manganese = 0.3, vitaminB1 = 0.07, vitaminB2 = 0.12, vitaminB3 = 0.9, vitaminB6 = 0.07)),
        food("Pesarattu", FoodCategory.SouthIndian, 90.0, "1 dosa (90 g)",
            Nutrients(calories = 165.0, carbs = 23.0, protein = 9.0, fat = 4.0, fiber = 4.0, sodium = 200.0,
                calcium = 30.0, iron = 2.2, magnesium = 60.0, potassium = 320.0, folate = 110.0, phosphorus = 120.0, selenium = 2.5, copper = 0.32, manganese = 0.55, vitaminB1 = 0.18, vitaminB2 = 0.08, vitaminB3 = 1.4, vitaminB6 = 0.1)),
        food("Adai", FoodCategory.SouthIndian, 100.0, "1 adai (100 g)",
            Nutrients(calories = 210.0, carbs = 32.0, protein = 9.0, fat = 4.0, fiber = 5.0, sodium = 260.0,
                calcium = 30.0, iron = 2.5, magnesium = 60.0, potassium = 290.0, folate = 90.0, phosphorus = 140.0, selenium = 3.5, copper = 0.35, manganese = 0.6, vitaminB1 = 0.2, vitaminB2 = 0.08, vitaminB3 = 1.5, vitaminB6 = 0.12)),
        food("Ragi Dosa", FoodCategory.SouthIndian, 80.0, "1 dosa (80 g)",
            Nutrients(calories = 195.0, carbs = 33.0, protein = 6.0, fat = 4.0, fiber = 4.5, sodium = 230.0,
                calcium = 200.0, iron = 3.0, magnesium = 90.0, potassium = 270.0, folate = 30.0, phosphorus = 90.0, selenium = 3.0, copper = 0.2, manganese = 0.85, vitaminB1 = 0.1, vitaminB2 = 0.05, vitaminB3 = 0.9, vitaminB6 = 0.06)),
        food("Bonda", FoodCategory.SouthIndian, 50.0, "1 bonda (50 g)",
            Nutrients(calories = 250.0, carbs = 28.0, protein = 5.0, fat = 13.0, saturatedFat = 3.0, fiber = 2.2,
                sodium = 320.0, calcium = 22.0, iron = 1.5, magnesium = 28.0, potassium = 240.0, phosphorus = 85.0, selenium = 3.5, copper = 0.16, manganese = 0.4, vitaminB1 = 0.09, vitaminB2 = 0.04, vitaminB3 = 1.0, vitaminB6 = 0.12)),
        food("Mysore Bonda", FoodCategory.SouthIndian, 40.0, "1 piece (40 g)",
            Nutrients(calories = 245.0, carbs = 26.0, protein = 5.5, fat = 13.0, fiber = 2.0, sodium = 300.0,
                calcium = 20.0, iron = 1.4, magnesium = 26.0, potassium = 200.0, phosphorus = 95.0, selenium = 3.0, copper = 0.28, manganese = 0.45, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 1.1, vitaminB6 = 0.09)),
        food("Bajji", FoodCategory.SouthIndian, 40.0, "1 piece (40 g)",
            Nutrients(calories = 280.0, carbs = 30.0, protein = 6.0, fat = 15.0, saturatedFat = 3.5, fiber = 3.0,
                sodium = 340.0, calcium = 24.0, iron = 1.8, magnesium = 32.0, potassium = 230.0, vitaminC = 4.0, phosphorus = 90.0, selenium = 3.0, copper = 0.3, manganese = 0.42, vitaminB1 = 0.11, vitaminB2 = 0.05, vitaminB3 = 1.0, vitaminB6 = 0.1)),
        food("Kothu Parotta", FoodCategory.SouthIndian, 200.0, "1 plate (200 g)",
            Nutrients(calories = 280.0, carbs = 36.0, protein = 8.0, fat = 11.0, saturatedFat = 4.5, fiber = 2.4,
                sodium = 460.0, calcium = 32.0, iron = 2.0, magnesium = 24.0, potassium = 180.0, phosphorus = 100.0, selenium = 12.0, copper = 0.13, manganese = 0.3, vitaminB1 = 0.1, vitaminB2 = 0.08, vitaminB3 = 1.4, vitaminB6 = 0.09)),
        food("Veg Kurma", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 110.0, carbs = 9.0, protein = 3.0, fat = 7.0, saturatedFat = 4.0, fiber = 2.5,
                sodium = 320.0, calcium = 26.0, iron = 1.0, magnesium = 22.0, potassium = 260.0,
                vitaminA = 120.0, vitaminC = 12.0, phosphorus = 55.0, selenium = 1.5, copper = 0.12, manganese = 0.25, vitaminB1 = 0.07, vitaminB2 = 0.04, vitaminB3 = 0.7, vitaminB6 = 0.12)),
        food("Vatha Kuzhambu", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 90.0, carbs = 10.0, protein = 2.0, fat = 5.0, fiber = 1.6, sodium = 380.0,
                calcium = 22.0, iron = 1.0, magnesium = 18.0, potassium = 220.0, vitaminC = 4.0, phosphorus = 45.0, selenium = 1.0, copper = 0.13, manganese = 0.3, vitaminB1 = 0.05, vitaminB2 = 0.03, vitaminB3 = 0.6, vitaminB6 = 0.1)),
        food("Mor Kuzhambu", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 85.0, carbs = 6.0, protein = 3.0, fat = 5.0, saturatedFat = 2.5, fiber = 1.0,
                sodium = 280.0, calcium = 80.0, iron = 0.7, magnesium = 18.0, potassium = 180.0, vitaminB12 = 0.2, phosphorus = 60.0, selenium = 2.0, copper = 0.1, manganese = 0.2, vitaminB1 = 0.05, vitaminB2 = 0.08, vitaminB3 = 0.5, vitaminB6 = 0.07)),
        food("Paruppu Usili", FoodCategory.SouthIndian, 120.0, "1 bowl (120 g)",
            Nutrients(calories = 175.0, carbs = 12.0, protein = 9.0, fat = 9.0, fiber = 4.5, sodium = 280.0,
                calcium = 30.0, iron = 2.4, magnesium = 50.0, potassium = 340.0, folate = 80.0, phosphorus = 130.0, selenium = 3.0, copper = 0.32, manganese = 0.55, vitaminB1 = 0.18, vitaminB2 = 0.08, vitaminB3 = 1.3, vitaminB6 = 0.13)),
        food("Cabbage Poriyal", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 80.0, carbs = 7.0, protein = 2.0, fat = 5.0, fiber = 2.8, sodium = 240.0,
                calcium = 40.0, iron = 0.8, magnesium = 14.0, potassium = 200.0, vitaminC = 30.0, folate = 40.0, phosphorus = 35.0, selenium = 0.6, copper = 0.05, manganese = 0.18, vitaminB1 = 0.05, vitaminB2 = 0.04, vitaminB3 = 0.3, vitaminB6 = 0.12)),
        food("Beans Poriyal", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 90.0, carbs = 8.0, protein = 2.5, fat = 5.0, fiber = 3.0, sodium = 230.0,
                calcium = 36.0, iron = 1.0, magnesium = 22.0, potassium = 230.0, vitaminA = 30.0, vitaminC = 8.0, phosphorus = 40.0, selenium = 0.6, copper = 0.07, manganese = 0.22, vitaminB1 = 0.06, vitaminB2 = 0.05, vitaminB3 = 0.5, vitaminB6 = 0.07)),
        food("Carrot Poriyal", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 85.0, carbs = 9.0, protein = 1.5, fat = 5.0, fiber = 2.8, sodium = 240.0,
                calcium = 32.0, iron = 0.6, magnesium = 12.0, potassium = 280.0, vitaminA = 690.0, vitaminC = 5.0, phosphorus = 35.0, selenium = 0.6, copper = 0.05, manganese = 0.15, vitaminB1 = 0.05, vitaminB2 = 0.04, vitaminB3 = 0.6, vitaminB6 = 0.1)),
        food("Mor Kootu", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 95.0, carbs = 8.0, protein = 3.0, fat = 6.0, fiber = 2.5, sodium = 260.0,
                calcium = 60.0, iron = 0.9, magnesium = 24.0, potassium = 240.0, vitaminB12 = 0.2, phosphorus = 70.0, selenium = 2.0, copper = 0.13, manganese = 0.3, vitaminB1 = 0.07, vitaminB2 = 0.07, vitaminB3 = 0.6, vitaminB6 = 0.09)),
        food("Chow Chow Kootu", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 80.0, carbs = 8.0, protein = 3.0, fat = 4.5, fiber = 2.4, sodium = 250.0,
                calcium = 28.0, iron = 1.0, magnesium = 22.0, potassium = 260.0, vitaminC = 10.0, phosphorus = 55.0, selenium = 1.5, copper = 0.11, manganese = 0.28, vitaminB1 = 0.06, vitaminB2 = 0.04, vitaminB3 = 0.5, vitaminB6 = 0.08)),
        food("Sambar Sadam", FoodCategory.SouthIndian, 250.0, "1 plate (250 g)",
            Nutrients(calories = 145.0, carbs = 24.0, protein = 4.5, fat = 3.5, fiber = 2.6, sodium = 360.0,
                calcium = 24.0, iron = 1.4, magnesium = 28.0, potassium = 240.0, vitaminA = 40.0, folate = 30.0, phosphorus = 75.0, selenium = 4.0, copper = 0.16, manganese = 0.4, vitaminB1 = 0.09, vitaminB2 = 0.04, vitaminB3 = 1.0, vitaminB6 = 0.09)),
        food("Tomato Rice", FoodCategory.SouthIndian, 200.0, "1 plate (200 g)",
            Nutrients(calories = 175.0, carbs = 30.0, protein = 3.0, fat = 4.5, fiber = 1.4, sodium = 300.0,
                calcium = 18.0, iron = 1.0, magnesium = 16.0, potassium = 180.0, vitaminA = 60.0, vitaminC = 8.0, phosphorus = 55.0, selenium = 4.5, copper = 0.11, manganese = 0.35, vitaminB1 = 0.07, vitaminB2 = 0.03, vitaminB3 = 1.0, vitaminB6 = 0.1)),
        food("Papad (fried)", FoodCategory.SouthIndian, 10.0, "1 piece (10 g)",
            Nutrients(calories = 380.0, carbs = 47.0, protein = 23.0, fat = 12.0, fiber = 8.0, sodium = 1640.0,
                calcium = 80.0, iron = 4.8, magnesium = 130.0, potassium = 720.0, folate = 90.0, phosphorus = 200.0, selenium = 5.0, copper = 0.45, manganese = 0.7, vitaminB1 = 0.12, vitaminB2 = 0.09, vitaminB3 = 1.5, vitaminB6 = 0.15)),
        food("Mango Pickle", FoodCategory.SouthIndian, 10.0, "1 tbsp (10 g)",
            Nutrients(calories = 180.0, carbs = 12.0, protein = 1.5, fat = 14.0, fiber = 2.4, sodium = 2200.0,
                calcium = 20.0, iron = 0.8, magnesium = 20.0, potassium = 140.0, vitaminA = 90.0, vitaminC = 14.0, phosphorus = 20.0, selenium = 0.5, copper = 0.1, manganese = 0.25, vitaminB1 = 0.03, vitaminB2 = 0.03, vitaminB3 = 0.4, vitaminB6 = 0.08)),
        food("Lime Pickle", FoodCategory.SouthIndian, 10.0, "1 tbsp (10 g)",
            Nutrients(calories = 110.0, carbs = 16.0, protein = 1.0, fat = 4.0, fiber = 2.0, sodium = 2400.0,
                calcium = 22.0, iron = 0.6, magnesium = 14.0, potassium = 120.0, vitaminC = 12.0, phosphorus = 20.0, selenium = 0.6, copper = 0.18, manganese = 0.45, vitaminB1 = 0.03, vitaminB2 = 0.04, vitaminB3 = 0.5, vitaminB6 = 0.08)),

        // South Indian — household additions: festival items, regional staples
        // (Karnataka / Kerala / Andhra-Telangana), tiffin sweets, and the
        // everyday vegetable curries that round out a home-cooked meal.
        food("Sweet Pongal (Sakkarai Pongal)", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 270.0, carbs = 45.0, sugar = 20.0, protein = 5.0, fat = 7.5, saturatedFat = 4.0,
                fiber = 1.6, sodium = 60.0, calcium = 35.0, iron = 1.4, magnesium = 28.0, potassium = 130.0,
                vitaminA = 60.0, phosphorus = 70.0, selenium = 3.5, copper = 0.15, manganese = 0.4, vitaminB1 = 0.08, vitaminB2 = 0.05, vitaminB3 = 0.8, vitaminB6 = 0.07)),
        food("Khara Bhath", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 185.0, carbs = 28.0, protein = 5.0, fat = 6.0, fiber = 2.4, sodium = 320.0,
                calcium = 24.0, iron = 1.4, magnesium = 22.0, potassium = 180.0,
                vitaminA = 80.0, vitaminC = 6.0, phosphorus = 55.0, selenium = 8.0, copper = 0.12, manganese = 0.35, vitaminB1 = 0.12, vitaminB2 = 0.05, vitaminB3 = 1.2, vitaminB6 = 0.08)),
        food("Rava Kesari", FoodCategory.SouthIndian, 100.0, "1 piece (100 g)",
            Nutrients(calories = 330.0, carbs = 55.0, sugar = 30.0, protein = 4.0, fat = 11.0, saturatedFat = 6.0,
                fiber = 0.8, sodium = 40.0, calcium = 14.0, iron = 1.0, magnesium = 18.0, potassium = 70.0,
                vitaminA = 50.0, phosphorus = 35.0, selenium = 6.0, copper = 0.08, manganese = 0.25, vitaminB1 = 0.09, vitaminB2 = 0.04, vitaminB3 = 0.9, vitaminB6 = 0.04)),
        food("Neer Dosa", FoodCategory.SouthIndian, 60.0, "1 dosa (60 g)",
            Nutrients(calories = 145.0, carbs = 26.0, protein = 3.0, fat = 3.5, saturatedFat = 2.0, fiber = 1.0,
                sodium = 90.0, calcium = 10.0, iron = 0.8, magnesium = 16.0, potassium = 80.0, phosphorus = 50.0, selenium = 4.0, copper = 0.1, manganese = 0.4, vitaminB1 = 0.06, vitaminB2 = 0.03, vitaminB3 = 0.9, vitaminB6 = 0.06)),
        food("Akki Roti", FoodCategory.SouthIndian, 80.0, "1 roti (80 g)",
            Nutrients(calories = 225.0, carbs = 40.0, protein = 4.0, fat = 5.5, fiber = 1.6, sodium = 280.0,
                calcium = 14.0, iron = 1.2, magnesium = 18.0, potassium = 140.0, vitaminC = 4.0, phosphorus = 75.0, selenium = 6.0, copper = 0.14, manganese = 0.6, vitaminB1 = 0.09, vitaminB2 = 0.04, vitaminB3 = 1.4, vitaminB6 = 0.1)),
        food("Ragi Mudde (Ragi Ball)", FoodCategory.SouthIndian, 150.0, "1 ball (150 g)",
            Nutrients(calories = 155.0, carbs = 32.0, protein = 4.0, fat = 1.0, fiber = 4.0, sodium = 5.0,
                calcium = 320.0, iron = 3.5, magnesium = 130.0, potassium = 350.0, folate = 28.0, phosphorus = 110.0, selenium = 2.5, copper = 0.35, manganese = 2.2, vitaminB1 = 0.15, vitaminB2 = 0.08, vitaminB3 = 0.8, vitaminB6 = 0.1)),
        food("Ragi Roti", FoodCategory.SouthIndian, 80.0, "1 roti (80 g)",
            Nutrients(calories = 210.0, carbs = 38.0, protein = 5.0, fat = 4.0, fiber = 4.5, sodium = 280.0,
                calcium = 250.0, iron = 3.2, magnesium = 110.0, potassium = 320.0, folate = 30.0, phosphorus = 130.0, selenium = 2.5, copper = 0.4, manganese = 2.6, vitaminB1 = 0.18, vitaminB2 = 0.1, vitaminB3 = 1.0, vitaminB6 = 0.12)),
        food("Kuzhi Paniyaram (Paddu)", FoodCategory.SouthIndian, 100.0, "5 pieces (100 g)",
            Nutrients(calories = 170.0, carbs = 28.0, protein = 5.0, fat = 5.0, fiber = 2.0, sodium = 260.0,
                calcium = 22.0, iron = 1.4, magnesium = 26.0, potassium = 130.0, folate = 22.0, phosphorus = 90.0, selenium = 5.0, copper = 0.18, manganese = 0.5, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 1.3, vitaminB6 = 0.09)),
        food("Rava Idli", FoodCategory.SouthIndian, 50.0, "1 idli (50 g)",
            Nutrients(calories = 155.0, carbs = 25.0, protein = 5.0, fat = 3.5, fiber = 1.4, sodium = 280.0,
                calcium = 18.0, iron = 1.2, magnesium = 22.0, potassium = 90.0, folate = 26.0, phosphorus = 60.0, selenium = 8.0, copper = 0.12, manganese = 0.35, vitaminB1 = 0.1, vitaminB2 = 0.05, vitaminB3 = 1.2, vitaminB6 = 0.07)),
        food("Masala Vada (Paruppu Vadai)", FoodCategory.SouthIndian, 35.0, "1 vada (35 g)",
            Nutrients(calories = 280.0, carbs = 28.0, protein = 11.0, fat = 14.0, saturatedFat = 3.0, fiber = 5.0,
                sodium = 360.0, calcium = 40.0, iron = 2.5, magnesium = 60.0, potassium = 380.0, folate = 110.0, phosphorus = 160.0, selenium = 4.0, copper = 0.45, manganese = 0.7, vitaminB1 = 0.25, vitaminB2 = 0.1, vitaminB3 = 1.5, vitaminB6 = 0.15)),
        food("Semiya Payasam (Vermicelli Kheer)", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 165.0, carbs = 25.0, sugar = 15.0, protein = 4.0, fat = 5.0, saturatedFat = 3.0,
                sodium = 40.0, calcium = 90.0, iron = 0.4, potassium = 110.0, vitaminB12 = 0.3, vitaminA = 60.0, phosphorus = 75.0, selenium = 5.0, copper = 0.08, manganese = 0.2, vitaminB1 = 0.08, vitaminB2 = 0.15, vitaminB3 = 0.7, vitaminB6 = 0.05)),
        food("Paal Payasam (Rice Kheer)", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 135.0, carbs = 21.0, sugar = 14.0, protein = 4.0, fat = 4.0, saturatedFat = 2.4,
                sodium = 45.0, calcium = 110.0, iron = 0.3, potassium = 130.0, vitaminB12 = 0.3, phosphorus = 85.0, selenium = 3.0, copper = 0.06, manganese = 0.2, vitaminB1 = 0.06, vitaminB2 = 0.18, vitaminB3 = 0.5, vitaminB6 = 0.05)),
        food("Sundal (Channa)", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 155.0, carbs = 22.0, protein = 8.0, fat = 4.0, fiber = 6.5, sodium = 280.0,
                calcium = 48.0, iron = 2.5, magnesium = 50.0, potassium = 320.0, folate = 95.0, phosphorus = 120.0, selenium = 3.5, copper = 0.35, manganese = 0.6, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 0.6, vitaminB6 = 0.12)),
        food("Thattai", FoodCategory.SouthIndian, 15.0, "1 piece (15 g)",
            Nutrients(calories = 510.0, carbs = 60.0, protein = 10.0, fat = 26.0, saturatedFat = 5.0, fiber = 4.0,
                sodium = 520.0, calcium = 30.0, iron = 3.0, magnesium = 50.0, potassium = 220.0, phosphorus = 140.0, selenium = 6.0, copper = 0.3, manganese = 0.8, vitaminB1 = 0.2, vitaminB2 = 0.07, vitaminB3 = 1.8, vitaminB6 = 0.12)),
        food("Mixture (South Indian)", FoodCategory.SouthIndian, 30.0, "1 small bowl (30 g)",
            Nutrients(calories = 540.0, carbs = 52.0, protein = 13.0, fat = 30.0, saturatedFat = 6.0, fiber = 5.0,
                sodium = 700.0, calcium = 40.0, iron = 3.5, magnesium = 70.0, potassium = 320.0, phosphorus = 180.0, selenium = 7.0, copper = 0.4, manganese = 1.0, vitaminB1 = 0.25, vitaminB2 = 0.1, vitaminB3 = 2.5, vitaminB6 = 0.18)),
        food("Idli Podi (Milagai Podi)", FoodCategory.SouthIndian, 10.0, "1 tbsp (10 g)",
            Nutrients(calories = 440.0, carbs = 38.0, protein = 18.0, fat = 22.0, fiber = 9.0, sodium = 1200.0,
                calcium = 80.0, iron = 6.0, magnesium = 150.0, potassium = 720.0, folate = 80.0, phosphorus = 280.0, selenium = 8.0, copper = 0.8, manganese = 1.5, vitaminB1 = 0.35, vitaminB2 = 0.15, vitaminB3 = 3.5, vitaminB6 = 0.3)),
        food("Vazhakkai Curry (Raw Banana)", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 95.0, carbs = 18.0, protein = 1.5, fat = 2.5, fiber = 2.8, sodium = 230.0,
                calcium = 16.0, iron = 0.7, magnesium = 28.0, potassium = 480.0, vitaminC = 14.0, phosphorus = 35.0, selenium = 1.0, copper = 0.1, manganese = 0.3, vitaminB1 = 0.05, vitaminB2 = 0.04, vitaminB3 = 0.7, vitaminB6 = 0.25)),
        food("Keerai Masiyal", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 55.0, carbs = 6.0, protein = 3.5, fat = 2.0, fiber = 2.2, sodium = 220.0,
                calcium = 110.0, iron = 2.8, magnesium = 60.0, potassium = 380.0,
                vitaminA = 380.0, vitaminC = 18.0, vitaminK = 380.0, folate = 130.0, phosphorus = 45.0, selenium = 0.9, copper = 0.13, manganese = 0.6, vitaminB1 = 0.06, vitaminB2 = 0.12, vitaminB3 = 0.6, vitaminB6 = 0.15)),
        food("Beetroot Poriyal", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 75.0, carbs = 9.0, sugar = 5.0, protein = 1.5, fat = 3.5, fiber = 2.6, sodium = 240.0,
                calcium = 22.0, iron = 0.8, magnesium = 22.0, potassium = 280.0, folate = 90.0, phosphorus = 40.0, selenium = 0.7, copper = 0.08, manganese = 0.35, vitaminB1 = 0.03, vitaminB2 = 0.04, vitaminB3 = 0.4, vitaminB6 = 0.07)),
        food("Kathirikai Curry (Brinjal)", FoodCategory.SouthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 110.0, carbs = 9.0, protein = 1.5, fat = 7.0, fiber = 3.0, sodium = 280.0,
                calcium = 16.0, iron = 0.6, magnesium = 16.0, potassium = 240.0, vitaminC = 3.0, phosphorus = 30.0, selenium = 0.4, copper = 0.1, manganese = 0.25, vitaminB1 = 0.04, vitaminB2 = 0.04, vitaminB3 = 0.6, vitaminB6 = 0.09)),
        food("Snake Gourd Poriyal (Pudalankai)", FoodCategory.SouthIndian, 100.0, "1 bowl (100 g)",
            Nutrients(calories = 60.0, carbs = 6.0, protein = 1.5, fat = 3.0, fiber = 1.8, sodium = 220.0,
                calcium = 22.0, iron = 0.5, magnesium = 16.0, potassium = 200.0, vitaminC = 6.0, phosphorus = 25.0, selenium = 0.5, copper = 0.05, manganese = 0.15, vitaminB1 = 0.04, vitaminB2 = 0.04, vitaminB3 = 0.4, vitaminB6 = 0.06)),
        food("Vegetable Stew (Kerala)", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 110.0, carbs = 10.0, protein = 2.0, fat = 7.0, saturatedFat = 5.5, fiber = 2.5,
                sodium = 260.0, calcium = 28.0, iron = 0.9, magnesium = 22.0, potassium = 320.0,
                vitaminA = 100.0, vitaminC = 14.0, phosphorus = 45.0, selenium = 0.8, copper = 0.12, manganese = 0.3, vitaminB1 = 0.06, vitaminB2 = 0.04, vitaminB3 = 0.7, vitaminB6 = 0.12)),
        food("Kadala Curry (Kerala Black Chickpea)", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 140.0, carbs = 16.0, protein = 6.0, fat = 6.0, saturatedFat = 4.0, fiber = 5.0,
                sodium = 320.0, calcium = 48.0, iron = 2.8, magnesium = 48.0, potassium = 320.0, folate = 110.0, phosphorus = 115.0, selenium = 3.0, copper = 0.32, manganese = 0.8, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 0.7, vitaminB6 = 0.13)),
        food("Mint Chutney (Pudina)", FoodCategory.SouthIndian, 30.0, "2 tbsp (30 g)",
            Nutrients(calories = 50.0, carbs = 6.0, protein = 2.0, fat = 2.0, fiber = 2.0, sodium = 240.0,
                calcium = 70.0, iron = 2.0, potassium = 220.0, vitaminA = 200.0, vitaminC = 8.0, phosphorus = 60.0, selenium = 1.0, copper = 0.25, manganese = 0.9, vitaminB1 = 0.06, vitaminB2 = 0.18, vitaminB3 = 1.0, vitaminB6 = 0.18)),
        food("Onion Chutney", FoodCategory.SouthIndian, 30.0, "2 tbsp (30 g)",
            Nutrients(calories = 85.0, carbs = 10.0, protein = 1.5, fat = 4.5, fiber = 1.6, sodium = 280.0,
                calcium = 14.0, iron = 0.5, potassium = 160.0, vitaminC = 6.0, phosphorus = 45.0, selenium = 0.6, copper = 0.12, manganese = 0.25, vitaminB1 = 0.05, vitaminB2 = 0.04, vitaminB3 = 0.6, vitaminB6 = 0.15)),
        food("Peanut Chutney", FoodCategory.SouthIndian, 30.0, "2 tbsp (30 g)",
            Nutrients(calories = 360.0, carbs = 14.0, protein = 13.0, fat = 28.0, saturatedFat = 4.0, fiber = 6.0,
                sodium = 320.0, calcium = 40.0, iron = 2.0, magnesium = 130.0, potassium = 480.0, folate = 80.0, phosphorus = 160.0, selenium = 3.0, copper = 0.4, manganese = 0.9, vitaminB1 = 0.25, vitaminB2 = 0.08, vitaminB3 = 6.0, vitaminB6 = 0.18)),
        food("Drumstick Sambar", FoodCategory.SouthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 75.0, carbs = 9.0, protein = 4.0, fat = 2.4, fiber = 3.5, sodium = 360.0,
                calcium = 38.0, iron = 1.5, magnesium = 36.0, potassium = 340.0,
                vitaminA = 80.0, vitaminC = 60.0, folate = 50.0, phosphorus = 70.0, selenium = 2.0, copper = 0.15, manganese = 0.3, vitaminB1 = 0.1, vitaminB2 = 0.05, vitaminB3 = 0.8, vitaminB6 = 0.12)),

        // ---------- NORTH INDIAN ----------
        food("Chapati / Roti", FoodCategory.NorthIndian, 40.0, "1 chapati (40 g)",
            Nutrients(calories = 290.0, carbs = 55.0, protein = 10.0, fat = 3.5, fiber = 7.0, sodium = 250.0,
                calcium = 24.0, iron = 3.5, magnesium = 80.0, potassium = 290.0, folate = 30.0, phosphorus = 160.0, selenium = 30.0, copper = 0.25, manganese = 1.2, vitaminB1 = 0.25, vitaminB2 = 0.1, vitaminB3 = 2.5, vitaminB6 = 0.15)),
        food("Naan", FoodCategory.NorthIndian, 90.0, "1 naan (90 g)",
            Nutrients(calories = 305.0, carbs = 50.0, protein = 9.0, fat = 7.5, fiber = 2.5, sodium = 460.0,
                calcium = 60.0, iron = 3.0, magnesium = 25.0, potassium = 120.0, phosphorus = 110.0, selenium = 20.0, copper = 0.15, manganese = 0.6, vitaminB1 = 0.3, vitaminB2 = 0.2, vitaminB3 = 2.8, vitaminB6 = 0.05)),
        food("Aloo Paratha", FoodCategory.NorthIndian, 120.0, "1 paratha (120 g)",
            Nutrients(calories = 260.0, carbs = 36.0, protein = 6.0, fat = 9.5, fiber = 3.5, sodium = 320.0,
                calcium = 22.0, iron = 2.0, magnesium = 30.0, potassium = 280.0, vitaminC = 8.0, phosphorus = 110.0, selenium = 18.0, copper = 0.18, manganese = 0.7, vitaminB1 = 0.18, vitaminB2 = 0.08, vitaminB3 = 1.8, vitaminB6 = 0.2)),
        food("Plain Paratha", FoodCategory.NorthIndian, 80.0, "1 paratha (80 g)",
            Nutrients(calories = 310.0, carbs = 45.0, protein = 7.5, fat = 11.0, fiber = 4.0, sodium = 300.0,
                calcium = 18.0, iron = 2.4, magnesium = 32.0, potassium = 130.0, phosphorus = 120.0, selenium = 22.0, copper = 0.18, manganese = 0.85, vitaminB1 = 0.2, vitaminB2 = 0.08, vitaminB3 = 2.0, vitaminB6 = 0.1)),
        food("Dal Tadka", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 90.0, carbs = 11.0, protein = 5.0, fat = 2.5, fiber = 3.0, sodium = 330.0,
                calcium = 25.0, iron = 2.0, magnesium = 35.0, potassium = 270.0, folate = 60.0, phosphorus = 90.0, selenium = 2.0, copper = 0.2, manganese = 0.35, vitaminB1 = 0.12, vitaminB2 = 0.05, vitaminB3 = 0.9, vitaminB6 = 0.1)),
        food("Dal Makhani", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 160.0, carbs = 14.0, protein = 7.0, fat = 8.0, saturatedFat = 4.5, fiber = 4.0,
                sodium = 380.0, calcium = 55.0, iron = 2.4, magnesium = 38.0, potassium = 380.0, folate = 65.0, phosphorus = 120.0, selenium = 2.5, copper = 0.3, manganese = 0.4, vitaminB1 = 0.15, vitaminB2 = 0.1, vitaminB3 = 1.0, vitaminB6 = 0.12)),
        food("Rajma", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 110.0, carbs = 16.0, protein = 6.0, fat = 2.5, fiber = 5.0, sodium = 330.0,
                calcium = 42.0, iron = 2.8, magnesium = 50.0, potassium = 460.0, folate = 80.0, phosphorus = 120.0, selenium = 1.5, copper = 0.3, manganese = 0.45, vitaminB1 = 0.15, vitaminB2 = 0.06, vitaminB3 = 0.7, vitaminB6 = 0.15)),
        food("Chole / Chana Masala", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 125.0, carbs = 17.0, protein = 7.0, fat = 3.0, fiber = 5.0, sodium = 360.0,
                calcium = 50.0, iron = 2.5, magnesium = 48.0, potassium = 350.0, folate = 70.0, phosphorus = 120.0, selenium = 3.0, copper = 0.35, manganese = 0.7, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 0.9, vitaminB6 = 0.25)),
        food("Paneer Butter Masala", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 250.0, carbs = 9.0, protein = 11.0, fat = 19.0, saturatedFat = 9.0, fiber = 1.8,
                sodium = 420.0, calcium = 220.0, iron = 1.2, magnesium = 28.0, potassium = 220.0,
                vitaminA = 110.0, vitaminB12 = 0.8, phosphorus = 130.0, selenium = 6.0, copper = 0.12, manganese = 0.2, vitaminB1 = 0.05, vitaminB2 = 0.15, vitaminB3 = 0.6, vitaminB6 = 0.1)),
        food("Palak Paneer", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 165.0, carbs = 7.0, protein = 9.0, fat = 12.0, fiber = 2.4,
                sodium = 380.0, calcium = 220.0, iron = 2.6, magnesium = 60.0, potassium = 360.0,
                vitaminA = 480.0, vitaminC = 16.0, vitaminK = 280.0, folate = 110.0, phosphorus = 120.0, selenium = 4.0, copper = 0.15, manganese = 0.45, vitaminB1 = 0.06, vitaminB2 = 0.18, vitaminB3 = 0.7, vitaminB6 = 0.15)),
        food("Butter Chicken", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 220.0, carbs = 6.0, protein = 14.0, fat = 16.0, saturatedFat = 7.5, fiber = 1.0,
                sodium = 410.0, calcium = 70.0, iron = 1.2, magnesium = 20.0, potassium = 280.0, vitaminB12 = 0.6, phosphorus = 140.0, selenium = 14.0, copper = 0.08, manganese = 0.12, vitaminB1 = 0.08, vitaminB2 = 0.18, vitaminB3 = 5.0, vitaminB6 = 0.3)),
        food("Tandoori Chicken", FoodCategory.NorthIndian, 100.0, "1 piece (100 g)",
            Nutrients(calories = 175.0, carbs = 3.0, protein = 26.0, fat = 6.5, fiber = 0.4, sodium = 420.0,
                calcium = 40.0, iron = 1.1, magnesium = 26.0, potassium = 260.0, vitaminB12 = 0.8, phosphorus = 180.0, selenium = 22.0, copper = 0.06, manganese = 0.05, vitaminB1 = 0.09, vitaminB2 = 0.2, vitaminB3 = 7.0, vitaminB6 = 0.4)),
        food("Veg Biryani", FoodCategory.NorthIndian, 250.0, "1 plate (250 g)",
            Nutrients(calories = 195.0, carbs = 30.0, protein = 5.0, fat = 6.5, fiber = 1.8, sodium = 400.0,
                calcium = 26.0, iron = 1.4, magnesium = 22.0, potassium = 200.0, vitaminA = 90.0, phosphorus = 75.0, selenium = 6.0, copper = 0.12, manganese = 0.4, vitaminB1 = 0.1, vitaminB2 = 0.04, vitaminB3 = 1.5, vitaminB6 = 0.12)),
        food("Chicken Biryani", FoodCategory.NorthIndian, 300.0, "1 plate (300 g)",
            Nutrients(calories = 230.0, carbs = 28.0, protein = 11.0, fat = 8.0, fiber = 1.5, sodium = 480.0,
                calcium = 28.0, iron = 1.6, magnesium = 24.0, potassium = 250.0, vitaminB12 = 0.4, phosphorus = 110.0, selenium = 12.0, copper = 0.1, manganese = 0.35, vitaminB1 = 0.1, vitaminB2 = 0.08, vitaminB3 = 3.5, vitaminB6 = 0.2)),
        food("Aloo Gobi", FoodCategory.NorthIndian, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 95.0, carbs = 12.0, protein = 3.0, fat = 4.5, fiber = 3.2, sodium = 280.0,
                calcium = 24.0, iron = 1.0, magnesium = 20.0, potassium = 320.0, vitaminC = 32.0, folate = 50.0, phosphorus = 55.0, selenium = 1.0, copper = 0.12, manganese = 0.2, vitaminB1 = 0.08, vitaminB2 = 0.05, vitaminB3 = 0.8, vitaminB6 = 0.2)),
        food("Bhindi Masala", FoodCategory.NorthIndian, 150.0, "1 bowl (150 g)",
            Nutrients(calories = 90.0, carbs = 8.0, protein = 2.5, fat = 5.5, fiber = 3.5, sodium = 300.0,
                calcium = 80.0, iron = 1.0, magnesium = 50.0, potassium = 290.0, vitaminC = 22.0, folate = 60.0, phosphorus = 55.0, selenium = 0.7, copper = 0.1, manganese = 0.4, vitaminB1 = 0.1, vitaminB2 = 0.06, vitaminB3 = 0.8, vitaminB6 = 0.15)),

        // ---------- SNACKS ----------
        food("Samosa", FoodCategory.Snack, 60.0, "1 samosa (60 g)",
            Nutrients(calories = 308.0, carbs = 32.0, protein = 5.0, fat = 18.0, saturatedFat = 4.0, fiber = 2.5,
                sodium = 420.0, calcium = 20.0, iron = 1.6, magnesium = 24.0, potassium = 250.0, phosphorus = 80.0, selenium = 8.0, copper = 0.15, manganese = 0.4, vitaminB1 = 0.15, vitaminB2 = 0.06, vitaminB3 = 1.8, vitaminB6 = 0.18)),
        food("Pakora", FoodCategory.Snack, 50.0, "1 serving (50 g)",
            Nutrients(calories = 315.0, carbs = 28.0, protein = 8.0, fat = 20.0, fiber = 4.0, sodium = 400.0,
                calcium = 30.0, iron = 2.5, magnesium = 60.0, potassium = 320.0, folate = 80.0, phosphorus = 110.0, selenium = 4.0, copper = 0.3, manganese = 0.5, vitaminB1 = 0.12, vitaminB2 = 0.07, vitaminB3 = 0.9, vitaminB6 = 0.2)),
        food("Bhel Puri", FoodCategory.Snack, 100.0, "1 plate (100 g)",
            Nutrients(calories = 195.0, carbs = 35.0, protein = 4.5, fat = 5.0, fiber = 3.0, sodium = 540.0,
                calcium = 22.0, iron = 1.6, magnesium = 30.0, potassium = 280.0, vitaminC = 8.0, phosphorus = 90.0, selenium = 6.0, copper = 0.18, manganese = 0.5, vitaminB1 = 0.12, vitaminB2 = 0.05, vitaminB3 = 1.5, vitaminB6 = 0.15)),
        food("Pani Puri", FoodCategory.Snack, 80.0, "6 pieces (80 g)",
            Nutrients(calories = 175.0, carbs = 28.0, protein = 4.0, fat = 5.5, fiber = 2.5, sodium = 480.0,
                calcium = 18.0, iron = 1.5, magnesium = 25.0, potassium = 220.0, phosphorus = 70.0, selenium = 7.0, copper = 0.15, manganese = 0.45, vitaminB1 = 0.1, vitaminB2 = 0.04, vitaminB3 = 1.3, vitaminB6 = 0.12)),
        food("Vada Pav", FoodCategory.Snack, 120.0, "1 piece (120 g)",
            Nutrients(calories = 290.0, carbs = 38.0, protein = 7.0, fat = 13.0, fiber = 3.0, sodium = 520.0,
                calcium = 50.0, iron = 2.5, magnesium = 30.0, potassium = 260.0, phosphorus = 105.0, selenium = 12.0, copper = 0.18, manganese = 0.45, vitaminB1 = 0.18, vitaminB2 = 0.08, vitaminB3 = 1.8, vitaminB6 = 0.12)),
        food("Pav Bhaji", FoodCategory.Snack, 300.0, "1 plate (300 g)",
            Nutrients(calories = 160.0, carbs = 22.0, protein = 4.5, fat = 6.5, saturatedFat = 3.0, fiber = 3.5,
                sodium = 460.0, calcium = 38.0, iron = 1.6, magnesium = 28.0, potassium = 380.0,
                vitaminA = 150.0, vitaminC = 28.0, phosphorus = 75.0, selenium = 6.0, copper = 0.14, manganese = 0.25, vitaminB1 = 0.12, vitaminB2 = 0.07, vitaminB3 = 1.2, vitaminB6 = 0.15)),
        food("Dhokla", FoodCategory.Snack, 100.0, "1 plate (100 g)",
            Nutrients(calories = 160.0, carbs = 24.0, protein = 6.0, fat = 4.5, fiber = 2.5, sodium = 360.0,
                calcium = 30.0, iron = 1.5, magnesium = 32.0, potassium = 180.0, folate = 35.0, phosphorus = 110.0, selenium = 8.0, copper = 0.22, manganese = 0.5, vitaminB1 = 0.15, vitaminB2 = 0.08, vitaminB3 = 1.0, vitaminB6 = 0.1)),
        food("Poha", FoodCategory.Snack, 200.0, "1 plate (200 g)",
            Nutrients(calories = 175.0, carbs = 32.0, protein = 3.5, fat = 4.0, fiber = 1.8, sodium = 280.0,
                calcium = 14.0, iron = 1.5, magnesium = 18.0, potassium = 110.0, phosphorus = 75.0, selenium = 7.0, copper = 0.15, manganese = 0.6, vitaminB1 = 0.2, vitaminB2 = 0.04, vitaminB3 = 1.5, vitaminB6 = 0.1)),
        food("Murukku", FoodCategory.Snack, 30.0, "1 small bowl (30 g)",
            Nutrients(calories = 520.0, carbs = 56.0, protein = 9.0, fat = 28.0, fiber = 3.0, sodium = 480.0,
                calcium = 25.0, iron = 3.0, magnesium = 40.0, potassium = 180.0, phosphorus = 130.0, selenium = 6.0, copper = 0.25, manganese = 0.7, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 1.4, vitaminB6 = 0.12)),
        food("Banana Chips", FoodCategory.Snack, 30.0, "1 small bowl (30 g)",
            Nutrients(calories = 520.0, carbs = 58.0, protein = 2.0, fat = 32.0, saturatedFat = 28.0, fiber = 7.5,
                sodium = 360.0, calcium = 18.0, iron = 1.2, magnesium = 75.0, potassium = 510.0, phosphorus = 40.0, selenium = 1.5, copper = 0.15, manganese = 0.3, vitaminB1 = 0.05, vitaminB2 = 0.05, vitaminB3 = 0.7, vitaminB6 = 0.3)),
        food("Khakhra", FoodCategory.Snack, 25.0, "1 piece (25 g)",
            Nutrients(calories = 360.0, carbs = 62.0, protein = 11.0, fat = 7.0, fiber = 5.0, sodium = 480.0,
                calcium = 30.0, iron = 3.5, magnesium = 60.0, potassium = 240.0, phosphorus = 140.0, selenium = 30.0, copper = 0.28, manganese = 1.2, vitaminB1 = 0.25, vitaminB2 = 0.08, vitaminB3 = 2.5, vitaminB6 = 0.12)),

        // ---------- BEVERAGES ----------
        food("Masala Chai", FoodCategory.Beverage, 150.0, "1 cup (150 ml)",
            Nutrients(calories = 60.0, carbs = 8.0, protein = 2.0, fat = 2.4, sugar = 7.5, sodium = 30.0,
                calcium = 70.0, potassium = 110.0, phosphorus = 50.0, selenium = 1.0, copper = 0.01, manganese = 0.05, vitaminB1 = 0.02, vitaminB2 = 0.09, vitaminB3 = 0.1, vitaminB6 = 0.02)),
        food("Lassi (sweet)", FoodCategory.Beverage, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 110.0, carbs = 14.0, protein = 3.0, fat = 4.5, saturatedFat = 2.5, sugar = 13.0,
                sodium = 50.0, calcium = 120.0, potassium = 170.0, vitaminB12 = 0.3, phosphorus = 90.0, selenium = 2.5, copper = 0.01, manganese = 0.01, vitaminB1 = 0.03, vitaminB2 = 0.15, vitaminB3 = 0.1, vitaminB6 = 0.03)),
        food("Buttermilk (Chaas)", FoodCategory.Beverage, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 35.0, carbs = 4.0, protein = 2.0, fat = 1.5, sugar = 4.0, sodium = 220.0,
                calcium = 80.0, potassium = 130.0, vitaminB12 = 0.2, phosphorus = 75.0, selenium = 2.0, copper = 0.01, manganese = 0.0, vitaminB1 = 0.03, vitaminB2 = 0.13, vitaminB3 = 0.1, vitaminB6 = 0.03)),
        food("Coconut Water", FoodCategory.Beverage, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 19.0, carbs = 3.7, sugar = 2.6, protein = 0.7, fat = 0.2, sodium = 105.0,
                calcium = 24.0, magnesium = 25.0, potassium = 250.0, vitaminC = 2.4, phosphorus = 20.0, selenium = 1.0, copper = 0.04, manganese = 0.14, vitaminB1 = 0.03, vitaminB2 = 0.06, vitaminB3 = 0.08, vitaminB6 = 0.03)),
        food("Nimbu Pani", FoodCategory.Beverage, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 30.0, carbs = 8.0, sugar = 7.0, sodium = 180.0, vitaminC = 12.0, phosphorus = 3.0, selenium = 0.1, copper = 0.01, manganese = 0.01, vitaminB1 = 0.01, vitaminB2 = 0.0, vitaminB3 = 0.05, vitaminB6 = 0.02)),
        food("Black Tea (no milk, no sugar)", FoodCategory.Beverage, 240.0, "1 cup (240 ml)",
            Nutrients(calories = 1.0, carbs = 0.3, protein = 0.0, fat = 0.0, sodium = 3.0, potassium = 37.0, phosphorus = 1.0, selenium = 0.1, copper = 0.01, manganese = 0.22, vitaminB1 = 0.0, vitaminB2 = 0.01, vitaminB3 = 0.0, vitaminB6 = 0.0)),
        food("Tea (milk, sugar)", FoodCategory.Beverage, 150.0, "1 cup (150 ml)",
            Nutrients(calories = 55.0, carbs = 7.5, sugar = 7.0, protein = 1.6, fat = 2.0, sodium = 25.0,
                calcium = 62.0, potassium = 90.0, phosphorus = 35.0, selenium = 1.0, copper = 0.01, manganese = 0.08, vitaminB1 = 0.02, vitaminB2 = 0.07, vitaminB3 = 0.1, vitaminB6 = 0.02)),
        food("Ginger Tea", FoodCategory.Beverage, 240.0, "1 cup (240 ml)",
            Nutrients(calories = 8.0, carbs = 2.0, sugar = 1.5, sodium = 3.0, potassium = 40.0, phosphorus = 4.0, selenium = 0.1, copper = 0.01, manganese = 0.15, vitaminB1 = 0.01, vitaminB2 = 0.01, vitaminB3 = 0.1, vitaminB6 = 0.01)),
        food("Black Coffee (no sugar)", FoodCategory.Beverage, 240.0, "1 cup (240 ml)",
            Nutrients(calories = 2.0, carbs = 0.0, protein = 0.3, fat = 0.0, sodium = 5.0, potassium = 49.0,
                magnesium = 3.0, phosphorus = 7.0, selenium = 0.0, copper = 0.01, manganese = 0.03, vitaminB1 = 0.0, vitaminB2 = 0.08, vitaminB3 = 0.7, vitaminB6 = 0.0)),
        food("Lemon Juice (plain)", FoodCategory.Beverage, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 7.0, carbs = 2.5, sugar = 1.0, protein = 0.1, fat = 0.0, sodium = 1.0,
                potassium = 25.0, vitaminC = 14.0, phosphorus = 8.0, selenium = 0.1, copper = 0.02, manganese = 0.01, vitaminB1 = 0.02, vitaminB2 = 0.01, vitaminB3 = 0.1, vitaminB6 = 0.05)),
        food("Sugarcane Juice", FoodCategory.Beverage, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 40.0, carbs = 10.0, sugar = 9.0, protein = 0.2, fat = 0.1, sodium = 12.0,
                calcium = 18.0, potassium = 75.0, iron = 0.4, phosphorus = 15.0, selenium = 0.6, copper = 0.03, manganese = 0.1, vitaminB1 = 0.03, vitaminB2 = 0.04, vitaminB3 = 0.2, vitaminB6 = 0.03)),
        food("Tender Coconut (with flesh)", FoodCategory.Beverage, 300.0, "1 fruit (300 ml + flesh)",
            Nutrients(calories = 45.0, carbs = 6.0, sugar = 3.0, protein = 1.0, fat = 1.5, fiber = 0.9,
                sodium = 80.0, potassium = 220.0, magnesium = 20.0, vitaminC = 2.0, phosphorus = 35.0, selenium = 1.5, copper = 0.1, manganese = 0.5, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.3, vitaminB6 = 0.03)),

        // ---------- DAIRY ----------
        food("Milk (toned, cow)", FoodCategory.Dairy, 250.0, "1 glass (250 ml)",
            Nutrients(calories = 58.0, carbs = 4.8, sugar = 4.8, protein = 3.2, fat = 3.0, saturatedFat = 1.9,
                sodium = 44.0, calcium = 125.0, magnesium = 11.0, potassium = 150.0, zinc = 0.4, vitaminA = 28.0, vitaminB12 = 0.4, vitaminD = 1.2, phosphorus = 95.0, selenium = 2.0, copper = 0.01, manganese = 0.0, vitaminB1 = 0.04, vitaminB2 = 0.18, vitaminB3 = 0.1, vitaminB6 = 0.04).detail(amino = AA_DAIRY)),
        food("Curd / Dahi", FoodCategory.Dairy, 100.0, "1 small bowl (100 g)",
            Nutrients(calories = 60.0, carbs = 4.7, protein = 3.1, fat = 3.3, saturatedFat = 2.1, sugar = 4.7,
                sodium = 36.0, calcium = 110.0, magnesium = 10.0, potassium = 140.0, zinc = 0.5, vitaminB12 = 0.4, phosphorus = 100.0, selenium = 2.2, copper = 0.01, manganese = 0.0, vitaminB1 = 0.04, vitaminB2 = 0.17, vitaminB3 = 0.1, vitaminB6 = 0.04).detail(amino = AA_DAIRY)),
        food("Paneer", FoodCategory.Dairy, 50.0, "50 g cube",
            Nutrients(calories = 265.0, carbs = 1.2, protein = 18.0, fat = 21.0, saturatedFat = 12.0, sodium = 22.0,
                calcium = 480.0, iron = 0.2, magnesium = 18.0, potassium = 104.0, zinc = 2.7, vitaminA = 200.0, vitaminB12 = 1.0, phosphorus = 210.0, selenium = 5.0, copper = 0.03, manganese = 0.02, vitaminB1 = 0.03, vitaminB2 = 0.2, vitaminB3 = 0.1, vitaminB6 = 0.06).detail(amino = AA_DAIRY)),
        food("Ghee", FoodCategory.Dairy, 5.0, "1 tsp (5 g)",
            Nutrients(calories = 900.0, fat = 100.0, saturatedFat = 62.0, cholesterol = 256.0, vitaminA = 700.0, phosphorus = 1.0, selenium = 0.0, copper = 0.0, manganese = 0.0, vitaminB1 = 0.0, vitaminB2 = 0.0, vitaminB3 = 0.0, vitaminB6 = 0.0)),
        food("Butter (salted)", FoodCategory.Dairy, 10.0, "2 tsp (10 g)",
            Nutrients(calories = 717.0, fat = 81.0, saturatedFat = 51.0, cholesterol = 215.0, sodium = 643.0, vitaminA = 684.0, phosphorus = 24.0, selenium = 1.0, copper = 0.0, manganese = 0.0, vitaminB1 = 0.0, vitaminB2 = 0.03, vitaminB3 = 0.04, vitaminB6 = 0.0)),

        // ---------- FRUITS ----------
        food("Banana", FoodCategory.Fruit, 120.0, "1 medium (120 g)",
            Nutrients(calories = 89.0, carbs = 23.0, sugar = 12.0, protein = 1.1, fat = 0.3, fiber = 2.6,
                potassium = 358.0, vitaminC = 8.7, magnesium = 27.0, vitaminA = 3.0, phosphorus = 22.0, selenium = 1.0, copper = 0.08, manganese = 0.27, vitaminB1 = 0.03, vitaminB2 = 0.07, vitaminB3 = 0.67, vitaminB6 = 0.37).detail(solubleFiberFraction = 0.27),
            servingVariants = listOf(
                ServingVariant("Medium", 120.0),
                ServingVariant("Small", 100.0),
                ServingVariant("Large", 140.0),
            )),
        food("Mango", FoodCategory.Fruit, 200.0, "1 medium (200 g)",
            Nutrients(calories = 60.0, carbs = 15.0, sugar = 13.7, protein = 0.8, fat = 0.4, fiber = 1.6,
                potassium = 168.0, vitaminA = 54.0, vitaminC = 36.4, folate = 43.0, phosphorus = 14.0, selenium = 0.6, copper = 0.11, manganese = 0.06, vitaminB1 = 0.03, vitaminB2 = 0.04, vitaminB3 = 0.67, vitaminB6 = 0.12),
            servingVariants = listOf(
                ServingVariant("Medium", 200.0),
                ServingVariant("Small", 150.0),
                ServingVariant("Large", 300.0),
            )),
        food("Apple", FoodCategory.Fruit, 150.0, "1 medium (150 g)",
            Nutrients(calories = 52.0, carbs = 14.0, sugar = 10.4, protein = 0.3, fat = 0.2, fiber = 2.4,
                potassium = 107.0, vitaminC = 4.6, phosphorus = 11.0, selenium = 0.0, copper = 0.03, manganese = 0.04, vitaminB1 = 0.02, vitaminB2 = 0.03, vitaminB3 = 0.09, vitaminB6 = 0.04).detail(solubleFiberFraction = 0.35)),
        food("Papaya", FoodCategory.Fruit, 150.0, "1 cup cubed (150 g)",
            Nutrients(calories = 43.0, carbs = 11.0, sugar = 8.0, protein = 0.5, fat = 0.3, fiber = 1.7,
                potassium = 182.0, vitaminA = 47.0, vitaminC = 60.9, folate = 37.0, phosphorus = 10.0, selenium = 0.6, copper = 0.05, manganese = 0.04, vitaminB1 = 0.02, vitaminB2 = 0.03, vitaminB3 = 0.36, vitaminB6 = 0.04)),
        food("Pomegranate", FoodCategory.Fruit, 150.0, "1 cup arils (150 g)",
            Nutrients(calories = 83.0, carbs = 19.0, sugar = 13.7, protein = 1.7, fat = 1.2, fiber = 4.0,
                potassium = 236.0, vitaminC = 10.2, vitaminK = 16.4, folate = 38.0, phosphorus = 36.0, selenium = 0.5, copper = 0.16, manganese = 0.12, vitaminB1 = 0.07, vitaminB2 = 0.05, vitaminB3 = 0.29, vitaminB6 = 0.08)),
        food("Guava", FoodCategory.Fruit, 100.0, "1 fruit (100 g)",
            Nutrients(calories = 68.0, carbs = 14.0, sugar = 8.9, protein = 2.6, fat = 1.0, fiber = 5.4,
                potassium = 417.0, vitaminA = 31.0, vitaminC = 228.0, folate = 49.0, phosphorus = 40.0, selenium = 0.6, copper = 0.23, manganese = 0.15, vitaminB1 = 0.07, vitaminB2 = 0.04, vitaminB3 = 1.08, vitaminB6 = 0.11).detail(solubleFiberFraction = 0.3)),
        food("Watermelon", FoodCategory.Fruit, 200.0, "1 cup (200 g)",
            Nutrients(calories = 30.0, carbs = 7.6, sugar = 6.2, protein = 0.6, fat = 0.2, fiber = 0.4,
                potassium = 112.0, vitaminA = 28.0, vitaminC = 8.1, phosphorus = 11.0, selenium = 0.4, copper = 0.04, manganese = 0.04, vitaminB1 = 0.03, vitaminB2 = 0.02, vitaminB3 = 0.18, vitaminB6 = 0.05)),
        food("Jackfruit (ripe)", FoodCategory.Fruit, 150.0, "1 cup (150 g)",
            Nutrients(calories = 95.0, carbs = 23.0, sugar = 19.0, protein = 1.7, fat = 0.6, fiber = 1.5,
                potassium = 303.0, magnesium = 29.0, vitaminA = 5.0, vitaminC = 13.7, folate = 24.0, phosphorus = 21.0, selenium = 0.6, copper = 0.08, manganese = 0.04, vitaminB1 = 0.1, vitaminB2 = 0.05, vitaminB3 = 0.92, vitaminB6 = 0.33)),
        food("Raw Jackfruit (cooked)", FoodCategory.Fruit, 150.0, "1 cup (150 g)",
            Nutrients(calories = 50.0, carbs = 11.0, protein = 1.7, fat = 0.4, fiber = 3.6,
                potassium = 220.0, calcium = 32.0, iron = 0.6, vitaminC = 9.0, phosphorus = 21.0, selenium = 0.6, copper = 0.19, manganese = 0.2, vitaminB1 = 0.03, vitaminB2 = 0.06, vitaminB3 = 0.4, vitaminB6 = 0.11)),
        food("Orange", FoodCategory.Fruit, 130.0, "1 medium (130 g)",
            Nutrients(calories = 47.0, carbs = 12.0, sugar = 9.4, protein = 0.9, fat = 0.1, fiber = 2.4,
                calcium = 40.0, potassium = 181.0, vitaminA = 11.0, vitaminC = 53.2, folate = 30.0, phosphorus = 14.0, selenium = 0.5, copper = 0.05, manganese = 0.03, vitaminB1 = 0.09, vitaminB2 = 0.04, vitaminB3 = 0.28, vitaminB6 = 0.06).detail(solubleFiberFraction = 0.6)),
        food("Mosambi (Sweet Lime)", FoodCategory.Fruit, 150.0, "1 fruit (150 g)",
            Nutrients(calories = 43.0, carbs = 9.3, sugar = 6.5, protein = 0.8, fat = 0.3, fiber = 1.8,
                calcium = 40.0, potassium = 170.0, vitaminC = 50.0, folate = 25.0, phosphorus = 13.0, selenium = 0.4, copper = 0.04, manganese = 0.03, vitaminB1 = 0.08, vitaminB2 = 0.03, vitaminB3 = 0.25, vitaminB6 = 0.05)),
        food("Grapes", FoodCategory.Fruit, 100.0, "1 cup (100 g)",
            Nutrients(calories = 67.0, carbs = 17.0, sugar = 16.0, protein = 0.6, fat = 0.4, fiber = 0.9,
                potassium = 191.0, vitaminC = 4.0, vitaminK = 14.6, phosphorus = 20.0, selenium = 0.1, copper = 0.13, manganese = 0.07, vitaminB1 = 0.07, vitaminB2 = 0.07, vitaminB3 = 0.19, vitaminB6 = 0.09)),
        food("Pineapple", FoodCategory.Fruit, 150.0, "1 cup chunks (150 g)",
            Nutrients(calories = 50.0, carbs = 13.0, sugar = 9.9, protein = 0.5, fat = 0.1, fiber = 1.4,
                potassium = 109.0, vitaminA = 3.0, vitaminC = 47.8, folate = 18.0, phosphorus = 8.0, selenium = 0.1, copper = 0.11, manganese = 0.93, vitaminB1 = 0.08, vitaminB2 = 0.03, vitaminB3 = 0.5, vitaminB6 = 0.11)),
        food("Sapota (Chikoo)", FoodCategory.Fruit, 100.0, "1 medium (100 g)",
            Nutrients(calories = 83.0, carbs = 20.0, sugar = 14.5, protein = 0.4, fat = 1.1, fiber = 5.3,
                potassium = 193.0, calcium = 21.0, iron = 0.8, vitaminC = 14.7, phosphorus = 12.0, selenium = 0.6, copper = 0.09, manganese = 0.09, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.2, vitaminB6 = 0.04)),

        // ---------- VEGETABLES ----------
        food("Tomato (raw)", FoodCategory.Vegetable, 100.0, "1 medium (100 g)",
            Nutrients(calories = 18.0, carbs = 3.9, sugar = 2.6, protein = 0.9, fat = 0.2, fiber = 1.2, sodium = 5.0,
                calcium = 10.0, potassium = 237.0, zinc = 0.17, vitaminA = 42.0, vitaminC = 14.0, vitaminK = 7.9, folate = 15.0, phosphorus = 24.0, selenium = 0.0, copper = 0.06, manganese = 0.11, vitaminB1 = 0.04, vitaminB2 = 0.02, vitaminB3 = 0.59, vitaminB6 = 0.08)),
        food("Onion (raw)", FoodCategory.Vegetable, 50.0, "1/2 medium (50 g)",
            Nutrients(calories = 40.0, carbs = 9.3, sugar = 4.2, protein = 1.1, fat = 0.1, fiber = 1.7,
                calcium = 23.0, magnesium = 10.0, potassium = 146.0, zinc = 0.17, vitaminC = 7.4, folate = 19.0, phosphorus = 29.0, selenium = 0.5, copper = 0.04, manganese = 0.13, vitaminB1 = 0.05, vitaminB2 = 0.03, vitaminB3 = 0.12, vitaminB6 = 0.12)),
        // Raw leaf spinach is fluffy: 1 loosely-packed cup ≈ 30 g, not 100 g.
        food("Spinach (raw)", FoodCategory.Vegetable, 30.0, "1 cup (30 g)",
            Nutrients(calories = 23.0, carbs = 3.6, protein = 2.9, fat = 0.4, fiber = 2.2, sodium = 79.0,
                calcium = 99.0, iron = 2.7, magnesium = 79.0, potassium = 558.0,
                vitaminA = 469.0, vitaminC = 28.1, vitaminK = 482.0, folate = 194.0, phosphorus = 49.0, selenium = 1.0, copper = 0.13, manganese = 0.9, vitaminB1 = 0.08, vitaminB2 = 0.19, vitaminB3 = 0.72, vitaminB6 = 0.2).detail(solubleFiberFraction = 0.27)),
        food("Carrot (raw)", FoodCategory.Vegetable, 80.0, "1 medium (80 g)",
            Nutrients(calories = 41.0, carbs = 9.6, sugar = 4.7, protein = 0.9, fat = 0.2, fiber = 2.8, sodium = 69.0,
                calcium = 33.0, magnesium = 12.0, potassium = 320.0, zinc = 0.24, vitaminA = 835.0, vitaminC = 5.9, vitaminK = 13.2, folate = 19.0, phosphorus = 35.0, selenium = 0.1, copper = 0.05, manganese = 0.14, vitaminB1 = 0.07, vitaminB2 = 0.06, vitaminB3 = 0.98, vitaminB6 = 0.14).detail(solubleFiberFraction = 0.4)),
        food("Potato (boiled)", FoodCategory.Vegetable, 150.0, "1 medium (150 g)",
            Nutrients(calories = 87.0, carbs = 20.0, sugar = 0.9, protein = 1.9, fat = 0.1, fiber = 1.8, sodium = 4.0,
                calcium = 5.0, iron = 0.3, magnesium = 20.0, potassium = 379.0, zinc = 0.30, vitaminC = 13.0, folate = 10.0, phosphorus = 44.0, selenium = 0.3, copper = 0.18, manganese = 0.14, vitaminB1 = 0.11, vitaminB2 = 0.02, vitaminB3 = 1.44, vitaminB6 = 0.3).detail(solubleFiberFraction = 0.44)),
        food("Ladyfinger (Bhindi, raw)", FoodCategory.Vegetable, 100.0, "1 cup (100 g)",
            Nutrients(calories = 33.0, carbs = 7.5, sugar = 1.5, protein = 1.9, fat = 0.2, fiber = 3.2, sodium = 7.0,
                calcium = 82.0, iron = 0.6, magnesium = 57.0, potassium = 299.0,
                vitaminA = 36.0, vitaminC = 23.0, vitaminK = 31.3, folate = 60.0, phosphorus = 61.0, selenium = 0.7, copper = 0.11, manganese = 0.79, vitaminB1 = 0.2, vitaminB2 = 0.06, vitaminB3 = 1.0, vitaminB6 = 0.22)),
        food("Brinjal (Eggplant, raw)", FoodCategory.Vegetable, 100.0, "1 cup cubed (100 g)",
            Nutrients(calories = 25.0, carbs = 5.9, sugar = 3.5, protein = 1.0, fat = 0.2, fiber = 3.0, sodium = 2.0,
                potassium = 229.0, zinc = 0.12, magnesium = 14.0, vitaminC = 2.2, folate = 22.0, phosphorus = 24.0, selenium = 0.3, copper = 0.08, manganese = 0.23, vitaminB1 = 0.04, vitaminB2 = 0.04, vitaminB3 = 0.65, vitaminB6 = 0.08)),
        food("Cauliflower (raw)", FoodCategory.Vegetable, 100.0, "1 cup florets (100 g)",
            Nutrients(calories = 25.0, carbs = 5.0, sugar = 1.9, protein = 1.9, fat = 0.3, fiber = 2.0, sodium = 30.0,
                calcium = 22.0, potassium = 299.0, zinc = 0.28, vitaminC = 48.2, vitaminK = 15.5, folate = 57.0, phosphorus = 44.0, selenium = 0.6, copper = 0.04, manganese = 0.16, vitaminB1 = 0.05, vitaminB2 = 0.06, vitaminB3 = 0.51, vitaminB6 = 0.18)),
        food("Cabbage (raw)", FoodCategory.Vegetable, 100.0, "1 cup shredded (100 g)",
            Nutrients(calories = 25.0, carbs = 5.8, sugar = 3.2, protein = 1.3, fat = 0.1, fiber = 2.5, sodium = 18.0,
                potassium = 170.0, vitaminC = 36.6, vitaminK = 76.0, folate = 43.0, phosphorus = 26.0, selenium = 0.3, copper = 0.02, manganese = 0.16, vitaminB1 = 0.06, vitaminB2 = 0.04, vitaminB3 = 0.23, vitaminB6 = 0.12)),
        food("Cucumber", FoodCategory.Vegetable, 100.0, "1 cup sliced (100 g)",
            Nutrients(calories = 16.0, carbs = 3.6, sugar = 1.7, protein = 0.7, fat = 0.1, fiber = 0.5, sodium = 2.0,
                potassium = 147.0, vitaminC = 2.8, vitaminK = 16.4, phosphorus = 24.0, selenium = 0.3, copper = 0.04, manganese = 0.08, vitaminB1 = 0.03, vitaminB2 = 0.03, vitaminB3 = 0.1, vitaminB6 = 0.04)),
        food("Beetroot (raw)", FoodCategory.Vegetable, 80.0, "1 medium (80 g)",
            Nutrients(calories = 43.0, carbs = 9.6, sugar = 6.8, protein = 1.6, fat = 0.2, fiber = 2.8, sodium = 78.0,
                potassium = 325.0, iron = 0.8, magnesium = 23.0, vitaminC = 4.9, folate = 109.0, phosphorus = 40.0, selenium = 0.7, copper = 0.08, manganese = 0.33, vitaminB1 = 0.03, vitaminB2 = 0.04, vitaminB3 = 0.33, vitaminB6 = 0.07)),
        food("Bottle Gourd (raw)", FoodCategory.Vegetable, 100.0, "1 cup cubed (100 g)",
            Nutrients(calories = 14.0, carbs = 3.4, sugar = 1.4, protein = 0.6, fat = 0.0, fiber = 0.5, sodium = 2.0,
                potassium = 150.0, vitaminC = 10.1, phosphorus = 13.0, selenium = 0.2, copper = 0.03, manganese = 0.07, vitaminB1 = 0.03, vitaminB2 = 0.02, vitaminB3 = 0.32, vitaminB6 = 0.04)),
        food("Drumstick (Moringa)", FoodCategory.Vegetable, 100.0, "1 cup chopped (100 g)",
            Nutrients(calories = 37.0, carbs = 8.5, sugar = 1.5, protein = 2.1, fat = 0.2, fiber = 3.2, sodium = 42.0,
                calcium = 30.0, iron = 0.4, potassium = 461.0, vitaminA = 4.0, vitaminC = 141.0, folate = 44.0, phosphorus = 50.0, selenium = 0.5, copper = 0.1, manganese = 0.26, vitaminB1 = 0.05, vitaminB2 = 0.07, vitaminB3 = 0.62, vitaminB6 = 0.12)),
        food("French Beans (raw)", FoodCategory.Vegetable, 100.0, "1 cup (100 g)",
            Nutrients(calories = 31.0, carbs = 7.0, sugar = 3.3, protein = 1.8, fat = 0.2, fiber = 3.4, sodium = 6.0,
                calcium = 37.0, iron = 1.0, potassium = 211.0, vitaminA = 35.0, vitaminC = 12.2, folate = 33.0, phosphorus = 38.0, selenium = 0.6, copper = 0.07, manganese = 0.22, vitaminB1 = 0.08, vitaminB2 = 0.1, vitaminB3 = 0.75, vitaminB6 = 0.14)),

        // ---------- GRAINS / STAPLES ----------
        food("White Rice (cooked)", FoodCategory.Grain, 150.0, "1 cup (150 g)",
            Nutrients(calories = 130.0, carbs = 28.0, protein = 2.7, fat = 0.3, fiber = 0.4, sodium = 1.0,
                calcium = 10.0, iron = 0.2, magnesium = 12.0, potassium = 35.0, zinc = 0.49, folate = 3.0, phosphorus = 43.0, selenium = 7.5, copper = 0.07, manganese = 0.47, vitaminB1 = 0.02, vitaminB2 = 0.01, vitaminB3 = 0.4, vitaminB6 = 0.05).detail(amino = AA_RICE_WHITE, solubleFiberFraction = 0.12)),
        food("Brown Rice (cooked)", FoodCategory.Grain, 150.0, "1 cup (150 g)",
            Nutrients(calories = 123.0, carbs = 25.6, protein = 2.7, fat = 1.0, fiber = 1.6, sodium = 4.0,
                calcium = 3.0, iron = 0.6, magnesium = 39.0, potassium = 86.0, zinc = 1.05, phosphorus = 83.0, selenium = 9.8, copper = 0.1, manganese = 0.97, vitaminB1 = 0.1, vitaminB2 = 0.03, vitaminB3 = 1.5, vitaminB6 = 0.15).detail(amino = AA_RICE_BROWN, solubleFiberFraction = 0.12)),
        food("Oats (cooked)", FoodCategory.Grain, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 71.0, carbs = 12.0, protein = 2.5, fat = 1.5, fiber = 1.7, sodium = 49.0,
                calcium = 9.0, iron = 0.9, magnesium = 26.0, potassium = 70.0, folate = 6.0, phosphorus = 77.0, selenium = 4.5, copper = 0.09, manganese = 0.6, vitaminB1 = 0.08, vitaminB2 = 0.02, vitaminB3 = 0.2, vitaminB6 = 0.01).detail(amino = AA_OATS, solubleFiberFraction = 0.5)),
        food("Whole Wheat Flour (atta)", FoodCategory.Grain, 30.0, "1/4 cup (30 g)",
            Nutrients(calories = 340.0, carbs = 72.0, protein = 13.0, fat = 2.5, fiber = 11.0, sodium = 2.0,
                calcium = 34.0, iron = 3.6, magnesium = 137.0, potassium = 363.0, zinc = 2.9, folate = 44.0, phosphorus = 355.0, selenium = 12.0, copper = 0.41, manganese = 3.8, vitaminB1 = 0.45, vitaminB2 = 0.17, vitaminB3 = 5.5, vitaminB6 = 0.34).detail(amino = AA_WHEAT, solubleFiberFraction = 0.18)),

        // ---------- PROTEINS ----------
        // Real egg weights, not a ×0.7/×1.4 scale of "1 large". USDA: medium ≈ 44 g,
        // large ≈ 50 g, jumbo ≈ 60 g. Default to one medium egg.
        food("Egg (boiled)", FoodCategory.Protein, 50.0, "1 egg (50 g)",
            Nutrients(calories = 155.0, carbs = 1.1, protein = 13.0, fat = 11.0, saturatedFat = 3.3,
                cholesterol = 373.0, sodium = 124.0, calcium = 50.0, iron = 1.2, magnesium = 10.0, potassium = 126.0, zinc = 1.3,
                vitaminA = 149.0, vitaminB12 = 1.1, vitaminD = 2.0, folate = 44.0, phosphorus = 172.0, selenium = 30.0, copper = 0.07, manganese = 0.03, vitaminB1 = 0.07, vitaminB2 = 0.51, vitaminB3 = 0.07, vitaminB6 = 0.12).detail(amino = AA_EGG),
            servingVariants = listOf(
                ServingVariant("Medium", 50.0),
                ServingVariant("Small", 44.0),
                ServingVariant("Large", 60.0),
            )),
        food("Chicken Breast (cooked)", FoodCategory.Protein, 100.0, "1 fillet (100 g)",
            Nutrients(calories = 165.0, carbs = 0.0, protein = 31.0, fat = 3.6, saturatedFat = 1.0,
                cholesterol = 85.0, sodium = 74.0, calcium = 15.0, iron = 1.0, magnesium = 29.0, potassium = 256.0, zinc = 1.0, vitaminB12 = 0.3, phosphorus = 210.0, selenium = 27.0, copper = 0.05, manganese = 0.02, vitaminB1 = 0.07, vitaminB2 = 0.12, vitaminB3 = 13.5, vitaminB6 = 0.6).detail(amino = AA_CHICKEN)),
        // South Indian home-style chicken curry — bone-in pieces in a coconut /
        // onion-tomato masala gravy (nattu kozhi / kozhi kuzhambu). Values are per
        // 100 g of served curry (bone-in portion + gravy), so protein reads lower
        // than boneless cuts because the weight includes bone.
        food("Chicken Curry (home-style, bone-in)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 190.0, carbs = 5.0, protein = 17.0, fat = 12.0, saturatedFat = 4.5, fiber = 1.0,
                cholesterol = 78.0, sodium = 400.0, calcium = 30.0, iron = 1.6, magnesium = 24.0, potassium = 260.0,
                vitaminB12 = 0.4, phosphorus = 130.0, selenium = 15.0, copper = 0.08, manganese = 0.12, vitaminB1 = 0.06, vitaminB2 = 0.12, vitaminB3 = 5.0, vitaminB6 = 0.25)),
        food("Toor Dal (cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 95.0, carbs = 14.0, protein = 6.0, fat = 1.5, fiber = 3.5,
                calcium = 17.0, iron = 1.6, magnesium = 38.0, potassium = 280.0, zinc = 1.3, folate = 110.0, phosphorus = 110.0, selenium = 3.0, copper = 0.25, manganese = 0.45, vitaminB1 = 0.15, vitaminB2 = 0.05, vitaminB3 = 0.7, vitaminB6 = 0.1).detail(amino = AA_TOOR, solubleFiberFraction = 0.12)),
        food("Moong Dal (cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 90.0, carbs = 14.0, protein = 6.0, fat = 1.0, fiber = 3.5,
                calcium = 27.0, iron = 1.4, magnesium = 48.0, potassium = 266.0, folate = 159.0, phosphorus = 100.0, selenium = 2.5, copper = 0.22, manganese = 0.3, vitaminB1 = 0.16, vitaminB2 = 0.06, vitaminB3 = 0.6, vitaminB6 = 0.07).detail(amino = AA_MUNG, solubleFiberFraction = 0.12)),
        food("Masoor Dal (cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 100.0, carbs = 15.0, protein = 7.0, fat = 1.0, fiber = 4.0,
                calcium = 19.0, iron = 3.3, magnesium = 36.0, potassium = 369.0, folate = 181.0, phosphorus = 120.0, selenium = 2.8, copper = 0.25, manganese = 0.49, vitaminB1 = 0.17, vitaminB2 = 0.07, vitaminB3 = 1.0, vitaminB6 = 0.18).detail(amino = AA_LENTIL, solubleFiberFraction = 0.13)),
        food("Horse Gram (Kollu, cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 130.0, carbs = 22.0, protein = 9.0, fat = 1.0, fiber = 5.0,
                calcium = 78.0, iron = 4.5, magnesium = 60.0, potassium = 287.0, folate = 90.0, phosphorus = 130.0, selenium = 3.0, copper = 0.3, manganese = 0.55, vitaminB1 = 0.14, vitaminB2 = 0.08, vitaminB3 = 0.55, vitaminB6 = 0.1)),
        food("Whole Moong (cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 105.0, carbs = 19.0, protein = 7.0, fat = 0.4, fiber = 7.6,
                calcium = 27.0, iron = 1.4, magnesium = 48.0, potassium = 266.0, folate = 159.0, phosphorus = 99.0, selenium = 2.5, copper = 0.21, manganese = 0.3, vitaminB1 = 0.16, vitaminB2 = 0.06, vitaminB3 = 0.58, vitaminB6 = 0.07).detail(amino = AA_MUNG, solubleFiberFraction = 0.12)),
        food("Whole Urad (Black Gram, cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 120.0, carbs = 20.0, protein = 8.0, fat = 0.4, fiber = 8.0,
                calcium = 22.0, iron = 2.9, magnesium = 39.0, potassium = 280.0, folate = 130.0, phosphorus = 140.0, selenium = 2.8, copper = 0.27, manganese = 0.5, vitaminB1 = 0.17, vitaminB2 = 0.06, vitaminB3 = 0.55, vitaminB6 = 0.08).detail(amino = AA_URAD, solubleFiberFraction = 0.12)),
        food("Kala Chana (Black Chickpea, cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 160.0, carbs = 28.0, protein = 9.0, fat = 2.5, fiber = 8.0,
                calcium = 49.0, iron = 2.9, magnesium = 48.0, potassium = 291.0, folate = 172.0, phosphorus = 120.0, selenium = 3.7, copper = 0.35, manganese = 1.0, vitaminB1 = 0.12, vitaminB2 = 0.06, vitaminB3 = 0.53, vitaminB6 = 0.14).detail(amino = AA_CHICKPEA, solubleFiberFraction = 0.17)),
        food("Cowpea / Lobia (cooked)", FoodCategory.Protein, 200.0, "1 bowl (200 g)",
            Nutrients(calories = 116.0, carbs = 21.0, protein = 8.0, fat = 0.5, fiber = 6.5,
                calcium = 24.0, iron = 2.5, magnesium = 53.0, potassium = 278.0, folate = 208.0, phosphorus = 120.0, selenium = 2.5, copper = 0.27, manganese = 0.48, vitaminB1 = 0.2, vitaminB2 = 0.06, vitaminB3 = 0.5, vitaminB6 = 0.1).detail(amino = AA_COWPEA, solubleFiberFraction = 0.12)),
        food("Sprouted Moong (raw)", FoodCategory.Protein, 100.0, "1 cup (100 g)",
            Nutrients(calories = 30.0, carbs = 5.9, protein = 3.0, fat = 0.2, fiber = 1.8, sodium = 6.0,
                calcium = 13.0, iron = 0.9, magnesium = 21.0, potassium = 149.0, vitaminC = 13.2, folate = 61.0, phosphorus = 54.0, selenium = 0.6, copper = 0.16, manganese = 0.19, vitaminB1 = 0.08, vitaminB2 = 0.12, vitaminB3 = 0.75, vitaminB6 = 0.09).detail(amino = AA_MUNG, solubleFiberFraction = 0.12)),
        food("Soya Chunks (cooked)", FoodCategory.Protein, 100.0, "1 cup (100 g)",
            Nutrients(calories = 100.0, carbs = 6.0, protein = 16.0, fat = 0.5, fiber = 3.5, sodium = 25.0,
                calcium = 60.0, iron = 5.6, magnesium = 80.0, potassium = 360.0, folate = 100.0, phosphorus = 290.0, selenium = 8.0, copper = 0.5, manganese = 1.2, vitaminB1 = 0.3, vitaminB2 = 0.25, vitaminB3 = 1.2, vitaminB6 = 0.2).detail(amino = AA_SOY, solubleFiberFraction = 0.15)),

        // ---------- OTHER ----------
        // Plain sugar — default serving is 1 tsp (~4 g), but log can be scaled
        // up for 1 tbsp (~12 g) or measured weights.
        food("Sugar", FoodCategory.Other, 4.0, "1 tsp (4 g)",
            Nutrients(calories = 387.0, carbs = 100.0, sugar = 100.0, protein = 0.0, fat = 0.0, sodium = 1.0,
                calcium = 1.0, potassium = 2.0, phosphorus = 0.0, selenium = 0.0, copper = 0.0, manganese = 0.0, vitaminB1 = 0.0, vitaminB2 = 0.0, vitaminB3 = 0.0, vitaminB6 = 0.0)),
    )
}
