package com.example.timesheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.timesheet.data.Employee
import com.example.timesheet.data.LedgerEntry
import com.example.timesheet.data.Organization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftJournalScreen(
    entries: List<LedgerEntry>,
    employees: List<Employee>,
    organizations: List<Organization>,
    onBack: () -> Unit,
    onFilterClick: () -> Unit,
    onEditEntry: (LedgerEntry) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Журнал смен", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Фильтр",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5CA02F)
                )
            )
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет записей в журнале")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                items(entries.sortedByDescending { it.date }, key = { it.id }) { entry ->
                    JournalEntryItem(
                        entry = entry,
                        employeeName = employees.find { it.id == entry.employeeId }?.name,
                        organizationName = organizations.find { it.id == entry.organizationId }?.name,
                        onEdit = { onEditEntry(it) },
                        onDelete = { onDeleteEntry(it) },
                        onRecalculate = {}
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}