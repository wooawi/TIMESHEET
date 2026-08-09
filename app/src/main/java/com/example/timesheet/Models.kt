package com.example.timesheet.data

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// ========== ОСНОВНЫЕ МОДЕЛИ ==========
data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val hourlyRate: Double = 0.0
)

data class Organization(
    val id: String = UUID.randomUUID().toString(),
    val name: String = ""
)

enum class EntryType {
    SHIFT,
    PAYMENT,
    TAX,
    ADJUSTMENT,
    EXPENSE
}

enum class ShiftType {
    DAY,
    NIGHT,
    HOLIDAY,
    OVERTIME,
    WEEKEND
}

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val shiftType: ShiftType = ShiftType.DAY,
    val type: EntryType = EntryType.SHIFT,
    val employeeId: String? = null,
    val organizationId: String? = null,
    val amount: Double = 0.0,
    val hours: Double = 0.0,
    val note: String = "",
    val expenseCategoryId: String? = null,
    val unitId: String? = null,
    val quantity: Double = 0.0
) {
    fun calculateHours(): Double {
        if (startTime == null || endTime == null) return hours
        val start = startTime.toSecondOfDay()
        val end = endTime.toSecondOfDay()
        val diffSeconds = if (end >= start) end - start else (end + 86400) - start
        return diffSeconds / 3600.0
    }
}

// ========== МОДЕЛИ ДЛЯ СПРАВОЧНИКОВ ==========

// 1. Типы времени
data class TimeType(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val code: String = "",
    val color: String = "#45B7D1",
    val isBuiltIn: Boolean = false
)

// 2. Категории расходов
data class ExpenseCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val isBuiltIn: Boolean = false
)

// 3. Единицы измерения (ТОЛЬКО ПРЕДУСТАНОВЛЕННЫЕ)
data class UnitOfMeasure(
    val id: String,
    val name: String,
    val shortName: String,
    val isBuiltIn: Boolean = true
)

// 4. Налоги
data class Tax(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val rate: Double = 0.0,
    val isBuiltIn: Boolean = false
)

// ========== ФАБРИКИ ПРЕДУСТАНОВЛЕННЫХ ДАННЫХ ==========

// ТОЛЬКО ЕДИНИЦЫ ИЗМЕРЕНИЯ - ПРЕДУСТАНОВЛЕННЫЕ
fun defaultUnits(): List<UnitOfMeasure> = listOf(
    UnitOfMeasure("unit_km", "Километр", "км"),
    UnitOfMeasure("unit_hour", "Час", "ч"),
    UnitOfMeasure("unit_piece", "Штука", "шт")
)

// Остальные справочники - ПУСТЫЕ
fun defaultTimeTypes(): List<TimeType> = emptyList()
fun defaultExpenseCategories(): List<ExpenseCategory> = emptyList()
fun defaultTaxes(): List<Tax> = emptyList()

data class AppState(
    val employees: List<Employee> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val entries: List<LedgerEntry> = emptyList(),
    val openingBalance: Double = 0.0,
    val surcharges: List<Surcharge> = emptyList(),
    val shiftTemplates: List<ShiftTemplate> = emptyList(),
    val timeTypes: List<TimeType> = defaultTimeTypes(),
    val expenseCategories: List<ExpenseCategory> = defaultExpenseCategories(),
    val units: List<UnitOfMeasure> = defaultUnits(),
    val taxes: List<Tax> = defaultTaxes()
)

// ========== ДОПЛАТЫ И ШАБЛОНЫ ==========

enum class SurchargeKind { BONUS, DEDUCTION }
enum class SurchargeCalcType { FIXED_PER_SHIFT, PER_HOUR, PERCENT }

data class Surcharge(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val kind: SurchargeKind = SurchargeKind.BONUS,
    val calcType: SurchargeCalcType = SurchargeCalcType.FIXED_PER_SHIFT,
    val amount: Double = 0.0,
    val taxable: Boolean = true,
    val activeWeekdays: Set<Int> = emptySet(),
    val isBuiltIn: Boolean = false
)

data class ShiftTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val employeeId: String? = null,
    val organizationId: String? = null,
    val shiftTypeName: String = "",
    val projectName: String = "",
    val surchargeIds: List<String> = emptyList(),
    val comment: String = ""
)

fun defaultSurchargeLibrary(): List<Surcharge> = listOf(
    Surcharge(
        id = "builtin_premium", name = "Премия",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.FIXED_PER_SHIFT, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_weekend", name = "Выходные дни",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PERCENT, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_night", name = "Ночные часы",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PER_HOUR, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_holiday", name = "Праздничные дни",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PERCENT, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_piecework", name = "Пример сдельной работы",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PER_HOUR, isBuiltIn = true
    )
)