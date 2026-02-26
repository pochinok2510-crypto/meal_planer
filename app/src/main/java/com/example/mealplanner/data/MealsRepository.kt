package com.example.mealplanner.data

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.example.mealplanner.data.local.IngredientDao
import com.example.mealplanner.data.local.IngredientGroupDao
import com.example.mealplanner.data.local.MealIngredientCrossRef
import com.example.mealplanner.data.local.MealIngredientRow
import com.example.mealplanner.data.local.MealPlannerDatabase
import com.example.mealplanner.data.local.PlannerStateEntity
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.model.Meal
import com.example.mealplanner.model.WeeklyPlanAssignment
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
    private val ingredientGroupDao: IngredientGroupDao = db.ingredientGroupDao()
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



    suspend fun buildDatabaseExportPayload(): DatabaseExportPayload = withContext(Dispatchers.IO) {
        val meals = mealDao.getAllMealsOnce()
        val ingredients = ingredientDao.getAllOnce()
        val crossRefs = mealDao.getAllCrossRefsOnce()
        val plannerState = plannerStateDao.getById()

        val parsedGroups = plannerState?.groupsJson?.let { groupsJson ->
            gson.fromJson<List<String>>(groupsJson, object : TypeToken<List<String>>() {}.type)
        } ?: DEFAULT_GROUPS

        val parsedPurchased = plannerState?.purchasedIngredientKeysJson?.let { purchasedJson ->
            gson.fromJson<List<String>>(purchasedJson, object : TypeToken<List<String>>() {}.type)
        } ?: emptyList()

        val parsedWeeklyPlan = plannerState?.weeklyPlanJson?.let { weeklyPlanJson ->
            gson.fromJson<List<WeeklyPlanAssignment>>(weeklyPlanJson, object : TypeToken<List<WeeklyPlanAssignment>>() {}.type)
        } ?: emptyList()

        DatabaseExportPayload(
            meals = meals,
            ingredients = ingredients,
            crossRefs = crossRefs,
            planner = PlannerExportPayload(
                purchasedIngredientKeys = parsedPurchased,
                weeklyPlan = parsedWeeklyPlan,
                dayCount = plannerState?.dayCount?.coerceIn(1, 30) ?: 1
            ),
            groups = normalizeGroups(parsedGroups)
        )
    }

    suspend fun importDatabaseFromJson(rawJson: String, overwritePlanner: Boolean): DatabaseImportResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = parseImportPayload(rawJson)
                val existingState = loadState()
                db.withTransaction {
                    val ingredientIdRemap = payload.ingredients.associate { ingredient ->
                        ingredient.id to findOrCreateIngredientId(ingredient.name.trim(), ingredient.unit.trim())
                    }

                    val mealIdRemap = mutableMapOf<Long, Long>()
                    payload.meals.forEach { meal ->
                        val insertedMealId = mealDao.insertMeal(
                            meal.copy(
                                id = 0,
                                name = meal.name.trim(),
                                groupName = meal.groupName.trim().ifBlank { UNCATEGORIZED_GROUP },
                                legacyIngredients = null
                            )
                        )
                        mealIdRemap[meal.id] = insertedMealId
                    }

                    val crossRefsToInsert = payload.crossRefs.mapNotNull { crossRef ->
                        val newMealId = mealIdRemap[crossRef.mealId] ?: return@mapNotNull null
                        val newIngredientId = ingredientIdRemap[crossRef.ingredientId] ?: return@mapNotNull null
                        MealIngredientCrossRef(
                            mealId = newMealId,
                            ingredientId = newIngredientId,
                            quantity = crossRef.quantity
                        )
                    }
                    mealDao.insertCrossRefs(crossRefsToInsert)

                    val importedAssignments = payload.planner.weeklyPlan.mapNotNull { assignment ->
                        val oldMealId = assignment.mealId.toDbMealIdOrNull() ?: return@mapNotNull null
                        val remappedMealId = mealIdRemap[oldMealId] ?: return@mapNotNull null
                        assignment.copy(mealId = remappedMealId.toDomainMealId())
                    }

                    val mergedWeeklyPlan = if (overwritePlanner) {
                        importedAssignments
                    } else {
                        (existingState.weeklyPlan + importedAssignments)
                            .distinctBy { assignment -> assignment.day to assignment.slot }
                    }

                    val mergedPurchasedKeys = if (overwritePlanner) {
                        payload.planner.purchasedIngredientKeys.distinct()
                    } else {
                        (existingState.purchasedIngredientKeys + payload.planner.purchasedIngredientKeys).distinct()
                    }

                    val mergedGroups = if (overwritePlanner) {
                        normalizeGroups(payload.groups)
                    } else {
                        normalizeGroups(existingState.groups + payload.groups)
                    }

                    saveState(
                        existingState.copy(
                            groups = mergedGroups,
                            purchasedIngredientKeys = mergedPurchasedKeys,
                            weeklyPlan = mergedWeeklyPlan,
                            dayMultiplier = if (overwritePlanner) payload.planner.dayCount.coerceIn(1, 30) else existingState.dayMultiplier
                        )
                    )

                    DatabaseImportResult(
                        importedMeals = mealIdRemap.size,
                        importedIngredients = ingredientIdRemap.size,
                        importedAssignments = importedAssignments.size
                    )
                }
            }.getOrElse {
                DatabaseImportResult(error = it.message ?: "Неверный файл импорта")
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
            val mealsFromEntity: List<Meal> = gson.fromJson(
                entity.mealsJson,
                object : TypeToken<List<Meal>>() {}.type
            ) ?: emptyList()

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
                    (gson.fromJson<List<String>>(
                        entity.groupsJson,
                        object : TypeToken<List<String>>() {}.type
                    ) ?: DEFAULT_GROUPS)
                ),
                purchasedIngredientKeys = (
                    gson.fromJson<List<String>>(
                        entity.purchasedIngredientKeysJson,
                        object : TypeToken<List<String>>() {}.type
                    ) ?: emptyList()
                ).distinct(),
                weeklyPlan = gson.fromJson<List<WeeklyPlanAssignment>>(
                    entity.weeklyPlanJson,
                    object : TypeToken<List<WeeklyPlanAssignment>>() {}.type
                ) ?: emptyList(),
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

                val state: LegacyPlannerState = gson.fromJson<LegacyPlannerState>(serialized, type)
                    ?: LegacyPlannerState()

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
        sharedPreferences.edit { remove(KEY_STATE) }
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
            com.example.mealplanner.data.local.Ingredient(name = name, unit = unit, groupId = ensureDefaultIngredientGroupId())
        )
        if (inserted > 0L) return inserted
        return ingredientDao.findByName(name)?.id ?: throw IllegalStateException("Cannot resolve ingredient id for $name")
    }

    private suspend fun ensureDefaultIngredientGroupId(): String {
        val existing = ingredientGroupDao.findByName(IngredientRepository.DEFAULT_GROUP_NAME)
        if (existing != null) return existing.id

        val created = com.example.mealplanner.data.local.IngredientGroup(
            id = UUID.randomUUID().toString(),
            name = IngredientRepository.DEFAULT_GROUP_NAME
        )
        ingredientGroupDao.insert(created)
        return created.id
    }

    private fun parseImportPayload(rawJson: String): DatabaseExportPayload {
        val root = JsonParser.parseString(rawJson)
        require(root.isJsonObject) { "Неверный JSON: ожидается объект" }
        val rootObj = root.asJsonObject

        validateArray(rootObj, "meals")
        validateArray(rootObj, "ingredients")
        validateArray(rootObj, "crossRefs")
        validateObject(rootObj, "planner")
        validateArray(rootObj, "groups")

        val payload = gson.fromJson(rootObj, DatabaseExportPayload::class.java)

        require(payload.meals.all { it.name.isNotBlank() }) { "Неверный JSON: meal.name пуст" }
        require(payload.ingredients.all { it.name.isNotBlank() && it.unit.isNotBlank() }) {
            "Неверный JSON: ingredient.name/unit пуст"
        }

        val mealIds = payload.meals.map { it.id }.toSet()
        val ingredientIds = payload.ingredients.map { it.id }.toSet()
        require(payload.crossRefs.all { it.mealId in mealIds && it.ingredientId in ingredientIds }) {
            "Неверный JSON: crossRefs содержит несуществующие ссылки"
        }

        return payload
    }

    private fun validateArray(root: JsonObject, key: String) {
        val element = root[key]
        require(element != null && element.isJsonArray) { "Неверный JSON: поле '$key' отсутствует или не массив" }
    }

    private fun validateObject(root: JsonObject, key: String) {
        val element: JsonElement? = root[key]
        require(element != null && element.isJsonObject) { "Неверный JSON: поле '$key' отсутствует или не объект" }
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

data class PlannerExportPayload(
    val purchasedIngredientKeys: List<String>,
    val weeklyPlan: List<WeeklyPlanAssignment>,
    val dayCount: Int
)

data class DatabaseExportPayload(
    val meals: List<com.example.mealplanner.data.local.Meal>,
    val ingredients: List<com.example.mealplanner.data.local.Ingredient>,
    val crossRefs: List<MealIngredientCrossRef>,
    val planner: PlannerExportPayload,
    val groups: List<String>
)

data class DatabaseImportResult(
    val importedMeals: Int = 0,
    val importedIngredients: Int = 0,
    val importedAssignments: Int = 0,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
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
