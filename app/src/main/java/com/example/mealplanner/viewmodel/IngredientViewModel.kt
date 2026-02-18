package com.example.mealplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.IngredientRepository
import com.example.mealplanner.data.local.Ingredient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IngredientRepository(application)

    val ingredients: StateFlow<List<Ingredient>> = repository.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ingredientNames: StateFlow<Set<String>> = ingredients
        .map { items -> items.map { it.name.lowercase() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun addIngredient(name: String, unit: String, category: String? = null) {
        val normalizedName = name.trim()
        val normalizedUnit = unit.trim()
        val normalizedCategory = category?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedName.isBlank() || normalizedUnit.isBlank()) return

        viewModelScope.launch {
            repository.insertOrIgnore(
                Ingredient(
                    name = normalizedName,
                    unit = normalizedUnit,
                    category = normalizedCategory
                )
            )
        }
    }

    fun removeIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.delete(ingredient)
        }
    }
}
