package com.example.foodtracker.data.backup

import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.Sex
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.ActivityLogEntity
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.data.db.WaterLogEntity
import com.example.foodtracker.data.db.WeightLogEntity
import com.example.foodtracker.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

/**
 * One self-contained snapshot of every persistent thing the app cares about.
 * Written to disk as JSON for both manual export/import and in-app backup
 * slots — the same shape powers both, just different destinations.
 *
 * The schema version lets us evolve the payload safely: importing an older
 * snapshot is best-effort and missing fields fall back to their defaults.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int = SCHEMA_VERSION,
    val createdAtMillis: Long,
    val appVersion: String,
    val profile: ProfileSnapshot,
    val foods: List<FoodSnapshot>,
    val foodLog: List<FoodLogSnapshot>,
    val waterLog: List<WaterLogSnapshot>,
    val activityLog: List<ActivityLogSnapshot>,
    val weightLog: List<WeightLogSnapshot>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class ProfileSnapshot(
    val name: String = "",
    val ageYears: Int = 25,
    val sex: String = Sex.Male.name,
    val heightCm: Int = 170,
    val weightKg: Double = 65.0,
    val activityLevel: String = ActivityLevel.Moderate.name,
    val goal: String = Goal.Maintain.name,
    val hapticsEnabled: Boolean = true,
    val themeMode: String = ThemeMode.Amoled.name,
) {
    fun toUserProfile() = UserProfile(
        name = name,
        ageYears = ageYears,
        sex = Sex.fromName(sex),
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = ActivityLevel.fromName(activityLevel),
        goal = Goal.fromName(goal),
    )

    companion object {
        fun from(profile: UserProfile, haptics: Boolean, theme: ThemeMode) = ProfileSnapshot(
            name = profile.name,
            ageYears = profile.ageYears,
            sex = profile.sex.name,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            activityLevel = profile.activityLevel.name,
            goal = profile.goal.name,
            hapticsEnabled = haptics,
            themeMode = theme.name,
        )
    }
}

@Serializable
data class FoodSnapshot(
    val id: Long,
    val name: String,
    val brand: String? = null,
    val category: String,
    val servingGrams: Double,
    val servingLabel: String,
    val servingVariants: List<ServingVariant> = emptyList(),
    val barcode: String? = null,
    val isCustom: Boolean = false,
    val isFavourite: Boolean = false,
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
    fun toEntity() = FoodEntity(
        id = id,
        name = name,
        brand = brand,
        category = category,
        servingGrams = servingGrams,
        servingLabel = servingLabel,
        servingVariants = servingVariants,
        barcode = barcode,
        isCustom = isCustom,
        isFavourite = isFavourite,
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
        fun from(e: FoodEntity) = FoodSnapshot(
            id = e.id,
            name = e.name,
            brand = e.brand,
            category = e.category,
            servingGrams = e.servingGrams,
            servingLabel = e.servingLabel,
            servingVariants = e.servingVariants,
            barcode = e.barcode,
            isCustom = e.isCustom,
            isFavourite = e.isFavourite,
            calories = e.calories,
            carbs = e.carbs,
            sugar = e.sugar,
            fiber = e.fiber,
            protein = e.protein,
            fat = e.fat,
            saturatedFat = e.saturatedFat,
            cholesterol = e.cholesterol,
            sodium = e.sodium,
            calcium = e.calcium,
            iron = e.iron,
            magnesium = e.magnesium,
            potassium = e.potassium,
            zinc = e.zinc,
            vitaminA = e.vitaminA,
            vitaminC = e.vitaminC,
            vitaminD = e.vitaminD,
            vitaminE = e.vitaminE,
            vitaminK = e.vitaminK,
            vitaminB12 = e.vitaminB12,
            folate = e.folate,
        )
    }
}

@Serializable
data class FoodLogSnapshot(
    val id: Long,
    val foodId: Long,
    val dateEpochDay: Long,
    val mealType: String,
    val servings: Double,
    val grams: Double,
    val servingLabel: String = "",
    val loggedAtMillis: Long,
) {
    fun toEntity() = FoodLogEntity(
        id = id,
        foodId = foodId,
        dateEpochDay = dateEpochDay,
        mealType = mealType,
        servings = servings,
        grams = grams,
        servingLabel = servingLabel,
        loggedAtMillis = loggedAtMillis,
    )

    companion object {
        fun from(e: FoodLogEntity) = FoodLogSnapshot(
            id = e.id,
            foodId = e.foodId,
            dateEpochDay = e.dateEpochDay,
            mealType = e.mealType,
            servings = e.servings,
            grams = e.grams,
            servingLabel = e.servingLabel,
            loggedAtMillis = e.loggedAtMillis,
        )
    }
}

@Serializable
data class WaterLogSnapshot(
    val id: Long,
    val dateEpochDay: Long,
    val amountMl: Int,
    val loggedAtMillis: Long,
) {
    fun toEntity() = WaterLogEntity(
        id = id,
        dateEpochDay = dateEpochDay,
        amountMl = amountMl,
        loggedAtMillis = loggedAtMillis,
    )

    companion object {
        fun from(e: WaterLogEntity) = WaterLogSnapshot(
            id = e.id,
            dateEpochDay = e.dateEpochDay,
            amountMl = e.amountMl,
            loggedAtMillis = e.loggedAtMillis,
        )
    }
}

@Serializable
data class ActivityLogSnapshot(
    val id: Long,
    val dateEpochDay: Long,
    val activityType: String,
    val minutes: Int,
    val caloriesBurned: Double,
    val loggedAtMillis: Long,
) {
    fun toEntity() = ActivityLogEntity(
        id = id,
        dateEpochDay = dateEpochDay,
        activityType = activityType,
        minutes = minutes,
        caloriesBurned = caloriesBurned,
        loggedAtMillis = loggedAtMillis,
    )

    companion object {
        fun from(e: ActivityLogEntity) = ActivityLogSnapshot(
            id = e.id,
            dateEpochDay = e.dateEpochDay,
            activityType = e.activityType,
            minutes = e.minutes,
            caloriesBurned = e.caloriesBurned,
            loggedAtMillis = e.loggedAtMillis,
        )
    }
}

@Serializable
data class WeightLogSnapshot(
    val id: Long,
    val dateEpochDay: Long,
    val weightKg: Double,
    val note: String? = null,
    val loggedAtMillis: Long,
) {
    fun toEntity() = WeightLogEntity(
        id = id,
        dateEpochDay = dateEpochDay,
        weightKg = weightKg,
        note = note,
        loggedAtMillis = loggedAtMillis,
    )

    companion object {
        fun from(e: WeightLogEntity) = WeightLogSnapshot(
            id = e.id,
            dateEpochDay = e.dateEpochDay,
            weightKg = e.weightKg,
            note = e.note,
            loggedAtMillis = e.loggedAtMillis,
        )
    }
}
