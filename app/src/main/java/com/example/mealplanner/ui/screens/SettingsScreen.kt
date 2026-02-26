package com.example.mealplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    onExportDatabase: () -> Unit,
    onImportDatabaseMerge: () -> Unit,
    onImportDatabaseOverwrite: () -> Unit
) {
    val density = LocalUiDensity.current
    val contentPadding = 16.dp * density.spacingMultiplier
    val sectionSpacing = 8.dp * density.spacingMultiplier
    val minCardHeight = 56.dp * density.cardHeightMultiplier
    val showOverwriteConfirm = remember { mutableStateOf(false) }
    val showBackupDialog = remember { mutableStateOf(false) }

    if (showOverwriteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirm.value = false },
            title = { Text("Подтверждение перезаписи") },
            text = {
                Text(
                    "Импорт с перезаписью обновит группы и план недели данными из файла. " +
                        "Блюда и ингредиенты будут безопасно объединены без удаления текущей базы."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverwriteConfirm.value = false
                    onImportDatabaseOverwrite()
                }) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirm.value = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showBackupDialog.value) {
        AlertDialog(
            onDismissRequest = { showBackupDialog.value = false },
            title = { Text("Резервная копия") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
                    Text(
                        "Экспортируйте базу данных в JSON или восстановите данные из резервной копии."
                    )
                    Button(onClick = {
                        showBackupDialog.value = false
                        onExportDatabase()
                    }) {
                        Text("Экспорт базы данных")
                    }
                    Button(onClick = {
                        showBackupDialog.value = false
                        onImportDatabaseMerge()
                    }) {
                        Text("Импорт (безопасное объединение)")
                    }
                    Button(onClick = {
                        showBackupDialog.value = false
                        showOverwriteConfirm.value = true
                    }) {
                        Text("Импорт (с перезаписью плана)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog.value = false }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog.value = false }) {
                    Text("Закрыть")
                }
            },
            tonalElevation = 4.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(contentPadding)
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

        SettingsSectionCard(minCardHeight = minCardHeight) {
            Text("Тема приложения")
            ThemeOptionRow("Светлая", settings.themeMode == AppThemeMode.LIGHT) {
                onThemeModeSelect(AppThemeMode.LIGHT)
            }
            ThemeOptionRow("Тёмная", settings.themeMode == AppThemeMode.DARK) {
                onThemeModeSelect(AppThemeMode.DARK)
            }
            ThemeOptionRow("Системная", settings.themeMode == AppThemeMode.SYSTEM) {
                onThemeModeSelect(AppThemeMode.SYSTEM)
            }
        }

        SettingsSectionCard(minCardHeight = minCardHeight) {
            Text("Акцентный цвет")
            AccentOptionRow("Изумруд", settings.accentPalette == AccentPalette.EMERALD) {
                onAccentPaletteSelect(AccentPalette.EMERALD)
            }
            AccentOptionRow("Океан", settings.accentPalette == AccentPalette.OCEAN) {
                onAccentPaletteSelect(AccentPalette.OCEAN)
            }
            AccentOptionRow("Закат", settings.accentPalette == AccentPalette.SUNSET) {
                onAccentPaletteSelect(AccentPalette.SUNSET)
            }
            AccentOptionRow("Лаванда", settings.accentPalette == AccentPalette.LAVENDER) {
                onAccentPaletteSelect(AccentPalette.LAVENDER)
            }
        }

        SettingsSectionCard(minCardHeight = minCardHeight) {
            Text("Плотность интерфейса")
            DensityOptionRow("Компактная", settings.densityMode == DensityMode.COMPACT) {
                onDensityModeSelect(DensityMode.COMPACT)
            }
            DensityOptionRow("Нормальная", settings.densityMode == DensityMode.NORMAL) {
                onDensityModeSelect(DensityMode.NORMAL)
            }
        }

        SettingsSectionCard(minCardHeight = minCardHeight) {
            Text("Резервная копия и восстановление")
            Text(
                "Быстрый доступ к экспорту и импорту базы данных.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { showBackupDialog.value = true }) {
                Text("Открыть инструменты")
            }
        }

        Button(onClick = onBack) {
            Text("← Назад")
        }
    }
}

@Composable
private fun SettingsSectionCard(
    minCardHeight: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val density = LocalUiDensity.current
    val sectionPadding = 12.dp * density.spacingMultiplier
    val sectionSpacing = 8.dp * density.spacingMultiplier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minCardHeight),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(sectionPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            content()
        }
    }
}

@Composable
private fun ThemeOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}

@Composable
private fun AccentOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}

@Composable
private fun DensityOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}
