package com.example.foodtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ServingVariant

@Entity(
    tableName = "foods",
    indices = [Index(value = ["barcode"], unique = false), Index(value = ["name"])],
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val category: String,
    @ColumnInfo(name = "serving_grams") val servingGrams: Double = 100.0,
    @ColumnInfo(name = "serving_label") val servingLabel: String = "1 serving (100 g)",
    @ColumnInfo(name = "serving_variants") val servingVariants: List<ServingVariant> = emptyList(),
    val barcode: String? = null,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean = false,
    @ColumnInfo(name = "is_favourite", defaultValue = "0") val isFavourite: Boolean = false,

    val calories: Double = 0.0,
    val carbs: Double = 0.0,
    val sugar: Double = 0.0,
    val fiber: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    @ColumnInfo(name = "saturated_fat") val saturatedFat: Double = 0.0,
    val cholesterol: Double = 0.0,
    val sodium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val potassium: Double = 0.0,
    val zinc: Double = 0.0,
    @ColumnInfo(name = "vitamin_a") val vitaminA: Double = 0.0,
    @ColumnInfo(name = "vitamin_c") val vitaminC: Double = 0.0,
    @ColumnInfo(name = "vitamin_d") val vitaminD: Double = 0.0,
    @ColumnInfo(name = "vitamin_e") val vitaminE: Double = 0.0,
    @ColumnInfo(name = "vitamin_k") val vitaminK: Double = 0.0,
    @ColumnInfo(name = "vitamin_b12") val vitaminB12: Double = 0.0,
    val folate: Double = 0.0,
) {
    /** Variants to show in pickers. Always non-empty — falls back to the canonical serving. */
    fun variantsOrDefault(): List<ServingVariant> = servingVariants.ifEmpty {
        listOf(ServingVariant(servingLabel, servingGrams))
    }

    fun nutrientsPer100g(): Nutrients = Nutrients(
        calories = calories,
        carbs = carbs,
        sugar = sugar,
        fiber = fiber,
        protein = protein,
        fat = fat,
        saturatedFat = saturatedFat,
        cholesterol = cholesterol,
        sodium = sodium,
        calcium = calcium,
        iron = iron,
        magnesium = magnesium,
        potassium = potassium,
        zinc = zinc,
        vitaminA = vitaminA,
        vitaminC = vitaminC,
        vitaminD = vitaminD,
        vitaminE = vitaminE,
        vitaminK = vitaminK,
        vitaminB12 = vitaminB12,
        folate = folate,
    )

    companion object {
        fun fromNutrientsPer100g(
            name: String,
            category: String,
            n: Nutrients,
            servingGrams: Double = 100.0,
            servingLabel: String = "1 serving (100 g)",
            servingVariants: List<ServingVariant> = emptyList(),
            brand: String? = null,
            barcode: String? = null,
            isCustom: Boolean = false,
            isFavourite: Boolean = false,
        ) = FoodEntity(
            name = name,
            brand = brand,
            category = category,
            servingGrams = servingGrams,
            servingLabel = servingLabel,
            servingVariants = servingVariants.ifEmpty {
                ServingVariant.defaultVariantsFor(servingGrams, servingLabel)
            },
            barcode = barcode,
            isCustom = isCustom,
            isFavourite = isFavourite,
            calories = n.calories,
            carbs = n.carbs,
            sugar = n.sugar,
            fiber = n.fiber,
            protein = n.protein,
            fat = n.fat,
            saturatedFat = n.saturatedFat,
            cholesterol = n.cholesterol,
            sodium = n.sodium,
            calcium = n.calcium,
            iron = n.iron,
            magnesium = n.magnesium,
            potassium = n.potassium,
            zinc = n.zinc,
            vitaminA = n.vitaminA,
            vitaminC = n.vitaminC,
            vitaminD = n.vitaminD,
            vitaminE = n.vitaminE,
            vitaminK = n.vitaminK,
            vitaminB12 = n.vitaminB12,
            folate = n.folate,
        )
    }
}
