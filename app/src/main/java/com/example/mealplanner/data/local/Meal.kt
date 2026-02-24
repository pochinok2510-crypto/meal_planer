package com.example.mealplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val groupName: String,
    /**
     * Legacy v1.x payload where ingredients were persisted as a plain string blob.
     *
     * Kept for backward compatibility while old records are still present.
     * New records should rely on [MealIngredientCrossRef] instead.
     */
    val legacyIngredients: String? = null
)
