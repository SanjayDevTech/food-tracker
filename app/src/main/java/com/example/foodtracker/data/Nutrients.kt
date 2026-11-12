package com.example.foodtracker.data

import androidx.compose.runtime.Immutable

@Immutable
data class Nutrients(
    val calories: Double = 0.0,
    val carbs: Double = 0.0,
    val sugar: Double = 0.0,
    val fiber: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val omega3: Double = 0.0,
    val cholesterol: Double = 0.0,
    val sodium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val potassium: Double = 0.0,
    val zinc: Double = 0.0,
    val phosphorus: Double = 0.0,
    val selenium: Double = 0.0,
    val copper: Double = 0.0,
    val manganese: Double = 0.0,
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminE: Double = 0.0,
    val vitaminK: Double = 0.0,
    val vitaminB12: Double = 0.0,
    val folate: Double = 0.0,
    val vitaminB1: Double = 0.0,
    val vitaminB2: Double = 0.0,
    val vitaminB3: Double = 0.0,
    val vitaminB6: Double = 0.0,

    // ── Optional detail ──────────────────────────────────────────────
    // Sub-components of fiber and protein. 0 means UNKNOWN here, not
    // "none": most foods carry no split at all (every custom food, every
    // AI food filled before this existed, and the seeded foods outside the
    // hand-filled subset). The UI must therefore hide the breakdown rather
    // than draw it as zero. See [hasFiberSplit] / [hasAminoAcids].
    val solubleFiber: Double = 0.0,
    val insolubleFiber: Double = 0.0,
    val essentialAminoAcids: Double = 0.0,
    val leucine: Double = 0.0,
) {
    operator fun plus(other: Nutrients) = Nutrients(
        calories + other.calories,
        carbs + other.carbs,
        sugar + other.sugar,
        fiber + other.fiber,
        protein + other.protein,
        fat + other.fat,
        saturatedFat + other.saturatedFat,
        omega3 + other.omega3,
        cholesterol + other.cholesterol,
        sodium + other.sodium,
        calcium + other.calcium,
        iron + other.iron,
        magnesium + other.magnesium,
        potassium + other.potassium,
        zinc + other.zinc,
        phosphorus + other.phosphorus,
        selenium + other.selenium,
        copper + other.copper,
        manganese + other.manganese,
        vitaminA + other.vitaminA,
        vitaminC + other.vitaminC,
        vitaminD + other.vitaminD,
        vitaminE + other.vitaminE,
        vitaminK + other.vitaminK,
        vitaminB12 + other.vitaminB12,
        folate + other.folate,
        vitaminB1 + other.vitaminB1,
        vitaminB2 + other.vitaminB2,
        vitaminB3 + other.vitaminB3,
        vitaminB6 + other.vitaminB6,
        solubleFiber + other.solubleFiber,
        insolubleFiber + other.insolubleFiber,
        essentialAminoAcids + other.essentialAminoAcids,
        leucine + other.leucine,
    )

    operator fun times(factor: Double) = Nutrients(
        calories * factor,
        carbs * factor,
        sugar * factor,
        fiber * factor,
        protein * factor,
        fat * factor,
        saturatedFat * factor,
        omega3 * factor,
        cholesterol * factor,
        sodium * factor,
        calcium * factor,
        iron * factor,
        magnesium * factor,
        potassium * factor,
        zinc * factor,
        phosphorus * factor,
        selenium * factor,
        copper * factor,
        manganese * factor,
        vitaminA * factor,
        vitaminC * factor,
        vitaminD * factor,
        vitaminE * factor,
        vitaminK * factor,
        vitaminB12 * factor,
        folate * factor,
        vitaminB1 * factor,
        vitaminB2 * factor,
        vitaminB3 * factor,
        vitaminB6 * factor,
        solubleFiber * factor,
        insolubleFiber * factor,
        essentialAminoAcids * factor,
        leucine * factor,
    )

    /** True when this food carries a soluble/insoluble split worth drawing. */
    fun hasFiberSplit(): Boolean = solubleFiber > 0.0 || insolubleFiber > 0.0

    /** True when this food carries an amino-acid breakdown worth drawing. */
    fun hasAminoAcids(): Boolean = essentialAminoAcids > 0.0

    companion object {
        val Zero = Nutrients()
    }
}

enum class NutrientCategory { Macros, Minerals, Vitamins }

enum class NutrientUnit(val symbol: String) {
    Kcal("kcal"), Gram("g"), Milligram("mg"), Microgram("µg")
}

/**
 * Whether a field's number is something to reach or something to stay under.
 *
 * [Limit] fields are capped by dietary guidance rather than targeted: sitting at
 * 40% of the number is a good day, not a shortfall. The UI must not present them
 * the same way it presents an RDA, or a low reading reads as a failure.
 */
enum class NutrientTarget { Goal, Limit }

data class NutrientField(
    val key: String,
    val label: String,
    val unit: NutrientUnit,
    val category: NutrientCategory,
    val read: (Nutrients) -> Double,
    val goal: (NutrientGoals) -> Double,
    /** Defaults to [NutrientTarget.Goal] - only the four capped fields opt out. */
    val target: NutrientTarget = NutrientTarget.Goal,
)

/**
 * [AllNutrientFields] keyed by category. Precomputed because the input is a
 * constant — the screens that render a category used to call groupBy on every
 * recomposition, rebuilding the same three lists each time.
 */
val NutrientFieldsByCategory: Map<NutrientCategory, List<NutrientField>> by lazy {
    AllNutrientFields.groupBy { it.category }
}

val AllNutrientFields: List<NutrientField> = listOf(
    NutrientField("calories", "Calories", NutrientUnit.Kcal, NutrientCategory.Macros, { it.calories }, { it.calories }),
    NutrientField("carbs", "Carbs", NutrientUnit.Gram, NutrientCategory.Macros, { it.carbs }, { it.carbs }),
    NutrientField("sugar", "Sugar", NutrientUnit.Gram, NutrientCategory.Macros, { it.sugar }, { it.sugar }, NutrientTarget.Limit),
    NutrientField("fiber", "Fiber", NutrientUnit.Gram, NutrientCategory.Macros, { it.fiber }, { it.fiber }),
    NutrientField("protein", "Protein", NutrientUnit.Gram, NutrientCategory.Macros, { it.protein }, { it.protein }),
    NutrientField("fat", "Fat", NutrientUnit.Gram, NutrientCategory.Macros, { it.fat }, { it.fat }),
    NutrientField("saturatedFat", "Saturated Fat", NutrientUnit.Gram, NutrientCategory.Macros, { it.saturatedFat }, { it.saturatedFat }, NutrientTarget.Limit),
    NutrientField("omega3", "Omega-3", NutrientUnit.Gram, NutrientCategory.Macros, { it.omega3 }, { it.omega3 }),
    NutrientField("cholesterol", "Cholesterol", NutrientUnit.Milligram, NutrientCategory.Macros, { it.cholesterol }, { it.cholesterol }, NutrientTarget.Limit),
    NutrientField("sodium", "Sodium", NutrientUnit.Milligram, NutrientCategory.Macros, { it.sodium }, { it.sodium }, NutrientTarget.Limit),

    NutrientField("calcium", "Calcium", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.calcium }, { it.calcium }),
    NutrientField("iron", "Iron", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.iron }, { it.iron }),
    NutrientField("magnesium", "Magnesium", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.magnesium }, { it.magnesium }),
    NutrientField("potassium", "Potassium", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.potassium }, { it.potassium }),
    NutrientField("zinc", "Zinc", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.zinc }, { it.zinc }),
    NutrientField("phosphorus", "Phosphorus", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.phosphorus }, { it.phosphorus }),
    NutrientField("selenium", "Selenium", NutrientUnit.Microgram, NutrientCategory.Minerals, { it.selenium }, { it.selenium }),
    NutrientField("copper", "Copper", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.copper }, { it.copper }),
    NutrientField("manganese", "Manganese", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.manganese }, { it.manganese }),

    NutrientField("vitaminA", "Vitamin A", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminA }, { it.vitaminA }),
    NutrientField("vitaminC", "Vitamin C", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminC }, { it.vitaminC }),
    NutrientField("vitaminD", "Vitamin D", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminD }, { it.vitaminD }),
    NutrientField("vitaminE", "Vitamin E", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminE }, { it.vitaminE }),
    NutrientField("vitaminK", "Vitamin K", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminK }, { it.vitaminK }),
    NutrientField("vitaminB1", "Vitamin B1 (Thiamin)", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminB1 }, { it.vitaminB1 }),
    NutrientField("vitaminB2", "Vitamin B2 (Riboflavin)", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminB2 }, { it.vitaminB2 }),
    NutrientField("vitaminB3", "Vitamin B3 (Niacin)", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminB3 }, { it.vitaminB3 }),
    NutrientField("vitaminB6", "Vitamin B6", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminB6 }, { it.vitaminB6 }),
    NutrientField("vitaminB12", "Vitamin B12", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminB12 }, { it.vitaminB12 }),
    NutrientField("folate", "Folate", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.folate }, { it.folate }),
)

/**
 * A component of a parent nutrient — soluble fiber under Fiber, leucine under
 * Protein. Deliberately NOT a [NutrientField]: sub-nutrients carry no goal, no
 * bar and no contribution sheet. They are a detail you can open when curious,
 * not a number to hit, and the UI is built so they never compete with the ten
 * macro rows above them.
 */
data class NutrientSubField(
    val key: String,
    val label: String,
    val read: (Nutrients) -> Double,
)

/** Shown under the Fiber row. Soluble + insoluble should total [Nutrients.fiber]. */
val FiberSubFields: List<NutrientSubField> = listOf(
    NutrientSubField("solubleFiber", "Soluble") { it.solubleFiber },
    NutrientSubField("insolubleFiber", "Insoluble") { it.insolubleFiber },
)

/**
 * Shown under the Protein row.
 *
 * Only two values, by design. The nine essential amino acids are each real and
 * each knowable, but outside a lab you get them from a composition table, and
 * for most of what this app logs there is no table — so the choice is between
 * two numbers that can be sanity-checked (essential AAs run 29-47% of protein,
 * leucine 15-24% of that) and nine that cannot. Leucine earns its own row
 * because it is the one with a number worth acting on: roughly 2.5 g in a meal
 * is what triggers muscle protein synthesis.
 */
val ProteinSubFields: List<NutrientSubField> = listOf(
    NutrientSubField("essentialAminoAcids", "Essential amino acids") { it.essentialAminoAcids },
    NutrientSubField("leucine", "of which leucine") { it.leucine },
)

/** Every sub-nutrient, for the exports that walk the whole set. All in grams. */
val AllNutrientSubFields: List<NutrientSubField> = FiberSubFields + ProteinSubFields

/** The sub-nutrients to offer under a given [NutrientField], if any. */
fun subFieldsFor(key: String): List<NutrientSubField> = when (key) {
    "fiber" -> FiberSubFields
    "protein" -> ProteinSubFields
    else -> emptyList()
}
