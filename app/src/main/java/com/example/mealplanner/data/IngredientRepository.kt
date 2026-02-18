package com.example.mealplanner.data

import android.content.Context
import com.example.mealplanner.data.local.Ingredient
import com.example.mealplanner.data.local.MealPlannerDatabase
import kotlinx.coroutines.flow.Flow

class IngredientRepository(context: Context) {
    private val ingredientDao = MealPlannerDatabase.getInstance(context).ingredientDao()

    fun getAllIngredients(): Flow<List<Ingredient>> = ingredientDao.getAll()

    suspend fun findByName(name: String): Ingredient? = ingredientDao.findByName(name.trim())

    suspend fun insert(ingredient: Ingredient): Long {
        return ingredientDao.insert(
            ingredient.copy(name = ingredient.name.trim(), unit = ingredient.unit.trim())
        )
    }

    suspend fun insertOrIgnore(ingredient: Ingredient): Long {
        return ingredientDao.insertOrIgnoreByNameCaseInsensitive(
            ingredient.copy(name = ingredient.name.trim(), unit = ingredient.unit.trim())
        )
    }

    suspend fun delete(ingredient: Ingredient) {
        ingredientDao.delete(ingredient)
    }
}
