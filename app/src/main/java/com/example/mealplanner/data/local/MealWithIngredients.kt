package com.example.mealplanner.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MealWithIngredients(
    @Embedded val meal: Meal,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(MealIngredientCrossRef::class)
    )
    val ingredients: List<Ingredient>
)
