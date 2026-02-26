package com.example.mealplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredient_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class IngredientGroup(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
