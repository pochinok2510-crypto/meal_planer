package com.example.mealplanner.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealWithIngredients(mealId: Long): Flow<MealWithIngredients>
}
