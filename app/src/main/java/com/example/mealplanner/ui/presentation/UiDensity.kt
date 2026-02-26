package com.example.mealplanner.ui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.mealplanner.model.DensityMode

@Immutable
data class UiDensitySpec(
    val spacingMultiplier: Float,
    val cardHeightMultiplier: Float,
    val fontScaleMultiplier: Float
)

val LocalUiDensity = staticCompositionLocalOf {
    UiDensitySpec(
        spacingMultiplier = 1f,
        cardHeightMultiplier = 1f,
        fontScaleMultiplier = 1f
    )
}

fun DensityMode.toUiDensitySpec(): UiDensitySpec {
    return when (this) {
        DensityMode.COMPACT -> UiDensitySpec(
            spacingMultiplier = 0.85f,
            cardHeightMultiplier = 0.9f,
            fontScaleMultiplier = 0.94f
        )

        DensityMode.NORMAL -> UiDensitySpec(
            spacingMultiplier = 1f,
            cardHeightMultiplier = 1f,
            fontScaleMultiplier = 1f
        )
    }
}

@Composable
fun ProvideUiDensity(densityMode: DensityMode, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalUiDensity provides densityMode.toUiDensitySpec()) {
        content()
    }
}
