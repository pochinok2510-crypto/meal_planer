package com.example.mealplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Ingredient

@Composable
fun AddMealScreen(
    onBack: () -> Unit,
    onSaveMeal: (String, List<Ingredient>) -> Unit
) {
    var mealName by remember { mutableStateOf("") }
    var ingredientName by remember { mutableStateOf("") }
    var ingredientAmount by remember { mutableStateOf("") }
    var ingredientUnit by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val ingredients = remember { mutableStateListOf<Ingredient>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = mealName,
            onValueChange = { mealName = it },
            label = { Text("Название блюда") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ingredientName,
            onValueChange = { ingredientName = it },
            label = { Text("Ингредиент") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = ingredientAmount,
                onValueChange = { ingredientAmount = it },
                label = { Text("Количество") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ingredientUnit,
                onValueChange = { ingredientUnit = it },
                label = { Text("Единица") },
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                val amount = ingredientAmount.toDoubleOrNull()
                when {
                    ingredientName.isBlank() -> error = "Введите название ингредиента"
                    amount == null || amount <= 0 -> error = "Количество должно быть числом больше 0"
                    ingredientUnit.isBlank() -> error = "Введите единицу измерения"
                    else -> {
                        ingredients += Ingredient(ingredientName.trim(), amount, ingredientUnit.trim())
                        ingredientName = ""
                        ingredientAmount = ""
                        ingredientUnit = ""
                        error = null
                    }
                }
            }
        ) {
            Text("Добавить ингредиент")
        }

        if (ingredients.isNotEmpty()) {
            Text("Добавленные ингредиенты:")
            ingredients.forEach {
                Text("• ${it.name} — ${it.amount} ${it.unit}")
            }
        }

        error?.let { Text(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("← Назад") }
            Button(onClick = {
                when {
                    mealName.isBlank() -> error = "Название блюда не может быть пустым"
                    ingredients.isEmpty() -> error = "Добавьте хотя бы один ингредиент"
                    else -> {
                        onSaveMeal(mealName, ingredients.toList())
                    }
                }
            }) {
                Text("Сохранить блюдо")
            }
        }
    }
}
