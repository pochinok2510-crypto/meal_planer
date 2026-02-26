package com.example.mealplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    onExportDatabase: () -> Unit,
    onImportDatabaseMerge: () -> Unit,
    onImportDatabaseOverwrite: () -> Unit
) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Резервная копия и восстановление")
                Text(
                    "Быстрый доступ к экспорту и импорту базы данных.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { showBackupDialog.value = true }) {
                    Text("Открыть инструменты")
                }
            }
        }

        Button(onClick = onBack) {
            Text("← Назад")
        }
    }
}
