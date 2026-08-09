package com.example.timesheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.timesheet.data.Employee
import com.example.timesheet.data.Organization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onMenuClick: () -> Unit,
    onOrganizationsClick: () -> Unit,
    organizationsMenuExpanded: Boolean,
    onOrganizationsMenuDismiss: () -> Unit,
    organizations: List<Organization>,
    selectedOrganizationId: String?,
    onOrganizationSelect: (String?) -> Unit,
    onPersonClick: () -> Unit,
    personMenuExpanded: Boolean,
    onPersonMenuDismiss: () -> Unit,
    employees: List<Employee>,
    selectedEmployeeId: String?,
    onEmployeeSelect: (String?) -> Unit,
    onShiftsClick: () -> Unit,
    shiftsMenuExpanded: Boolean,
    onShiftsMenuDismiss: () -> Unit,
    onOpenPeriodSettings: () -> Unit = {},
    onOpenShiftJournal: () -> Unit = {},
    onOpenTemplates: () -> Unit = {}
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Меню",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = onOrganizationsClick) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = "Организация",
                        modifier = Modifier.size(32.dp)
                    )
                }
                OrganizationFilterMenu(
                    expanded = organizationsMenuExpanded,
                    onDismiss = onOrganizationsMenuDismiss,
                    organizations = organizations,
                    selectedId = selectedOrganizationId,
                    onSelect = onOrganizationSelect
                )
            }
            Box {
                IconButton(onClick = onPersonClick) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Сотрудник",
                        modifier = Modifier.size(32.dp)
                    )
                }
                EmployeeFilterMenu(
                    expanded = personMenuExpanded,
                    onDismiss = onPersonMenuDismiss,
                    employees = employees,
                    selectedId = selectedEmployeeId,
                    onSelect = onEmployeeSelect
                )
            }
            Box {
                IconButton(onClick = onShiftsClick) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Смены",
                        modifier = Modifier.size(32.dp)
                    )
                }
                ShiftsMenu(
                    expanded = shiftsMenuExpanded,
                    onDismiss = onShiftsMenuDismiss,
                    onOpenPeriodSettings = onOpenPeriodSettings,
                    onOpenShiftJournal = onOpenShiftJournal,
                    onOpenTemplates = onOpenTemplates
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF5CA02F),
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}