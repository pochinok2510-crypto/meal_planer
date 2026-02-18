package com.example.mealplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    indices = [Index(value = ["name"], unique = true)]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String,
    val category: String? = null
)
