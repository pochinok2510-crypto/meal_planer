package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.ui.presentation.toRussianUnitLabel
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListScreen(
    ingredients: List<Ingredient>,
    categoriesByStorageKey: Map<String, String>,
    dayCount: Int,
    purchasedIngredientKeys: Set<String>,
    onIngredientPurchasedChange: (Ingredient, Boolean) -> Unit,
    onRemoveIngredient: (Ingredient) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onDayCountChange: (Int) -> Unit,
    onSend: () -> Unit,
    onSavePdf: () -> Unit
) {
    val showSendOptions = remember { mutableStateOf(false) }
    val groupedIngredients = remember(ingredients, categoriesByStorageKey) {
        ingredients.groupBy { ingredient ->
            categoriesByStorageKey[ingredient.storageKey()] ?: DEFAULT_CATEGORY
        }
    }
    val orderedCategories = remember(groupedIngredients) {
        groupedIngredients.keys
            .sortedWith(compareBy<String> { it == DEFAULT_CATEGORY }.thenBy { it.lowercase(Locale.getDefault()) })
    }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                orderedCategories.forEach { category ->
                    val categoryIngredients = groupedIngredients[category].orEmpty()
                    if (categoryIngredients.isNotEmpty()) {
                        item(key = "category-$category") {
                            CategorySection(
                                category = category,
                                ingredients = categoryIngredients,
                                purchasedIngredientKeys = purchasedIngredientKeys,
                                onIngredientPurchasedChange = onIngredientPurchasedChange,
                                onRemoveIngredient = onRemoveIngredient
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

@Composable
private fun CategorySection(
    category: String,
    ingredients: List<Ingredient>,
    purchasedIngredientKeys: Set<String>,
    onIngredientPurchasedChange: (Ingredient, Boolean) -> Unit,
    onRemoveIngredient: (Ingredient) -> Unit
) {
    val accentColor = categoryAccentColor(category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = lerp(accentColor, MaterialTheme.colorScheme.surface, 0.88f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        ingredients.forEachIndexed { index, ingredient ->
            val isPurchased = ingredient.storageKey() in purchasedIngredientKeys
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPurchased,
                    onCheckedChange = { checked ->
                        onIngredientPurchasedChange(ingredient, checked)
                    }
                )
                Text(
                    text = "${ingredient.name}: ${ingredient.amount} ${ingredient.unit.toRussianUnitLabel()}",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                IconButton(onClick = { onRemoveIngredient(ingredient) }) {
                    Text("🗑️")
                }
            }

            if (index != ingredients.lastIndex) {
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun categoryAccentColor(category: String): Color {
    val accents = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error
    )
    val index = category.hashCode().mod(accents.size)
    return accents[index]
}

private fun Ingredient.storageKey(): String {
    return "${name.trim().lowercase(Locale.getDefault())}|${unit.trim().lowercase(Locale.getDefault())}"
}

private const val DEFAULT_CATEGORY = "Other"
