package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Meal
import com.example.mealplanner.model.MealSlot
import com.example.mealplanner.model.PlanDay
import com.example.mealplanner.ui.presentation.LocalUiDensity

@Composable
fun WeeklyPlannerScreen(
    meals: List<Meal>,
    weeklyPlan: Map<Pair<PlanDay, MealSlot>, String>,
    onAssignMeal: (PlanDay, MealSlot, String?) -> Unit
) {
    val density = LocalUiDensity.current
    val contentPadding = 16.dp * density.spacingMultiplier
    val sectionSpacing = 12.dp * density.spacingMultiplier
    val cardPadding = 12.dp * density.spacingMultiplier
    val rowSpacing = 8.dp * density.spacingMultiplier
    val minCardHeight = 84.dp * density.cardHeightMultiplier
    val daySlotOrder = remember {
        mutableStateMapOf<PlanDay, List<MealSlot>>().apply {
            PlanDay.entries.forEach { day ->
                put(day, MealSlot.entries.toList())
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        items(
            items = PlanDay.entries.toList(),
            key = { day -> day.name }
        ) { day ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardPadding),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing)
                ) {
                    Text(day.title, style = MaterialTheme.typography.titleMedium)
                    var draggingIndex by remember(day) { mutableStateOf<Int?>(null) }
                    val orderedSlots = daySlotOrder[day].orEmpty()
                    orderedSlots.forEachIndexed { index, slot ->
                        val assignedId = weeklyPlan[day to slot]
                        val mealOptions = meals.filter { it.group == slot.title }.ifEmpty { meals }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(orderedSlots) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = index
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                            if (dragAmount.y > 20f && currentIndex < orderedSlots.lastIndex) {
                                                daySlotOrder[day] = orderedSlots.toMutableList().apply {
                                                    add(currentIndex + 1, removeAt(currentIndex))
                                                }
                                                draggingIndex = currentIndex + 1
                                            } else if (dragAmount.y < -20f && currentIndex > 0) {
                                                daySlotOrder[day] = orderedSlots.toMutableList().apply {
                                                    add(currentIndex - 1, removeAt(currentIndex))
                                                }
                                                draggingIndex = currentIndex - 1
                                            }
                                        }
                                    )
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⋮⋮", modifier = Modifier.padding(top = 16.dp))
                            MealSelectorRow(
                                slot = slot,
                                selectedMealId = assignedId,
                                mealOptions = mealOptions,
                                onAssign = { onAssignMeal(day, slot, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSelectorRow(
    slot: MealSlot,
    selectedMealId: String?,
    mealOptions: List<Meal>,
    onAssign: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = selectedMealId,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                },
                label = "meal_selection"
            ) { targetMealId ->
                val selectedName = mealOptions.firstOrNull { it.id == targetMealId }?.name
                    ?: "Не выбрано"
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(slot.title) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Удалить") },
                    onClick = {
                        onAssign(null)
                        expanded = false
                    }
                )
                mealOptions.forEach { meal ->
                    DropdownMenuItem(
                        text = { Text(meal.name) },
                        onClick = {
                            onAssign(meal.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        TextButton(onClick = { onAssign(null) }) {
            Text("Очистить")
        }
    }
}
