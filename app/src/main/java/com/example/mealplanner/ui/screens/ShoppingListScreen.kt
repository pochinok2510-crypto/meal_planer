package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Ingredient
import java.util.Locale

@Composable
fun ShoppingListScreen(
    ingredients: List<Ingredient>,
    dayCount: Int,
    purchasedIngredientKeys: Set<String>,
    onIngredientPurchasedChange: (Ingredient, Boolean) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onDayCountChange: (Int) -> Unit,
    onSend: () -> Unit,
    onSavePdf: () -> Unit
) {
    val showSendOptions = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Дней: $dayCount", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDayCountChange(dayCount - 1) }) { Text("-1") }
                Button(onClick = { onDayCountChange(dayCount + 1) }) { Text("+1") }
            }
        }

        AnimatedVisibility(
            visible = ingredients.isEmpty(),
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
        ) {
            Text("Список покупок пуст.", style = MaterialTheme.typography.bodyLarge)
        }

        AnimatedVisibility(
            visible = ingredients.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ingredients, key = { it.storageKey() }) { ingredient ->
                    val isPurchased = ingredient.storageKey() in purchasedIngredientKeys
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPurchased,
                                onCheckedChange = { checked ->
                                    onIngredientPurchasedChange(ingredient, checked)
                                }
                            )
                            Text(
                                text = "${ingredient.name}: ${ingredient.amount} ${ingredient.unit}",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { showSendOptions.value = true }, modifier = Modifier.weight(1f)) {
                Text("📤 Отправить")
            }
            Button(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("🗑 Очистить")
            }
        }
        Button(onClick = onBack) {
            Text("← Назад")
        }
    }

    if (showSendOptions.value) {
        AlertDialog(
            onDismissRequest = { showSendOptions.value = false },
            title = { Text("Отправить список") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSend()
                            showSendOptions.value = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть меню отправки")
                    }
                    Button(
                        onClick = {
                            onSavePdf()
                            showSendOptions.value = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сохранить в PDF")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSendOptions.value = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

private fun Ingredient.storageKey(): String {
    return "${name.trim().lowercase(Locale.getDefault())}|${unit.trim().lowercase(Locale.getDefault())}"
}
