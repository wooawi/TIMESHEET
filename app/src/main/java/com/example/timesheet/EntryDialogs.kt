package com.example.timesheet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.timesheet.data.Employee
import com.example.timesheet.data.EntryType
import com.example.timesheet.data.LedgerEntry
import com.example.timesheet.data.Organization
import java.time.LocalDate

/**
 * Универсальный диалог добавления записи журнала.
 * showHours=true -> поле "часы" (для смен), иначе поле "сумма".
 */
@Composable
fun AddEntryDialog(
    title: String,
    entryType: EntryType,
    showHours: Boolean,
    employees: List<Employee>,
    organizations: List<Organization>,
    preselectedEmployeeId: String?,
    preselectedOrganizationId: String?,
    onDismiss: () -> Unit,
    onConfirm: (LedgerEntry) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf(preselectedEmployeeId) }
    var organizationId by remember { mutableStateOf(preselectedOrganizationId) }
    var employeeMenuOpen by remember { mutableStateOf(false) }
    var organizationMenuOpen by remember { mutableStateOf(false) }

    val employeeName = employees.firstOrNull { it.id == employeeId }?.name ?: "Не выбран"
    val organizationName = organizations.firstOrNull { it.id == organizationId }?.name ?: "Не выбрана"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (showHours) "Часы" else "Сумма, ₽") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Комментарий") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row {
                    TextButton(onClick = { employeeMenuOpen = true }) {
                        Text("Сотрудник: $employeeName")
                    }
                    DropdownMenu(expanded = employeeMenuOpen, onDismissRequest = { employeeMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Не выбран") }, onClick = {
                            employeeId = null; employeeMenuOpen = false
                        })
                        employees.forEach { e ->
                            DropdownMenuItem(text = { Text(e.name) }, onClick = {
                                employeeId = e.id; employeeMenuOpen = false
                            })
                        }
                    }
                }

                Row {
                    TextButton(onClick = { organizationMenuOpen = true }) {
                        Text("Организация: $organizationName")
                    }
                    DropdownMenu(expanded = organizationMenuOpen, onDismissRequest = { organizationMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Не выбрана") }, onClick = {
                            organizationId = null; organizationMenuOpen = false
                        })
                        organizations.forEach { o ->
                            DropdownMenuItem(text = { Text(o.name) }, onClick = {
                                organizationId = o.id; organizationMenuOpen = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                onConfirm(
                    LedgerEntry(
                        date = LocalDate.now(),
                        type = entryType,
                        employeeId = employeeId,
                        organizationId = organizationId,
                        amount = if (showHours) 0.0 else value,
                        hours = if (showHours) value else 0.0,
                        note = noteText
                    )
                )
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
