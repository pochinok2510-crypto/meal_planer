package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Ingredient
import com.example.mealplanner.ui.presentation.LocalUiDensity
import com.example.mealplanner.ui.presentation.toRussianUnitLabel
import com.example.mealplanner.viewmodel.ShoppingGroup

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListScreen(
    groupedIngredients: List<ShoppingGroup>,
    dayCount: Int,
    animationsEnabled: Boolean,
    purchasedIngredientKeys: Set<String>,
    onIngredientPurchasedChange: (Ingredient, Boolean) -> Unit,
    onRemoveIngredient: (Ingredient) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onDayCountChange: (Int) -> Unit,
    onSend: () -> Unit,
    onSavePdf: () -> Unit,
    onCheckAll: () -> Unit,
    onUncheckAll: () -> Unit
) {
    val density = LocalUiDensity.current
    val contentPadding = 16.dp * density.spacingMultiplier
    val sectionSpacing = 12.dp * density.spacingMultiplier
    val minCardHeight = 64.dp * density.cardHeightMultiplier
    var collapsedGroups by rememberSaveable { mutableStateOf(setOf<String>()) }
    val hasIngredients = groupedIngredients.any { it.ingredients.isNotEmpty() }
    val hasCollapsedGroups = groupedIngredients.any { it.groupId in collapsedGroups }
    val hasExpandedGroups = groupedIngredients.any { it.groupId !in collapsedGroups }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ShoppingActionsBar(
                onSend = onSend,
                onSavePdf = onSavePdf
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Дней: $dayCount", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDayCountChange(dayCount - 1) }) { Text("-1") }
                    Button(onClick = { onDayCountChange(dayCount + 1) }) { Text("+1") }
                }
            }

            if (hasIngredients) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onCheckAll, modifier = Modifier.weight(1f)) {
                        Text("✅ Отметить всё")
                    }
                    TextButton(onClick = onUncheckAll, modifier = Modifier.weight(1f)) {
                        Text("⬜ Снять всё")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            collapsedGroups = groupedIngredients.map { it.groupId }.toSet()
                        },
                        enabled = hasExpandedGroups,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Свернуть все")
                    }
                    TextButton(
                        onClick = { collapsedGroups = emptySet() },
                        enabled = hasCollapsedGroups,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Развернуть все")
                    }
                }
            }

            if (animationsEnabled) {
                AnimatedVisibility(
                    visible = !hasIngredients,
                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
                ) {
                    Text("Список покупок пуст.", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (!hasIngredients) {
                Text("Список покупок пуст.", style = MaterialTheme.typography.bodyLarge)
            }

            if (animationsEnabled) {
                AnimatedVisibility(
                    visible = hasIngredients,
                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(groupedIngredients, key = { it.groupId }) { group ->
                            CategorySection(
                                group = group,
                                purchasedIngredientKeys = purchasedIngredientKeys,
                                isCollapsed = group.groupId in collapsedGroups,
                                onToggleCollapsed = {
                                    collapsedGroups = if (group.groupId in collapsedGroups) {
                                        collapsedGroups - group.groupId
                                    } else {
                                        collapsedGroups + group.groupId
                                    }
                                },
                                onIngredientPurchasedChange = onIngredientPurchasedChange,
                                onRemoveIngredient = onRemoveIngredient
                            )
                        }
                    }
                }
            } else if (hasIngredients) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groupedIngredients, key = { it.groupId }) { group ->
                        CategorySection(
                            group = group,
                            purchasedIngredientKeys = purchasedIngredientKeys,
                            isCollapsed = group.groupId in collapsedGroups,
                            onToggleCollapsed = {
                                collapsedGroups = if (group.groupId in collapsedGroups) {
                                    collapsedGroups - group.groupId
                                } else {
                                    collapsedGroups + group.groupId
                                }
                            },
                            onIngredientPurchasedChange = onIngredientPurchasedChange,
                            onRemoveIngredient = onRemoveIngredient
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("🗑 Очистить")
                }
                Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("← Назад")
                }
            }
        }
    }
}

@Composable
private fun ShoppingActionsBar(
    onSend: () -> Unit,
    onSavePdf: () -> Unit
) {
    Surface(shadowElevation = 6.dp) {
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSend, modifier = Modifier.weight(1f)) {
                    Text("📤 Отправить")
                }
                Button(onClick = onSavePdf, modifier = Modifier.weight(1f)) {
                    Text("💾 Сохранить PDF")
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    group: ShoppingGroup,
    purchasedIngredientKeys: Set<String>,
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onIngredientPurchasedChange: (Ingredient, Boolean) -> Unit,
    onRemoveIngredient: (Ingredient) -> Unit
) {
    val density = LocalUiDensity.current
    val minCardHeight = 64.dp * density.cardHeightMultiplier
    val accentColor = categoryAccentColor(group.groupName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minCardHeight),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = lerp(accentColor, MaterialTheme.colorScheme.surface, 0.88f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapsed)
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
                text = group.groupName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(if (isCollapsed) "▸" else "▾")
        }

        AnimatedVisibility(
            visible = !isCollapsed,
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120))
        ) {
            Column {
                group.ingredients.forEachIndexed { index, ingredient ->
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

                    if (index != group.ingredients.lastIndex) {
                        Divider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
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

private fun Ingredient.storageKey(): String = "${name.trim().lowercase()}|${unit.trim().lowercase()}"
