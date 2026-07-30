package com.example.timesheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timesheet.data.AppViewModel
import com.example.timesheet.data.Employee
import com.example.timesheet.data.EntryType
import com.example.timesheet.data.Organization
import com.example.timesheet.data.PayrollCalculator
import com.example.timesheet.ui.AddEntryDialog
import com.example.timesheet.ui.BackupsScreen
import com.example.timesheet.ui.EmployeeFilterMenu
import com.example.timesheet.ui.IncomeBar
import com.example.timesheet.ui.IncomeBreakdownDialog
import com.example.timesheet.ui.MonthHeaderBar
import com.example.timesheet.ui.OrganizationFilterMenu
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TIMESHEETApp()
        }
    }
}

/** Состояние диалога "новая/редактируемая запись" для организаций (просто имя). */
data class DialogState(
    val editingId: String?,
    val initialName: String
)

/** Состояние диалога для сотрудника (имя + почасовая ставка). */
data class EmployeeDialogState(
    val editingId: String?,
    val initialName: String,
    val initialRate: Double
)

/** Что именно добавляем через FAB-меню на главном экране. */
data class AddEntryRequest(
    val title: String,
    val type: EntryType,
    val showHours: Boolean
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TIMESHEETApp(viewModel: AppViewModel = viewModel()) {
    var shiftsMenuExpanded by remember { mutableStateOf(false) }
    var personMenuExpanded by remember { mutableStateOf(false) }
    var organizationsMenuExpanded by remember { mutableStateOf(false) }
    var addMenuExpanded by remember { mutableStateOf(false) }

    val employees by viewModel.employees.collectAsState()
    val organizations by viewModel.organizations.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedEmployeeId by viewModel.selectedEmployeeId.collectAsState()
    val selectedOrganizationId by viewModel.selectedOrganizationId.collectAsState()
    val openingBalance by viewModel.openingBalance.collectAsState()
    val cloudSyncEnabled by viewModel.cloudSyncEnabled.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val breakdown = remember(entries, employees, currentMonth, selectedEmployeeId, selectedOrganizationId, openingBalance) {
        PayrollCalculator.calculate(
            entries = entries,
            employees = employees,
            month = currentMonth,
            employeeFilter = selectedEmployeeId,
            organizationFilter = selectedOrganizationId,
            openingBalance = openingBalance
        )
    }

    var expandedEmployeeId by remember { mutableStateOf<String?>(null) }
    var expandedOrganizationId by remember { mutableStateOf<String?>(null) }
    var employeeDialogState by remember { mutableStateOf<EmployeeDialogState?>(null) }
    var organizationDialogState by remember { mutableStateOf<DialogState?>(null) }
    var addEntryRequest by remember { mutableStateOf<AddEntryRequest?>(null) }
    var showIncomeDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Журнал расчетов") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentScreen = currentScreen,
                onNavigate = { key ->
                    currentScreen = key
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        val onMenuClick: () -> Unit = { scope.launch { drawerState.open() } }

        when (currentScreen) {
            "Трекер" -> EntityListScreen(
                onMenuClick = onMenuClick,
                title = "Трекер",
                icon = Icons.Filled.Person,
                entries = employees,
                idOf = { it.id },
                nameOf = { it.name },
                expandedEntryId = expandedEmployeeId,
                onEntryClick = { id ->
                    expandedEmployeeId = if (expandedEmployeeId == id) null else id
                },
                onEditClick = { entry ->
                    employeeDialogState = EmployeeDialogState(entry.id, entry.name, entry.hourlyRate)
                },
                onDeleteClick = { id ->
                    viewModel.deleteEmployee(id)
                    if (expandedEmployeeId == id) expandedEmployeeId = null
                },
                onAddClick = { employeeDialogState = EmployeeDialogState(null, "", 0.0) }
            )

            "Организация" -> EntityListScreen(
                onMenuClick = onMenuClick,
                title = "Организация",
                icon = Icons.Filled.ShoppingBag,
                entries = organizations,
                idOf = { it.id },
                nameOf = { it.name },
                expandedEntryId = expandedOrganizationId,
                onEntryClick = { id ->
                    expandedOrganizationId = if (expandedOrganizationId == id) null else id
                },
                onEditClick = { entry ->
                    organizationDialogState = DialogState(entry.id, entry.name)
                },
                onDeleteClick = { id ->
                    viewModel.deleteOrganization(id)
                    if (expandedOrganizationId == id) expandedOrganizationId = null
                },
                onAddClick = { organizationDialogState = DialogState(null, "") }
            )

            "Журнал расчетов" -> MainScreen(
                onMenuClick = onMenuClick,
                employees = employees,
                organizations = organizations,
                currentMonth = currentMonth,
                selectedEmployeeId = selectedEmployeeId,
                selectedOrganizationId = selectedOrganizationId,
                accrued = breakdown.accrued,
                organizationsMenuExpanded = organizationsMenuExpanded,
                onOrganizationsClick = { organizationsMenuExpanded = true },
                onOrganizationsMenuDismiss = { organizationsMenuExpanded = false },
                onOrganizationSelect = { viewModel.selectOrganization(it) },
                personMenuExpanded = personMenuExpanded,
                onPersonClick = { personMenuExpanded = true },
                onPersonMenuDismiss = { personMenuExpanded = false },
                onEmployeeSelect = { viewModel.selectEmployee(it) },
                shiftsMenuExpanded = shiftsMenuExpanded,
                onShiftsClick = { shiftsMenuExpanded = true },
                onShiftsMenuDismiss = { shiftsMenuExpanded = false },
                addMenuExpanded = addMenuExpanded,
                onAddClick = { addMenuExpanded = true },
                onAddMenuDismiss = { addMenuExpanded = false },
                onAddEntryRequest = { addEntryRequest = it },
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onPickMonth = { viewModel.goToMonth(it) },
                onIncomeClick = { showIncomeDialog = true }
            )


            "Отчеты" -> ReportsMenuScreen(
                onMenuClick = onMenuClick,
                onNavigate = { key -> currentScreen = key }
            )

            "Бекапы" -> BackupsScreen(
                viewModel = viewModel,
                cloudSyncEnabled = cloudSyncEnabled,
                syncStatus = syncStatus,
                onMenuClick = onMenuClick
            )


            else -> PlaceholderScreen(
                title = currentScreen.substringAfterLast("/"),
                onMenuClick = onMenuClick
            )
        }

        employeeDialogState?.let { state ->
            EmployeeEditDialog(
                title = if (state.editingId == null) "Новый сотрудник" else "Редактировать сотрудника",
                initialName = state.initialName,
                initialRate = state.initialRate,
                onDismiss = { employeeDialogState = null },
                onConfirm = { name, rate ->
                    viewModel.addOrUpdateEmployee(state.editingId, name, rate)
                    employeeDialogState = null
                }
            )
        }

        organizationDialogState?.let { state ->
            EntityEditDialog(
                title = if (state.editingId == null) "Новая организация" else "Редактировать организацию",
                initialName = state.initialName,
                onDismiss = { organizationDialogState = null },
                onConfirm = { name ->
                    viewModel.addOrUpdateOrganization(state.editingId, name)
                    organizationDialogState = null
                }
            )
        }

        addEntryRequest?.let { request ->
            AddEntryDialog(
                title = request.title,
                entryType = request.type,
                showHours = request.showHours,
                employees = employees,
                organizations = organizations,
                preselectedEmployeeId = selectedEmployeeId,
                preselectedOrganizationId = selectedOrganizationId,
                onDismiss = { addEntryRequest = null },
                onConfirm = { entry ->
                    viewModel.addEntry(entry)
                    addEntryRequest = null
                }
            )
        }

        if (showIncomeDialog) {
            IncomeBreakdownDialog(
                monthLabel = com.example.timesheet.ui.formatMonth(currentMonth),
                breakdown = breakdown,
                onDismiss = { showIncomeDialog = false },
                onOpeningBalanceChange = { viewModel.setOpeningBalance(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onMenuClick: () -> Unit,
    employees: List<Employee>,
    organizations: List<Organization>,
    currentMonth: java.time.YearMonth,
    selectedEmployeeId: String?,
    selectedOrganizationId: String?,
    accrued: Double,
    organizationsMenuExpanded: Boolean,
    onOrganizationsClick: () -> Unit,
    onOrganizationsMenuDismiss: () -> Unit,
    onOrganizationSelect: (String?) -> Unit,
    personMenuExpanded: Boolean,
    onPersonClick: () -> Unit,
    onPersonMenuDismiss: () -> Unit,
    onEmployeeSelect: (String?) -> Unit,
    shiftsMenuExpanded: Boolean,
    onShiftsClick: () -> Unit,
    onShiftsMenuDismiss: () -> Unit,
    addMenuExpanded: Boolean,
    onAddClick: () -> Unit,
    onAddMenuDismiss: () -> Unit,
    onAddEntryRequest: (AddEntryRequest) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (java.time.YearMonth) -> Unit,
    onIncomeClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                AppTopBar(
                    onMenuClick = onMenuClick,
                    onOrganizationsClick = onOrganizationsClick,
                    organizationsMenuExpanded = organizationsMenuExpanded,
                    onOrganizationsMenuDismiss = onOrganizationsMenuDismiss,
                    organizations = organizations,
                    selectedOrganizationId = selectedOrganizationId,
                    onOrganizationSelect = onOrganizationSelect,
                    onPersonClick = onPersonClick,
                    personMenuExpanded = personMenuExpanded,
                    onPersonMenuDismiss = onPersonMenuDismiss,
                    employees = employees,
                    selectedEmployeeId = selectedEmployeeId,
                    onEmployeeSelect = onEmployeeSelect,
                    onShiftsClick = onShiftsClick,
                    shiftsMenuExpanded = shiftsMenuExpanded,
                    onShiftsMenuDismiss = onShiftsMenuDismiss
                )
                MonthHeaderBar(
                    month = currentMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onPick = onPickMonth
                )
            }
        },
        bottomBar = {
            IncomeBar(accrued = accrued, onClick = onIncomeClick)
        },
        floatingActionButton = {
            AddFab(onClick = onAddClick)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (employees.isEmpty() && organizations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Добавьте сотрудников и организации через меню \u2261,\nчтобы начать вести журнал.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                AddMenu(
                    expanded = addMenuExpanded,
                    onDismiss = onAddMenuDismiss,
                    onSelect = onAddEntryRequest
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EntityListScreen(
    onMenuClick: () -> Unit,
    title: String,
    icon: ImageVector,
    entries: List<T>,
    idOf: (T) -> String,
    nameOf: (T) -> String,
    expandedEntryId: String?,
    onEntryClick: (String) -> Unit,
    onEditClick: (T) -> Unit,
    onDeleteClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Меню",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Добавить",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5CA02F),
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Список пуст. Нажмите \"+\", чтобы добавить.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                items(entries, key = { idOf(it) }) { entry ->
                    EntityListItem(
                        name = nameOf(entry),
                        icon = icon,
                        expanded = expandedEntryId == idOf(entry),
                        onClick = { onEntryClick(idOf(entry)) },
                        onEditClick = { onEditClick(entry) },
                        onDeleteClick = { onDeleteClick(idOf(entry)) }
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsMenuScreen(
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val reportItems = listOf(
        Triple("Отчеты/Расчетный лист", "Расчетный лист", Icons.Filled.Assignment),
        Triple("Отчеты/Рабочее время по проектам", "Рабочее время по проектам", Icons.Filled.AccessTime),
        Triple("Отчеты/Расходы", "Расходы", Icons.Filled.AttachMoney)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Отчеты", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Меню",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5CA02F),
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(reportItems, key = { it.first }) { (key, title, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(key) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = title, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
fun EntityListItem(
    name: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}

/** Диалог организации: только имя. */
@Composable
fun EntityEditDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/** Диалог сотрудника: имя + почасовая ставка (используется в расчёте дохода). */
@Composable
fun EmployeeEditDialog(
    title: String,
    initialName: String,
    initialRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var rateText by remember(initialRate) { mutableStateOf(if (initialRate == 0.0) "" else initialRate.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Ставка в час, ₽ (необязательно)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val rate = rateText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onConfirm(name, rate)
                }
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    onMenuClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Меню",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5CA02F),
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TODO: содержимое раздела
        }
    }
}

@Composable
fun AppDrawerContent(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {

    var referencesExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF5CA02F),
        modifier = Modifier.width(280.dp)
    ) {
        Spacer(modifier = Modifier.size(16.dp))

        DrawerRow(
            title = "Журнал расчетов",
            icon = Icons.Filled.CalendarMonth,
            selected = currentScreen == "Журнал расчетов",
            onClick = { onNavigate("Журнал расчетов") }
        )
        DrawerRow(
            title = "Трекер",
            icon = Icons.Filled.AccessTime,
            selected = currentScreen == "Трекер",
            onClick = { onNavigate("Трекер") }
        )
        DrawerRow(
            title = "Организация",
            icon = Icons.Filled.ShoppingBag,
            selected = currentScreen == "Организация",
            onClick = { onNavigate("Организация") }
        )
        DrawerRow(
            title = "Отчеты",
            icon = Icons.Filled.Poll,
            selected = currentScreen == "Отчеты" || currentScreen.startsWith("Отчеты/"),
            onClick = { onNavigate("Отчеты") }
        )


        DrawerRow(
            title = "Справочники",
            icon = Icons.Filled.MenuBook,
            selected = currentScreen.startsWith("Справочники/"),
            onClick = { referencesExpanded = !referencesExpanded },
            trailingIcon = if (referencesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore
        )
        if (referencesExpanded) {
            DrawerRow(
                title = "Типы времени",
                icon = Icons.Filled.AccessTime,
                selected = currentScreen == "Справочники/Типы времени",
                onClick = { onNavigate("Справочники/Типы времени") },
                indent = true
            )
            DrawerRow(
                title = "Категории расходов",
                icon = Icons.Filled.AttachMoney,
                selected = currentScreen == "Справочники/Категории расходов",
                onClick = { onNavigate("Справочники/Категории расходов") },
                indent = true
            )
            DrawerRow(
                title = "Единицы измерения",
                icon = Icons.Filled.Straighten,
                selected = currentScreen == "Справочники/Единицы измерения",
                onClick = { onNavigate("Справочники/Единицы измерения") },
                indent = true
            )
            DrawerRow(
                title = "Налоги",
                icon = Icons.Filled.MoneyOff,
                selected = currentScreen == "Справочники/Налоги",
                onClick = { onNavigate("Справочники/Налоги") },
                indent = true
            )
        }

        DrawerRow(
            title = "Бекапы",
            icon = Icons.Filled.CloudDownload,
            selected = currentScreen == "Бекапы",
            onClick = { onNavigate("Бекапы") }
        )
    }
}

@Composable
fun DrawerRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    indent: Boolean = false,
    trailingIcon: ImageVector? = null
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(if (indent) 24.dp else 32.dp),
                tint = Color.White
            )
        },
        label = {
            Text(
                text = title,
                fontSize = if (indent) 18.sp else 24.sp,
                color = Color.White
            )
        },
        badge = trailingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = if (indent) Modifier.padding(start = 24.dp) else Modifier,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFF4A8025),
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = Color.White,
            unselectedTextColor = Color.White,
            selectedIconColor = Color.White,
            unselectedIconColor = Color.White
        )
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onMenuClick: () -> Unit,
    onOrganizationsClick: () -> Unit,
    organizationsMenuExpanded: Boolean,
    onOrganizationsMenuDismiss: () -> Unit,
    organizations: List<Organization>,
    selectedOrganizationId: String?,
    onOrganizationSelect: (String?) -> Unit,
    onPersonClick: () -> Unit,
    personMenuExpanded: Boolean,
    onPersonMenuDismiss: () -> Unit,
    employees: List<Employee>,
    selectedEmployeeId: String?,
    onEmployeeSelect: (String?) -> Unit,
    onShiftsClick: () -> Unit,
    shiftsMenuExpanded: Boolean,
    onShiftsMenuDismiss: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Меню",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = onOrganizationsClick) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = "Организация",
                        modifier = Modifier.size(32.dp)
                    )
                }
                OrganizationFilterMenu(
                    expanded = organizationsMenuExpanded,
                    onDismiss = onOrganizationsMenuDismiss,
                    organizations = organizations,
                    selectedId = selectedOrganizationId,
                    onSelect = onOrganizationSelect
                )
            }
            Box {
                IconButton(onClick = onPersonClick) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Сотрудник",
                        modifier = Modifier.size(32.dp)
                    )
                }
                EmployeeFilterMenu(
                    expanded = personMenuExpanded,
                    onDismiss = onPersonMenuDismiss,
                    employees = employees,
                    selectedId = selectedEmployeeId,
                    onSelect = onEmployeeSelect
                )
            }
            Box {
                IconButton(onClick = onShiftsClick) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Смены",
                        modifier = Modifier.size(32.dp)
                    )
                }
                ShiftsMenu(
                    expanded = shiftsMenuExpanded,
                    onDismiss = onShiftsMenuDismiss
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF5CA02F),
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
@Composable
fun ShiftsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Настроить период") },
            onClick = {
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Журнал смен") },
            onClick = {
                onDismiss()
            }
        )
    }
}


@Composable
fun AddFab(onClick: () -> Unit) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFFFF9800),
        contentColor = Color.White
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Добавить"
        )
    }
}

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
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.CreditCard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выплата")
                }
            },
            onClick = { select(AddEntryRequest("Выплата", EntryType.PAYMENT, showHours = false)) }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.MoneyOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Налог")
                }
            },
            onClick = { select(AddEntryRequest("Налог", EntryType.TAX, showHours = false)) }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Запись табеля")
                }
            },
            onClick = { select(AddEntryRequest("Запись табеля", EntryType.SHIFT, showHours = true)) }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.AttachMoney, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Доплата, удержание")
                }
            },
            onClick = { select(AddEntryRequest("Доплата (+) или удержание (-)", EntryType.ADJUSTMENT, showHours = false)) }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.Assignment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Смена по шаблону")
                }
            },
            onClick = { select(AddEntryRequest("Смена по шаблону", EntryType.SHIFT, showHours = true)) }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.PermContactCalendar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Смена")
                }
            },
            onClick = { select(AddEntryRequest("Смена", EntryType.SHIFT, showHours = true)) }
        )
    }
}