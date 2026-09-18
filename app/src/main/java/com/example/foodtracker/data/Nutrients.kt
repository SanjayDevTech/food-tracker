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
    )

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
enum class NutrientTarget {
    /** Something to reach. The default, and shown without comment. */
    Goal,

    /** A ceiling to stay under. Badged, because a low reading is a good day. */
    Limit,

    /**
     * Tracked and shown, but with no number to hit either way.
     *
     * Sugar is the case this exists for. The obvious ceiling is the WHO's
     * "under 10% of energy", but that guideline is about FREE sugars, while the
     * value here comes from OpenFoodFacts / USDA `sugars`, which is TOTAL sugar
     * — the lactose in milk and the fructose in fruit included. Holding total
     * sugar to a free-sugar ceiling makes a banana look like a failure, and the
     * app has no data to separate the two, so it states the amount and leaves
     * the judgement to the user.
     */
    None,
}

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

val AllNutrientFields: List<NutrientField> = listOf(
    NutrientField("calories", "Calories", NutrientUnit.Kcal, NutrientCategory.Macros, { it.calories }, { it.calories }),
    NutrientField("carbs", "Carbs", NutrientUnit.Gram, NutrientCategory.Macros, { it.carbs }, { it.carbs }),
    NutrientField("sugar", "Sugar", NutrientUnit.Gram, NutrientCategory.Macros, { it.sugar }, { it.sugar }, NutrientTarget.None),
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
