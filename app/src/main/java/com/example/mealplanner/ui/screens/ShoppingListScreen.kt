package com.example.mealplanner.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Ingredient

@Composable
fun ShoppingListScreen(
    ingredients: List<Ingredient>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ingredients.isEmpty()) {
            Text("Список покупок пуст.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ingredients) { ingredient ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${ingredient.name}: ${ingredient.amount} ${ingredient.unit}",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                Text("📄 Экспорт PDF")
            }
            Button(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("🗑 Очистить")
            }
        }
        Button(onClick = onBack) {
            Text("← Назад")
        }
    }
}
