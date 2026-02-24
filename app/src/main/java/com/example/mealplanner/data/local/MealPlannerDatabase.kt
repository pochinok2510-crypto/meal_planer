package com.example.mealplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlannerStateEntity::class,
        Ingredient::class,
        Meal::class,
        MealIngredientCrossRef::class
    ],
    version = 5,
    exportSchema = false
)
abstract class MealPlannerDatabase : RoomDatabase() {
    abstract fun plannerStateDao(): PlannerStateDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealDao(): MealDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        groupName TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meal_ingredient_cross_ref (
                        mealId INTEGER NOT NULL,
                        ingredientId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        PRIMARY KEY(mealId, ingredientId),
                        FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(ingredientId) REFERENCES ingredients(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_meal_ingredient_cross_ref_mealId ON meal_ingredient_cross_ref(mealId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_meal_ingredient_cross_ref_ingredientId ON meal_ingredient_cross_ref(ingredientId)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
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

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meal_ingredient_cross_ref (
                        mealId INTEGER NOT NULL,
                        ingredientId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        PRIMARY KEY(mealId, ingredientId),
                        FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(ingredientId) REFERENCES ingredients(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_meal_ingredient_cross_ref_mealId ON meal_ingredient_cross_ref(mealId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_meal_ingredient_cross_ref_ingredientId ON meal_ingredient_cross_ref(ingredientId)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn(tableName = "meals", columnName = "legacyIngredients")) {
                    db.execSQL("ALTER TABLE meals ADD COLUMN legacyIngredients TEXT")
                }
            }
        }

        private fun SupportSQLiteDatabase.hasColumn(tableName: String, columnName: String): Boolean {
            query(SimpleSQLiteQuery("PRAGMA table_info($tableName)")).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }

        fun getInstance(context: Context): MealPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MealPlannerDatabase::class.java,
                    "meal_planner.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
