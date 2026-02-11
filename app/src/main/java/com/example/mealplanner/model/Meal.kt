package com.example.mealplanner.model

data class Meal(
    val id: String,
    val name: String,
    val group: String,
    val ingredients: List<Ingredient>
)
