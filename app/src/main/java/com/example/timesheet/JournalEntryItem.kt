package com.example.timesheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timesheet.data.EntryType
import com.example.timesheet.data.LedgerEntry
import com.example.timesheet.data.ShiftType
import com.example.timesheet.data.TimeType
import java.time.format.DateTimeFormatter

@Composable
fun JournalEntryItem(
    entry: LedgerEntry,
    employeeName: String?,
    organizationName: String?,
    timeTypes: List<TimeType> = emptyList(),
    onEdit: (LedgerEntry) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onRecalculate: (String) -> Unit = {},
    onAttachments: (LedgerEntry) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy г. EEE")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val shiftColor = if (entry.type == EntryType.SHIFT) {
        val shiftTypeName = when (entry.shiftType) {
            ShiftType.DAY -> "Дневная"
            ShiftType.NIGHT -> "Ночная"
            ShiftType.HOLIDAY -> "Праздничная"
            ShiftType.OVERTIME -> "Сверхурочная"
            ShiftType.WEEKEND -> "Выходной день"
        }
        val timeType = timeTypes.find { it.name == shiftTypeName }
        if (timeType != null) {
            try {
                Color(android.graphics.Color.parseColor(timeType.color))
            } catch (e: Exception) {
                Color(0xFF4CAF50)
            }
        } else {
            when (entry.shiftType) {
                ShiftType.DAY -> Color(0xFF4CAF50)
                ShiftType.NIGHT -> Color(0xFF2196F3)
                ShiftType.HOLIDAY -> Color(0xFFFF9800)
                ShiftType.OVERTIME -> Color(0xFF9C27B0)
                ShiftType.WEEKEND -> Color(0xFFF44336)
            }
        }
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (entry.type == EntryType.SHIFT) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = shiftColor,
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                    )
            )
        }

        OutlinedCard(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (entry.type == EntryType.SHIFT) 0.dp else 0.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color.White
            ),
            shape = if (entry.type == EntryType.SHIFT) {
                RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(4.dp)
            }
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

                        val breakHours = 1
                        val breakMinutes = 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.date.format(dateFormatter)}, $startStr - $endStr, ${hoursInt}ч ${minutes.toString().padStart(2, '0')}м",
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatCurrency(entry.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(start = 8.dp)
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
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = entry.note,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "${breakHours}ч ${breakMinutes.toString().padStart(2, '0')}м",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }

                        if (employeeName != null || organizationName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (employeeName != null) {
                                    Text(
                                        text = "👤 $employeeName",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (organizationName != null) {
                                    Text(
                                        text = "🏢 $organizationName",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    EntryType.PAYMENT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.date.format(dateFormatter)}, 00:00",
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatCurrency(entry.amount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(start = 8.dp)
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
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    EntryType.TAX -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.date.format(dateFormatter)}, 00:00",
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatCurrency(entry.amount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier.padding(start = 8.dp)
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
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    EntryType.ADJUSTMENT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.date.format(dateFormatter)}, 00:00",
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatCurrency(entry.amount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.amount < 0) Color.Red else Color(0xFFFF9800),
                                modifier = Modifier.padding(start = 8.dp)
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
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    EntryType.EXPENSE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.date.format(dateFormatter)}, 00:00",
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatCurrency(entry.amount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.padding(start = 8.dp)
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
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entry.unitId != null && entry.quantity > 0) {
                            Text(
                                text = "Количество: ${entry.quantity} ${entry.unitId}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entry.note.isNotBlank()) {
                            Text(
                                text = entry.note,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            onClick = { onAttachments(entry) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = "Вложения",
                                modifier = Modifier.size(16.dp)
                            )
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
}