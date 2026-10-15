package com.example.ui.screens.attendance

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.ExcelExportHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceListScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allAttendance by viewModel.allAttendance.collectAsState()
    val sites by viewModel.allSites.collectAsState()

    var selectedFilterDate by remember { mutableStateOf<String?>(null) } // null means all dates
    var selectedFilterSiteCode by remember { mutableStateOf<String?>(null) } // null means all sites
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("card") } // "card" or "table"
    var recordToDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    val filteredList = remember(allAttendance, selectedFilterDate, selectedFilterSiteCode, searchQuery) {
        allAttendance.filter { record ->
            val matchesDate = selectedFilterDate == null || record.date == selectedFilterDate
            val matchesSite = selectedFilterSiteCode == null || record.siteCode == selectedFilterSiteCode
            val matchesSearch = searchQuery.isBlank() ||
                    record.workerName.contains(searchQuery, ignoreCase = true) ||
                    record.workerId.contains(searchQuery, ignoreCase = true) ||
                    record.workerType.contains(searchQuery, ignoreCase = true) ||
                    record.remark.contains(searchQuery, ignoreCase = true)
            matchesDate && matchesSite && matchesSearch
        }
    }

    val totalEarnedSum = filteredList.sumOf { it.totalEarned }
    val totalAdvanceSum = filteredList.sumOf { it.advanceToday }
    val totalPaidSum = filteredList.sumOf { it.paidToday }
    val totalBalanceSum = totalEarnedSum - totalAdvanceSum - totalPaidSum
    val totalManDays = filteredList.sumOf { it.totalDayUnits }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedFilterDate = formatted
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("attendance_list_screen")
    ) {
        // Top Toolbar
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
                            text = "Daily Attendance Log",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredList.size} records • ${String.format(Locale.US, "%.1f", totalManDays)} Man-Days",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }

                    // View Mode Switcher (Card vs PC Table View)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewMode = "card" },
                            modifier = Modifier
                                .background(
                                    if (viewMode == "card") PrimaryContainerLight else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("view_mode_card_btn")
                        ) {
                            Icon(
                                Icons.Default.ViewAgenda,
                                contentDescription = "Cards View",
                                tint = if (viewMode == "card") PrimaryLight else Slate500
                            )
                        }

                        IconButton(
                            onClick = { viewMode = "table" },
                            modifier = Modifier
                                .background(
                                    if (viewMode == "table") PrimaryContainerLight else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("view_mode_table_btn")
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = "PC Table View",
                                tint = if (viewMode == "table") PrimaryLight else Slate500
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.EXCEL_EXPORT) },
                            modifier = Modifier.testTag("export_excel_shortcut_btn")
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Excel Export",
                                tint = WagesGreenDark
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search worker name, ID, trade, remark...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Date & Site Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilterDate != null,
                        onClick = {
                            if (selectedFilterDate != null) {
                                selectedFilterDate = null
                            } else {
                                datePickerDialog.show()
                            }
                        },
                        label = {
                            Text(selectedFilterDate ?: "All Dates")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (selectedFilterDate != null) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.testTag("filter_date_chip")
                    )

                    var siteDropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        FilterChip(
                            selected = selectedFilterSiteCode != null,
                            onClick = { siteDropdownExpanded = true },
                            label = {
                                val sName = sites.find { it.siteCode == selectedFilterSiteCode }?.siteName
                                Text(sName ?: "All Sites")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (selectedFilterSiteCode != null) {
                                    IconButton(
                                        onClick = { selectedFilterSiteCode = null },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                } else {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.testTag("filter_site_chip")
                        )

                        DropdownMenu(
                            expanded = siteDropdownExpanded,
                            onDismissRequest = { siteDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sites") },
                                onClick = {
                                    selectedFilterSiteCode = null
                                    siteDropdownExpanded = false
                                }
                            )
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = { Text(site.siteName) },
                                    onClick = {
                                        selectedFilterSiteCode = site.siteCode
                                        siteDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            selectedFilterDate = viewModel.todayDateString
                        }
                    ) {
                        Text("Today")
                    }
                }

                // Summary Row for Filtered Data
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Wages", fontSize = 10.sp, color = Slate400)
                            Text(ExcelExportHelper.formatCurrency(totalEarnedSum), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WagesGreen)
                        }
                        Column {
                            Text("Advance", fontSize = 10.sp, color = Slate400)
                            Text(ExcelExportHelper.formatCurrency(totalAdvanceSum), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdvanceRedLight)
                        }
                        Column {
                            Text("Paid Today", fontSize = 10.sp, color = Slate400)
                            Text(ExcelExportHelper.formatCurrency(totalPaidSum), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OtBlueLight)
                        }
                        Column {
                            Text("Net Balance", fontSize = 10.sp, color = Slate400)
                            Text(ExcelExportHelper.formatCurrency(totalBalanceSum), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ConstructionGold)
                        }
                    }
                }
            }
        }

        // Content Area
        if (filteredList.isEmpty()) {
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
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No attendance records found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try clearing filters or log new attendance in the POS tab.",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.POS_ENTRY) },
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
                    ) {
                        Text("Log Attendance in POS", color = Slate900, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (viewMode == "table") {
            // PC TABLE SPREADSHEET VIEW (Horizontal & Vertical scroll, Excel structure)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        // Table Header matching Excel sheet structure
                        Row(
                            modifier = Modifier
                                .background(Slate900, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell("Date", 90.dp, isHeader = true)
                            TableCell("Site", 110.dp, isHeader = true)
                            TableCell("Worker ID", 80.dp, isHeader = true)
                            TableCell("Worker Name", 130.dp, isHeader = true)
                            TableCell("Trade", 90.dp, isHeader = true)
                            TableCell("AM", 45.dp, isHeader = true)
                            TableCell("PM", 45.dp, isHeader = true)
                            TableCell("Units", 55.dp, isHeader = true)
                            TableCell("Rate", 70.dp, isHeader = true)
                            TableCell("Daily W.", 75.dp, isHeader = true)
                            TableCell("OT (h)", 55.dp, isHeader = true)
                            TableCell("OT Amt", 70.dp, isHeader = true)
                            TableCell("Total", 80.dp, isHeader = true)
                            TableCell("Advance", 75.dp, isHeader = true)
                            TableCell("Paid", 75.dp, isHeader = true)
                            TableCell("Balance", 80.dp, isHeader = true)
                            TableCell("Remark", 120.dp, isHeader = true)
                            TableCell("Action", 90.dp, isHeader = true)
                        }
                    }

                    items(filteredList) { r ->
                        Row(
                            modifier = Modifier
                                .background(
                                    if (filteredList.indexOf(r) % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(0.5.dp, Slate200)
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell(r.date, 90.dp)
                            TableCell(r.siteName, 110.dp)
                            TableCell(r.workerId, 80.dp)
                            TableCell(r.workerName, 130.dp, isBold = true)
                            TableCell(r.workerType, 90.dp)
                            TableCell(r.morningAttendance, 45.dp)
                            TableCell(r.eveningAttendance, 45.dp)
                            TableCell(String.format(Locale.US, "%.2f", r.totalDayUnits), 55.dp)
                            TableCell(ExcelExportHelper.formatCurrency(r.dailyRate), 70.dp)
                            TableCell(ExcelExportHelper.formatCurrency(r.dailyAmount), 75.dp)
                            TableCell("${r.otHours}h", 55.dp)
                            TableCell(ExcelExportHelper.formatCurrency(r.otAmount), 70.dp)
                            TableCell(ExcelExportHelper.formatCurrency(r.totalEarned), 80.dp, color = WagesGreenDark, isBold = true)
                            TableCell(ExcelExportHelper.formatCurrency(r.advanceToday), 75.dp, color = if (r.advanceToday > 0) AdvanceRed else Slate600)
                            TableCell(ExcelExportHelper.formatCurrency(r.paidToday), 75.dp, color = if (r.paidToday > 0) OtBlue else Slate600)
                            TableCell(ExcelExportHelper.formatCurrency(r.balance), 80.dp, color = ConstructionGoldDark, isBold = true)
                            TableCell(r.remark.ifBlank { "-" }, 120.dp)

                            Row(
                                modifier = Modifier.width(90.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.editRecord(r) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OtBlue, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { recordToDelete = r },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdvanceRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MOBILE CARDS VIEW
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { record ->
                    DetailedAttendanceCard(
                        record = record,
                        onEdit = { viewModel.editRecord(record) },
                        onDelete = { recordToDelete = record }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Attendance Record?") },
            text = {
                Text("Are you sure you want to delete the record for ${recordToDelete?.workerName} on ${recordToDelete?.date}? This will remove it from earnings and balances.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { viewModel.deleteAttendance(it) }
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailedAttendanceCard(
    record: AttendanceRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_item_${record.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TradeIcon(trade = record.workerType)
                    Column {
                        Text(
                            text = record.workerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${record.workerId} • ${record.workerType}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(status = "Date: ${record.date}")
                    StatusBadge(status = record.siteCode)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(status = "AM: ${record.morningAttendance}")
                    StatusBadge(status = "PM: ${record.eveningAttendance}")
                    if (record.otHours > 0) {
                        StatusBadge(status = "OT: ${record.otHours}h")
                    }
                }
            }

            // Financial breakdown row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily: ${ExcelExportHelper.formatCurrency(record.dailyAmount)}", fontSize = 11.sp)
                    Text("OT: ${ExcelExportHelper.formatCurrency(record.otAmount)}", fontSize = 11.sp, color = OtBlue)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (record.advanceToday > 0) {
                        Text("Adv: ${ExcelExportHelper.formatCurrency(record.advanceToday)}", fontSize = 11.sp, color = AdvanceRed)
                    }
                    if (record.paidToday > 0) {
                        Text("Paid: ${ExcelExportHelper.formatCurrency(record.paidToday)}", fontSize = 11.sp, color = OtBlue)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Earned: ${ExcelExportHelper.formatCurrency(record.totalEarned)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = WagesGreenDark
                    )
                    Text(
                        text = "Bal: ${ExcelExportHelper.formatCurrency(record.balance)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ConstructionGoldDark
                    )
                }
            }

            if (record.remark.isNotBlank()) {
                Text(
                    text = "Remark: ${record.remark}",
                    fontSize = 11.sp,
                    color = Slate600,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        fontSize = if (isHeader) 11.sp else 12.sp,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        color = if (isHeader) Color.White else color,
        maxLines = 1
    )
}
