package com.example.mealplanner.data

import android.content.Context
import androidx.room.withTransaction
import com.example.mealplanner.data.local.Ingredient
import com.example.mealplanner.data.local.IngredientGroup
import com.example.mealplanner.data.local.MealPlannerDatabase
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class IngredientRepository(context: Context) {
    private val db = MealPlannerDatabase.getInstance(context)
    private val ingredientDao = db.ingredientDao()
    private val ingredientGroupDao = db.ingredientGroupDao()

    fun getAllIngredients(): Flow<List<Ingredient>> = ingredientDao.getAll()

    fun getAllGroups(): Flow<List<IngredientGroup>> = ingredientGroupDao.getAll()

    suspend fun getAllGroupsOnce(): List<IngredientGroup> {
        ensureDefaultGroup()
        return ingredientGroupDao.getAllOnce()
    }

    suspend fun findByName(name: String): Ingredient? = ingredientDao.findByName(name.trim())

    suspend fun ensureDefaultGroup(): IngredientGroup = db.withTransaction {
        ingredientGroupDao.findByName(DEFAULT_GROUP_NAME) ?: IngredientGroup(
            id = DEFAULT_GROUP_ID,
            name = DEFAULT_GROUP_NAME
        ).also { ingredientGroupDao.insert(it) }
    }

    suspend fun createGroup(name: String): Boolean = db.withTransaction {
        ensureDefaultGroup()
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return@withTransaction false
        if (ingredientGroupDao.findByName(normalizedName) != null) return@withTransaction false

        ingredientGroupDao.insert(
            IngredientGroup(
                id = UUID.randomUUID().toString(),
                name = normalizedName
            )
        )
        true
    }

    suspend fun deleteGroup(groupId: String): Boolean = db.withTransaction {
        val targetGroup = ingredientGroupDao.findById(groupId) ?: return@withTransaction false
        val defaultGroup = ensureDefaultGroup()
        if (targetGroup.id == defaultGroup.id) return@withTransaction false

        ingredientDao.moveGroupIngredients(sourceGroupId = targetGroup.id, targetGroupId = defaultGroup.id)
        ingredientGroupDao.deleteById(targetGroup.id)
        true
    }

    suspend fun insert(ingredient: Ingredient): Long {
        ensureDefaultGroup()
        return ingredientDao.insert(
            ingredient.copy(name = ingredient.name.trim(), unit = ingredient.unit.trim())
        )
    }

    suspend fun insertOrIgnore(ingredient: Ingredient): Long {
        ensureDefaultGroup()
        return ingredientDao.insertOrIgnoreByNameCaseInsensitive(
            ingredient.copy(name = ingredient.name.trim(), unit = ingredient.unit.trim())
        )
    }

    suspend fun delete(ingredient: Ingredient) {
        ingredientDao.delete(ingredient)
    }

    suspend fun deleteIngredient(ingredientId: Long) {
        ingredientDao.deleteById(ingredientId)
    }

    companion object {
        const val DEFAULT_GROUP_NAME = "Other"
        const val DEFAULT_GROUP_ID = "default-other-group"
    }
}
