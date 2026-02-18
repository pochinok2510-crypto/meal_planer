package com.example.mealplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlannerStateEntity::class, Ingredient::class],
    version = 2,
    exportSchema = false
)
abstract class MealPlannerDatabase : RoomDatabase() {
    abstract fun plannerStateDao(): PlannerStateDao
    abstract fun ingredientDao(): IngredientDao

    companion object {
        @Volatile
        private var INSTANCE: MealPlannerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ingredients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        category TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_ingredients_name ON ingredients(name)"
                )
            }
        }

        fun getInstance(context: Context): MealPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MealPlannerDatabase::class.java,
                    "meal_planner.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
