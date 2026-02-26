package com.example.mealplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientGroupDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: IngredientGroup)

    @Query("SELECT * FROM ingredient_groups ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<IngredientGroup>>

    @Query("SELECT * FROM ingredient_groups ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<IngredientGroup>

    @Query("SELECT * FROM ingredient_groups WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): IngredientGroup?

    @Query("SELECT * FROM ingredient_groups WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): IngredientGroup?

    @Query("DELETE FROM ingredient_groups WHERE id = :id")
    suspend fun deleteById(id: String)
}
