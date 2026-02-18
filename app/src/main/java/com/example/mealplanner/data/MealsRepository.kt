package com.example.mealplanner.data

import android.content.Context
import com.example.mealplanner.data.local.MealPlannerDatabase
import com.example.mealplanner.data.local.PlannerStateEntity
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.model.Meal
import com.example.mealplanner.model.WeeklyPlanAssignment
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealsRepository(context: Context) {

    private val dao = MealPlannerDatabase.getInstance(context).plannerStateDao()
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun saveState(state: PlannerState) = withContext(Dispatchers.IO) {
        val entity = PlannerStateEntity(
            mealsJson = gson.toJson(state.meals),
            groupsJson = gson.toJson(state.groups),
            purchasedIngredientKeysJson = gson.toJson(state.purchasedIngredientKeys),
            weeklyPlanJson = gson.toJson(state.weeklyPlan),
            dayCount = state.dayMultiplier
        )
        dao.upsert(entity)
    }

    suspend fun loadState(): PlannerState = withContext(Dispatchers.IO) {
        dao.getById()?.let { entity ->
            return@withContext PlannerState(
                meals = gson.fromJson(entity.mealsJson, object : TypeToken<List<Meal>>() {}.type) ?: emptyList(),
                groups = normalizeGroups(
                    gson.fromJson(entity.groupsJson, object : TypeToken<List<String>>() {}.type)
                        ?: DEFAULT_GROUPS
                ),
                purchasedIngredientKeys = (gson.fromJson(entity.purchasedIngredientKeysJson, object : TypeToken<List<String>>() {}.type)
                    ?: emptyList<String>()).distinct(),
                weeklyPlan = gson.fromJson(entity.weeklyPlanJson, object : TypeToken<List<WeeklyPlanAssignment>>() {}.type)
                    ?: emptyList<WeeklyPlanAssignment>(),
                dayMultiplier = entity.dayCount.coerceIn(1, 30)
            )
        }

        return@withContext migrateFromLegacySharedPreferences()
    }

    private suspend fun migrateFromLegacySharedPreferences(): PlannerState {
        val serialized = sharedPreferences.getString(KEY_STATE, null)
            ?: return PlannerState(groups = DEFAULT_GROUPS)

        val migrated = runCatching {
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
                val type = object : TypeToken<LegacyPlannerState>() {}.type
                val state: LegacyPlannerState = gson.fromJson(serialized, type)
                PlannerState(
                    meals = state.meals,
                    groups = normalizeGroups(state.groups),
                    purchasedIngredientKeys = state.purchasedIngredientKeys.distinct(),
                    weeklyPlan = state.weeklyPlan,
                    dayMultiplier = state.dayCount.coerceIn(1, 30)
                )
            }
        }.getOrElse {
            PlannerState(groups = DEFAULT_GROUPS)
        }

        saveState(migrated)
        sharedPreferences.edit().remove(KEY_STATE).apply()
        return migrated
    }

    private fun normalizeGroups(groups: List<String>): List<String> {
        return (DEFAULT_GROUPS + groups + UNCATEGORIZED_GROUP).distinct()
    }

    companion object {
        private const val PREFS_NAME = "meal_planner_data"
        private const val KEY_STATE = "planner_state"
        const val UNCATEGORIZED_GROUP = "Без категории"
        val DEFAULT_GROUPS = listOf("Завтрак", "Перекус", "Обед", "Ужин", "Десерт")
    }
}

data class PlannerState(
    val meals: List<Meal> = emptyList(),
    val groups: List<String> = MealsRepository.DEFAULT_GROUPS,
    val purchasedIngredientKeys: List<String> = emptyList(),
    val weeklyPlan: List<WeeklyPlanAssignment> = emptyList(),
    val dayMultiplier: Int = 1
)

private data class LegacyMeal(
    val name: String,
    val ingredients: List<Ingredient>
)

private data class LegacyPlannerState(
    val meals: List<Meal> = emptyList(),
    val groups: List<String> = MealsRepository.DEFAULT_GROUPS,
    val purchasedIngredientKeys: List<String> = emptyList(),
    val weeklyPlan: List<WeeklyPlanAssignment> = emptyList(),
    val dayCount: Int = 1
)
