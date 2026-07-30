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
import java.time.YearMonth

class AppViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _cloudSyncEnabled = MutableStateFlow(false)
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val snapshotFile: File by lazy { File(getApplication<Application>().filesDir, "app_state.json") }

    init {
        loadLocalSnapshot()
        if (_cloudSyncEnabled.value) startCloudSync()
    }

    // ---------- Навигация по месяцам ----------
    fun nextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
    fun previousMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }
    fun goToMonth(month: YearMonth) {
        _currentMonth.value = month
        persistLocalSnapshot()
    }

    // ---------- Фильтры (сотрудник / организация) ----------
    fun selectEmployee(id: String?) {
        _selectedEmployeeId.value = id
        persistLocalSnapshot()
    }

    fun selectOrganization(id: String?) {
        _selectedOrganizationId.value = id
        persistLocalSnapshot()
    }

    // ---------- Сотрудники ----------
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

    // ---------- Организации ----------
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

    // ---------- Записи журнала (смены / выплаты / налоги / доплаты) ----------
    fun addEntry(entry: LedgerEntry) {
        _entries.value = _entries.value + entry
        syncLaunch { FirebaseRepository.upsertEntry(entry) }
        persistLocalSnapshot()
    }

    fun deleteEntry(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
        syncLaunch { FirebaseRepository.deleteEntry(id) }
        persistLocalSnapshot()
    }

    fun setOpeningBalance(value: Double) {
        _openingBalance.value = value
        syncLaunch { FirebaseRepository.pushOpeningBalance(value) }
        persistLocalSnapshot()
    }

    // ---------- Расчёт дохода за текущий месяц (с учётом фильтров) ----------
    fun currentBreakdown(): PayrollBreakdown = PayrollCalculator.calculate(
        entries = _entries.value,
        employees = _employees.value,
        month = _currentMonth.value,
        employeeFilter = _selectedEmployeeId.value,
        organizationFilter = _selectedOrganizationId.value,
        openingBalance = _openingBalance.value
    )

    // ---------- Firebase (облако) ----------
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

    // ---------- Бекапы (локальный JSON, хранится внутри памяти телефона) ----------
    fun currentState(): AppState = AppState(
        employees = _employees.value,
        organizations = _organizations.value,
        entries = _entries.value,
        openingBalance = _openingBalance.value
    )

    private fun currentSettingsJson(): JSONObject = JSONObject().apply {
        put("cloudSyncEnabled", _cloudSyncEnabled.value)
        put("selectedEmployeeId", _selectedEmployeeId.value ?: JSONObject.NULL)
        put("selectedOrganizationId", _selectedOrganizationId.value ?: JSONObject.NULL)
        put("currentMonth", _currentMonth.value.toString())
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
        if (!settings.isNull("selectedEmployeeId")) {
            _selectedEmployeeId.value = settings.optString("selectedEmployeeId").takeIf { it.isNotBlank() }
        }
        if (!settings.isNull("selectedOrganizationId")) {
            _selectedOrganizationId.value = settings.optString("selectedOrganizationId").takeIf { it.isNotBlank() }
        }
        runCatching { _currentMonth.value = YearMonth.parse(settings.optString("currentMonth")) }
        _cloudSyncEnabled.value = settings.optBoolean("cloudSyncEnabled", false)
        persistLocalSnapshot()
    }

    fun deleteBackup(file: File) = BackupManager.deleteBackup(file)

    // ---------- Автосохранение снимка (чтобы данные не терялись при закрытии приложения) ----------
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

                val settings = root.optJSONObject("settings") ?: JSONObject()
                _cloudSyncEnabled.value = settings.optBoolean("cloudSyncEnabled", false)
                if (!settings.isNull("selectedEmployeeId")) {
                    _selectedEmployeeId.value = settings.optString("selectedEmployeeId").takeIf { it.isNotBlank() }
                }
                if (!settings.isNull("selectedOrganizationId")) {
                    _selectedOrganizationId.value = settings.optString("selectedOrganizationId").takeIf { it.isNotBlank() }
                }
                runCatching { _currentMonth.value = YearMonth.parse(settings.optString("currentMonth")) }
            }
        }
    }
}
