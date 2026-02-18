package com.example.mealplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "meal_ingredient_cross_ref",
    primaryKeys = ["mealId", "ingredientId"],
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId"), Index("ingredientId")]
)
data class MealIngredientCrossRef(
    val mealId: Long,
    val ingredientId: Long,
    val quantity: Double
)
