package com.example.ui.screens.workers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConstructionTrade
import com.example.data.model.Worker
import com.example.ui.components.CategorySelectionDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMasterScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val workers by viewModel.allWorkers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Active", "Inactive"
    var categoryFilter by remember { mutableStateOf("All") } // "All" or trade code
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<Worker?>(null) }
    var workerToDelete by remember { mutableStateOf<Worker?>(null) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }

    val existingWorkerTypes = remember(workers) {
        workers.map { it.workerType }.distinct()
    }

    val filteredWorkers = remember(workers, searchQuery, statusFilter, categoryFilter) {
        workers.filter { w ->
            val myanmarTrade = CategoryManager.getMyanmarName(w.workerType)
            val matchesSearch = searchQuery.isBlank() ||
                    w.name.contains(searchQuery, ignoreCase = true) ||
                    w.workerId.contains(searchQuery, ignoreCase = true) ||
                    w.workerType.contains(searchQuery, ignoreCase = true) ||
                    myanmarTrade.contains(searchQuery, ignoreCase = true) ||
                    w.phone.contains(searchQuery)

            val matchesStatus = statusFilter == "All" || w.status == statusFilter

            val matchesCategory = categoryFilter == "All" ||
                    w.workerType.equals(categoryFilter, ignoreCase = true) ||
                    myanmarTrade.equals(categoryFilter, ignoreCase = true)

            matchesSearch && matchesStatus && matchesCategory
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("worker_master_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ConstructionGold,
                contentColor = Slate900,
                modifier = Modifier.testTag("add_worker_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Worker", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header & Filters
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Worker Master (လုပ်သားစာရင်း)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${workers.size} registered (${workers.count { it.status == "Active" }} active)",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        // Manage Categories Button
                        OutlinedButton(
                            onClick = { showManageCategoriesDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("manage_categories_btn")
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp), tint = SafetyOrangeDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("အမျိုးအစားများ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SafetyOrangeDark)
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, ID (W-101), trade (လက်သမား၊ ပန်းရံ)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_search_bar"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Status Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Active", "Inactive").forEach { status ->
                            FilterChip(
                                selected = statusFilter == status,
                                onClick = { statusFilter = status },
                                label = { Text(status) },
                                modifier = Modifier.testTag("status_filter_${status.lowercase()}")
                            )
                        }
                    }

                    // Construction Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category:", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.SemiBold)

                        FilterChip(
                            selected = categoryFilter == "All",
                            onClick = { categoryFilter = "All" },
                            label = { Text("All", fontSize = 12.sp) }
                        )

                        val commonTrades = listOf(
                            "Carpenter" to "လက်သမား",
                            "Mason" to "ပန်းရံ",
                            "Helper" to "အကူ",
                            "Bar Bender" to "သံချည်",
                            "Welder" to "ဝရိန်",
                            "Electrician" to "လျှပ်စစ်",
                            "Painter" to "ဆေးသုတ်",
                            "Foreman" to "ဖိုမင်"
                        )

                        commonTrades.forEach { (code, mmName) ->
                            val isSelected = categoryFilter.equals(code, ignoreCase = true) || categoryFilter.equals(mmName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryFilter = if (isSelected) "All" else code
                                },
                                label = { Text(mmName, fontSize = 12.sp) },
                                leadingIcon = {
                                    TradeIcon(trade = code, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Workers List
            if (filteredWorkers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOff,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No workers match criteria",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap '+ Add Worker' to register a new construction worker.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredWorkers) { worker ->
                        WorkerItemCard(
                            worker = worker,
                            onEdit = { editingWorker = worker },
                            onDelete = { workerToDelete = worker }
                        )
                    }
                }
            }
        }
    }

    // Add Worker Dialog
    if (showAddDialog) {
        val nextId = remember(workers) {
            val maxNum = workers.mapNotNull { it.workerId.removePrefix("W-").toIntOrNull() }.maxOrNull() ?: 100
            "W-${maxNum + 1}"
        }
        WorkerFormDialog(
            initialWorker = Worker(workerId = nextId, name = ""),
            isNew = true,
            existingWorkerTypes = existingWorkerTypes,
            onDismiss = { showAddDialog = false },
            onSave = { newWorker ->
                viewModel.addWorker(newWorker) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Worker Dialog
    if (editingWorker != null) {
        WorkerFormDialog(
            initialWorker = editingWorker!!,
            isNew = false,
            existingWorkerTypes = existingWorkerTypes,
            onDismiss = { editingWorker = null },
            onSave = { updated ->
                viewModel.updateWorker(updated) {
                    editingWorker = null
                }
            }
        )
    }

    // Manage Categories Dialog
    if (showManageCategoriesDialog) {
        CategorySelectionDialog(
            currentTradeCode = categoryFilter,
            existingWorkerTypes = existingWorkerTypes,
            onDismiss = { showManageCategoriesDialog = false },
            onSelectTrade = { selectedTrade ->
                categoryFilter = selectedTrade.code
                showManageCategoriesDialog = false
            }
        )
    }

    // Delete Worker Confirmation
    if (workerToDelete != null) {
        val target = workerToDelete!!
        val isMy = currentLanguage == AppLanguage.MYANMAR
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = if (isMy) "လုပ်သားစာရင်း ဖျက်ရန်" else "Delete Worker?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isMy)
                            "${target.name} (${target.workerId}) ကို လုပ်သားစာရင်းမှ ဖျက်မည်မှာ သေချာပါသလား။"
                        else
                            "Are you sure you want to delete ${target.name} (${target.workerId}) from worker master?",
                        fontSize = 13.sp
                    )
                    Surface(
                        color = AdvanceRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("👷 ကျွမ်းကျင်မှု: ${CategoryManager.getDisplayName(target.workerType)}", fontSize = 12.sp)
                            Text("💵 နေ့တွက်နှုန်း: ${ExcelExportHelper.formatCurrency(target.dailyRate)}/ရက်", fontSize = 12.sp)
                            Text(
                                text = if (isMy)
                                    "ℹ️ သတိပြုရန်- လုပ်သားကို ဖျက်သော်လည်း စာရင်းစစ်ဆေးမှုများ မပျက်စီးစေရန် ယခင်တက်ရောက်မှု မှတ်တမ်းဟောင်းများ ဆက်လက်ကျန်ရှိနေမည်ဖြစ်သည်။"
                                else
                                    "Note: Past attendance records will remain preserved for audit ledger integrity.",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorker(target)
                        workerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_worker_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isMy) "အတည်ပြု ဖျက်မည်" else "Confirm Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { workerToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isMy) "မဖျက်ပါ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkerItemCard(
    worker: Worker,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("worker_card_${worker.workerId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                TradeIcon(trade = worker.workerType)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatusBadge(status = worker.status)
                    }

                    Text(
                        text = "${worker.workerId} • ${CategoryManager.getDisplayName(worker.workerType)} • ${worker.gender}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Daily: ${ExcelExportHelper.formatCurrency(worker.dailyRate)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WagesGreenDark
                        )
                        Text(
                            text = "OT: ${ExcelExportHelper.formatCurrency(worker.otRate)}/hr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OtBlue
                        )
                    }

                    if (worker.phone.isNotBlank()) {
                        Text(
                            text = "📞 ${worker.phone}",
                            fontSize = 11.sp,
                            color = Slate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row {
                if (worker.phone.isNotBlank()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${worker.phone.replace(" ", "")}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("call_worker_btn_${worker.workerId}")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = WagesGreenDark, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("edit_worker_btn_${worker.workerId}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OtBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("delete_worker_btn_${worker.workerId}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdvanceRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFormDialog(
    initialWorker: Worker,
    isNew: Boolean,
    existingWorkerTypes: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Worker) -> Unit
) {
    val context = LocalContext.current
    var workerId by remember { mutableStateOf(initialWorker.workerId) }
    var name by remember { mutableStateOf(initialWorker.name) }
    var gender by remember { mutableStateOf(initialWorker.gender) }
    var workerType by remember { mutableStateOf(initialWorker.workerType) }
    var dailyRateStr by remember { mutableStateOf(initialWorker.dailyRate.toInt().toString()) }
    var otRateStr by remember { mutableStateOf(initialWorker.otRate.toInt().toString()) }
    var status by remember { mutableStateOf(initialWorker.status) }
    var phone by remember { mutableStateOf(initialWorker.phone) }

    var showCategoryPickerDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isNew) "Add New Worker (လုပ်သားအသစ်)" else "Edit Worker (လုပ်သားပြင်ဆင်ခြင်း)",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = workerId,
                        onValueChange = { workerId = it },
                        label = { Text("Worker ID (ကုဒ် e.g. W-101)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_id_input"),
                        enabled = isNew,
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name (လုပ်သားအမည်) *") },
                        placeholder = { Text("ဥပမာ- ကိုအောင်ကျော်၊ ဦးဘစိန်") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_name_input"),
                        singleLine = true
                    )
                }

                // CATEGORY / TRADE SELECTION SECTION (Robust, Guaranteed to open & customizable)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ဆောက်လုပ်ရေး အမျိုးအစား (Trade) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            TextButton(
                                onClick = { showCategoryPickerDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("အားလုံးကြည့် / စိတ်ကြိုက်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Selected Category Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryPickerDialog = true }
                                .testTag("worker_type_picker"),
                            shape = RoundedCornerShape(10.dp),
                            color = ConstructionGold.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ConstructionGoldDark)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TradeIcon(trade = workerType)
                                    Column {
                                        Text(
                                            text = CategoryManager.getMyanmarName(workerType),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = workerType,
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ပြောင်းရန်", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SafetyOrangeDark)
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select",
                                        tint = SafetyOrangeDark
                                    )
                                }
                            }
                        }

                        // Quick-Tap Category Chips for instantaneous 1-tap selection
                        Text("အသုံးများသော ဆောက်လုပ်ရေး အသုံးအနှုန်းများ-", fontSize = 11.sp, color = Slate500)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickTrades = listOf(
                                "Carpenter" to "လက်သမား",
                                "Mason" to "ပန်းရံ",
                                "Helper" to "အကူ",
                                "Bar Bender" to "သံချည်",
                                "Welder" to "ဝရိန်",
                                "Electrician" to "လျှပ်စစ်",
                                "Painter" to "ဆေးသုတ်",
                                "Foreman" to "ဖိုမင်"
                            )

                            quickTrades.forEach { (code, mm) ->
                                val isSelected = workerType.equals(code, ignoreCase = true) || workerType.equals(mm, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        workerType = code
                                        val std = CategoryManager.getTradeForCode(code, context)
                                        if (isNew || dailyRateStr == "20000" || dailyRateStr.isBlank()) {
                                            dailyRateStr = std.defaultDailyRate.toInt().toString()
                                            otRateStr = std.defaultOtRate.toInt().toString()
                                        }
                                    },
                                    label = { Text(mm, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ConstructionGold,
                                        selectedLabelColor = Slate900
                                    )
                                )
                            }
                        }

                        // Direct text customization if needed
                        OutlinedTextField(
                            value = workerType,
                            onValueChange = { workerType = it },
                            label = { Text("Custom Trade Name (စိတ်ကြိုက် အမည်ရိုက်ထည့်ရန်)") },
                            placeholder = { Text("ဥပမာ- သံကူကွန်ကရစ်၊ ကြွေပြားကပ်") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_worker_type_text"),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Gender (ကျား/မ):", fontSize = 12.sp, color = Slate600)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male" to "ကျား", "Female" to "မ", "Other" to "အခြား").forEach { (g, mm) ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text("$g ($mm)") }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyRateStr,
                            onValueChange = { dailyRateStr = it },
                            label = { Text("Daily Rate (ကျပ်/ရက်)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("daily_rate_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = otRateStr,
                            onValueChange = { otRateStr = it },
                            label = { Text("OT Rate (ကျပ်/နာရီ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("ot_rate_input"),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Status (အလုပ်အခြေအနေ):", fontSize = 12.sp, color = Slate600)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Active" to "လက်ရှိ (Active)", "Inactive" to "ရပ်နား (Inactive)").forEach { (s, label) ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (ဖုန်းနံပါတ်)") },
                        placeholder = { Text("09xxxxxxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("worker_phone_input"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && workerId.isNotBlank()) {
                        val dRate = dailyRateStr.toDoubleOrNull() ?: 20000.0
                        val oRate = otRateStr.toDoubleOrNull() ?: 3000.0
                        onSave(
                            Worker(
                                workerId = workerId.trim(),
                                name = name.trim(),
                                gender = gender,
                                workerType = workerType.trim(),
                                dailyRate = dRate,
                                otRate = oRate,
                                status = status,
                                phone = phone.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && workerId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                modifier = Modifier.testTag("save_worker_dialog_button")
            ) {
                Text(if (isNew) "Add Worker" else "Save Changes", color = Slate900, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Category Selector Dialog
    if (showCategoryPickerDialog) {
        CategorySelectionDialog(
            currentTradeCode = workerType,
            existingWorkerTypes = existingWorkerTypes,
            onDismiss = { showCategoryPickerDialog = false },
            onSelectTrade = { selectedTrade ->
                workerType = selectedTrade.code
                if (isNew || dailyRateStr == "20000" || dailyRateStr.isBlank()) {
                    dailyRateStr = selectedTrade.defaultDailyRate.toInt().toString()
                    otRateStr = selectedTrade.defaultOtRate.toInt().toString()
                }
                showCategoryPickerDialog = false
            }
        )
    }
}
