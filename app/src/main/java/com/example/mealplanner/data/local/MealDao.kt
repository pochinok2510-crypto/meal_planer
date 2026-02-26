package com.example.mealplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal): Long

    @Query("UPDATE meals SET groupName = :groupName WHERE id = :mealId")
    suspend fun updateMealGroup(mealId: Long, groupName: String)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<MealIngredientCrossRef>)

    @Query("DELETE FROM meal_ingredient_cross_ref WHERE mealId = :mealId")
    suspend fun deleteCrossRefsForMeal(mealId: Long)

    @Query("SELECT * FROM meals")
    suspend fun getAllMealsOnce(): List<Meal>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealWithIngredients(mealId: Long): Flow<MealWithIngredients>

    @Transaction
    @Query(
        """
        SELECT
            m.id AS mealId,
            m.name AS mealName,
            m.groupName AS groupName,
            i.name AS ingredientName,
            i.unit AS ingredientUnit,
            c.quantity AS quantity
        FROM meals m
        LEFT JOIN meal_ingredient_cross_ref c ON c.mealId = m.id
        LEFT JOIN ingredients i ON i.id = c.ingredientId
        ORDER BY m.name COLLATE NOCASE ASC
        """
    )
    fun observeMealIngredientRows(): Flow<List<MealIngredientRow>>

    @Query("SELECT * FROM meal_ingredient_cross_ref WHERE mealId = :mealId")
    suspend fun getCrossRefsForMeal(mealId: Long): List<MealIngredientCrossRef>

    @Query("SELECT * FROM meal_ingredient_cross_ref")
    suspend fun getAllCrossRefsOnce(): List<MealIngredientCrossRef>
}

data class MealIngredientRow(
    val mealId: Long,
    val mealName: String,
    val groupName: String,
    val ingredientName: String?,
    val ingredientUnit: String?,
    val quantity: Double?
)
