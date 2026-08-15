package com.example.timesheet.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ShiftsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenPeriodSettings: () -> Unit = {},
    onOpenShiftJournal: () -> Unit = {},
    onOpenTemplates: () -> Unit = {} // параметр оставлен для совместимости, но не используется
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Настроить период") },
            onClick = {
                onDismiss()
                onOpenPeriodSettings()
            }
        )
        DropdownMenuItem(
            text = { Text("Журнал смен") },
            onClick = {
                onDismiss()
                onOpenShiftJournal()
            }
        )
        // Пункт "Шаблоны смен" удален согласно ТЗ
    }
}