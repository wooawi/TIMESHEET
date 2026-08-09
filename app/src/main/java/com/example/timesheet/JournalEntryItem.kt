package com.example.timesheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.unit.sp
import com.example.timesheet.data.EntryType
import com.example.timesheet.data.LedgerEntry
import com.example.timesheet.data.ShiftType
import java.time.format.DateTimeFormatter

@Composable
fun JournalEntryItem(
    entry: LedgerEntry,
    employeeName: String?,
    organizationName: String?,
    onEdit: (LedgerEntry) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onRecalculate: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy г. EEE")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (entry.type) {
                EntryType.SHIFT -> {
                    val startStr = entry.startTime?.format(timeFormatter) ?: "--:--"
                    val endStr = entry.endTime?.format(timeFormatter) ?: "--:--"
                    val shiftTypeName = when (entry.shiftType) {
                        ShiftType.DAY -> "Дневная"
                        ShiftType.NIGHT -> "Ночная"
                        ShiftType.HOLIDAY -> "Праздничная"
                        ShiftType.OVERTIME -> "Сверхурочная"
                        ShiftType.WEEKEND -> "Выходной день"
                    }
                    val hours = entry.calculateHours()
                    val hoursInt = hours.toInt()
                    val minutes = ((hours - hoursInt) * 60).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${entry.date.format(dateFormatter)}, $startStr - $endStr, ${hoursInt}ч ${minutes.toString().padStart(2, '0')}м",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = formatCurrency(entry.amount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = shiftTypeName,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                        if (entry.note.isNotBlank()) {
                            Text(
                                text = entry.note,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${hoursInt}ч ${minutes.toString().padStart(2, '0')}м",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    if (employeeName != null) {
                        Text(
                            text = "👤 $employeeName",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                    if (organizationName != null) {
                        Text(
                            text = "🏢 $organizationName",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                EntryType.PAYMENT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.date.format(dateFormatter),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = formatCurrency(entry.amount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Выплата",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                EntryType.TAX -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.date.format(dateFormatter),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = formatCurrency(entry.amount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Налог",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                EntryType.ADJUSTMENT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.date.format(dateFormatter),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = formatCurrency(entry.amount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.amount < 0) Color.Red else Color(0xFFFF9800)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (entry.amount < 0) "Удержание" else "Доплата",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                EntryType.EXPENSE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.date.format(dateFormatter),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = formatCurrency(entry.amount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Расход",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    if (entry.expenseCategoryId != null) {
                        Text(
                            text = "Категория: ${entry.expenseCategoryId}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (entry.unitId != null && entry.quantity > 0) {
                        Text(
                            text = "Количество: ${entry.quantity} ${entry.unitId}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { /* TODO: Вложения */ }
                    ) {
                        Text("Вложения", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onEdit(entry) }
                    ) {
                        Text("Редактировать", fontSize = 12.sp, color = Color(0xFF2196F3))
                    }
                    TextButton(
                        onClick = { onRecalculate(entry.id) }
                    ) {
                        Text("Пересчитать", fontSize = 12.sp, color = Color(0xFFFF9800))
                    }
                    TextButton(
                        onClick = { onDelete(entry.id) }
                    ) {
                        Text("Удалить", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { expanded = !expanded }
                ) {
                    Text(
                        text = if (expanded) "Скрыть" else "Подробнее",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}