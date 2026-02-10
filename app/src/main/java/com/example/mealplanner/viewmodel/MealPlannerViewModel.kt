package com.example.mealplanner.viewmodel

import android.app.Application
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.MealsRepository
import com.example.mealplanner.data.SettingsDataStore
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.model.Meal
import com.example.mealplanner.model.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MealPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val mealsRepository = MealsRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _meals = MutableStateFlow(emptyList<Meal>())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _selectedMealNames = MutableStateFlow(setOf<String>())
    val selectedMealNames: StateFlow<Set<String>> = _selectedMealNames.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { state ->
                _settings.value = state
                if (state.persistDataBetweenLaunches) {
                    _meals.value = mealsRepository.loadMeals()
                } else {
                    _meals.value = emptyList()
                }
            }
        }
    }

    fun addMeal(name: String, ingredients: List<Ingredient>) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || ingredients.isEmpty()) return

        val meal = Meal(name = normalizedName, ingredients = ingredients)
        _meals.update { it + meal }
        persistMealsIfEnabled()
    }

    fun removeMeal(meal: Meal) {
        _meals.update { current -> current.filterNot { it == meal } }
        _selectedMealNames.update { it - meal.name }
        persistMealsIfEnabled()
    }

    fun toggleMealSelection(mealName: String) {
        _selectedMealNames.update { selected ->
            if (mealName in selected) selected - mealName else selected + mealName
        }
    }

    fun clearShoppingSelection() {
        _selectedMealNames.value = emptySet()
    }

    fun getAggregatedShoppingList(): List<Ingredient> {
        val selectedMeals = meals.value.filter { it.name in selectedMealNames.value }
        val grouped = selectedMeals
            .flatMap { it.ingredients }
            .groupBy { ingredient -> ingredient.name.trim().lowercase() to ingredient.unit.trim().lowercase() }

        return grouped.map { (key, items) ->
            Ingredient(
                name = key.first.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                amount = items.sumOf { it.amount },
                unit = key.second
            )
        }.sortedBy { it.name }
    }

    fun exportShoppingListToPdf(): File? {
        val ingredients = getAggregatedShoppingList()
        if (ingredients.isEmpty()) return null

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { textSize = 14f }

        var y = 50f
        canvas.drawText("Список покупок", 40f, y, paint)
        y += 30f

        ingredients.forEach { ingredient ->
            val line = "• ${ingredient.name}: ${ingredient.amount} ${ingredient.unit}"
            canvas.drawText(line, 40f, y, paint)
            y += 24f
        }

        document.finishPage(page)

        val outputDir = getApplication<Application>().getExternalFilesDir(null)
            ?: getApplication<Application>().filesDir
        val outputFile = File(outputDir, "shopping-list-${System.currentTimeMillis()}.pdf")

        outputFile.outputStream().use { document.writeTo(it) }
        document.close()

        if (settings.value.clearShoppingAfterExport) {
            clearShoppingSelection()
        }

        return outputFile
    }

    fun updatePersistDataBetweenLaunches(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // Persist current in-memory meals first so settings observers never reload stale data.
                mealsRepository.saveMeals(meals.value)
                settingsDataStore.setPersistDataBetweenLaunches(true)
            } else {
                settingsDataStore.setPersistDataBetweenLaunches(false)
                mealsRepository.saveMeals(emptyList())
                _meals.value = emptyList()
            }
        }
    }

    fun updateClearShoppingAfterExport(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setClearShoppingAfterExport(enabled)
        }
    }

    private fun persistMealsIfEnabled() {
        if (settings.value.persistDataBetweenLaunches) {
            mealsRepository.saveMeals(meals.value)
        }
    }
}
