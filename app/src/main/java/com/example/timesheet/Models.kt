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
    val quantity: Double = 0.0,
    // ДОБАВЛЕНО (ТЗ «настроить правильную математику»): доплаты/удержания,
    // применённые к этой смене (переносятся из шаблона смены при применении).
    val surchargeIds: List<String> = emptyList(),
    // ДОБАВЛЕНО (диалог «Смена» по макету): неоплачиваемые перерывы (минуты),
    // признак «Оплата сверхурочных часов» и привязанный проект.
    val unpaidBreakMinutes: Int = 0,
    val overtimeEnabled: Boolean = false,
    val projectName: String = "",
    // ДОБАВЛЕНО (ТЗ: полноэкранные диалоги «Доплата, удержание» и «Запись табеля»):
    // выбранный тип доплаты/удержания (включая собственные варианты пользователя)
    // и привязка к записи справочника «Типы времени» для записей табеля.
    val adjustmentTypeName: String = "",
    val timeTypeId: String? = null,
    // ДОБАВЛЕНО (ТЗ «сделать открытие галереи/файлов настоящими»): реальные вложения
    // записи — content:// URI файлов/фото, выбранных через системный выбор файлов.
    val attachments: List<String> = emptyList()
) {
    /** Полная длительность смены по времени начала/конца (без вычета перерывов). */
    fun calculateHours(): Double {
        if (startTime == null || endTime == null) return hours
        val start = startTime.toSecondOfDay()
        val end = endTime.toSecondOfDay()
        val diffSeconds = if (end >= start) end - start else (end + 86400) - start
        return diffSeconds / 3600.0
    }

    /** Оплачиваемые часы = длительность смены минус неоплачиваемые перерывы. */
    fun calculatePaidHours(): Double {
        val raw = calculateHours() - unpaidBreakMinutes / 60.0
        return if (raw < 0.0) 0.0 else raw
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

fun defaultUnits(): List<UnitOfMeasure> = listOf(
    UnitOfMeasure("unit_km", "Километр", "км"),
    UnitOfMeasure("unit_hour", "Час", "ч"),
    UnitOfMeasure("unit_piece", "Штука", "шт")
)

// ДОБАВЛЕНО (ТЗ: «Сделать в справочнике встроенными данные категории (с
// возможностью редактирования)»): раньше все три фабрики возвращали пустой
// список — экраны «Типы времени» / «Категории расходов» / «Налоги» были
// пустыми при первом запуске. Данные ниже и порядок цветов взяты из
// референс-скриншотов в ТЗ. isBuiltIn = true просто помечает происхождение
// записи (сама фабрика), удаление/редактирование этим не ограничено —
// экраны и так уже поддерживают и то, и другое.
fun defaultTimeTypes(): List<TimeType> = listOf(
    TimeType(id = "tt_sick", name = "Больничный", code = "Б", color = "#8395A7", isBuiltIn = true),
    TimeType(id = "tt_evening", name = "Вечерняя смена", code = "В", color = "#48DBFB", isBuiltIn = true),
    TimeType(id = "tt_holiday", name = "Выходные и нерабочие праздничные", code = "ВП", color = "#FF6FB7", isBuiltIn = true),
    TimeType(id = "tt_day", name = "Дневная смена", code = "Д", color = "#5CA02F", isBuiltIn = true),
    TimeType(id = "tt_unpaid_vacation", name = "Неоплачиваемый отпуск", code = "ДО", color = "#10AC84", isBuiltIn = true),
    TimeType(id = "tt_night", name = "Ночная смена", code = "Н", color = "#8395A7", isBuiltIn = true),
    TimeType(id = "tt_paid_vacation", name = "Оплачиваемый отпуск", code = "ОТ", color = "#5F27CD", isBuiltIn = true),
    TimeType(id = "tt_business_trip", name = "Служебная командировка", code = "К", color = "#FF9F43", isBuiltIn = true)
)

fun defaultExpenseCategories(): List<ExpenseCategory> = listOf(
    ExpenseCategory(id = "ec_transport", name = "Проезд", isBuiltIn = true),
    ExpenseCategory(id = "ec_accommodation", name = "Проживание", isBuiltIn = true),
    ExpenseCategory(id = "ec_other", name = "Прочее", isBuiltIn = true),
    ExpenseCategory(id = "ec_fuel", name = "Топливо/километраж", isBuiltIn = true)
)

fun defaultTaxes(): List<Tax> = listOf(
    Tax(id = "tax_ndfl", name = "НДФЛ", rate = 13.0, isBuiltIn = true)
)

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