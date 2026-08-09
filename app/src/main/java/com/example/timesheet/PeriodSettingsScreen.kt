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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ========== МОДЕЛЬ ДЛЯ БЫСТРЫХ ПЕРИОДОВ ==========
data class QuickPeriod(
    val id: String,
    val name: String,
    val getRange: () -> Pair<LocalDate, LocalDate>
)

// ========== ПРЕДУСТАНОВЛЕННЫЕ БЫСТРЫЕ ПЕРИОДЫ ==========
fun getQuickPeriods(): List<QuickPeriod> = listOf(
    QuickPeriod("today", "Сегодня") {
        val today = LocalDate.now()
        today to today
    },
    QuickPeriod("this_week", "Эта неделя") {
        val today = LocalDate.now()
        val start = today.minusDays(today.dayOfWeek.value - 1L)
        start to today
    },
    QuickPeriod("this_month", "Этот месяц") {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        start to today
    },
    QuickPeriod("this_year", "Этот год") {
        val today = LocalDate.now()
        val start = today.withDayOfYear(1)
        start to today
    },
    QuickPeriod("yesterday", "Вчера") {
        val yesterday = LocalDate.now().minusDays(1)
        yesterday to yesterday
    },
    QuickPeriod("last_month", "Прошлый месяц") {
        val lastMonth = YearMonth.now().minusMonths(1)
        val start = lastMonth.atDay(1)
        val end = lastMonth.atEndOfMonth()
        start to end
    },
    QuickPeriod("last_year", "Прошлый год") {
        val lastYear = YearMonth.now().minusYears(1)
        val start = lastYear.atDay(1)
        val end = lastYear.atEndOfMonth()
        start to end
    },
    QuickPeriod("all_time", "Весь период") {
        val start = LocalDate.of(2000, 1, 1)
        val end = LocalDate.now()
        start to end
    }
)

// ========== ЭКРАН НАСТРОЙКИ ПЕРИОДА ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSettingsScreen(
    onBack: () -> Unit,
    onApplyPeriod: (LocalDate, LocalDate) -> Unit,
    onApplyQuickPeriod: (String) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var selectedQuickPeriod by remember { mutableStateOf<String?>("this_month") }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Настроить период", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // ===== БЫСТРЫЕ ПЕРИОДЫ =====
            Text(
                text = "Быстрые периоды",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5CA02F),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                items(getQuickPeriods()) { period ->
                    QuickPeriodItem(
                        period = period,
                        isSelected = selectedQuickPeriod == period.id,
                        onClick = {
                            selectedQuickPeriod = period.id
                            val (start, end) = period.getRange()
                            startDate = start
                            endDate = end
                            onApplyQuickPeriod(period.id)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== ВЫБОР ДАТ =====
            Text(
                text = "Произвольный период",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5CA02F),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("От") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        TextButton(
                            onClick = { showStartDatePicker = true }
                        ) {
                            Text("📅")
                        }
                    }
                )

                OutlinedTextField(
                    value = endDate.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("До") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        TextButton(
                            onClick = { showEndDatePicker = true }
                        ) {
                            Text("📅")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onApplyPeriod(startDate, endDate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5CA02F)
                )
            ) {
                Text(
                    text = "Применить период",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            endDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ========== ЭЛЕМЕНТ БЫСТРОГО ПЕРИОДА ==========
@Composable
fun QuickPeriodItem(
    period: QuickPeriod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = period.name,
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