package com.example.mealplanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.data.local.Ingredient
import com.example.mealplanner.viewmodel.AddMealUiState

private const val INGREDIENT_OTHER_CATEGORY = "Other"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    groups: List<String>,
    filteredIngredients: List<Ingredient>,
    groupedFilteredIngredients: Map<String, List<Ingredient>>,
    state: AddMealUiState,
    onBack: () -> Unit,
    onMealNameChange: (String) -> Unit,
    onGroupSelect: (String) -> Unit,
    onOpenIngredientSheet: () -> Unit,
    onCloseIngredientSheet: () -> Unit,
    onIngredientSearchChange: (String) -> Unit,
    onIngredientSelect: (String, String) -> Unit,
    onIngredientUnitChange: (String) -> Unit,
    onIngredientQuantityChange: (String) -> Unit,
    onConfirmIngredient: () -> Unit,
    onEditDraftIngredient: (Int) -> Unit,
    onRemoveDraftIngredient: (Int) -> Unit,
    onSaveMeal: () -> Unit
) {
    var groupMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        OutlinedTextField(
            value = state.mealName,
            onValueChange = onMealNameChange,
            label = { Text("Название блюда") },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = groupMenuExpanded,
            onExpandedChange = { groupMenuExpanded = !groupMenuExpanded }
        ) {
            OutlinedTextField(
                value = state.selectedGroup,
                onValueChange = {},
                readOnly = true,
                label = { Text("Группа") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            androidx.compose.material3.DropdownMenu(
                expanded = groupMenuExpanded,
                onDismissRequest = { groupMenuExpanded = false }
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onGroupSelect(group)
                            groupMenuExpanded = false
                        }
                    )
                }
            }
        }

        Text("Ингредиенты")

        Button(onClick = onOpenIngredientSheet, modifier = Modifier.fillMaxWidth()) {
            Text("+ Добавить ингредиент")
        }

        if (state.selectedIngredients.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    state.selectedIngredients,
                    key = { _, item -> "${item.name}_${item.unit}" }
                ) { index, item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${item.quantityInput} ${item.unit}")
                            }
                            IconButton(onClick = { onEditDraftIngredient(index) }) {
                                Text("✏️")
                            }
                            IconButton(onClick = { onRemoveDraftIngredient(index) }) {
                                Text("🗑️")
                            }
                        }
                    }
                }
            }
        }

        state.error?.let {
            Text(it)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) {
                Text("← Назад")
            }
            Button(onClick = onSaveMeal) {
                Text("Сохранить блюдо")
            }
        }
    }

    if (state.isIngredientSheetVisible) {
        IngredientSheet(
            state = state,
            filteredIngredients = filteredIngredients,
            groupedFilteredIngredients = groupedFilteredIngredients,
            onDismiss = onCloseIngredientSheet,
            onIngredientSearchChange = onIngredientSearchChange,
            onIngredientSelect = onIngredientSelect,
            onUnitChange = onIngredientUnitChange,
            onQuantityChange = onIngredientQuantityChange,
            onConfirm = onConfirmIngredient
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientSheet(
    state: AddMealUiState,
    filteredIngredients: List<Ingredient>,
    groupedFilteredIngredients: Map<String, List<Ingredient>>,
    onDismiss: () -> Unit,
    onIngredientSearchChange: (String) -> Unit,
    onIngredientSelect: (String, String) -> Unit,
    onUnitChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var unitExpanded by remember { mutableStateOf(false) }
    val unitOptions = remember(filteredIngredients, state.ingredientUnitInput) {
        (filteredIngredients.map { it.unit } + state.ingredientUnitInput + listOf("г", "кг", "мл", "л", "шт"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Добавление ингредиента")

            OutlinedTextField(
                value = state.ingredientSearchQuery,
                onValueChange = onIngredientSearchChange,
                label = { Text("Поиск или новый ингредиент") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedFilteredIngredients.forEach { (category, ingredients) ->
                    item(key = "header_$category") {
                        Text(
                            text = category.ifBlank { INGREDIENT_OTHER_CATEGORY },
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                    items(items = ingredients, key = { item -> item.id }) { ingredient ->
                        Text(
                            text = "${ingredient.name} (${ingredient.unit})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIngredientSelect(ingredient.name, ingredient.unit) }
                                .padding(8.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded }
            ) {
                OutlinedTextField(
                    value = state.ingredientUnitInput,
                    onValueChange = onUnitChange,
                    label = { Text("Единица") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    unitOptions.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                onUnitChange(unit)
                                unitExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.ingredientQuantityInput,
                onValueChange = onQuantityChange,
                label = { Text("Количество") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить")
            }
        }
    }
}
