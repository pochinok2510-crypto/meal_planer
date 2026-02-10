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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Meal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuScreen(
    meals: List<Meal>,
    selectedMeals: Set<String>,
    onMealSelectionToggle: (String) -> Unit,
    onRemoveMeal: (Meal) -> Unit,
    onNavigateToAddMeal: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (meals.isEmpty()) {
            Text("Пока нет блюд. Добавьте первое блюдо.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(meals) { meal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onMealSelectionToggle(meal.name) },
                                onLongClick = { onRemoveMeal(meal) }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = meal.name in selectedMeals,
                                onCheckedChange = { onMealSelectionToggle(meal.name) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                                Text("Ингредиентов: ${meal.ingredients.size}")
                            }
                            IconButton(onClick = { onRemoveMeal(meal) }) {
                                Text("🗑")
                            }
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
    }
}
