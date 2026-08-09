package com.example.timesheet.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.timesheet.data.Employee
import com.example.timesheet.data.Organization

@Composable
fun <T> FilterDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<T>,
    idOf: (T) -> String,
    nameOf: (T) -> String,
    selectedId: String?,
    allLabel: String,
    onSelect: (String?) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(allLabel) },
            leadingIcon = if (selectedId == null) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else null,
            onClick = {
                onSelect(null)
                onDismiss()
            },
            modifier = Modifier.width(220.dp)
        )
        if (items.isEmpty()) {
            DropdownMenuItem(
                text = { Text("Список пуст", color = Color.Gray) },
                onClick = { onDismiss() },
                modifier = Modifier.width(220.dp)
            )
        }
        items.forEach { item ->
            val id = idOf(item)
            DropdownMenuItem(
                text = { Text(nameOf(item)) },
                leadingIcon = if (selectedId == id) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
                onClick = {
                    onSelect(id)
                    onDismiss()
                },
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

@Composable
fun EmployeeFilterMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    employees: List<Employee>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    FilterDropdownMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        items = employees,
        idOf = { it.id },
        nameOf = { it.name },
        selectedId = selectedId,
        allLabel = "Все сотрудники",
        onSelect = onSelect
    )
}

@Composable
fun OrganizationFilterMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    organizations: List<Organization>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    FilterDropdownMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        items = organizations,
        idOf = { it.id },
        nameOf = { it.name },
        selectedId = selectedId,
        allLabel = "Все организации",
        onSelect = onSelect
    )
}