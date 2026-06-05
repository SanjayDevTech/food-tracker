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
    val cholesterol: Double = 0.0,
    val sodium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val potassium: Double = 0.0,
    val zinc: Double = 0.0,
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminE: Double = 0.0,
    val vitaminK: Double = 0.0,
    val vitaminB12: Double = 0.0,
    val folate: Double = 0.0,
) {
    operator fun plus(other: Nutrients) = Nutrients(
        calories + other.calories,
        carbs + other.carbs,
        sugar + other.sugar,
        fiber + other.fiber,
        protein + other.protein,
        fat + other.fat,
        saturatedFat + other.saturatedFat,
        cholesterol + other.cholesterol,
        sodium + other.sodium,
        calcium + other.calcium,
        iron + other.iron,
        magnesium + other.magnesium,
        potassium + other.potassium,
        zinc + other.zinc,
        vitaminA + other.vitaminA,
        vitaminC + other.vitaminC,
        vitaminD + other.vitaminD,
        vitaminE + other.vitaminE,
        vitaminK + other.vitaminK,
        vitaminB12 + other.vitaminB12,
        folate + other.folate,
    )

    operator fun times(factor: Double) = Nutrients(
        calories * factor,
        carbs * factor,
        sugar * factor,
        fiber * factor,
        protein * factor,
        fat * factor,
        saturatedFat * factor,
        cholesterol * factor,
        sodium * factor,
        calcium * factor,
        iron * factor,
        magnesium * factor,
        potassium * factor,
        zinc * factor,
        vitaminA * factor,
        vitaminC * factor,
        vitaminD * factor,
        vitaminE * factor,
        vitaminK * factor,
        vitaminB12 * factor,
        folate * factor,
    )

    companion object {
        val Zero = Nutrients()
    }
}

enum class NutrientCategory { Macros, Minerals, Vitamins }

enum class NutrientUnit(val symbol: String) {
    Kcal("kcal"), Gram("g"), Milligram("mg"), Microgram("µg")
}

data class NutrientField(
    val key: String,
    val label: String,
    val unit: NutrientUnit,
    val category: NutrientCategory,
    val read: (Nutrients) -> Double,
    val goal: (NutrientGoals) -> Double,
)

val AllNutrientFields: List<NutrientField> = listOf(
    NutrientField("calories", "Calories", NutrientUnit.Kcal, NutrientCategory.Macros, { it.calories }, { it.calories }),
    NutrientField("carbs", "Carbs", NutrientUnit.Gram, NutrientCategory.Macros, { it.carbs }, { it.carbs }),
    NutrientField("sugar", "Sugar", NutrientUnit.Gram, NutrientCategory.Macros, { it.sugar }, { it.sugar }),
    NutrientField("fiber", "Fiber", NutrientUnit.Gram, NutrientCategory.Macros, { it.fiber }, { it.fiber }),
    NutrientField("protein", "Protein", NutrientUnit.Gram, NutrientCategory.Macros, { it.protein }, { it.protein }),
    NutrientField("fat", "Fat", NutrientUnit.Gram, NutrientCategory.Macros, { it.fat }, { it.fat }),
    NutrientField("saturatedFat", "Saturated Fat", NutrientUnit.Gram, NutrientCategory.Macros, { it.saturatedFat }, { it.saturatedFat }),
    NutrientField("cholesterol", "Cholesterol", NutrientUnit.Milligram, NutrientCategory.Macros, { it.cholesterol }, { it.cholesterol }),
    NutrientField("sodium", "Sodium", NutrientUnit.Milligram, NutrientCategory.Macros, { it.sodium }, { it.sodium }),

    NutrientField("calcium", "Calcium", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.calcium }, { it.calcium }),
    NutrientField("iron", "Iron", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.iron }, { it.iron }),
    NutrientField("magnesium", "Magnesium", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.magnesium }, { it.magnesium }),
    NutrientField("potassium", "Potassium", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.potassium }, { it.potassium }),
    NutrientField("zinc", "Zinc", NutrientUnit.Milligram, NutrientCategory.Minerals, { it.zinc }, { it.zinc }),

    NutrientField("vitaminA", "Vitamin A", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminA }, { it.vitaminA }),
    NutrientField("vitaminC", "Vitamin C", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminC }, { it.vitaminC }),
    NutrientField("vitaminD", "Vitamin D", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminD }, { it.vitaminD }),
    NutrientField("vitaminE", "Vitamin E", NutrientUnit.Milligram, NutrientCategory.Vitamins, { it.vitaminE }, { it.vitaminE }),
    NutrientField("vitaminK", "Vitamin K", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminK }, { it.vitaminK }),
    NutrientField("vitaminB12", "Vitamin B12", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.vitaminB12 }, { it.vitaminB12 }),
    NutrientField("folate", "Folate", NutrientUnit.Microgram, NutrientCategory.Vitamins, { it.folate }, { it.folate }),
)
