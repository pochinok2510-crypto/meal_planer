package com.example.mealplanner.data

import android.content.Context
import androidx.room.withTransaction
import com.example.mealplanner.data.local.IngredientDao
import com.example.mealplanner.data.local.MealIngredientCrossRef
import com.example.mealplanner.data.local.MealIngredientRow
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MealsRepository(context: Context) {

    private val db = MealPlannerDatabase.getInstance(context)
    private val mealDao = db.mealDao()
    private val ingredientDao: IngredientDao = db.ingredientDao()
    private val plannerStateDao = db.plannerStateDao()
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun observeMeals(): Flow<List<Meal>> {
        return mealDao.observeMealIngredientRows().map { rows -> rows.toDomainMeals() }
    }

    suspend fun addMeal(name: String, group: String, ingredients: List<Ingredient>) {
        db.withTransaction {
            insertMealInternal(name = name, group = group, ingredients = ingredients)
        }
    }


    private suspend fun insertMealInternal(name: String, group: String, ingredients: List<Ingredient>) {
        val mealId = mealDao.insertMeal(
            com.example.mealplanner.data.local.Meal(
                name = name,
                groupName = group,
                legacyIngredients = null
            )
        )
        val crossRefs = ingredients.mapNotNull { ingredient ->
            val normalizedName = ingredient.name.trim()
            val normalizedUnit = ingredient.unit.trim()
            if (normalizedName.isBlank() || normalizedUnit.isBlank()) return@mapNotNull null
            val ingredientId = findOrCreateIngredientId(normalizedName, normalizedUnit)
            MealIngredientCrossRef(
                mealId = mealId,
                ingredientId = ingredientId,
                quantity = ingredient.amount
            )
        }
        mealDao.insertCrossRefs(crossRefs)
    }

    suspend fun moveMealToGroup(mealId: String, group: String) {
        mealId.toDbMealIdOrNull()?.let { dbId -> mealDao.updateMealGroup(dbId, group) }
    }

    suspend fun removeMeal(mealId: String) {
        mealId.toDbMealIdOrNull()?.let { dbId -> mealDao.deleteMealById(dbId) }
    }

    suspend fun duplicateMealToGroup(mealId: String, targetGroup: String) {
        val sourceMealId = mealId.toDbMealIdOrNull() ?: return
        db.withTransaction {
            val sourceMeal = mealDao.getAllMealsOnce().firstOrNull { it.id == sourceMealId } ?: return@withTransaction
            val sourceRefs = mealDao.getCrossRefsForMeal(sourceMealId)
            val duplicatedMealId = mealDao.insertMeal(
                sourceMeal.copy(id = 0, groupName = targetGroup, legacyIngredients = null)
            )
            mealDao.insertCrossRefs(sourceRefs.map { it.copy(mealId = duplicatedMealId) })
        }
    }

    suspend fun saveState(state: PlannerState) = withContext(Dispatchers.IO) {
        val entity = PlannerStateEntity(
            mealsJson = "[]",
            groupsJson = gson.toJson(state.groups),
            purchasedIngredientKeysJson = gson.toJson(state.purchasedIngredientKeys),
            weeklyPlanJson = gson.toJson(state.weeklyPlan),
            dayCount = state.dayMultiplier
        )
        plannerStateDao.upsert(entity)
    }

    suspend fun loadState(): PlannerState = withContext(Dispatchers.IO) {
        val dbMeals = getMealsSnapshot()
        plannerStateDao.getById()?.let { entity ->
            val mealsFromEntity = gson.fromJson(entity.mealsJson, object : TypeToken<List<Meal>>() {}.type) ?: emptyList()
            val effectiveMeals = if (dbMeals.isEmpty() && mealsFromEntity.isNotEmpty()) {
                migrateLegacyMealsToDatabase(mealsFromEntity).also {
                    plannerStateDao.upsert(entity.copy(mealsJson = "[]"))
                }
            } else {
                dbMeals
            }

            return@withContext PlannerState(
                meals = effectiveMeals,
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

        return@withContext migrateFromLegacySharedPreferences(dbMeals)
    }

    private suspend fun migrateFromLegacySharedPreferences(currentDbMeals: List<Meal>): PlannerState {
        val serialized = sharedPreferences.getString(KEY_STATE, null)
            ?: return PlannerState(groups = DEFAULT_GROUPS, meals = currentDbMeals)

        val migrated = runCatching {
            val json = JsonParser.parseString(serialized)
            if (json.isJsonArray) {
                val type = object : TypeToken<List<LegacyMeal>>() {}.type
                val legacyMeals: List<LegacyMeal> = gson.fromJson(serialized, type) ?: emptyList()
                val meals = legacyMeals.map {
                    Meal(
                        id = UUID.randomUUID().toString(),
                        name = it.name,
                        group = DEFAULT_GROUPS.first(),
                        ingredients = it.ingredients
                    )
                }
                PlannerState(
                    meals = migrateLegacyMealsToDatabase(meals),
                    groups = DEFAULT_GROUPS
                )
            } else {
                val type = object : TypeToken<LegacyPlannerState>() {}.type
                val state: LegacyPlannerState = gson.fromJson(serialized, type)
                val migratedMeals = migrateLegacyMealsToDatabase(state.meals)
                val idMap = migratedMeals.mapNotNull { migratedMeal ->
                    state.meals.firstOrNull { it.name == migratedMeal.name && it.group == migratedMeal.group }?.id?.let { oldId ->
                        oldId to migratedMeal.id
                    }
                }.toMap()
                PlannerState(
                    meals = migratedMeals,
                    groups = normalizeGroups(state.groups),
                    purchasedIngredientKeys = state.purchasedIngredientKeys.distinct(),
                    weeklyPlan = state.weeklyPlan.mapNotNull { assignment ->
                        val newMealId = idMap[assignment.mealId] ?: return@mapNotNull null
                        assignment.copy(mealId = newMealId)
                    },
                    dayMultiplier = state.dayCount.coerceIn(1, 30)
                )
            }
        }.getOrElse {
            PlannerState(groups = DEFAULT_GROUPS, meals = currentDbMeals)
        }

        saveState(migrated)
        sharedPreferences.edit().remove(KEY_STATE).apply()
        return migrated
    }

    private fun normalizeGroups(groups: List<String>): List<String> {
        return (DEFAULT_GROUPS + groups + UNCATEGORIZED_GROUP).distinct()
    }

    private suspend fun migrateLegacyMealsToDatabase(legacyMeals: List<Meal>): List<Meal> {
        if (legacyMeals.isEmpty()) return getMealsSnapshot()
        db.withTransaction {
            if (mealDao.getAllMealsOnce().isNotEmpty()) return@withTransaction
            legacyMeals.forEach { legacy ->
                insertMealInternal(legacy.name.trim(), legacy.group.trim().ifBlank { UNCATEGORIZED_GROUP }, legacy.ingredients)
            }
        }
        return getMealsSnapshot()
    }

    private suspend fun getMealsSnapshot(): List<Meal> {
        return mealDao.observeMealIngredientRows().map { it.toDomainMeals() }.first()
    }

    private suspend fun findOrCreateIngredientId(name: String, unit: String): Long {
        val existing = ingredientDao.findByName(name)
        if (existing != null) return existing.id
        val inserted = ingredientDao.insertOrIgnoreByNameCaseInsensitive(
            com.example.mealplanner.data.local.Ingredient(name = name, unit = unit)
        )
        if (inserted > 0L) return inserted
        return ingredientDao.findByName(name)?.id ?: throw IllegalStateException("Cannot resolve ingredient id for $name")
    }

    private fun List<MealIngredientRow>.toDomainMeals(): List<Meal> {
        return groupBy { it.mealId }.values.map { rows ->
            val first = rows.first()
            Meal(
                id = first.mealId.toDomainMealId(),
                name = first.mealName,
                group = first.groupName,
                ingredients = rows.mapNotNull { row ->
                    val ingredientName = row.ingredientName ?: return@mapNotNull null
                    val ingredientUnit = row.ingredientUnit ?: return@mapNotNull null
                    val quantity = row.quantity ?: return@mapNotNull null
                    Ingredient(name = ingredientName, amount = quantity, unit = ingredientUnit)
                }
            )
        }
    }

    private fun Long.toDomainMealId(): String = "db_$this"

    private fun String.toDbMealIdOrNull(): Long? {
        return removePrefix("db_").toLongOrNull()
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

