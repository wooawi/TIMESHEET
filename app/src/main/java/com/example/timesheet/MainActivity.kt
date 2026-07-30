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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
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


data class ListEntry(
    val id: Long,
    val name: String
)


data class DialogState(
    val editingId: Long?,
    val initialName: String
)

private fun newEntryId(): Long = System.nanoTime()


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TIMESHEETApp() {
    var shiftsMenuExpanded by remember { mutableStateOf(false) }
    var personMenuExpanded by remember { mutableStateOf(false) }
    var organizationsMenuExpanded by remember { mutableStateOf(false) }
    var addMenuExpanded by remember { mutableStateOf(false) }
    val employees = remember { mutableStateListOf<ListEntry>() }
    val organizations = remember { mutableStateListOf<ListEntry>() }
    var expandedEmployeeId by remember { mutableStateOf<Long?>(null) }
    var expandedOrganizationId by remember { mutableStateOf<Long?>(null) }
    var employeeDialogState by remember { mutableStateOf<DialogState?>(null) }
    var organizationDialogState by remember { mutableStateOf<DialogState?>(null) }
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
                expandedEntryId = expandedEmployeeId,
                onEntryClick = { id ->
                    expandedEmployeeId = if (expandedEmployeeId == id) null else id
                },
                onEditClick = { entry ->
                    employeeDialogState = DialogState(entry.id, entry.name)
                },
                onDeleteClick = { id ->
                    employees.removeAll { it.id == id }
                    if (expandedEmployeeId == id) expandedEmployeeId = null
                },
                onAddClick = { employeeDialogState = DialogState(null, "") }
            )

            "Организация" -> EntityListScreen(
                onMenuClick = onMenuClick,
                title = "Организация",
                icon = Icons.Filled.ShoppingBag,
                entries = organizations,
                expandedEntryId = expandedOrganizationId,
                onEntryClick = { id ->
                    expandedOrganizationId = if (expandedOrganizationId == id) null else id
                },
                onEditClick = { entry ->
                    organizationDialogState = DialogState(entry.id, entry.name)
                },
                onDeleteClick = { id ->
                    organizations.removeAll { it.id == id }
                    if (expandedOrganizationId == id) expandedOrganizationId = null
                },
                onAddClick = { organizationDialogState = DialogState(null, "") }
            )

            "Журнал расчетов" -> MainScreen(
                onMenuClick = onMenuClick,
                organizationsMenuExpanded = organizationsMenuExpanded,
                onOrganizationsClick = { organizationsMenuExpanded = true },
                onOrganizationsMenuDismiss = { organizationsMenuExpanded = false },
                personMenuExpanded = personMenuExpanded,
                onPersonClick = { personMenuExpanded = true },
                onPersonMenuDismiss = { personMenuExpanded = false },
                shiftsMenuExpanded = shiftsMenuExpanded,
                onShiftsClick = { shiftsMenuExpanded = true },
                onShiftsMenuDismiss = { shiftsMenuExpanded = false },
                addMenuExpanded = addMenuExpanded,
                onAddClick = { addMenuExpanded = true },
                onAddMenuDismiss = { addMenuExpanded = false }
            )


            "Отчеты" -> ReportsMenuScreen(
                onMenuClick = onMenuClick,
                onNavigate = { key -> currentScreen = key }
            )


            else -> PlaceholderScreen(
                title = currentScreen.substringAfterLast("/"),
                onMenuClick = onMenuClick
            )
        }

        employeeDialogState?.let { state ->
            EntityEditDialog(
                title = if (state.editingId == null) "Новый сотрудник" else "Редактировать сотрудника",
                initialName = state.initialName,
                onDismiss = { employeeDialogState = null },
                onConfirm = { name ->
                    if (state.editingId == null) {
                        employees.add(ListEntry(id = newEntryId(), name = name))
                    } else {
                        val index = employees.indexOfFirst { it.id == state.editingId }
                        if (index >= 0) employees[index] = employees[index].copy(name = name)
                    }
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
                    if (state.editingId == null) {
                        organizations.add(ListEntry(id = newEntryId(), name = name))
                    } else {
                        val index = organizations.indexOfFirst { it.id == state.editingId }
                        if (index >= 0) organizations[index] = organizations[index].copy(name = name)
                    }
                    organizationDialogState = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onMenuClick: () -> Unit,
    organizationsMenuExpanded: Boolean,
    onOrganizationsClick: () -> Unit,
    onOrganizationsMenuDismiss: () -> Unit,
    personMenuExpanded: Boolean,
    onPersonClick: () -> Unit,
    onPersonMenuDismiss: () -> Unit,
    shiftsMenuExpanded: Boolean,
    onShiftsClick: () -> Unit,
    onShiftsMenuDismiss: () -> Unit,
    addMenuExpanded: Boolean,
    onAddClick: () -> Unit,
    onAddMenuDismiss: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                onMenuClick = onMenuClick,
                onOrganizationsClick = onOrganizationsClick,
                organizationsMenuExpanded = organizationsMenuExpanded,
                onOrganizationsMenuDismiss = onOrganizationsMenuDismiss,
                onPersonClick = onPersonClick,
                personMenuExpanded = personMenuExpanded,
                onPersonMenuDismiss = onPersonMenuDismiss,
                onShiftsClick = onShiftsClick,
                shiftsMenuExpanded = shiftsMenuExpanded,
                onShiftsMenuDismiss = onShiftsMenuDismiss
            )
        },
        floatingActionButton = {
            AddFab(onClick = onAddClick)
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(end = 24.dp, bottom = 24.dp)
                .wrapContentSize(Alignment.BottomEnd)
        ) {
            AddMenu(
                expanded = addMenuExpanded,
                onDismiss = onAddMenuDismiss
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityListScreen(
    onMenuClick: () -> Unit,
    title: String,
    icon: ImageVector,
    entries: List<ListEntry>,
    expandedEntryId: Long?,
    onEntryClick: (Long) -> Unit,
    onEditClick: (ListEntry) -> Unit,
    onDeleteClick: (Long) -> Unit,
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
                items(entries, key = { it.id }) { entry ->
                    EntityListItem(
                        entry = entry,
                        icon = icon,
                        expanded = expandedEntryId == entry.id,
                        onClick = { onEntryClick(entry.id) },
                        onEditClick = { onEditClick(entry) },
                        onDeleteClick = { onDeleteClick(entry.id) }
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
    entry: ListEntry,
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
                    text = entry.name,
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
    onPersonClick: () -> Unit,
    personMenuExpanded: Boolean,
    onPersonMenuDismiss: () -> Unit,
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
                OrganizationsMenu(
                    expanded = organizationsMenuExpanded,
                    onDismiss = onOrganizationsMenuDismiss
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
                PersonMenu(
                    expanded = personMenuExpanded,
                    onDismiss = onPersonMenuDismiss
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
fun PersonMenu(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // TODO: список сотрудников с кнопкой добавления (плюс) и удаления,
        // содержимое должно быть выровнено по центру экрана
    }
}

@Composable
fun OrganizationsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // TODO: список организаций с кнопкой добавления (плюс) и удаления,
        // содержимое должно быть выровнено по центру экрана
    }
}


@Composable
fun AddFab(onClick: () -> Unit) {
    FloatingActionButton(
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
    onDismiss: () -> Unit
) {
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
            onClick = {
                onDismiss()

            }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.MoneyOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Налог")
                }
            },
            onClick = {
                onDismiss()

            }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Запись табеля")
                }
            },
            onClick = {
                onDismiss()

            }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.AttachMoney, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Доплата, удержание")
                }
            },
            onClick = {
                onDismiss()

            }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.Assignment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Смена по шаблону")
                }
            },
            onClick = {
                onDismiss()

            }
        )
        DropdownMenuItem(
            text = {
                Row {
                    Icon(imageVector = Icons.Filled.PermContactCalendar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Смена")
                }
            },
            onClick = {
                onDismiss()

            }
        )
    }
}