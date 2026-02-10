package com.example.mealplanner.data

import android.content.Context
import com.example.mealplanner.model.Meal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MealsRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveMeals(meals: List<Meal>) {
        val serialized = gson.toJson(meals)
        sharedPreferences.edit().putString(KEY_MEALS, serialized).apply()
    }

    fun loadMeals(): List<Meal> {
        val serialized = sharedPreferences.getString(KEY_MEALS, null) ?: return emptyList()
        val type = object : TypeToken<List<Meal>>() {}.type
        return gson.fromJson(serialized, type) ?: emptyList()
    }

    companion object {
        private const val PREFS_NAME = "meal_planner_data"
        private const val KEY_MEALS = "meals"
    }
}
