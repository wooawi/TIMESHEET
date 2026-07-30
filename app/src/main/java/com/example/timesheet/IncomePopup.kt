package com.example.timesheet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timesheet.data.PayrollBreakdown

/** Нижняя строка "Доход <сумма>" — тап открывает подробный расчёт. */
@Composable
fun IncomeBar(accrued: Double, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Доход", fontWeight = FontWeight.Medium)
            Text(
                text = formatCurrency(accrued),
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Подробная разбивка дохода за месяц + возможность поправить остаток на начало. */
@Composable
fun IncomeBreakdownDialog(
    monthLabel: String,
    breakdown: PayrollBreakdown,
    onDismiss: () -> Unit,
    onOpeningBalanceChange: (Double) -> Unit
) {
    var editingBalance by remember { mutableStateOf(false) }
    var balanceText by remember { mutableStateOf(breakdown.openingBalance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(monthLabel.replaceFirstChar { it.uppercase() }) },
        text = {
            Column {
                BreakdownRow("Начислено", breakdown.accrued)
                BreakdownRow("Доход", breakdown.accrued, bold = true)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                if (editingBalance) {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Остаток на начало") },
                        singleLine = true
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingBalance = true },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Остаток на начало")
                        Text(formatCurrency(breakdown.openingBalance))
                    }
                }
                BreakdownRow("Выплата", breakdown.payments)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                BreakdownRow("Остаток на конец", breakdown.closingBalance, bold = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (editingBalance) {
                    balanceText.toDoubleOrNull()?.let { onOpeningBalanceChange(it) }
                }
                onDismiss()
            }) {
                Text("Готово")
            }
        }
    )
}

@Composable
private fun BreakdownRow(label: String, value: Double, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            formatCurrency(value),
            color = Color(0xFF2E7D32),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
