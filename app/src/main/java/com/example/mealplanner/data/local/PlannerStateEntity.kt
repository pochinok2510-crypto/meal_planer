package com.example.mealplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_state")
data class PlannerStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val mealsJson: String,
    val groupsJson: String,
    val purchasedIngredientKeysJson: String,
    val weeklyPlanJson: String,
    val dayCount: Int
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
