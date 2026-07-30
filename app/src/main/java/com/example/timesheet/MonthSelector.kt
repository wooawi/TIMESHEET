package com.example.timesheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.time.YearMonth

val monthNamesRu = listOf(
    "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
)

fun formatMonth(month: YearMonth): String = "${monthNamesRu[month.monthValue - 1]} ${month.year}"

fun formatCurrency(value: Double): String {
    // Форматирование без зависимости от локали устройства.
    val rounded = Math.round(value * 100) / 100.0
    val sign = if (rounded < 0) "-" else ""
    val abs = kotlin.math.abs(rounded)
    val whole = abs.toLong()
    val frac = Math.round((abs - whole) * 100)
    val wholeStr = whole.toString().reversed().chunked(3).joinToString(" ").reversed()
    return "$sign$wholeStr,${frac.toString().padStart(2, '0')} ₽"
}

/** Панель "< июль 2026 >" — стрелки листают месяц, тап по названию открывает выбор даты. */
@Composable
fun MonthHeaderBar(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: (YearMonth) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6CB143))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий месяц", tint = Color.White)
        }
        Text(
            text = formatMonth(month),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { showPicker = true }
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий месяц", tint = Color.White)
        }
    }

    if (showPicker) {
        MonthPickerDialog(
            initialMonth = month,
            onDismiss = { showPicker = false },
            onConfirm = {
                onPick(it)
                showPicker = false
            }
        )
    }
}

/** Диалог выбора месяца и года (сетка месяцев + переключение года стрелками). */
@Composable
fun MonthPickerDialog(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    var year by remember { mutableStateOf(initialMonth.year) }
    var selectedMonthValue by remember { mutableStateOf(initialMonth.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { year -= 1 }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий год")
                }
                Text(year.toString(), fontWeight = FontWeight.Bold)
                IconButton(onClick = { year += 1 }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий год")
                }
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.size(280.dp, 200.dp)
            ) {
                items(12) { index ->
                    val monthValue = index + 1
                    val selected = monthValue == selectedMonthValue
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(if (selected) Color(0xFF5CA02F) else Color.Transparent)
                            .clickable { selectedMonthValue = monthValue }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monthNamesRu[index].replaceFirstChar { it.uppercase() },
                            color = if (selected) Color.White else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(year, selectedMonthValue)) }) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}