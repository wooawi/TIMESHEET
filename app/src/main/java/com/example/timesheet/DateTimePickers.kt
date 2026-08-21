package com.example.timesheet.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.window.Dialog
import com.example.timesheet.data.dateRangeDays
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * ДОБАВЛЕНО (ТЗ): «сделать нормальный календарь» / «и время тоже».
 *
 * Единые переиспользуемые диалоги выбора даты и времени на основе стандартных
 * компонентов Material3 (DatePicker/DatePickerDialog, TimePicker) — тот же подход,
 * что и в туториалах metanit.com, на которые ссылается ТЗ, только в Compose-варианте
 * (уже был правильно использован в PeriodSettingsScreen.kt — здесь он вынесен в общие
 * функции и подключён во всех остальных местах, где раньше стояли заглушки
 * "TODO: DatePicker" / "TODO: TimePicker").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    // Материал выбирает дату в UTC — переводим в системную зону, чтобы
                    // не "съезжало" на день назад/вперёд в восточных часовых поясах.
                    val picked = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()
                    onConfirm(picked)
                } else {
                    onDismiss()
                }
            }) { Text("Выбрать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Выберите время",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TimePicker(state = state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onConfirm(LocalTime.of(state.hour, state.minute))
                    }) { Text("Выбрать") }
                }
            }
        }
    }
}

/**
 * ДОБАВЛЕНО (ТЗ: «теперь надо чтобы человек мог добавить в смены и остальные
 * вкладки промежуток дат, а сейчас можно выбрать только одно число»):
 * переиспользуемый блок «Добавить на несколько дней» — переключатель +
 * выбор даты начала и окончания периода. Раньше диапазон дат при добавлении
 * записи умел выбирать только один экран («Запись табеля»,
 * `TimesheetEntryDialog`), и то ЛОКАЛЬНО, без общего компонента. Теперь один
 * и тот же блок подключён во все диалоги добавления записи (Смена, Выплата/
 * Налог, Расход, Доплата/удержание) — при выключенном переключателе всё
 * работает как раньше (одна дата), при включённом создаётся одна запись на
 * каждый день диапазона [начало; конец] включительно (см. `dateRangeDays`).
 */
@Composable
fun DateRangeToggle(
    rangeEnabled: Boolean,
    onRangeEnabledChange: (Boolean) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    dateFormatter: DateTimeFormatter
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // ИСПРАВЛЕНО (ТЗ: «не работает кнопка добавления на неск дней (или сделай
    // чтобы визуально это было видно)»): переключатель не был сломан — он был
    // просто малозаметным серым текстом без рамки на белом фоне и терялся
    // среди остальных полей формы, из-за чего казалось, что тап по нему
    // ничего не делает. Теперь это отдельный заметный блок с рамкой, который
    // явно подсвечивается зелёным, когда диапазон включён, а тап работает по
    // всей строке (не только по самому переключателю).
    val borderColor = if (rangeEnabled) Color(0xFF5CA02F) else Color(0xFFBDBDBD)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable { onRangeEnabledChange(!rangeEnabled) },
        color = if (rangeEnabled) Color(0xFF5CA02F).copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Добавить на несколько дней",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (rangeEnabled) Color(0xFF2E5E15) else Color.Gray
                    )
                }
                Switch(checked = rangeEnabled, onCheckedChange = onRangeEnabledChange)
            }

            if (rangeEnabled) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("С") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { showStartPicker = true }) { Text("📅") }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = endDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("По") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { showEndPicker = true }) { Text("📅") }
                        }
                    )
                }
                Text(
                    text = "Будет создана отдельная запись на каждый день периода (${dateRangeDays(startDate, endDate).size} дн.)",
                    fontSize = 11.sp,
                    color = Color(0xFF2E5E15),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    if (showStartPicker) {
        AppDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                onStartDateChange(it)
                if (endDate.isBefore(it)) onEndDateChange(it)
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        AppDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                onEndDateChange(if (it.isBefore(startDate)) startDate else it)
                showEndPicker = false
            }
        )
    }
}