package com.example.timesheet.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.timesheet.AddEntryRequest
import com.example.timesheet.data.EntryType

@Composable
fun AddFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFFFF9800),
        contentColor = Color.White,
        modifier = Modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Добавить"
        )
    }
}

/**
 * ИЗМЕНЕНО (ТЗ): убран пункт «Расход» из меню по нажатию «+».
 * Добавление расхода теперь возможно только другими способами (если потребуются в будущем).
 */
@Composable
fun AddMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AddEntryRequest) -> Unit
) {
    fun select(request: AddEntryRequest) {
        onSelect(request)
        onDismiss()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
    ) {
        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Выплата")
                }
            },
            onClick = { select(AddEntryRequest("Выплата", EntryType.PAYMENT, showHours = false)) }
        )

        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Filled.MoneyOff,
                        contentDescription = null,
                        tint = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Налог")
                }
            },
            onClick = { select(AddEntryRequest("Налог", EntryType.TAX, showHours = false)) }
        )

        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Запись табеля")
                }
            },
            onClick = { select(AddEntryRequest("Запись табеля", EntryType.SHIFT, showHours = true)) }
        )

        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Filled.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Доплата, удержание")
                }
            },
            onClick = { select(AddEntryRequest("Доплата (+) или удержание (-)", EntryType.ADJUSTMENT, showHours = false)) }
        )

        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Filled.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Смена по шаблону")
                }
            },
            onClick = { select(AddEntryRequest("Смена по шаблону", EntryType.SHIFT, showHours = true)) }
        )

        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Filled.PermContactCalendar,
                        contentDescription = null,
                        tint = Color(0xFF795548)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Смена")
                }
            },
            onClick = { select(AddEntryRequest("Смена", EntryType.SHIFT, showHours = true)) }
        )

        // Пункт «Расход» удалён согласно ТЗ.
    }
}