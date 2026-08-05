package com.example.foodtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_log",
    indices = [Index(value = ["date_epoch_day"]), Index(value = ["food_id"])],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_id") val foodId: Long,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "meal_type") val mealType: String,
    val servings: Double,
    @ColumnInfo(name = "grams") val grams: Double,
    @ColumnInfo(name = "serving_label", defaultValue = "") val servingLabel: String = "",
    @ColumnInfo(name = "logged_at_millis") val loggedAtMillis: Long,
)

@Entity(
    tableName = "water_log",
    indices = [Index(value = ["date_epoch_day"])],
)
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "amount_ml") val amountMl: Int,
    @ColumnInfo(name = "logged_at_millis") val loggedAtMillis: Long,
)

@Entity(
    tableName = "activity_log",
    indices = [Index(value = ["date_epoch_day"])],
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "activity_type") val activityType: String,
    val minutes: Int,
    @ColumnInfo(name = "calories_burned") val caloriesBurned: Double,
    @ColumnInfo(name = "logged_at_millis") val loggedAtMillis: Long,
)

@Entity(
    tableName = "weight_log",
    indices = [Index(value = ["date_epoch_day"], unique = true)],
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    val note: String? = null,
    @ColumnInfo(name = "logged_at_millis") val loggedAtMillis: Long,
)
