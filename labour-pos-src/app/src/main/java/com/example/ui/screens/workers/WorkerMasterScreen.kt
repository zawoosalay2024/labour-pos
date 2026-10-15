package com.example.ui.screens.workers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Worker
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.ExcelExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMasterScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val workers by viewModel.allWorkers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Active", "Inactive"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<Worker?>(null) }
    var workerToDelete by remember { mutableStateOf<Worker?>(null) }

    val filteredWorkers = remember(workers, searchQuery, statusFilter) {
        workers.filter { w ->
            val matchesSearch = searchQuery.isBlank() ||
                    w.name.contains(searchQuery, ignoreCase = true) ||
                    w.workerId.contains(searchQuery, ignoreCase = true) ||
                    w.workerType.contains(searchQuery, ignoreCase = true) ||
                    w.phone.contains(searchQuery)
            val matchesStatus = statusFilter == "All" || w.status == statusFilter
            matchesSearch && matchesStatus
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
                                text = "Worker Master",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${workers.size} registered workers (${workers.count { it.status == "Active" }} active)",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, ID (e.g. W-101), trade...") },
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

                    // Filter Chips
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
            onDismiss = { editingWorker = null },
            onSave = { updated ->
                viewModel.updateWorker(updated) {
                    editingWorker = null
                }
            }
        )
    }

    // Delete Worker Confirmation
    if (workerToDelete != null) {
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            title = { Text("Delete Worker?") },
            text = {
                Text("Delete ${workerToDelete?.name} (${workerToDelete?.workerId})? Note: past attendance records will remain for audit ledger.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        workerToDelete?.let { viewModel.deleteWorker(it) }
                        workerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { workerToDelete = null }) {
                    Text("Cancel")
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        text = "${worker.workerId} • ${worker.workerType} • ${worker.gender}",
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
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OtBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdvanceRed, modifier = Modifier.size(18.dp))
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
    onDismiss: () -> Unit,
    onSave: (Worker) -> Unit
) {
    var workerId by remember { mutableStateOf(initialWorker.workerId) }
    var name by remember { mutableStateOf(initialWorker.name) }
    var gender by remember { mutableStateOf(initialWorker.gender) }
    var workerType by remember { mutableStateOf(initialWorker.workerType) }
    var dailyRateStr by remember { mutableStateOf(initialWorker.dailyRate.toString()) }
    var otRateStr by remember { mutableStateOf(initialWorker.otRate.toString()) }
    var status by remember { mutableStateOf(initialWorker.status) }
    var phone by remember { mutableStateOf(initialWorker.phone) }

    val tradeOptions = listOf(
        "Carpenter", "Mason", "Helper", "Electrician",
        "Plumber", "Painter", "Welder", "Bar Bender", "Foreman"
    )

    var tradeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Add New Worker" else "Edit Worker Details", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = workerId,
                        onValueChange = { workerId = it },
                        label = { Text("Worker ID (e.g. W-101)") },
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
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Rajesh Kumar") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_name_input"),
                        singleLine = true
                    )
                }

                item {
                    // Trade / Type Dropdown
                    Box {
                        OutlinedTextField(
                            value = workerType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Skill / Category *") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tradeDropdownExpanded = true }
                                .testTag("worker_type_picker")
                        )
                        DropdownMenu(
                            expanded = tradeDropdownExpanded,
                            onDismissRequest = { tradeDropdownExpanded = false }
                        ) {
                            tradeOptions.forEach { trade ->
                                DropdownMenuItem(
                                    text = { Text(trade) },
                                    onClick = {
                                        workerType = trade
                                        tradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Gender:", fontSize = 12.sp, color = Slate600)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) }
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
                            label = { Text("Daily Rate (Ks / ကျပ်)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = otRateStr,
                            onValueChange = { otRateStr = it },
                            label = { Text("OT Rate (Ks/hr)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Status:", fontSize = 12.sp, color = Slate600)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Active", "Inactive").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
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
                                workerType = workerType,
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
}
