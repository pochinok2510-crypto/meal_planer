package com.example.mealplanner.data

import android.content.Context
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.model.Meal
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.UUID

class MealsRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveState(state: PlannerState) {
        val serialized = gson.toJson(state)
        sharedPreferences.edit().putString(KEY_STATE, serialized).apply()
    }

    fun loadState(): PlannerState {
        val serialized = sharedPreferences.getString(KEY_STATE, null)
            ?: return PlannerState(groups = DEFAULT_GROUPS)

        return runCatching {
            val json = JsonParser.parseString(serialized)
            if (json.isJsonArray) {
                val type = object : TypeToken<List<LegacyMeal>>() {}.type
                val legacyMeals: List<LegacyMeal> = gson.fromJson(serialized, type) ?: emptyList()
                PlannerState(
                    meals = legacyMeals.map {
                        Meal(
                            id = UUID.randomUUID().toString(),
                            name = it.name,
                            group = DEFAULT_GROUPS.first(),
                            ingredients = it.ingredients
                        )
                    },
                    groups = DEFAULT_GROUPS
                )
            } else {
                val type = object : TypeToken<PlannerState>() {}.type
                val state: PlannerState = gson.fromJson(serialized, type)
                state.copy(
                    groups = (DEFAULT_GROUPS + state.groups).distinct(),
                    purchasedIngredientKeys = state.purchasedIngredientKeys.distinct()
                )
            }
        }.getOrElse {
            PlannerState(groups = DEFAULT_GROUPS)
        }
    }

    companion object {
        private const val PREFS_NAME = "meal_planner_data"
        private const val KEY_STATE = "planner_state"
        val DEFAULT_GROUPS = listOf("Завтрак", "Перекус", "Обед", "Ужин", "Десерт")
    }
}

data class PlannerState(
    val meals: List<Meal> = emptyList(),
    val groups: List<String> = MealsRepository.DEFAULT_GROUPS,
    val purchasedIngredientKeys: List<String> = emptyList()
)

private data class LegacyMeal(
    val name: String,
    val ingredients: List<Ingredient>
)
