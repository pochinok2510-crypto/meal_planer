package com.example.mealplanner.viewmodel

import android.app.Application
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.IngredientRepository
import com.example.mealplanner.data.MealsRepository
import com.example.mealplanner.data.PlannerState
import com.example.mealplanner.data.SettingsDataStore
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.model.Meal
import com.example.mealplanner.model.MealSlot
import com.example.mealplanner.model.PlanDay
import com.example.mealplanner.model.WeeklyPlanAssignment
import com.example.mealplanner.model.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class MealIngredientDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String,
    val quantityInput: String
)

data class AddMealUiState(
    val mealName: String = "",
    val selectedGroup: String = "",
    val mealType: String = "",
    val selectedStep: AddMealStep = AddMealStep.BASIC_INFO,
    val selectedIngredients: List<MealIngredientDraft> = emptyList(),
    val isIngredientSheetVisible: Boolean = false,
    val ingredientSearchQuery: String = "",
    val ingredientUnitInput: String = "",
    val ingredientQuantityInput: String = "",
    val error: String? = null
)

data class UndoUiState(
    val id: Long,
    val message: String,
    val actionLabel: String = "Отменить"
)

private sealed interface PendingUndoAction {
    data class DraftIngredientRemoval(
        val ingredient: MealIngredientDraft,
        val index: Int
    ) : PendingUndoAction

    data class ShoppingIngredientRemoval(
        val ingredient: Ingredient,
        val wasPurchased: Boolean
    ) : PendingUndoAction
}

private fun List<MealIngredientDraft>.sortedAlphabetically(): List<MealIngredientDraft> {
    return sortedWith(
        compareBy<MealIngredientDraft>(
            { it.name.lowercase(Locale.getDefault()) },
            { it.unit.lowercase(Locale.getDefault()) }
        )
    )
}

enum class AddMealStep {
    BASIC_INFO,
    INGREDIENTS
}

class MealPlannerViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val mealsRepository = MealsRepository(application)
    private val ingredientRepository = IngredientRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _meals = MutableStateFlow(emptyList<Meal>())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _groups = MutableStateFlow(MealsRepository.DEFAULT_GROUPS)
    val groups: StateFlow<List<String>> = _groups.asStateFlow()

    private val _weeklyPlan = MutableStateFlow(emptyMap<Pair<PlanDay, MealSlot>, String>())
    val weeklyPlan: StateFlow<Map<Pair<PlanDay, MealSlot>, String>> = _weeklyPlan.asStateFlow()

    private val _dayCount = MutableStateFlow(1)
    val dayCount: StateFlow<Int> = _dayCount.asStateFlow()

    private val _purchasedIngredientKeys = MutableStateFlow(setOf<String>())
    val purchasedIngredientKeys: StateFlow<Set<String>> = _purchasedIngredientKeys.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _hiddenShoppingIngredientKeys = MutableStateFlow(setOf<String>())

    private val _undoUiState = MutableStateFlow<UndoUiState?>(null)
    val undoUiState: StateFlow<UndoUiState?> = _undoUiState.asStateFlow()

    private var pendingUndoAction: PendingUndoAction? = null
    private var undoEventId: Long = 0

    val ingredientCatalog = ingredientRepository.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _addMealUiState = MutableStateFlow(
        AddMealUiState(
            mealName = savedStateHandle[KEY_MEAL_NAME] ?: "",
            selectedGroup = savedStateHandle[KEY_SELECTED_GROUP] ?: "",
            mealType = savedStateHandle[KEY_MEAL_TYPE] ?: "",

            selectedStep = savedStateHandle.get<String>(KEY_ADD_MEAL_STEP)
                ?.let { rawStep -> runCatching { AddMealStep.valueOf(rawStep) }.getOrDefault(AddMealStep.BASIC_INFO) }
                ?: AddMealStep.BASIC_INFO,

            selectedIngredients = decodeDraftIngredients(savedStateHandle[KEY_SELECTED_INGREDIENTS]),
            isIngredientSheetVisible = savedStateHandle[KEY_INGREDIENT_SHEET_VISIBLE] ?: false,
            ingredientSearchQuery = savedStateHandle[KEY_INGREDIENT_SEARCH_QUERY] ?: "",
            ingredientUnitInput = savedStateHandle[KEY_INGREDIENT_UNIT_INPUT] ?: "",
            ingredientQuantityInput = savedStateHandle[KEY_INGREDIENT_QUANTITY_INPUT] ?: "",
            error = savedStateHandle[KEY_ADD_MEAL_ERROR]
        )
    )
    val addMealUiState: StateFlow<AddMealUiState> = _addMealUiState.asStateFlow()

    private val ingredientSearchQuery = _addMealUiState
        .map { it.ingredientSearchQuery }
        .distinctUntilChanged()

    val filteredIngredientCatalog = combine(ingredientCatalog, ingredientSearchQuery) { catalog, searchQuery ->
        val query = searchQuery.trim()
        if (query.isBlank()) {
            catalog.take(40)
        } else {
            catalog.filter { ingredient -> ingredient.name.contains(query, ignoreCase = true) }.take(40)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groupedFilteredIngredientCatalog = filteredIngredientCatalog
        .map { catalog ->
            val grouped = catalog.groupBy { ingredient ->
                ingredient.category?.trim().takeUnless { it.isNullOrBlank() } ?: OTHER_INGREDIENT_CATEGORY
            }

            val orderedCategories = grouped.keys
                .filterNot { it == OTHER_INGREDIENT_CATEGORY }
                .sorted()

            buildMap {
                orderedCategories.forEach { category ->
                    put(category, grouped.getValue(category))
                }
                grouped[OTHER_INGREDIENT_CATEGORY]?.let { otherIngredients ->
                    put(OTHER_INGREDIENT_CATEGORY, otherIngredients)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        observeMeals()
        observeSettings()
    }

    private fun observeMeals() {
        viewModelScope.launch {
            mealsRepository.observeMeals().collect { loadedMeals ->
                if (settings.value.persistDataBetweenLaunches) {
                    _meals.value = loadedMeals
                }
            }
        }
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

        viewModelScope.launch {
            mealsRepository.addMeal(normalizedName, normalizedGroup, ingredients)
            persistPlannerStateIfEnabled()
        }
    }

    fun onAddMealScreenVisible(availableGroups: List<String>) {
        val current = _addMealUiState.value
        if (current.selectedGroup.isNotBlank()) return
        val defaultGroup = availableGroups.firstOrNull().orEmpty()
        if (defaultGroup.isBlank()) return
        updateAddMealState { it.copy(selectedGroup = defaultGroup) }
    }

    fun updateAddMealName(value: String) {
        updateAddMealState { it.copy(mealName = value, error = null) }
    }

    fun updateAddMealGroup(value: String) {
        updateAddMealState { it.copy(selectedGroup = value, error = null) }
    }

    fun updateAddMealType(value: String) {
        updateAddMealState { it.copy(mealType = value, error = null) }
    }

    fun updateAddMealStep(step: AddMealStep) {
        updateAddMealState { it.copy(selectedStep = step, error = null) }
    }

    fun openIngredientSheet() {
        updateAddMealState { it.copy(isIngredientSheetVisible = true, error = null) }
    }

    fun closeIngredientSheet() {
        updateAddMealState {
            it.copy(
                isIngredientSheetVisible = false,
                ingredientSearchQuery = "",
                ingredientUnitInput = "",
                ingredientQuantityInput = "",
                error = null
            )
        }
    }

    fun updateIngredientSearchQuery(value: String) {
        val match = ingredientCatalog.value.firstOrNull { ingredient ->
            ingredient.name.equals(value.trim(), ignoreCase = true)
        }
        updateAddMealState {
            it.copy(
                ingredientSearchQuery = value,
                ingredientUnitInput = match?.unit ?: it.ingredientUnitInput,
                error = null
            )
        }
    }

    fun selectIngredientFromCatalog(name: String, unit: String) {
        updateAddMealState {
            it.copy(
                ingredientSearchQuery = name,
                ingredientUnitInput = unit,
                error = null
            )
        }
    }

    fun updateIngredientUnitInput(value: String) {
        updateAddMealState { it.copy(ingredientUnitInput = value, error = null) }
    }

    fun updateIngredientQuantityInput(value: String) {
        updateAddMealState { it.copy(ingredientQuantityInput = value, error = null) }
    }

    fun confirmIngredientFromSheet(): Boolean {
        val state = _addMealUiState.value
        val added = addIngredientToMealDraft(
            nameInput = state.ingredientSearchQuery,
            unitInput = state.ingredientUnitInput,
            quantityInput = state.ingredientQuantityInput
        )
        if (added) {
            closeIngredientSheet()
        }
        return added
    }

    fun addIngredientToMealDraft(nameInput: String, unitInput: String, quantityInput: String): Boolean {
        val normalizedName = nameInput.trim()
        val quantity = quantityInput.trim().toDoubleOrNull()
        val existingIngredient = ingredientCatalog.value.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
        val normalizedUnit = (existingIngredient?.unit ?: unitInput).trim()

        when {
            normalizedName.isBlank() -> {
                updateAddMealState { it.copy(error = "Выберите ингредиент") }
                return false
            }
            quantity == null || quantity <= 0 -> {
                updateAddMealState { it.copy(error = "Количество должно быть числом больше 0") }
                return false
            }
            normalizedUnit.isBlank() -> {
                updateAddMealState { it.copy(error = "Введите единицу измерения") }
                return false
            }
            else -> {
                if (existingIngredient == null) {
                    viewModelScope.launch {
                        ingredientRepository.insertOrIgnore(
                            com.example.mealplanner.data.local.Ingredient(
                                name = normalizedName,
                                unit = normalizedUnit
                            )
                        )
                    }
                }

                updateAddMealState { current ->
                    val newDraft = MealIngredientDraft(
                        name = normalizedName,
                        unit = normalizedUnit,
                        quantityInput = quantityInput.trim()
                    )

                    val existingIndex = current.selectedIngredients.indexOfFirst {
                        it.name.equals(normalizedName, ignoreCase = true) && it.unit.equals(normalizedUnit, ignoreCase = true)
                    }
                    val updated = if (existingIndex >= 0) {
                        current.selectedIngredients.toMutableList().apply {
                            val existingId = current.selectedIngredients[existingIndex].id
                            set(existingIndex, newDraft.copy(id = existingId))
                        }
                    } else {
                        current.selectedIngredients + newDraft
                    }

                    current.copy(
                        selectedIngredients = updated.sortedAlphabetically(),
                        error = null
                    )
                }
                return true
            }
        }
    }

    fun removeDraftIngredient(draftId: String) {
        updateAddMealState { state ->
            val index = state.selectedIngredients.indexOfFirst { it.id == draftId }
            if (index < 0) return@updateAddMealState state

            val removedIngredient = state.selectedIngredients[index]
            pendingUndoAction = PendingUndoAction.DraftIngredientRemoval(
                ingredient = removedIngredient,
                index = index
            )
            publishUndoEvent("Ингредиент удалён")

            val updated = state.selectedIngredients.toMutableList().apply {
                removeAt(index)
            }
            state.copy(selectedIngredients = updated, error = null)
        }
    }

    fun removeShoppingIngredient(ingredient: Ingredient) {
        val key = ingredient.storageKey()
        val wasPurchased = key in _purchasedIngredientKeys.value

        _hiddenShoppingIngredientKeys.update { it + key }
        _purchasedIngredientKeys.update { it - key }

        pendingUndoAction = PendingUndoAction.ShoppingIngredientRemoval(
            ingredient = ingredient,
            wasPurchased = wasPurchased
        )
        publishUndoEvent("Позиция удалена из списка")
        persistPlannerStateIfEnabled()
    }

    fun undoLastRemoval() {
        when (val action = pendingUndoAction) {
            is PendingUndoAction.DraftIngredientRemoval -> {
                updateAddMealState { state ->
                    val restored = state.selectedIngredients.toMutableList().apply {
                        add(action.index.coerceIn(0, size), action.ingredient)
                    }
                    state.copy(selectedIngredients = restored, error = null)
                }
            }

            is PendingUndoAction.ShoppingIngredientRemoval -> {
                val key = action.ingredient.storageKey()
                _hiddenShoppingIngredientKeys.update { it - key }
                if (action.wasPurchased) {
                    _purchasedIngredientKeys.update { it + key }
                }
                persistPlannerStateIfEnabled()
            }

            null -> Unit
        }

        clearUndoState()
    }

    fun dismissUndoState() {
        clearUndoState()
    }

    fun editDraftIngredient(draftId: String) {
        updateAddMealState { state ->
            val draft = state.selectedIngredients.firstOrNull { it.id == draftId } ?: return@updateAddMealState state
            state.copy(
                isIngredientSheetVisible = true,
                ingredientSearchQuery = draft.name,
                ingredientUnitInput = draft.unit,
                ingredientQuantityInput = draft.quantityInput,
                error = null
            )
        }
    }

    fun saveMealFromDraft(onSuccess: () -> Unit) {
        val state = _addMealUiState.value
        when {
            state.mealName.isBlank() -> updateAddMealState { it.copy(error = "Название блюда не может быть пустым") }
            state.selectedGroup.isBlank() -> updateAddMealState { it.copy(error = "Выберите группу") }
            state.selectedIngredients.isEmpty() -> updateAddMealState { it.copy(error = "Добавьте хотя бы один ингредиент") }
            else -> {
                val ingredients = state.selectedIngredients.mapNotNull { draft ->
                    val quantity = draft.quantityInput.toDoubleOrNull()
                    if (quantity == null || quantity <= 0) null else Ingredient(draft.name, quantity, draft.unit)
                }
                if (ingredients.size != state.selectedIngredients.size) {
                    updateAddMealState { it.copy(error = "Проверьте количество у всех ингредиентов") }
                    return
                }
                addMeal(state.mealName, state.selectedGroup, ingredients)
                resetAddMealDraft(keepGroup = true)
                onSuccess()
            }
        }
    }

    fun resetAddMealDraft(keepGroup: Boolean = false) {
        val selectedGroup = if (keepGroup) _addMealUiState.value.selectedGroup else ""
        updateAddMealState {
            AddMealUiState(selectedGroup = selectedGroup, selectedStep = AddMealStep.BASIC_INFO)
        }
    }

    fun removeMeal(meal: Meal) {
        _weeklyPlan.update { current -> current.filterValues { it != meal.id } }
        viewModelScope.launch {
            mealsRepository.removeMeal(meal.id)
            persistPlannerStateIfEnabled()
        }
    }

    fun moveMealToGroup(meal: Meal, targetGroup: String) {
        viewModelScope.launch {
            mealsRepository.moveMealToGroup(meal.id, targetGroup)
            persistPlannerStateIfEnabled()
        }
    }

    fun duplicateMealToGroup(meal: Meal, targetGroup: String) {
        viewModelScope.launch {
            mealsRepository.duplicateMealToGroup(meal.id, targetGroup)
            persistPlannerStateIfEnabled()
        }
    }

    fun assignMealToSlot(day: PlanDay, slot: MealSlot, mealId: String?) {
        _weeklyPlan.update { current ->
            val key = day to slot
            if (mealId.isNullOrBlank()) current - key else current + (key to mealId)
        }
        persistPlannerStateIfEnabled()
    }

    fun clearShoppingSelection() {
        _weeklyPlan.value = emptyMap()
        _hiddenShoppingIngredientKeys.value = emptySet()
        persistPlannerStateIfEnabled()
    }

    fun updateDayCount(days: Int) {
        _dayCount.value = days.coerceIn(1, 30)
        persistPlannerStateIfEnabled()
    }

    fun setIngredientPurchased(ingredient: Ingredient, purchased: Boolean) {
        val key = ingredient.storageKey()
        _purchasedIngredientKeys.update { current ->
            if (purchased) current + key else current - key
        }
        persistPlannerStateIfEnabled()
    }

    fun getAggregatedShoppingList(): List<Ingredient> {
        val mealMap = meals.value.associateBy { it.id }
        val ingredientIdByKey = ingredientCatalog.value.associateBy(
            keySelector = { ingredient -> ingredient.name.normalizedKey() to ingredient.unit.normalizedKey() },
            valueTransform = { ingredient -> ingredient.id }
        )

        val multiplier = BigDecimal.valueOf(dayCount.value.toLong())

        val grouped = weeklyPlan.value.values
            .mapNotNull { mealId -> mealMap[mealId] }
            .flatMap { meal ->
                meal.ingredients.map { ingredient ->
                    val normalizedName = ingredient.name.normalizedKey()
                    val normalizedUnit = ingredient.unit.normalizedKey()
                    val ingredientId = ingredientIdByKey[normalizedName to normalizedUnit]
                        ?: syntheticIngredientId(normalizedName, normalizedUnit)
                    ShoppingIngredientItem(
                        ingredientId = ingredientId,
                        displayName = ingredient.name.trim(),
                        normalizedName = normalizedName,
                        displayUnit = ingredient.unit.trim(),
                        normalizedUnit = normalizedUnit,
                        amount = BigDecimal.valueOf(ingredient.amount)
                    )
                }
            }
            .groupBy { item -> item.ingredientId }

        return grouped.values
            .mapNotNull { items ->
                val first = items.firstOrNull() ?: return@mapNotNull null
                val totalAmount = items
                    .fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
                    .multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP)

                Ingredient(
                    name = first.displayName.ifBlank {
                        first.normalizedName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    },
                    amount = totalAmount.toDouble(),
                    unit = first.displayUnit.ifBlank { first.normalizedUnit }
                )
            }
            .filterNot { ingredient -> ingredient.storageKey() in _hiddenShoppingIngredientKeys.value }
            .sortedBy { it.name }
    }

    fun buildShoppingListMessage(): String {
        val ingredients = getAggregatedShoppingList()
        if (ingredients.isEmpty()) return "Список покупок пуст"

        val title = "Список покупок на ${dayCount.value} дн."
        val body = ingredients.joinToString("\n") { "• ${it.name}: ${formatAmount(it.amount)} ${it.unit}" }
        return "$title\n$body"
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
        _dayCount.value = state.dayMultiplier
        val mealIds = state.meals.map { it.id }.toSet()
        _weeklyPlan.value = state.weeklyPlan
            .filter { it.mealId in mealIds }
            .associate { (it.day to it.slot) to it.mealId }
    }

    private fun clearPlannerData() {
        _meals.value = emptyList()
        _groups.value = MealsRepository.DEFAULT_GROUPS
        _weeklyPlan.value = emptyMap()
        _purchasedIngredientKeys.value = emptySet()
        _hiddenShoppingIngredientKeys.value = emptySet()
        _dayCount.value = 1
    }

    private fun publishUndoEvent(message: String) {
        undoEventId += 1
        _undoUiState.value = UndoUiState(id = undoEventId, message = message)
    }

    private fun clearUndoState() {
        pendingUndoAction = null
        _undoUiState.value = null
    }

    private fun persistPlannerStateIfEnabled() {
        if (settings.value.persistDataBetweenLaunches) {
            persistPlannerState()
        }
    }

    private fun persistPlannerState() {
        val snapshot = PlannerState(
            meals = meals.value,
            groups = groups.value,
            purchasedIngredientKeys = purchasedIngredientKeys.value.toList(),
            weeklyPlan = weeklyPlan.value.map { (key, mealId) ->
                WeeklyPlanAssignment(day = key.first, slot = key.second, mealId = mealId)
            },
            dayMultiplier = dayCount.value
        )
        viewModelScope.launch {
            mealsRepository.saveState(snapshot)
        }
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

    private data class ShoppingIngredientItem(
        val ingredientId: Long,
        val displayName: String,
        val normalizedName: String,
        val displayUnit: String,
        val normalizedUnit: String,
        val amount: BigDecimal
    )

    private fun String.normalizedKey(): String {
        return trim().lowercase(Locale.getDefault())
    }

    private fun syntheticIngredientId(name: String, unit: String): Long {
        return ("$name|$unit").hashCode().toLong()
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

    private fun updateAddMealState(transform: (AddMealUiState) -> AddMealUiState) {
        _addMealUiState.update { current ->
            val next = transform(current)
            savedStateHandle[KEY_MEAL_NAME] = next.mealName
            savedStateHandle[KEY_SELECTED_GROUP] = next.selectedGroup
            savedStateHandle[KEY_MEAL_TYPE] = next.mealType
            savedStateHandle[KEY_ADD_MEAL_STEP] = next.selectedStep.name
            savedStateHandle[KEY_SELECTED_INGREDIENTS] = encodeDraftIngredients(next.selectedIngredients)
            savedStateHandle[KEY_INGREDIENT_SHEET_VISIBLE] = next.isIngredientSheetVisible
            savedStateHandle[KEY_INGREDIENT_SEARCH_QUERY] = next.ingredientSearchQuery
            savedStateHandle[KEY_INGREDIENT_UNIT_INPUT] = next.ingredientUnitInput
            savedStateHandle[KEY_INGREDIENT_QUANTITY_INPUT] = next.ingredientQuantityInput
            savedStateHandle[KEY_ADD_MEAL_ERROR] = next.error
            next
        }
    }

    private fun encodeDraftIngredients(items: List<MealIngredientDraft>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("name", item.name)
                    put("unit", item.unit)
                    put("quantityInput", item.quantityInput)
                    put("id", item.id)
                }
            )
        }
        return array.toString()
    }

    private fun decodeDraftIngredients(raw: String?): List<MealIngredientDraft> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            List(jsonArray.length()) { index ->
                val obj = jsonArray.getJSONObject(index)
                MealIngredientDraft(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = obj.optString("name"),
                    unit = obj.optString("unit"),
                    quantityInput = obj.optString("quantityInput")
                )
            }.sortedAlphabetically()
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val OTHER_INGREDIENT_CATEGORY = "Other"

        private const val KEY_MEAL_NAME = "add_meal_name"
        private const val KEY_SELECTED_GROUP = "add_meal_group"
        private const val KEY_MEAL_TYPE = "add_meal_type"
        private const val KEY_ADD_MEAL_STEP = "add_meal_step"
        private const val KEY_SELECTED_INGREDIENTS = "add_meal_selected_ingredients"
        private const val KEY_INGREDIENT_SHEET_VISIBLE = "add_meal_ingredient_sheet_visible"
        private const val KEY_INGREDIENT_SEARCH_QUERY = "add_meal_ingredient_search_query"
        private const val KEY_INGREDIENT_UNIT_INPUT = "add_meal_ingredient_unit_input"
        private const val KEY_INGREDIENT_QUANTITY_INPUT = "add_meal_ingredient_quantity_input"
        private const val KEY_ADD_MEAL_ERROR = "add_meal_error"
    }
}
