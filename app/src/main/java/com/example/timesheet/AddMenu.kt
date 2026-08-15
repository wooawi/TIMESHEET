package com.example.timesheet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
 * УДАЛЕН пункт «Смена по шаблону» согласно ТЗ.
 * Меню смещено ниже — увеличен отступ от кнопки.
 */
@Composable
fun AddMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AddEntryRequest) -> Unit
) {
    if (!expanded) return

    fun select(request: AddEntryRequest) {
        onSelect(request)
        onDismiss()
    }

    // Увеличен отступ от кнопки — меню поднимается меньше, чтобы быть ниже
    val density = LocalDensity.current
    val liftAboveFab = with(density) { 56.dp.roundToPx() } // было 72.dp

    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(x = 0, y = -liftAboveFab),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.width(240.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                AddMenuRow(
                    icon = Icons.Filled.CreditCard,
                    tint = Color(0xFF4CAF50),
                    label = "Выплата",
                    onClick = { select(AddEntryRequest("Выплата", EntryType.PAYMENT, showHours = false)) }
                )

                AddMenuRow(
                    icon = Icons.Filled.MoneyOff,
                    tint = Color(0xFFF44336),
                    label = "Налог",
                    onClick = { select(AddEntryRequest("Налог", EntryType.TAX, showHours = false)) }
                )

                AddMenuRow(
                    icon = Icons.Filled.CalendarMonth,
                    tint = Color(0xFF2196F3),
                    label = "Запись табеля",
                    onClick = { select(AddEntryRequest("Запись табеля", EntryType.SHIFT, showHours = true)) }
                )

                AddMenuRow(
                    icon = Icons.Filled.AttachMoney,
                    tint = Color(0xFFFF9800),
                    label = "Доплата, удержание",
                    onClick = { select(AddEntryRequest("Доплата (+) или удержание (-)", EntryType.ADJUSTMENT, showHours = false)) }
                )

                // УДАЛЕН пункт «Смена по шаблону» (был с иконкой Assignment)

                AddMenuRow(
                    icon = Icons.Filled.PermContactCalendar,
                    tint = Color(0xFF795548),
                    label = "Смена",
                    onClick = { select(AddEntryRequest("Смена", EntryType.SHIFT, showHours = true)) }
                )
            }
        }
    }
}

@Composable
private fun AddMenuRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label)
    }
}