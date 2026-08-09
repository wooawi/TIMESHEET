package com.example.timesheet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.timesheet.data.ShiftTemplate
import com.example.timesheet.data.Surcharge
import com.example.timesheet.data.SurchargeCalcType
import com.example.timesheet.data.SurchargeKind

private val BrandGreen = Color(0xFF5CA02F)
private val BrandOrange = Color(0xFFFF9800)

/** "Шаблоны смен - выберите": список шаблонов, тап раскрывает действия, "+" создаёт новый. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTemplateListScreen(
    templates: List<ShiftTemplate>,
    onClose: () -> Unit,
    onAddNew: () -> Unit,
    onApply: (ShiftTemplate) -> Unit,
    onEdit: (ShiftTemplate) -> Unit,
    onDelete: (String) -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Шаблоны смен", color = Color.White, fontSize = 16.sp)
                        Text("выберите", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew, containerColor = BrandOrange, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить шаблон")
            }
        }
    ) { innerPadding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Шаблонов пока нет. Нажмите \"+\", чтобы создать.", modifier = Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            ) {
                items(templates, key = { it.id }) { t ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedId = if (expandedId == t.id) null else t.id }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer()
                                Column(Modifier.weight(1f)) {
                                    Text(t.name.ifBlank { "Без названия" }, fontSize = 18.sp)
                                    if (t.shiftTypeName.isNotBlank()) {
                                        Text(t.shiftTypeName, fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            }
                            if (expandedId == t.id) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onApply(t) }) { Text("ПРИМЕНИТЬ") }
                                    IconButton(onClick = { onEdit(t) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Изменить")
                                    }
                                    IconButton(onClick = {
                                        onDelete(t.id)
                                        expandedId = null
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                                    }
                                }
                            }
                        }
                    }
                    Box(Modifier.size(8.dp))
                }
            }
        }
    }
}

/** "Шаблон смены - новый/редактирование": сотрудник, организация, тип смены, проект, доплаты, комментарий. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTemplateEditScreen(
    initial: ShiftTemplate,
    employees: List<Employee>,
    organizations: List<Organization>,
    allSurcharges: List<Surcharge>,
    onClose: () -> Unit,
    onSave: (ShiftTemplate) -> Unit,
    onDelete: ((String) -> Unit)?,
    onOpenSurchargeLibrary: (ShiftTemplate) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var employeeId by remember(initial) { mutableStateOf(initial.employeeId) }
    var organizationId by remember(initial) { mutableStateOf(initial.organizationId) }
    var shiftTypeName by remember(initial) { mutableStateOf(initial.shiftTypeName) }
    var projectName by remember(initial) { mutableStateOf(initial.projectName) }
    var comment by remember(initial) { mutableStateOf(initial.comment) }
    var employeeMenuOpen by remember { mutableStateOf(false) }
    var organizationMenuOpen by remember { mutableStateOf(false) }

    val employeeName = employees.firstOrNull { it.id == employeeId }?.name ?: "Я"
    val organizationName = organizations.firstOrNull { it.id == organizationId }?.name ?: "Я"
    val chosenSurcharges = allSurcharges.filter { it.id in initial.surchargeIds }

    fun currentDraft() = initial.copy(
        name = name,
        employeeId = employeeId,
        organizationId = organizationId,
        shiftTypeName = shiftTypeName,
        projectName = projectName,
        comment = comment
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Шаблон смены", color = Color.White, fontSize = 16.sp)
                        Text(if (initial.name.isBlank()) "новый" else "редактирование", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val draft = currentDraft()
                        onSave(if (draft.name.isBlank()) draft.copy(name = shiftTypeName.ifBlank { "Шаблон смены" }) else draft)
                        onClose()
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Сохранить", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Название шаблона") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(12.dp)
            Box {
                TextButton(onClick = { employeeMenuOpen = true }) { Text("Сотрудник: $employeeName") }
                DropdownMenu(expanded = employeeMenuOpen, onDismissRequest = { employeeMenuOpen = false }) {
                    employees.forEach { e ->
                        DropdownMenuItem(text = { Text(e.name) }, onClick = { employeeId = e.id; employeeMenuOpen = false })
                    }
                }
            }
            Box {
                TextButton(onClick = { organizationMenuOpen = true }) { Text("Организация: $organizationName") }
                DropdownMenu(expanded = organizationMenuOpen, onDismissRequest = { organizationMenuOpen = false }) {
                    organizations.forEach { o ->
                        DropdownMenuItem(text = { Text(o.name) }, onClick = { organizationId = o.id; organizationMenuOpen = false })
                    }
                }
            }
            OutlinedTextField(
                value = shiftTypeName, onValueChange = { shiftTypeName = it },
                label = { Text("Тип смены") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = projectName, onValueChange = { projectName = it },
                label = { Text("Проект") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Spacer(20.dp)
            Text("Доплаты и удержания", color = BrandGreen, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onOpenSurchargeLibrary(currentDraft()) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (chosenSurcharges.isEmpty()) "Доплаты" else chosenSurcharges.joinToString(", ") { it.name },
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.Add, contentDescription = "Добавить", tint = BrandOrange)
            }
            Divider(modifier = Modifier.padding(top = 8.dp))

            Spacer(16.dp)
            OutlinedTextField(
                value = comment, onValueChange = { comment = it },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth()
            )

            if (onDelete != null) {
                Box(Modifier.weight(1f))
                TextButton(onClick = { onDelete(initial.id); onClose() }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Удалить шаблон")
                }
            }
        }
    }
}

/** "Доплаты и удержания - выберите": библиотека, тап переключает выбор для текущего шаблона. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurchargeListScreen(
    surcharges: List<Surcharge>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onAddNew: () -> Unit,
    onEditExisting: (Surcharge) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Доплаты и удержания", color = Color.White, fontSize = 14.sp)
                        Text("выберите", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew, containerColor = BrandOrange, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                "Доплаты",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
            if (surcharges.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Список пуст. Нажмите \"+\", чтобы добавить.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(surcharges, key = { it.id }) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(s.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(s.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (s.id in selectedIds) {
                                Icon(Icons.Filled.Check, contentDescription = "Выбрано", tint = BrandGreen)
                                Spacer(8.dp)
                            }
                            if (!s.isBuiltIn) {
                                IconButton(onClick = { onEditExisting(s) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Изменить")
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

/** "Доплаты и удержания - новый": наименование, тип, сумма, налог, условия по дням недели. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurchargeEditScreen(
    initial: Surcharge,
    onClose: () -> Unit,
    onSave: (Surcharge) -> Unit,
    onDelete: ((String) -> Unit)?
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var kind by remember(initial) { mutableStateOf(initial.kind) }
    var calcType by remember(initial) { mutableStateOf(initial.calcType) }
    var amountText by remember(initial) { mutableStateOf(if (initial.amount == 0.0) "" else initial.amount.toString()) }
    var taxable by remember(initial) { mutableStateOf(initial.taxable) }
    var conditionsEnabled by remember(initial) { mutableStateOf(initial.activeWeekdays.isNotEmpty()) }
    var weekdays by remember(initial) { mutableStateOf(initial.activeWeekdays) }
    var kindMenuOpen by remember { mutableStateOf(false) }
    var calcMenuOpen by remember { mutableStateOf(false) }

    fun save() {
        if (name.isBlank()) return
        onSave(
            initial.copy(
                name = name,
                kind = kind,
                calcType = calcType,
                amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                taxable = taxable,
                activeWeekdays = if (conditionsEnabled) weekdays else emptySet()
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Доплаты и удержания", color = Color.White, fontSize = 14.sp)
                        Text(if (initial.name.isBlank()) "новый" else "редактирование", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { save(); onClose() }) {
                        Icon(Icons.Filled.Check, contentDescription = "Сохранить", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Наименование") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(12.dp)
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { kindMenuOpen = true }) {
                        Text(if (kind == SurchargeKind.BONUS) "Доплата" else "Удержание")
                    }
                    DropdownMenu(expanded = kindMenuOpen, onDismissRequest = { kindMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Доплата") }, onClick = { kind = SurchargeKind.BONUS; kindMenuOpen = false })
                        DropdownMenuItem(text = { Text("Удержание") }, onClick = { kind = SurchargeKind.DEDUCTION; kindMenuOpen = false })
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { calcMenuOpen = true }) { Text(calcTypeLabel(calcType)) }
                    DropdownMenu(expanded = calcMenuOpen, onDismissRequest = { calcMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("За смену") }, onClick = { calcType = SurchargeCalcType.FIXED_PER_SHIFT; calcMenuOpen = false })
                        DropdownMenuItem(text = { Text("За час") }, onClick = { calcType = SurchargeCalcType.PER_HOUR; calcMenuOpen = false })
                        DropdownMenuItem(text = { Text("%") }, onClick = { calcType = SurchargeCalcType.PERCENT; calcMenuOpen = false })
                    }
                }
            }
            Spacer(12.dp)
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text(if (calcType == SurchargeCalcType.PERCENT) "Значение, %" else "Фикс. сумма, ₽") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(16.dp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Облагать налогом")
                Switch(checked = taxable, onCheckedChange = { taxable = it })
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Условия", fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Дни недели")
                Switch(checked = conditionsEnabled, onCheckedChange = { conditionsEnabled = it })
            }
            if (conditionsEnabled) {
                val labels = listOf("вс" to 7, "пн" to 1, "вт" to 2, "ср" to 3, "чт" to 4, "пт" to 5, "сб" to 6)
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        labels.take(4).forEach { (label, day) ->
                            WeekdayCheckbox(label, day in weekdays) {
                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        labels.drop(4).forEach { (label, day) ->
                            WeekdayCheckbox(label, day in weekdays) {
                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                            }
                        }
                    }
                }
            }

            if (onDelete != null) {
                Box(Modifier.weight(1f))
                TextButton(onClick = { onDelete(initial.id); onClose() }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Удалить")
                }
            }
        }
    }
}

private fun calcTypeLabel(t: SurchargeCalcType) = when (t) {
    SurchargeCalcType.FIXED_PER_SHIFT -> "За смену"
    SurchargeCalcType.PER_HOUR -> "За час"
    SurchargeCalcType.PERCENT -> "%"
}

@Composable
private fun WeekdayCheckbox(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 12.dp, top = 4.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label)
    }
}

@Composable
private fun Spacer(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size))
}

@Composable
private fun Spacer() {
    Box(Modifier.width(12.dp))
}
