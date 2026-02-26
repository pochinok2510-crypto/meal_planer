package com.example.mealplanner.ui.presentation

import java.util.Locale

private val RUSSIAN_UNITS_BY_KEY = mapOf(
    "g" to "г",
    "kg" to "кг",
    "ml" to "мл",
    "l" to "л",
    "pcs" to "шт",
    "tsp" to "ч.л.",
    "tbsp" to "ст.л.",
    "pack" to "уп."
)

fun String.toRussianUnitLabel(): String {
    val normalizedKey = trim().lowercase(Locale.ROOT)
    return RUSSIAN_UNITS_BY_KEY[normalizedKey] ?: this
}
