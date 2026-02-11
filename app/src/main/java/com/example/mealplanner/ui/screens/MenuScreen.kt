package com.example.mealplanner.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Meal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuScreen(
    meals: List<Meal>,
    groups: List<String>,
    selectedMealIds: Set<String>,
    onMealSelectionToggle: (String) -> Unit,
    onRemoveMeal: (Meal) -> Unit,
    onMoveMealToGroup: (Meal, String) -> Unit,
    onDuplicateMealToGroup: (Meal, String) -> Unit,
    onCreateGroup: (String) -> Boolean,
    onDeleteGroup: (String) -> Unit,
    onNavigateToAddMeal: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var newGroupName by remember { mutableStateOf("") }
    var groupError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newGroupName,
                onValueChange = { newGroupName = it },
                label = { Text("Новая группа") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (onCreateGroup(newGroupName)) {
                    newGroupName = ""
                    groupError = null
                } else {
                    groupError = "Группа уже существует или пустая"
                }
            }) {
                Text("Создать")
            }
        }
        groupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (meals.isEmpty()) {
            Text("Пока нет блюд. Добавьте первое блюдо.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { group ->
                    val mealsInGroup = meals.filter { it.group == group }
                    item {
                        GroupHeader(
                            group = group,
                            mealCount = mealsInGroup.size,
                            onDelete = { onDeleteGroup(group) }
                        )
                    }

                    items(mealsInGroup, key = { it.id }) { meal ->
                        MealCard(
                            meal = meal,
                            groups = groups,
                            isSelected = meal.id in selectedMealIds,
                            onToggleSelection = { onMealSelectionToggle(meal.id) },
                            onRemove = { onRemoveMeal(meal) },
                            onMove = { target -> onMoveMealToGroup(meal, target) },
                            onDuplicate = { target -> onDuplicateMealToGroup(meal, target) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onNavigateToAddMeal, modifier = Modifier.weight(1f)) {
                Text("➕ Добавить блюдо")
            }
            Button(onClick = onNavigateToShopping, modifier = Modifier.weight(1f)) {
                Text("🛒 Покупки")
            }
        }

        TextButton(onClick = onNavigateToSettings, modifier = Modifier.align(Alignment.End)) {
            Text("⚙️ Настройки")
        }
    }
}

@Composable
private fun GroupHeader(
    group: String,
    mealCount: Int,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$group ($mealCount)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (group !in listOf("Завтрак", "Перекус", "Обед", "Ужин", "Десерт")) {
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealCard(
    meal: Meal,
    groups: List<String>,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit,
    onMove: (String) -> Unit,
    onDuplicate: (String) -> Unit
) {
    var moveExpanded by remember { mutableStateOf(false) }
    var duplicateExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggleSelection,
                onLongClick = onRemove
            )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                    Text("Ингредиентов: ${meal.ingredients.size}")
                }
                IconButton(onClick = onRemove) {
                    Text("🗑")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { moveExpanded = true }, modifier = Modifier.weight(1f)) {
                    Text("Переместить")
                }
                Button(onClick = { duplicateExpanded = true }, modifier = Modifier.weight(1f)) {
                    Text("Дублировать")
                }
            }

            DropdownMenu(expanded = moveExpanded, onDismissRequest = { moveExpanded = false }) {
                groups.filterNot { it == meal.group }.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onMove(group)
                            moveExpanded = false
                        }
                    )
                }
            }

            DropdownMenu(expanded = duplicateExpanded, onDismissRequest = { duplicateExpanded = false }) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onDuplicate(group)
                            duplicateExpanded = false
                        }
                    )
                }
            }
        }
    }
}
