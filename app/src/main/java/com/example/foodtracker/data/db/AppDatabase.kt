package com.example.foodtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodEntity::class,
        FoodLogEntity::class,
        WaterLogEntity::class,
        ActivityLogEntity::class,
        WeightLogEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun weightLogDao(): WeightLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * v4 → v5: adds four mineral columns to `foods` (phosphorus, selenium,
         * copper, manganese). A real ALTER-based migration — NOT destructive —
         * so existing foods, custom foods, and the entire food log survive the
         * update. The columns have no `@ColumnInfo(defaultValue=...)`, so Room
         * doesn't validate their DB default; `DEFAULT 0` just satisfies SQLite's
         * NOT NULL requirement for the backfill of existing rows.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN phosphorus REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN selenium REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN copper REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN manganese REAL NOT NULL DEFAULT 0")
            }
        }

        /**
         * v5 → v6: adds four B-complex vitamin columns to `foods` (B1/thiamin,
         * B2/riboflavin, B3/niacin, B6). Non-destructive ALTER, same rationale
         * as [MIGRATION_4_5] — all foods and the food log survive the update.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN vitamin_b1 REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN vitamin_b2 REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN vitamin_b3 REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN vitamin_b6 REAL NOT NULL DEFAULT 0")
            }
        }

        /**
         * v6 → v7: adds the `omega3` column to `foods` (grams of omega-3 fatty
         * acids). Non-destructive ALTER, same rationale as [MIGRATION_4_5] — all
         * foods and the food log survive the update.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN omega3 REAL NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "food_tracker.db",
            )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                // Scoped to the pre-release versions that never had migrations
                // written. Deliberately NOT the blanket
                // fallbackToDestructiveMigration: a *future* forgotten
                // migration must fail loudly rather than silently dropping
                // every food, water, activity and weight log the user has ever
                // recorded. A crash is recoverable by shipping the migration;
                // the data is not.
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                .build()
                .also { instance = it }
        }
    }
}
