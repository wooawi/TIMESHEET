package com.example.timesheet.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import com.example.timesheet.data.getQuickPeriods

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // ========== ОСНОВНЫЕ StateFlow ==========
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
    val organizations: StateFlow<List<Organization>> = _organizations.asStateFlow()

    private val _entries = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val entries: StateFlow<List<LedgerEntry>> = _entries.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedEmployeeId = MutableStateFlow<String?>(null)
    val selectedEmployeeId: StateFlow<String?> = _selectedEmployeeId.asStateFlow()

    private val _selectedOrganizationId = MutableStateFlow<String?>(null)
    val selectedOrganizationId: StateFlow<String?> = _selectedOrganizationId.asStateFlow()

    private val _openingBalance = MutableStateFlow(0.0)
    val openingBalance: StateFlow<Double> = _openingBalance.asStateFlow()

    private val _surcharges = MutableStateFlow<List<Surcharge>>(emptyList())
    val surcharges: StateFlow<List<Surcharge>> = _surcharges.asStateFlow()

    private val _shiftTemplates = MutableStateFlow<List<ShiftTemplate>>(emptyList())
    val shiftTemplates: StateFlow<List<ShiftTemplate>> = _shiftTemplates.asStateFlow()

    private val _cloudSyncEnabled = MutableStateFlow(false)
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    // ========== СПРАВОЧНИКИ ==========
    private val _timeTypes = MutableStateFlow<List<TimeType>>(defaultTimeTypes())
    val timeTypes: StateFlow<List<TimeType>> = _timeTypes.asStateFlow()

    private val _expenseCategories = MutableStateFlow<List<ExpenseCategory>>(defaultExpenseCategories())
    val expenseCategories: StateFlow<List<ExpenseCategory>> = _expenseCategories.asStateFlow()

    private val _units = MutableStateFlow<List<UnitOfMeasure>>(defaultUnits())
    val units: StateFlow<List<UnitOfMeasure>> = _units.asStateFlow()

    private val _taxes = MutableStateFlow<List<Tax>>(defaultTaxes())
    val taxes: StateFlow<List<Tax>> = _taxes.asStateFlow()

    // ДОБАВЛЕНО (ТЗ): единый выбранный период для «Настроить период» -> «Журнал смен» -> отчёты.
    private val _reportPeriodStart = MutableStateFlow(YearMonth.now().atDay(1))
    val reportPeriodStart: StateFlow<LocalDate> = _reportPeriodStart.asStateFlow()

    private val _reportPeriodEnd = MutableStateFlow(LocalDate.now())
    val reportPeriodEnd: StateFlow<LocalDate> = _reportPeriodEnd.asStateFlow()

    private val _reportQuickPeriodId = MutableStateFlow<String?>("this_month")
    val reportQuickPeriodId: StateFlow<String?> = _reportQuickPeriodId.asStateFlow()

    fun setReportPeriod(start: LocalDate, end: LocalDate) {
        _reportPeriodStart.value = start
        _reportPeriodEnd.value = end
        _reportQuickPeriodId.value = null
        _currentMonth.value = YearMonth.from(start)
        persistLocalSnapshot()
    }

    fun applyQuickReportPeriod(periodId: String) {
        val period = getQuickPeriods().find { it.id == periodId } ?: return
        val (start, end) = period.getRange()
        _reportPeriodStart.value = start
        _reportPeriodEnd.value = end
        _reportQuickPeriodId.value = periodId
        _currentMonth.value = YearMonth.from(start)
        persistLocalSnapshot()
    }

    private val snapshotFile: File by lazy {
        File(getApplication<Application>().filesDir, "app_state.json")
    }

    init {
        loadLocalSnapshot()
        if (_surcharges.value.isEmpty()) {
            _surcharges.value = defaultSurchargeLibrary()
            persistLocalSnapshot()
        }
        if (_cloudSyncEnabled.value) startCloudSync()
    }

    // ========== НАВИГАЦИЯ ПО МЕСЯЦАМ ==========
    // ИЗМЕНЕНО (ТЗ: «кнопка выбора периода должна реально влиять на отображение
    // смен на главной странице»): раньше currentMonth и «Настроить период»
    // (reportPeriodStart/End) были двумя независимыми, никак не связанными
    // источниками правды, поэтому на главном экране список смен фильтровался
    // только по currentMonth, а изменение периода в «Настроить период» на него
    // не влияло вообще. Теперь стрелки/выбор месяца тоже двигают настроенный
    // период (на границы этого календарного месяца), а «Настроить период»
    // применяется тем же способом — единый период, который реально видно
    // на главном экране.
    fun nextMonth() = setCurrentMonth(_currentMonth.value.plusMonths(1))
    fun previousMonth() = setCurrentMonth(_currentMonth.value.minusMonths(1))
    fun goToMonth(month: YearMonth) = setCurrentMonth(month)

    private fun setCurrentMonth(month: YearMonth) {
        _currentMonth.value = month
        _reportPeriodStart.value = month.atDay(1)
        _reportPeriodEnd.value = month.atEndOfMonth()
        _reportQuickPeriodId.value = null
        persistLocalSnapshot()
    }

    // ========== ФИЛЬТРЫ ==========
    fun selectEmployee(id: String?) {
        _selectedEmployeeId.value = id
        persistLocalSnapshot()
    }

    fun selectOrganization(id: String?) {
        _selectedOrganizationId.value = id
        persistLocalSnapshot()
    }

    // ========== СОТРУДНИКИ ==========
    fun addOrUpdateEmployee(id: String?, name: String, hourlyRate: Double) {
        val list = _employees.value.toMutableList()
        val entry = if (id == null) {
            Employee(name = name, hourlyRate = hourlyRate).also { list.add(it) }
        } else {
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx].copy(name = name, hourlyRate = hourlyRate).also { list[idx] = it }
            else null
        }
        _employees.value = list
        entry?.let { syncLaunch { FirebaseRepository.upsertEmployee(it) } }
        persistLocalSnapshot()
    }

    fun deleteEmployee(id: String) {
        _employees.value = _employees.value.filterNot { it.id == id }
        if (_selectedEmployeeId.value == id) _selectedEmployeeId.value = null
        syncLaunch { FirebaseRepository.deleteEmployee(id) }
        persistLocalSnapshot()
    }

    // ========== ОРГАНИЗАЦИИ ==========
    fun addOrUpdateOrganization(id: String?, name: String) {
        val list = _organizations.value.toMutableList()
        val entry = if (id == null) {
            Organization(name = name).also { list.add(it) }
        } else {
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx].copy(name = name).also { list[idx] = it }
            else null
        }
        _organizations.value = list
        entry?.let { syncLaunch { FirebaseRepository.upsertOrganization(it) } }
        persistLocalSnapshot()
    }

    fun deleteOrganization(id: String) {
        _organizations.value = _organizations.value.filterNot { it.id == id }
        if (_selectedOrganizationId.value == id) _selectedOrganizationId.value = null
        syncLaunch { FirebaseRepository.deleteOrganization(id) }
        persistLocalSnapshot()
    }

    // ========== ЗАПИСИ ЖУРНАЛА ==========
    fun addEntry(entry: LedgerEntry) {
        _entries.value = _entries.value + entry
        syncLaunch { FirebaseRepository.upsertEntry(entry) }
        persistLocalSnapshot()
    }

    fun updateEntry(entry: LedgerEntry) {
        val list = _entries.value.toMutableList()
        val idx = list.indexOfFirst { it.id == entry.id }
        if (idx >= 0) {
            list[idx] = entry
            _entries.value = list
            syncLaunch { FirebaseRepository.upsertEntry(entry) }
            persistLocalSnapshot()
        }
    }

    fun deleteEntry(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
        syncLaunch { FirebaseRepository.deleteEntry(id) }
        persistLocalSnapshot()
    }

    // ДОБАВЛЕНО (ТЗ: «сделать логику пересчета»).
    // Для смены (SHIFT) сумма может быть вручную переопределена в поле «Основная
    // оплата» диалога «Смена» (LedgerEntry.amount > 0 — см. PayrollCalculator.shiftBaseAmount).
    // «Пересчитать» сбрасывает это ручное переопределение и часы, заставляя сумму
    // и часы снова считаться по формуле: оплачиваемые часы × ставка × коэффициент
    // типа смены (+ доплаты/удержания), пересчитанные из фактического start/end.
    // Для остальных типов записи (Выплата/Налог/Доплата,удержание/Расход) сумма
    // всегда вводится вручную и не имеет отдельной формулы — пересчитывать нечего,
    // поэтому запись не изменяется.
    fun recalculateEntry(id: String) {
        val entry = _entries.value.find { it.id == id } ?: return
        if (entry.type != EntryType.SHIFT) return

        val recalculated = entry.copy(
            amount = 0.0,
            hours = entry.calculateHours()
        )
        updateEntry(recalculated)
    }

    // ДОБАВЛЕНО (ТЗ: «сделать открытие галереи/файлов настоящими»): сохраняем
    // реальные URI файлов/фото, выбранных через системный выбор документов,
    // в списке вложений записи.
    fun addAttachments(entryId: String, uris: List<String>) {
        val entry = _entries.value.find { it.id == entryId } ?: return
        updateEntry(entry.copy(attachments = uris))
    }

    fun setOpeningBalance(value: Double) {
        _openingBalance.value = value
        syncLaunch { FirebaseRepository.pushOpeningBalance(value) }
        persistLocalSnapshot()
    }

    // ========== ДОПЛАТЫ ==========
    fun addOrUpdateSurcharge(surcharge: Surcharge) {
        val list = _surcharges.value.toMutableList()
        val idx = list.indexOfFirst { it.id == surcharge.id }
        if (idx >= 0) list[idx] = surcharge else list.add(surcharge)
        _surcharges.value = list
        persistLocalSnapshot()
    }

    fun deleteSurcharge(id: String) {
        _surcharges.value = _surcharges.value.filterNot { it.id == id }
        _shiftTemplates.value = _shiftTemplates.value.map {
            if (id in it.surchargeIds) it.copy(surchargeIds = it.surchargeIds - id) else it
        }
        persistLocalSnapshot()
    }

    // ========== ШАБЛОНЫ СМЕН ==========
    fun addOrUpdateShiftTemplate(template: ShiftTemplate) {
        val list = _shiftTemplates.value.toMutableList()
        val idx = list.indexOfFirst { it.id == template.id }
        if (idx >= 0) list[idx] = template else list.add(template)
        _shiftTemplates.value = list
        persistLocalSnapshot()
    }

    fun deleteShiftTemplate(id: String) {
        _shiftTemplates.value = _shiftTemplates.value.filterNot { it.id == id }
        persistLocalSnapshot()
    }

    // ========== МЕТОДЫ ДЛЯ СПРАВОЧНИКОВ ==========

    fun addOrUpdateTimeType(timeType: TimeType) {
        val list = _timeTypes.value.toMutableList()
        val idx = list.indexOfFirst { it.id == timeType.id }
        if (idx >= 0) list[idx] = timeType else list.add(timeType)
        _timeTypes.value = list
        persistLocalSnapshot()
    }

    fun deleteTimeType(id: String) {
        _timeTypes.value = _timeTypes.value.filterNot { it.id == id }
        persistLocalSnapshot()
    }

    fun addOrUpdateExpenseCategory(category: ExpenseCategory) {
        val list = _expenseCategories.value.toMutableList()
        val idx = list.indexOfFirst { it.id == category.id }
        if (idx >= 0) list[idx] = category else list.add(category)
        _expenseCategories.value = list
        persistLocalSnapshot()
    }

    fun deleteExpenseCategory(id: String) {
        _expenseCategories.value = _expenseCategories.value.filterNot { it.id == id }
        persistLocalSnapshot()
    }

    fun addOrUpdateTax(tax: Tax) {
        val list = _taxes.value.toMutableList()
        val idx = list.indexOfFirst { it.id == tax.id }
        if (idx >= 0) list[idx] = tax else list.add(tax)
        _taxes.value = list
        persistLocalSnapshot()
    }

    fun deleteTax(id: String) {
        _taxes.value = _taxes.value.filterNot { it.id == id }
        persistLocalSnapshot()
    }

    fun getUnits(): List<UnitOfMeasure> = defaultUnits()

    // ========== РАСЧЁТ ==========
    fun currentBreakdown(): PayrollBreakdown = PayrollCalculator.calculate(
        entries = _entries.value,
        employees = _employees.value,
        month = _currentMonth.value,
        employeeFilter = _selectedEmployeeId.value,
        organizationFilter = _selectedOrganizationId.value,
        openingBalance = _openingBalance.value,
        surcharges = _surcharges.value
    )

    // ========== FIREBASE ==========
    fun setCloudSyncEnabled(enabled: Boolean) {
        _cloudSyncEnabled.value = enabled
        persistLocalSnapshot()
        if (enabled) startCloudSync() else _syncStatus.value = "Синхронизация выключена"
    }

    private fun startCloudSync() {
        _syncStatus.value = "Подключение к облаку..."
        viewModelScope.launch {
            runCatching {
                FirebaseRepository.observeEmployees().collect {
                    _employees.value = it
                    _syncStatus.value = "Синхронизировано"
                }
            }.onFailure {
                _syncStatus.value = "Firebase не настроен: добавьте app/google-services.json"
                _cloudSyncEnabled.value = false
            }
        }
        viewModelScope.launch {
            runCatching { FirebaseRepository.observeOrganizations().collect { _organizations.value = it } }
        }
        viewModelScope.launch {
            runCatching { FirebaseRepository.observeEntries().collect { _entries.value = it } }
        }
    }

    private fun syncLaunch(block: suspend () -> Unit) {
        if (!_cloudSyncEnabled.value) return
        viewModelScope.launch { runCatching { block() } }
    }

    // ========== БЕКАПЫ ==========
    fun currentState(): AppState = AppState(
        employees = _employees.value,
        organizations = _organizations.value,
        entries = _entries.value,
        openingBalance = _openingBalance.value,
        surcharges = _surcharges.value,
        shiftTemplates = _shiftTemplates.value,
        timeTypes = _timeTypes.value,
        expenseCategories = _expenseCategories.value,
        units = _units.value,
        taxes = _taxes.value
    )

    private fun currentSettingsJson(): JSONObject = JSONObject().apply {
        put("cloudSyncEnabled", _cloudSyncEnabled.value)
        put("selectedEmployeeId", _selectedEmployeeId.value ?: JSONObject.NULL)
        put("selectedOrganizationId", _selectedOrganizationId.value ?: JSONObject.NULL)
        put("currentMonth", _currentMonth.value.toString())
        put("reportPeriodStart", _reportPeriodStart.value.toString())
        put("reportPeriodEnd", _reportPeriodEnd.value.toString())
        put("reportQuickPeriodId", _reportQuickPeriodId.value ?: JSONObject.NULL)
    }

    fun createBackup(label: String? = null): File =
        BackupManager.createBackup(getApplication(), currentState(), currentSettingsJson(), label)

    fun listBackups(): List<BackupFile> = BackupManager.listBackups(getApplication())

    fun restoreBackup(file: File) {
        val (state, settings) = BackupManager.restoreBackup(file)
        _employees.value = state.employees
        _organizations.value = state.organizations
        _entries.value = state.entries
        _openingBalance.value = state.openingBalance
        _surcharges.value = state.surcharges.ifEmpty { defaultSurchargeLibrary() }
        _shiftTemplates.value = state.shiftTemplates
        _timeTypes.value = state.timeTypes.ifEmpty { defaultTimeTypes() }
        _expenseCategories.value = state.expenseCategories.ifEmpty { defaultExpenseCategories() }
        _units.value = state.units.ifEmpty { defaultUnits() }
        _taxes.value = state.taxes.ifEmpty { defaultTaxes() }

        applySettingsJson(settings)
        persistLocalSnapshot()
    }

    fun deleteBackup(file: File) = BackupManager.deleteBackup(file)

    private fun applySettingsJson(settings: JSONObject) {
        if (!settings.isNull("selectedEmployeeId")) {
            _selectedEmployeeId.value = settings.optString("selectedEmployeeId").takeIf { it.isNotBlank() }
        }
        if (!settings.isNull("selectedOrganizationId")) {
            _selectedOrganizationId.value = settings.optString("selectedOrganizationId").takeIf { it.isNotBlank() }
        }
        runCatching { _currentMonth.value = YearMonth.parse(settings.optString("currentMonth")) }
        runCatching { _reportPeriodStart.value = LocalDate.parse(settings.optString("reportPeriodStart")) }
        runCatching { _reportPeriodEnd.value = LocalDate.parse(settings.optString("reportPeriodEnd")) }
        if (!settings.isNull("reportQuickPeriodId")) {
            _reportQuickPeriodId.value = settings.optString("reportQuickPeriodId").takeIf { it.isNotBlank() }
        }
        _cloudSyncEnabled.value = settings.optBoolean("cloudSyncEnabled", false)
    }

    private fun persistLocalSnapshot() {
        runCatching {
            val root = JSONObject()
            root.put("state", BackupManager.stateToJson(currentState()))
            root.put("settings", currentSettingsJson())
            snapshotFile.writeText(root.toString())
        }
    }

    private fun loadLocalSnapshot() {
        runCatching {
            if (snapshotFile.exists()) {
                val root = JSONObject(snapshotFile.readText())
                val state = BackupManager.jsonToState(root.getJSONObject("state"))
                _employees.value = state.employees
                _organizations.value = state.organizations
                _entries.value = state.entries
                _openingBalance.value = state.openingBalance
                _surcharges.value = state.surcharges
                _shiftTemplates.value = state.shiftTemplates
                _timeTypes.value = state.timeTypes.ifEmpty { defaultTimeTypes() }
                _expenseCategories.value = state.expenseCategories.ifEmpty { defaultExpenseCategories() }
                _units.value = state.units.ifEmpty { defaultUnits() }
                _taxes.value = state.taxes.ifEmpty { defaultTaxes() }

                val settings = root.optJSONObject("settings") ?: JSONObject()
                applySettingsJson(settings)
            }
        }
    }
}