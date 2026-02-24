package com.example.mealplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    groups: List<String>,
    ingredientCatalog: List<Ingredient>,
    state: AddMealUiState,
    onBack: () -> Unit,
    onMealNameChange: (String) -> Unit,
    onGroupSelect: (String) -> Unit,
    onAddIngredientClick: (String, String, String) -> Boolean,
    onDraftQuantityChange: (Int, String) -> Unit,
    onRemoveDraftIngredient: (Int) -> Unit,
    onSaveMeal: () -> Unit
) {
    var groupMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var ingredientMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var ingredientQuery by rememberSaveable { mutableStateOf("") }
    var ingredientUnitInput by rememberSaveable { mutableStateOf("") }
    var ingredientQuantityInput by rememberSaveable { mutableStateOf("") }

    val filteredIngredients = remember(ingredientCatalog, ingredientQuery) {
        val query = ingredientQuery.trim()
        if (query.isBlank()) {
            ingredientCatalog.take(20)
        } else {
            ingredientCatalog
                .filter { it.name.contains(query, ignoreCase = true) }
                .take(20)
        }
    }

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

        // GROUP DROPDOWN
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

            DropdownMenu(
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

        // INGREDIENT SEARCH DROPDOWN
        ExposedDropdownMenuBox(
            expanded = ingredientMenuExpanded,
            onExpandedChange = { ingredientMenuExpanded = !ingredientMenuExpanded }
        ) {
            OutlinedTextField(
                value = ingredientQuery,
                onValueChange = {
                    ingredientQuery = it
                    ingredientMenuExpanded = true
                },
                label = { Text("Ингредиент") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingredientMenuExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            DropdownMenu(
                expanded = ingredientMenuExpanded,
                onDismissRequest = { ingredientMenuExpanded = false }
            ) {
                filteredIngredients.forEach { ingredient ->
                    DropdownMenuItem(
                        text = { Text("${ingredient.name} (${ingredient.unit})") },
                        onClick = {
                            ingredientQuery = ingredient.name
                            ingredientUnitInput = ingredient.unit
                            ingredientMenuExpanded = false
                        }
                    )
                }
            }
        }

        // QUANTITY + UNIT INPUT
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = ingredientQuantityInput,
                onValueChange = { ingredientQuantityInput = it },
                label = { Text("Количество") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ingredientUnitInput,
                onValueChange = { ingredientUnitInput = it },
                label = { Text("Единица") },
                modifier = Modifier.weight(1f)
            )
        }

        Button(onClick = {
            val added = onAddIngredientClick(ingredientQuery, ingredientUnitInput, ingredientQuantityInput)
            if (added) {
                ingredientQuery = ""
                ingredientUnitInput = ""
                ingredientQuantityInput = ""
                ingredientMenuExpanded = false
            }
        }) {
            Text("Добавить ингредиент")
        }

        if (state.selectedIngredients.isNotEmpty()) {

            Text("Добавленные ингредиенты:")

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${item.name} (${item.unit})",
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = item.quantityInput,
                            onValueChange = { onDraftQuantityChange(index, it) },
                            label = { Text("Кол-во") },
                            modifier = Modifier.weight(1f)
                        )

                        Button(onClick = { onRemoveDraftIngredient(index) }) {
                            Text("Удалить")
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
}
