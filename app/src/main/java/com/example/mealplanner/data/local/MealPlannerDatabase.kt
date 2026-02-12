package com.example.mealplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlannerStateEntity::class], version = 1, exportSchema = false)
abstract class MealPlannerDatabase : RoomDatabase() {
    abstract fun plannerStateDao(): PlannerStateDao

    companion object {
        @Volatile
        private var INSTANCE: MealPlannerDatabase? = null

        fun getInstance(context: Context): MealPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MealPlannerDatabase::class.java,
                    "meal_planner.db"
                ).fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
