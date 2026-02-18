package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.data.MealsRepository
import com.example.mealplanner.model.Meal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuScreen(
    meals: List<Meal>,
    groups: List<String>,
    onRemoveMeal: (Meal) -> Unit,
    onMoveMealToGroup: (Meal, String) -> Unit,
    onDuplicateMealToGroup: (Meal, String) -> Unit,
    onCreateGroup: (String) -> Boolean,
    onDeleteGroup: (String) -> Unit,
    onEditGroup: (String, String) -> Boolean,
    onNavigateToAddMeal: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var newGroupName by remember { mutableStateOf("") }
    var groupError by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var groupPendingDelete by remember { mutableStateOf<String?>(null) }
    var groupPendingEdit by remember { mutableStateOf<String?>(null) }
    var editGroupName by remember { mutableStateOf("") }
    var expandedGroups by rememberSaveable { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val normalizedSearch = searchQuery.trim().lowercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
@@ -104,57 +102,55 @@ fun MenuScreen(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { group ->
                    val mealsInGroup = meals.filter { meal ->
                        meal.group == group && (normalizedSearch.isBlank() || meal.name.lowercase().contains(normalizedSearch))
                    }
                    val isExpanded = expandedGroups[group] ?: true

                    item(key = "group_$group") {
                        GroupHeader(
                            group = group,
                            mealCount = mealsInGroup.size,
                            isExpanded = isExpanded,
                            canManage = group !in MealsRepository.DEFAULT_GROUPS && group != MealsRepository.UNCATEGORIZED_GROUP,
                            onToggleExpanded = {
                                expandedGroups = expandedGroups + (group to !isExpanded)
                            },
                            onEdit = {
                                groupPendingEdit = group
                                editGroupName = group
                            },
                            onDelete = { groupPendingDelete = group }
                        )
                    }

                    items(mealsInGroup, key = { it.id }) { meal ->
                        AnimatedVisibility(visible = isExpanded) {
                            MealCard(
                                meal = meal,
                                groups = groups,
                                onRemove = { onRemoveMeal(meal) },
                                onMove = { target -> onMoveMealToGroup(meal, target) },
                                onDuplicate = { target -> onDuplicateMealToGroup(meal, target) }
                            )
                        }
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
@@ -204,99 +200,93 @@ fun MenuScreen(
            },
            dismissButton = {
                TextButton(onClick = { groupPendingEdit = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun GroupHeader(
    group: String,
    mealCount: Int,
    isExpanded: Boolean,
    canManage: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onToggleExpanded) {
            Text(if (isExpanded) "▲" else "▼")
        }
        Text(
            text = "$group ($mealCount)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (canManage) {
            TextButton(onClick = onEdit) {
                Text("Изм.")
            }
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
                onClick = {},
                onLongClick = onRemove
            )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
