package com.example.mealplanner.ui.screens

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

@Composable
fun ShoppingListScreen(
    ingredients: List<Ingredient>,
    dayCount: Int,
    isIngredientPurchased: (Ingredient) -> Boolean,
    onIngredientPurchasedChange: (Ingredient, Boolean) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onDayCountChange: (Int) -> Unit,
    onShareViaViber: () -> Unit,
    onShareViaTelegram: () -> Unit,
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

        if (ingredients.isEmpty()) {
            Text("Список покупок пуст.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ingredients, key = { "${it.name}_${it.unit}" }) { ingredient ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isIngredientPurchased(ingredient),
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
                            onShareViaViber()
                            showSendOptions.value = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Вайбер")
                    }
                    Button(
                        onClick = {
                            onShareViaTelegram()
                            showSendOptions.value = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Телеграм")
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
