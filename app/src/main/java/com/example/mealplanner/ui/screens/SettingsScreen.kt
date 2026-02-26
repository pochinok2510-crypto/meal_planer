package com.example.mealplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.SettingsState

@Composable
fun SettingsScreen(
    settings: SettingsState,
    onBack: () -> Unit,
    onPersistDataToggle: (Boolean) -> Unit,
    onClearAfterExportToggle: (Boolean) -> Unit,
    onExportDatabase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сохранять данные между запусками", modifier = Modifier.weight(1f))
            Switch(
                checked = settings.persistDataBetweenLaunches,
                onCheckedChange = onPersistDataToggle
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Очищать список после экспорта", modifier = Modifier.weight(1f))
            Switch(
                checked = settings.clearShoppingAfterExport,
                onCheckedChange = onClearAfterExportToggle
            )
        }

        Button(onClick = onExportDatabase) {
            Text("Экспорт базы данных (JSON)")
        }

        Button(onClick = onBack) {
            Text("← Назад")
        }
    }
}
