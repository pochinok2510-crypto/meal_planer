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
        IngredientGroup::class,
        Meal::class,
        MealIngredientCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
abstract class MealPlannerDatabase : RoomDatabase() {
    abstract fun plannerStateDao(): PlannerStateDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun ingredientGroupDao(): IngredientGroupDao
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


        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ingredient_groups (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_ingredient_groups_name ON ingredient_groups(name)"
                )

                val defaultGroupId = "default-other-group"
                db.execSQL(
                    "INSERT OR IGNORE INTO ingredient_groups(id, name, createdAt) VALUES('$defaultGroupId', 'Other', strftime('%s','now') * 1000)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ingredients_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        groupId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(groupId) REFERENCES ingredient_groups(id) ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO ingredients_new(id, name, unit, groupId, createdAt)
                    SELECT id, name, unit, '$defaultGroupId', strftime('%s','now') * 1000
                    FROM ingredients
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE ingredients")
                db.execSQL("ALTER TABLE ingredients_new RENAME TO ingredients")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ingredients_name ON ingredients(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ingredients_groupId ON ingredients(groupId)")
            }
        }


        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn(tableName = "ingredient_groups", columnName = "createdAt")) {
                    db.execSQL("ALTER TABLE ingredient_groups ADD COLUMN createdAt INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)")
                }
                if (!db.hasColumn(tableName = "ingredients", columnName = "createdAt")) {
                    db.execSQL("ALTER TABLE ingredients ADD COLUMN createdAt INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            db.execSQL("INSERT OR IGNORE INTO ingredient_groups(id, name, createdAt) VALUES('default-other-group', 'Other', strftime('%s','now') * 1000)")
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
