package com.example.mealplanner.model

enum class PlanDay(val title: String) {
    MONDAY("Понедельник"),
    TUESDAY("Вторник"),
    WEDNESDAY("Среда"),
    THURSDAY("Четверг"),
    FRIDAY("Пятница"),
    SATURDAY("Суббота"),
    SUNDAY("Воскресенье")
}

enum class MealSlot(val title: String) {
    BREAKFAST("Завтрак"),
    SNACK("Перекус"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    DESSERT("Десерт")
}

data class WeeklyPlanAssignment(
    val day: PlanDay,
    val slot: MealSlot,
    val mealId: String
)
