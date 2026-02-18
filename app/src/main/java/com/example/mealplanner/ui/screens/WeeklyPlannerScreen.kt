package com.example.mealplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.Meal
import com.example.mealplanner.model.MealSlot
import com.example.mealplanner.model.PlanDay

@Composable
fun WeeklyPlannerScreen(
    meals: List<Meal>,
    weeklyPlan: Map<Pair<PlanDay, MealSlot>, String>,
    onAssignMeal: (PlanDay, MealSlot, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PlanDay.entries) { day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(day.title, style = MaterialTheme.typography.titleMedium)
                    MealSlot.entries.forEach { slot ->
                        val assignedId = weeklyPlan[day to slot]
                        val mealOptions = meals.filter { it.group == slot.title }.ifEmpty { meals }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSelectorRow(
    slot: MealSlot,
    selectedMealId: String?,
    mealOptions: List<Meal>,
    onAssign: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = mealOptions.firstOrNull { it.id == selectedMealId }?.name
        ?: "Не выбрано"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text(slot.title) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
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
