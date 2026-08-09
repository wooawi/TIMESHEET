package com.example.timesheet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timesheet.data.Employee
import com.example.timesheet.data.Organization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMenuScreen(
    organizations: List<Organization>,
    employees: List<Employee>,
    selectedOrganizationId: String?,
    selectedEmployeeId: String?,
    onBack: () -> Unit,
    onApplyFilter: (String?, String?) -> Unit
) {
    var tempOrgId by remember(selectedOrganizationId) { mutableStateOf(selectedOrganizationId) }
    var tempEmpId by remember(selectedEmployeeId) { mutableStateOf(selectedEmployeeId) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Фильтр", color = Color.White) },
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
                    IconButton(
                        onClick = {
                            onApplyFilter(tempOrgId, tempEmpId)
                            onBack()
                        }
                    ) {
                        Text("Применить", color = Color.White, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5CA02F)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "ОРГАНИЗАЦИЯ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FilterOptionItem(
                label = "Все организации",
                isSelected = tempOrgId == null,
                onClick = { tempOrgId = null }
            )
            organizations.forEach { org ->
                FilterOptionItem(
                    label = org.name,
                    isSelected = tempOrgId == org.id,
                    onClick = { tempOrgId = org.id }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "РАБОТНИК",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FilterOptionItem(
                label = "Все работники",
                isSelected = tempEmpId == null,
                onClick = { tempEmpId = null }
            )
            employees.forEach { emp ->
                FilterOptionItem(
                    label = emp.name,
                    isSelected = tempEmpId == emp.id,
                    onClick = { tempEmpId = emp.id }
                )
            }
        }
    }
}

@Composable
fun FilterOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF2E7D32) else Color.Black
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Выбрано",
                    tint = Color(0xFF5CA02F)
                )
            }
        }
    }
}