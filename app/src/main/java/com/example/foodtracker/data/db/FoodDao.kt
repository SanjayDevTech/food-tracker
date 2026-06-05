package com.example.foodtracker.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class FoodLogWithFood(
    @Embedded val log: FoodLogEntity,
    @Relation(parentColumn = "food_id", entityColumn = "id")
    val food: FoodEntity,
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR brand LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
          CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
          name COLLATE NOCASE ASC
        LIMIT 100
        """
    )
    fun search(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE is_favourite = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeFavourites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE name = :name COLLATE NOCASE AND is_custom = 0 LIMIT 1")
    suspend fun findSeededByName(name: String): FoodEntity?

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(foods: List<FoodEntity>)

    @Update
    suspend fun update(food: FoodEntity)

    @Query("UPDATE foods SET is_favourite = :favourite WHERE id = :id")
    suspend fun setFavourite(id: Long, favourite: Boolean)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM foods")
    suspend fun getAll(): List<FoodEntity>

    @Query("DELETE FROM foods")
    suspend fun deleteAll()
}

@Dao
interface FoodLogDao {
    @Transaction
    @Query("SELECT * FROM food_log WHERE date_epoch_day = :epochDay ORDER BY logged_at_millis ASC")
    fun observeForDay(epochDay: Long): Flow<List<FoodLogWithFood>>

    @Transaction
    @Query("SELECT * FROM food_log WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY date_epoch_day ASC, logged_at_millis ASC")
    fun observeForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<FoodLogWithFood>>

    /** Recently logged foods, most recent first, deduplicated. */
    @Transaction
    @Query(
        """
        SELECT f.* FROM foods f
        INNER JOIN (
            SELECT food_id, MAX(logged_at_millis) AS last_used
            FROM food_log
            GROUP BY food_id
        ) recent ON recent.food_id = f.id
        ORDER BY recent.last_used DESC
        LIMIT :limit
        """
    )
    fun observeRecentFoods(limit: Int): Flow<List<FoodEntity>>

    /** Most-logged foods over the last `days` days. */
    @Transaction
    @Query(
        """
        SELECT f.* FROM foods f
        INNER JOIN (
            SELECT food_id, COUNT(*) AS uses
            FROM food_log
            WHERE date_epoch_day >= :sinceEpochDay
            GROUP BY food_id
        ) freq ON freq.food_id = f.id
        ORDER BY freq.uses DESC, f.name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeFrequentFoods(sinceEpochDay: Long, limit: Int): Flow<List<FoodEntity>>

    @Insert
    suspend fun insert(entry: FoodLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodLogEntity>)

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM food_log")
    suspend fun getAll(): List<FoodLogEntity>

    @Query("DELETE FROM food_log")
    suspend fun deleteAll()
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_log WHERE date_epoch_day = :epochDay ORDER BY logged_at_millis ASC")
    fun observeForDay(epochDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(amount_ml), 0) FROM water_log WHERE date_epoch_day = :epochDay")
    fun observeTotalForDay(epochDay: Long): Flow<Int>

    @Query("SELECT date_epoch_day AS dateEpochDay, COALESCE(SUM(amount_ml), 0) AS totalMl FROM water_log WHERE date_epoch_day BETWEEN :start AND :end GROUP BY date_epoch_day")
    fun observeTotalsForRange(start: Long, end: Long): Flow<List<DailyWaterTotal>>

    @Insert
    suspend fun insert(entry: WaterLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<WaterLogEntity>)

    @Query("DELETE FROM water_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM water_log")
    suspend fun getAll(): List<WaterLogEntity>

    @Query("DELETE FROM water_log")
    suspend fun deleteAll()
}

data class DailyWaterTotal(val dateEpochDay: Long, val totalMl: Int)

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_log WHERE date_epoch_day = :epochDay ORDER BY logged_at_millis ASC")
    fun observeForDay(epochDay: Long): Flow<List<ActivityLogEntity>>

    @Query("SELECT COALESCE(SUM(calories_burned), 0.0) FROM activity_log WHERE date_epoch_day = :epochDay")
    fun observeCaloriesBurnedForDay(epochDay: Long): Flow<Double>

    @Query("SELECT date_epoch_day AS dateEpochDay, COALESCE(SUM(calories_burned), 0.0) AS totalCalories FROM activity_log WHERE date_epoch_day BETWEEN :start AND :end GROUP BY date_epoch_day")
    fun observeCaloriesForRange(start: Long, end: Long): Flow<List<DailyCaloriesTotal>>

    @Insert
    suspend fun insert(entry: ActivityLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ActivityLogEntity>)

    @Query("DELETE FROM activity_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM activity_log")
    suspend fun getAll(): List<ActivityLogEntity>

    @Query("DELETE FROM activity_log")
    suspend fun deleteAll()
}

data class DailyCaloriesTotal(val dateEpochDay: Long, val totalCalories: Double)

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_log ORDER BY date_epoch_day DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log WHERE date_epoch_day BETWEEN :start AND :end ORDER BY date_epoch_day ASC")
    fun observeForRange(start: Long, end: Long): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY date_epoch_day DESC LIMIT 1")
    fun observeLatest(): Flow<WeightLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<WeightLogEntity>)

    @Query("DELETE FROM weight_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM weight_log")
    suspend fun getAll(): List<WeightLogEntity>

    @Query("DELETE FROM weight_log")
    suspend fun deleteAll()
}
