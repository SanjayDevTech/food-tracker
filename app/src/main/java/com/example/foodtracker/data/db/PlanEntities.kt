package com.example.foodtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.StyleOption
import kotlinx.coroutines.flow.Flow

/**
 * One line of the plan: a food, a meal, and the amount the user decided on.
 *
 * **There is no `plans` table and no "active" flag.** The app holds exactly one
 * plan, so a parent row would carry a name nobody reads and an id every query
 * would have to filter on. If a second plan is ever wanted, that parent arrives
 * then; inventing it now would be a join in every read for a feature that does
 * not exist.
 *
 * The amount columns are deliberately the same four as [FoodLogEntity] —
 * `servings`, `grams`, `servingLabel`, plus the preparation pair — because
 * checking an item off copies them straight across. Any divergence here would
 * have to be reconciled at the one moment the feature exists to make fast.
 *
 * **`(food_id, meal_type)` is unique**, which is what keeps the tick marks
 * honest. An item reads as logged when today's diary holds an entry for the
 * same food in the same meal; two plan rows for rice at lunch would both answer
 * to one bowl of rice, and no amount of care at the UI layer can disambiguate
 * them afterwards. Adding a food already planned for that meal therefore
 * updates the amount rather than inserting a second row.
 */
@Entity(
    tableName = "plan_items",
    indices = [
        Index(value = ["food_id", "meal_type"], unique = true),
        Index(value = ["food_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_id") val foodId: Long,
    @ColumnInfo(name = "meal_type") val mealType: String,
    val servings: Double,
    val grams: Double,
    @ColumnInfo(name = "serving_label") val servingLabel: String,
    /** Cooking fat this item was planned in, or null for the food's own default. */
    @ColumnInfo(name = "oil") val oil: String? = null,
    /** Preparation option planned, or null for the food's default. */
    @ColumnInfo(name = "style") val style: String? = null,
    @ColumnInfo(name = "sort_index") val sortIndex: Int = 0,
)

data class PlanItemWithFood(
    @Embedded val item: PlanItemEntity,
    @Relation(parentColumn = "food_id", entityColumn = "id")
    val food: FoodEntity,
)

/**
 * What this planned item would contribute if logged exactly as written —
 * the same arithmetic [FoodLogWithFood.nutrients] does for an entry that
 * already exists, so the kcal on a plan row and the kcal on the diary row it
 * becomes cannot disagree.
 */
fun PlanItemWithFood.nutrients(): Nutrients = food.nutrientsPer100g(
    oil = CookingOil.fromName(item.oil) ?: food.defaultOil,
    style = StyleOption.find(food.styleOptions, item.style) ?: food.styleOptions.firstOrNull(),
) * (item.grams / 100.0)

@Dao
interface PlanDao {
    /**
     * The whole plan, in order.
     *
     * The join is the soft-delete rule: a food the user has hidden drops out of
     * the plan the moment it is hidden. Filtering here as well as deleting the
     * row (see `FoodRepository.deleteCustomFood`) is belt and braces — the
     * delete is what makes it permanent, this is what makes it immediate, and a
     * plan row orphaned by any path the delete does not cover still cannot
     * render a food that is no longer in the picker.
     */
    @Transaction
    @Query(
        """
        SELECT p.* FROM plan_items p
        INNER JOIN foods f ON f.id = p.food_id
        WHERE f.is_hidden = 0
        ORDER BY p.sort_index ASC
        """
    )
    fun observeAll(): Flow<List<PlanItemWithFood>>

    @Query("SELECT COUNT(*) FROM plan_items")
    fun observeCount(): Flow<Int>

    /** The row for this food in this meal, if the plan already has one. */
    @Query("SELECT * FROM plan_items WHERE food_id = :foodId AND meal_type = :mealType LIMIT 1")
    suspend fun find(foodId: Long, mealType: String): PlanItemEntity?

    @Query("SELECT COALESCE(MAX(sort_index), -1) FROM plan_items")
    suspend fun maxSortIndex(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlanItemEntity>)

    @Update
    suspend fun update(item: PlanItemEntity)

    @Query("DELETE FROM plan_items WHERE id = :id")
    suspend fun delete(id: Long)

    /** Drop every planned line that points at a food the user has just hidden. */
    @Query("DELETE FROM plan_items WHERE food_id = :foodId")
    suspend fun deleteForFood(foodId: Long)

    @Query("DELETE FROM plan_items WHERE food_id IN (SELECT id FROM foods WHERE is_custom = 1)")
    suspend fun deleteForAllCustomFoods()

    @Query("SELECT * FROM plan_items ORDER BY sort_index ASC")
    suspend fun getAll(): List<PlanItemEntity>

    @Query("DELETE FROM plan_items")
    suspend fun deleteAll()
}
