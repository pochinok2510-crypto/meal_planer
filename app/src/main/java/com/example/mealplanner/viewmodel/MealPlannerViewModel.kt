package com.example.mealplanner.viewmodel

import android.app.Application
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.MealsRepository
import com.example.mealplanner.data.PlannerState
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
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class MealPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val mealsRepository = MealsRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _meals = MutableStateFlow(emptyList<Meal>())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _groups = MutableStateFlow(MealsRepository.DEFAULT_GROUPS)
    val groups: StateFlow<List<String>> = _groups.asStateFlow()

    private val _selectedMealIds = MutableStateFlow(setOf<String>())
    val selectedMealIds: StateFlow<Set<String>> = _selectedMealIds.asStateFlow()

    private val _dayCount = MutableStateFlow(1)
    val dayCount: StateFlow<Int> = _dayCount.asStateFlow()

    private val _purchasedIngredientKeys = MutableStateFlow(setOf<String>())
    val purchasedIngredientKeys: StateFlow<Set<String>> = _purchasedIngredientKeys.asStateFlow()

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
                    val persisted = mealsRepository.loadState()
                    restorePlannerState(persisted)
                } else {
                    clearPlannerData()
                }
            }
        }
    }

    fun addGroup(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false

        val exists = groups.value.any { it.equals(normalized, ignoreCase = true) }
        if (exists) return false

        _groups.update { it + normalized }
        persistPlannerStateIfEnabled()
        return true
    }

    fun removeGroup(name: String) {
        if (name in MealsRepository.DEFAULT_GROUPS) return
        if (name == MealsRepository.UNCATEGORIZED_GROUP) return

        _groups.update { current ->
            val withoutDeleted = current.filterNot { it == name }
            if (MealsRepository.UNCATEGORIZED_GROUP in withoutDeleted) {
                withoutDeleted
            } else {
                withoutDeleted + MealsRepository.UNCATEGORIZED_GROUP
            }
        }
        _meals.update { currentMeals ->
            currentMeals.map { meal ->
                if (meal.group == name) {
                    meal.copy(group = MealsRepository.UNCATEGORIZED_GROUP)
                } else {
                    meal
                }
            }
        }
        persistPlannerStateIfEnabled()
    }

    fun renameGroup(oldName: String, newName: String): Boolean {
        if (oldName in MealsRepository.DEFAULT_GROUPS || oldName == MealsRepository.UNCATEGORIZED_GROUP) {
            return false
        }

        val normalized = newName.trim()
        if (normalized.isBlank()) return false
        if (normalized.equals(oldName, ignoreCase = true)) return false

        val exists = groups.value.any { it.equals(normalized, ignoreCase = true) }
        if (exists) return false

        _groups.update { current ->
            current.map { if (it == oldName) normalized else it }
        }
        _meals.update { currentMeals ->
            currentMeals.map { meal ->
                if (meal.group == oldName) meal.copy(group = normalized) else meal
            }
        }
        persistPlannerStateIfEnabled()
        return true
    }

    fun addMeal(name: String, group: String, ingredients: List<Ingredient>) {
        val normalizedName = name.trim()
        val normalizedGroup = group.trim()
        if (normalizedName.isBlank() || ingredients.isEmpty() || normalizedGroup.isBlank()) return

        val meal = Meal(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            group = normalizedGroup,
            ingredients = ingredients
        )
        _meals.update { it + meal }
        persistPlannerStateIfEnabled()
    }

    fun removeMeal(meal: Meal) {
        _meals.update { current -> current.filterNot { it.id == meal.id } }
        _selectedMealIds.update { it - meal.id }
        persistPlannerStateIfEnabled()
    }

    fun moveMealToGroup(meal: Meal, targetGroup: String) {
        _meals.update { current ->
            current.map {
                if (it.id == meal.id) it.copy(group = targetGroup) else it
            }
        }
        persistPlannerStateIfEnabled()
    }

    fun duplicateMealToGroup(meal: Meal, targetGroup: String) {
        val duplicate = meal.copy(id = UUID.randomUUID().toString(), group = targetGroup)
        _meals.update { it + duplicate }
        persistPlannerStateIfEnabled()
    }

    fun toggleMealSelection(mealId: String) {
        _selectedMealIds.update { selected ->
            if (mealId in selected) selected - mealId else selected + mealId
        }
    }

    fun clearShoppingSelection() {
        _selectedMealIds.value = emptySet()
    }

    fun updateDayCount(days: Int) {
        _dayCount.value = days.coerceIn(1, 30)
    }

    fun setIngredientPurchased(ingredient: Ingredient, purchased: Boolean) {
        val key = ingredient.storageKey()
        _purchasedIngredientKeys.update { current ->
            if (purchased) current + key else current - key
        }
        persistPlannerStateIfEnabled()
    }

    fun isIngredientPurchased(ingredient: Ingredient): Boolean {
        return ingredient.storageKey() in purchasedIngredientKeys.value
    }

    fun getAggregatedShoppingList(): List<Ingredient> {
        val selectedMeals = meals.value.filter { it.id in selectedMealIds.value }
        val grouped = selectedMeals
            .flatMap { it.ingredients }
            .groupBy { ingredient -> ingredient.name.trim().lowercase() to ingredient.unit.trim().lowercase() }

        val multiplier = dayCount.value

        return grouped.map { (key, items) ->
            Ingredient(
                name = key.first.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                amount = ((items.sumOf { it.amount } * multiplier) * 100.0).roundToInt() / 100.0,
                unit = key.second
            )
        }.sortedBy { it.name }
    }

    fun buildShoppingListMessage(): String {
        val ingredients = getAggregatedShoppingList()
        if (ingredients.isEmpty()) return "Список покупок пуст"

        val title = "Список покупок на ${dayCount.value} дн."
        val body = ingredients.joinToString("\n") { "• ${it.name}: ${formatAmount(it.amount)} ${it.unit}" }
        return "$title\n$body"
    }

    fun exportShoppingListToPdfFile(): File? {
        val ingredients = getAggregatedShoppingList()
        if (ingredients.isEmpty()) return null

        val outputDir = getApplication<Application>().getExternalFilesDir(null)
            ?: getApplication<Application>().filesDir
        val outputFile = File(outputDir, "shopping-list-${System.currentTimeMillis()}.pdf")

        outputFile.outputStream().use { output ->
            writePdf(ingredients, output)
        }
        onAfterShareCompleted()
        return outputFile
    }

    fun createSharePdfFile(): File? {
        val ingredients = getAggregatedShoppingList()
        if (ingredients.isEmpty()) return null

        val outputFile = File(
            getApplication<Application>().cacheDir,
            "shopping-list-share-${System.currentTimeMillis()}.pdf"
        )

        outputFile.outputStream().use { output ->
            writePdf(ingredients, output)
        }
        onAfterShareCompleted()
        return outputFile
    }

    fun saveShoppingListPdfToUri(uri: Uri): Boolean {
        val ingredients = getAggregatedShoppingList()
        if (ingredients.isEmpty()) return false

        val resolver = getApplication<Application>().contentResolver
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                writePdf(ingredients, output)
            } ?: return false
            onAfterShareCompleted()
            true
        }.getOrDefault(false)
    }

    fun updatePersistDataBetweenLaunches(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPersistDataBetweenLaunches(enabled)
            if (enabled) {
                persistPlannerState()
            } else {
                mealsRepository.saveState(PlannerState(groups = MealsRepository.DEFAULT_GROUPS))
                clearPlannerData()
            }
        }
    }

    fun updateClearShoppingAfterExport(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setClearShoppingAfterExport(enabled)
        }
    }

    private fun restorePlannerState(state: PlannerState) {
        _meals.value = state.meals
        _groups.value = state.groups
        _purchasedIngredientKeys.value = state.purchasedIngredientKeys.toSet()
        val mealIds = state.meals.map { it.id }.toSet()
        _selectedMealIds.update { it.intersect(mealIds) }
    }

    private fun clearPlannerData() {
        _meals.value = emptyList()
        _groups.value = MealsRepository.DEFAULT_GROUPS
        _selectedMealIds.value = emptySet()
        _purchasedIngredientKeys.value = emptySet()
        _dayCount.value = 1
    }

    private fun persistPlannerStateIfEnabled() {
        if (settings.value.persistDataBetweenLaunches) {
            persistPlannerState()
        }
    }

    private fun persistPlannerState() {
        mealsRepository.saveState(
            PlannerState(
                meals = meals.value,
                groups = groups.value,
                purchasedIngredientKeys = purchasedIngredientKeys.value.toList()
            )
        )
    }

    private fun onAfterShareCompleted() {
        if (settings.value.clearShoppingAfterExport) {
            clearShoppingSelection()
        }
    }

    private fun writePdf(ingredients: List<Ingredient>, output: java.io.OutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { textSize = 14f }

        var y = 50f
        canvas.drawText("Список покупок на ${dayCount.value} дн.", 40f, y, paint)
        y += 30f

        ingredients.forEach { ingredient ->
            val line = "• ${ingredient.name}: ${formatAmount(ingredient.amount)} ${ingredient.unit}"
            canvas.drawText(line, 40f, y, paint)
            y += 24f
        }

        document.finishPage(page)
        document.writeTo(output)
        document.close()
    }

    private fun Ingredient.storageKey(): String {
        return "${name.trim().lowercase(Locale.getDefault())}|${unit.trim().lowercase(Locale.getDefault())}"
    }

    private fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }
}
