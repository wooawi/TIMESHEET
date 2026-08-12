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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timesheet.data.AppViewModel
import com.example.timesheet.data.Employee
import com.example.timesheet.data.EntryType
import com.example.timesheet.data.ExpenseCategory
import com.example.timesheet.data.LedgerEntry
import com.example.timesheet.data.Organization
import com.example.timesheet.data.PayrollCalculator
import com.example.timesheet.data.ShiftTemplate
import com.example.timesheet.data.Surcharge
import com.example.timesheet.data.Tax
import com.example.timesheet.data.TimeType
import com.example.timesheet.ui.AddEntryDialog
import com.example.timesheet.ui.AddFab
import com.example.timesheet.ui.AddMenu
import com.example.timesheet.ui.AppTopBar
import com.example.timesheet.ui.BackupsScreen
import com.example.timesheet.ui.ExpenseCategoriesScreen
import com.example.timesheet.ui.ExpenseCategoryEditDialog
import com.example.timesheet.ui.ExpensesReportScreen
import com.example.timesheet.ui.FilterMenuScreen
import com.example.timesheet.ui.IncomeBar
import com.example.timesheet.ui.IncomeBreakdownDialog
import com.example.timesheet.ui.JournalEntryItem
import com.example.timesheet.ui.MonthHeaderBar
import com.example.timesheet.ui.PayslipReportScreen
import com.example.timesheet.ui.PeriodSettingsScreen
import com.example.timesheet.ui.ShiftJournalScreen
import com.example.timesheet.ui.ShiftTemplateEditScreen
import com.example.timesheet.ui.ShiftTemplateListScreen
import com.example.timesheet.ui.SurchargeEditScreen
import com.example.timesheet.ui.SurchargeListScreen
import com.example.timesheet.ui.TaxesScreen
import com.example.timesheet.ui.TaxEditDialog
import com.example.timesheet.ui.TimeTypesScreen
import com.example.timesheet.ui.TimeTypeEditDialog
import com.example.timesheet.ui.UnitsScreen
import com.example.timesheet.ui.WorkHoursReportScreen
import com.example.timesheet.ui.formatMonth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimesheetApp()
        }
    }
}

data class DialogState(
    val editingId: String?,
    val initialName: String
)

data class EmployeeDialogState(
    val editingId: String?,
    val initialName: String,
    val initialRate: Double
)

data class AddEntryRequest(
    val title: String,
    val type: EntryType,
    val showHours: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesheetApp(viewModel: AppViewModel = viewModel()) {
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
    val shiftTemplates by viewModel.shiftTemplates.collectAsState()
    val surcharges by viewModel.surcharges.collectAsState()
    val timeTypes by viewModel.timeTypes.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val units by viewModel.units.collectAsState()
    val taxes by viewModel.taxes.collectAsState()

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
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    var showShiftTemplates by remember { mutableStateOf(false) }
    var showSurchargeLibrary by remember { mutableStateOf(false) }
    var editingSurcharge by remember { mutableStateOf<Surcharge?>(null) }
    var editingShiftTemplate by remember { mutableStateOf<ShiftTemplate?>(null) }
    var shiftTemplateToApply by remember { mutableStateOf<ShiftTemplate?>(null) }

    var editingTimeType by remember { mutableStateOf<TimeType?>(null) }
    var editingExpenseCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editingTax by remember { mutableStateOf<Tax?>(null) }

    // ===== НОВЫЕ СОСТОЯНИЯ ДЛЯ НАВИГАЦИИ =====
    var showPeriodSettings by remember { mutableStateOf(false) }
    var showShiftJournal by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterOrganizationId by remember { mutableStateOf<String?>(null) }
    var filterEmployeeId by remember { mutableStateOf<String?>(null) }

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
                entries = entries,
                timeTypes = timeTypes,
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
                onOpenPeriodSettings = { currentScreen = "Настроить период" },
                onOpenShiftJournal = { currentScreen = "Журнал смен" },
                onOpenShiftTemplates = { showShiftTemplates = true },
                addMenuExpanded = addMenuExpanded,
                onAddClick = { addMenuExpanded = true },
                onAddMenuDismiss = { addMenuExpanded = false },
                onAddEntryRequest = { addEntryRequest = it },
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onPickMonth = { viewModel.goToMonth(it) },
                onIncomeClick = { showIncomeDialog = true },
                onEditEntry = { editingEntry = it },
                onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                onRecalculateEntry = { id ->
                    // TODO: логика пересчёта
                }
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

            "Справочники/Типы времени" -> TimeTypesScreen(
                timeTypes = timeTypes,
                onMenuClick = onMenuClick,
                onAdd = { editingTimeType = TimeType() },
                onEdit = { editingTimeType = it },
                onDelete = { viewModel.deleteTimeType(it) }
            )

            "Справочники/Категории расходов" -> ExpenseCategoriesScreen(
                categories = expenseCategories,
                onMenuClick = onMenuClick,
                onAdd = { editingExpenseCategory = ExpenseCategory() },
                onEdit = { editingExpenseCategory = it },
                onDelete = { viewModel.deleteExpenseCategory(it) }
            )

            "Справочники/Единицы измерения" -> UnitsScreen(
                units = units,
                onMenuClick = onMenuClick
            )

            "Справочники/Налоги" -> TaxesScreen(
                taxes = taxes,
                onMenuClick = onMenuClick,
                onAdd = { editingTax = Tax() },
                onEdit = { editingTax = it },
                onDelete = { viewModel.deleteTax(it) }
            )

            // ===== НОВЫЕ ЭКРАНЫ =====
            "Настроить период" -> PeriodSettingsScreen(
                onBack = { currentScreen = "Журнал расчетов" },
                onApplyPeriod = { start, end ->
                    viewModel.setReportPeriod(start, end)
                    currentScreen = "Журнал расчетов"
                },
                onApplyQuickPeriod = { periodId ->
                    viewModel.applyQuickReportPeriod(periodId)
                    currentScreen = "Журнал расчетов"
                }
            )

            "Журнал смен" -> ShiftJournalScreen(
                entries = entries,
                employees = employees,
                organizations = organizations,
                timeTypes = timeTypes,
                periodStart = viewModel.reportPeriodStart.value,
                periodEnd = viewModel.reportPeriodEnd.value,
                filterOrganizationId = filterOrganizationId,
                filterEmployeeId = filterEmployeeId,
                onBack = { currentScreen = "Журнал расчетов" },
                onFilterClick = { currentScreen = "Фильтр" },
                onEditEntry = { editingEntry = it },
                onDeleteEntry = { viewModel.deleteEntry(it) }
            )

            "Фильтр" -> FilterMenuScreen(
                organizations = organizations,
                employees = employees,
                selectedOrganizationId = filterOrganizationId,
                selectedEmployeeId = filterEmployeeId,
                onBack = { currentScreen = "Журнал смен" },
                onApplyFilter = { orgId, empId ->
                    filterOrganizationId = orgId
                    filterEmployeeId = empId
                }
            )

            "Отчеты/Расчетный лист" -> PayslipReportScreen(
                viewModel = viewModel,
                employees = employees,
                organizations = organizations,
                onBack = { currentScreen = "Отчеты" }
            )

            "Отчеты/Рабочее время по проектам" -> WorkHoursReportScreen(
                viewModel = viewModel,
                employees = employees,
                organizations = organizations,
                onBack = { currentScreen = "Отчеты" }
            )

            "Отчеты/Расходы" -> ExpensesReportScreen(
                viewModel = viewModel,
                employees = employees,
                organizations = organizations,
                onBack = { currentScreen = "Отчеты" }
            )

            else -> PlaceholderScreen(
                title = currentScreen.substringAfterLast("/"),
                onMenuClick = onMenuClick
            )
        }

        // ========== ДИАЛОГИ РЕДАКТИРОВАНИЯ ==========

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
                expenseCategories = expenseCategories,
                units = units,
                preselectedEmployeeId = selectedEmployeeId,
                preselectedOrganizationId = selectedOrganizationId,
                onDismiss = { addEntryRequest = null },
                onConfirm = { entry ->
                    val duplicate = entries.any {
                        it.date == entry.date &&
                                it.type == entry.type &&
                                it.employeeId == entry.employeeId &&
                                it.hours == entry.hours &&
                                it.amount == entry.amount
                    }
                    if (!duplicate) {
                        viewModel.addEntry(entry)
                    }
                    addEntryRequest = null
                }
            )
        }

        editingEntry?.let { entry ->
            AddEntryDialog(
                title = "Редактировать запись",
                entryType = entry.type,
                showHours = entry.type == EntryType.SHIFT,
                employees = employees,
                organizations = organizations,
                expenseCategories = expenseCategories,
                units = units,
                preselectedEmployeeId = entry.employeeId,
                preselectedOrganizationId = entry.organizationId,
                initialNote = entry.note,
                initialDate = entry.date,
                initialStartTime = entry.startTime,
                initialEndTime = entry.endTime,
                initialShiftType = entry.shiftType,
                onDismiss = { editingEntry = null },
                onConfirm = { updated ->
                    viewModel.updateEntry(updated.copy(id = entry.id))
                    editingEntry = null
                }
            )
        }

        if (showIncomeDialog) {
            IncomeBreakdownDialog(
                monthLabel = formatMonth(currentMonth),
                breakdown = breakdown,
                onDismiss = { showIncomeDialog = false },
                onOpeningBalanceChange = { viewModel.setOpeningBalance(it) }
            )
        }

        // ========== ШАБЛОНЫ И ДОПЛАТЫ ==========

        if (showShiftTemplates) {
            ShiftTemplateListScreen(
                templates = shiftTemplates,
                onClose = { showShiftTemplates = false },
                onAddNew = {
                    editingShiftTemplate = ShiftTemplate()
                    showShiftTemplates = false
                },
                onApply = { template ->
                    shiftTemplateToApply = template
                    showShiftTemplates = false
                },
                onEdit = { template ->
                    editingShiftTemplate = template
                    showShiftTemplates = false
                },
                onDelete = { id ->
                    viewModel.deleteShiftTemplate(id)
                }
            )
        }

        editingShiftTemplate?.let { template ->
            ShiftTemplateEditScreen(
                initial = template,
                employees = employees,
                organizations = organizations,
                allSurcharges = surcharges,
                onClose = { editingShiftTemplate = null },
                onSave = { updated ->
                    viewModel.addOrUpdateShiftTemplate(updated)
                    editingShiftTemplate = null
                },
                onDelete = if (template.id.isNotBlank() && !template.id.startsWith("builtin")) { id ->
                    viewModel.deleteShiftTemplate(id)
                    editingShiftTemplate = null
                } else null,
                onOpenSurchargeLibrary = { draft ->
                    showSurchargeLibrary = true
                    editingShiftTemplate = draft
                }
            )
        }

        if (showSurchargeLibrary) {
            val currentTemplate = editingShiftTemplate ?: ShiftTemplate()
            SurchargeListScreen(
                surcharges = surcharges,
                selectedIds = currentTemplate.surchargeIds.toSet(),
                onToggle = { surchargeId ->
                    val newIds = if (surchargeId in currentTemplate.surchargeIds) {
                        currentTemplate.surchargeIds - surchargeId
                    } else {
                        currentTemplate.surchargeIds + surchargeId
                    }
                    editingShiftTemplate = currentTemplate.copy(surchargeIds = newIds)
                },
                onAddNew = {
                    showSurchargeLibrary = false
                    editingSurcharge = Surcharge()
                },
                onEditExisting = { surcharge ->
                    editingSurcharge = surcharge
                    showSurchargeLibrary = false
                },
                onClose = { showSurchargeLibrary = false }
            )
        }

        editingSurcharge?.let { surcharge ->
            SurchargeEditScreen(
                initial = surcharge,
                onClose = { editingSurcharge = null },
                onSave = { updated ->
                    viewModel.addOrUpdateSurcharge(updated)
                    editingSurcharge = null
                },
                onDelete = if (!surcharge.isBuiltIn) { id ->
                    viewModel.deleteSurcharge(id)
                    editingSurcharge = null
                } else null
            )
        }

        shiftTemplateToApply?.let { template ->
            addEntryRequest = AddEntryRequest(
                title = "Смена по шаблону: ${template.name}",
                type = EntryType.SHIFT,
                showHours = true
            )
            shiftTemplateToApply = null
        }

        // ========== ДИАЛОГИ ДЛЯ СПРАВОЧНИКОВ ==========

        editingTimeType?.let { type ->
            TimeTypeEditDialog(
                initial = type,
                onDismiss = { editingTimeType = null },
                onSave = { updated ->
                    viewModel.addOrUpdateTimeType(updated)
                    editingTimeType = null
                }
            )
        }

        editingExpenseCategory?.let { category ->
            ExpenseCategoryEditDialog(
                initial = category,
                onDismiss = { editingExpenseCategory = null },
                onSave = { updated ->
                    viewModel.addOrUpdateExpenseCategory(updated)
                    editingExpenseCategory = null
                }
            )
        }

        editingTax?.let { tax ->
            TaxEditDialog(
                initial = tax,
                onDismiss = { editingTax = null },
                onSave = { updated ->
                    viewModel.addOrUpdateTax(updated)
                    editingTax = null
                }
            )
        }
    }
}

// ========== ОСТАЛЬНЫЕ ФУНКЦИИ ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onMenuClick: () -> Unit,
    employees: List<Employee>,
    organizations: List<Organization>,
    entries: List<LedgerEntry>,
    timeTypes: List<TimeType>,
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
    onOpenPeriodSettings: () -> Unit,
    onOpenShiftJournal: () -> Unit,
    onOpenShiftTemplates: () -> Unit,
    addMenuExpanded: Boolean,
    onAddClick: () -> Unit,
    onAddMenuDismiss: () -> Unit,
    onAddEntryRequest: (AddEntryRequest) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    onIncomeClick: () -> Unit,
    onEditEntry: (LedgerEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onRecalculateEntry: (String) -> Unit
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
                    onShiftsMenuDismiss = onShiftsMenuDismiss,
                    onOpenPeriodSettings = onOpenPeriodSettings,
                    onOpenShiftJournal = onOpenShiftJournal,
                    onOpenTemplates = onOpenShiftTemplates
                )
                MonthHeaderBar(
                    month = currentMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onPick = onPickMonth
                )
                IncomeBar(accrued = accrued, onClick = onIncomeClick)
            }
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
            } else {
                val filteredEntries = entries.filter { entry ->
                    val inMonth = entry.date.year == currentMonth.year &&
                            entry.date.month == currentMonth.month
                    val employeeOk = selectedEmployeeId == null || entry.employeeId == selectedEmployeeId
                    val orgOk = selectedOrganizationId == null || entry.organizationId == selectedOrganizationId
                    inMonth && employeeOk && orgOk
                }

                if (filteredEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Нет записей за выбранный период",
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(filteredEntries.sortedByDescending { it.date }, key = { it.id }) { entry ->
                            JournalEntryItem(
                                entry = entry,
                                employeeName = employees.find { it.id == entry.employeeId }?.name,
                                organizationName = organizations.find { it.id == entry.organizationId }?.name,
                                timeTypes = timeTypes,
                                onEdit = { onEditEntry(it) },
                                onDelete = { onDeleteEntry(it) },
                                onRecalculate = { onRecalculateEntry(it) },
                                onAttachments = { /* TODO: открыть диалог выбора файла/фото */ }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, bottom = 80.dp),
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