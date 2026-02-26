package com.example.mealplanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ingredient: Ingredient): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(ingredient: Ingredient): Long

    @Query("SELECT * FROM ingredients ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): Ingredient?

    @Delete
    suspend fun delete(ingredient: Ingredient)

    @Transaction
    suspend fun insertOrIgnoreByNameCaseInsensitive(ingredient: Ingredient): Long {
        val normalizedName = ingredient.name.trim()
        if (normalizedName.isBlank()) return -1L
        val existing = findByName(normalizedName)
        if (existing != null) return -1L
        return insertOrIgnore(ingredient.copy(name = normalizedName))
    }
}
