package com.example.mealplanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.model.AccentPalette
import com.example.mealplanner.model.AppThemeMode
import com.example.mealplanner.model.DensityMode
import com.example.mealplanner.model.SettingsState
import com.example.mealplanner.ui.presentation.LocalUiDensity

@Composable
fun SettingsScreen(
    settings: SettingsState,
    onBack: () -> Unit,
    onPersistDataToggle: (Boolean) -> Unit,
    onClearAfterExportToggle: (Boolean) -> Unit,
    onThemeModeSelect: (AppThemeMode) -> Unit,
    onAccentPaletteSelect: (AccentPalette) -> Unit,
    onDensityModeSelect: (DensityMode) -> Unit,
    onAnimationsToggle: (Boolean) -> Unit,
    onExportDatabase: () -> Unit,
    onImportDatabaseMerge: () -> Unit,
    onImportDatabaseOverwrite: () -> Unit
) {
    val density = LocalUiDensity.current
    val contentPadding = 16.dp * density.spacingMultiplier
    val sectionSpacing = 8.dp * density.spacingMultiplier
    val minCardHeight = 56.dp * density.cardHeightMultiplier
    val showOverwriteConfirm = remember { mutableStateOf(false) }
    val expandedSections = remember { mutableStateListOf("General") }
    var showColors by remember { mutableStateOf(false) }

    if (showOverwriteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirm.value = false },
            title = { Text("Подтверждение перезаписи") },
            text = { Text("Импорт с перезаписью обновит группы и план недели данными из файла.") },
            confirmButton = {
                TextButton(onClick = {
                    showOverwriteConfirm.value = false
                    onImportDatabaseOverwrite()
                }) { Text("Импортировать") }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirm.value = false }) { Text("Отмена") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(contentPadding)
    ) {
        item {
            ExpandableSection(
                title = "General",
                minCardHeight = minCardHeight,
                expanded = "General" in expandedSections,
                onToggle = { toggleSection(expandedSections, "General") }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Сохранять данные между запусками", modifier = Modifier.weight(1f))
                    Switch(checked = settings.persistDataBetweenLaunches, onCheckedChange = onPersistDataToggle)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Очищать список после экспорта", modifier = Modifier.weight(1f))
                    Switch(checked = settings.clearShoppingAfterExport, onCheckedChange = onClearAfterExportToggle)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Анимации интерфейса", modifier = Modifier.weight(1f))
                    Switch(checked = settings.animationsEnabled, onCheckedChange = onAnimationsToggle)
                }
            }
        }

        item {
            ExpandableSection(
                title = "Theme & Colors",
                minCardHeight = minCardHeight,
                expanded = "Theme" in expandedSections,
                onToggle = { toggleSection(expandedSections, "Theme") }
            ) {
                Text("Тема приложения")
                ThemeOptionRow("Светлая", settings.themeMode == AppThemeMode.LIGHT) { onThemeModeSelect(AppThemeMode.LIGHT) }
                ThemeOptionRow("Тёмная", settings.themeMode == AppThemeMode.DARK) { onThemeModeSelect(AppThemeMode.DARK) }
                ThemeOptionRow("Системная", settings.themeMode == AppThemeMode.SYSTEM) { onThemeModeSelect(AppThemeMode.SYSTEM) }

                TextButton(onClick = { showColors = !showColors }) {
                    Text(if (showColors) "Скрыть цвета" else "Показать цвета")
                }
                if (showColors) {
                    AccentOptionRow("Изумруд", settings.accentPalette == AccentPalette.EMERALD) { onAccentPaletteSelect(AccentPalette.EMERALD) }
                    AccentOptionRow("Океан", settings.accentPalette == AccentPalette.OCEAN) { onAccentPaletteSelect(AccentPalette.OCEAN) }
                    AccentOptionRow("Закат", settings.accentPalette == AccentPalette.SUNSET) { onAccentPaletteSelect(AccentPalette.SUNSET) }
                    AccentOptionRow("Лаванда", settings.accentPalette == AccentPalette.LAVENDER) { onAccentPaletteSelect(AccentPalette.LAVENDER) }
                }

                Text("Плотность интерфейса")
                DensityOptionRow("Компактная", settings.densityMode == DensityMode.COMPACT) { onDensityModeSelect(DensityMode.COMPACT) }
                DensityOptionRow("Нормальная", settings.densityMode == DensityMode.NORMAL) { onDensityModeSelect(DensityMode.NORMAL) }
            }
        }

        item {
            ExpandableSection(
                title = "Import / Export",
                minCardHeight = minCardHeight,
                expanded = "ImportExport" in expandedSections,
                onToggle = { toggleSection(expandedSections, "ImportExport") }
            ) {
                Button(onClick = onExportDatabase, modifier = Modifier.fillMaxWidth()) { Text("Экспорт базы данных") }
                Button(onClick = onImportDatabaseMerge, modifier = Modifier.fillMaxWidth()) { Text("Импорт (безопасное объединение)") }
                Button(onClick = { showOverwriteConfirm.value = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Импорт (с перезаписью плана)")
                }
            }
        }

        item {
            ExpandableSection(
                title = "Database",
                minCardHeight = minCardHeight,
                expanded = "Database" in expandedSections,
                onToggle = { toggleSection(expandedSections, "Database") }
            ) {
                Text("Инструменты резервного копирования доступны в разделе Import / Export")
            }
        }

        item { Button(onClick = onBack) { Text("← Назад") } }
    }
}

private fun toggleSection(expandedSections: MutableList<String>, key: String) {
    if (key in expandedSections) expandedSections.remove(key) else expandedSections.add(key)
}

@Composable
private fun ExpandableSection(
    title: String,
    minCardHeight: androidx.compose.ui.unit.Dp,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalUiDensity.current
    val sectionPadding = 12.dp * density.spacingMultiplier
    val sectionSpacing = 8.dp * density.spacingMultiplier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minCardHeight)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(sectionPadding), verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (expanded) content()
        }
    }
}

@Composable
private fun ThemeOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}

@Composable
private fun AccentOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}

@Composable
private fun DensityOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}
