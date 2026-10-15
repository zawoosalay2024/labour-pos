package com.example.ui.screens.attendance

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendancePreset
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceEntryPosScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val form by viewModel.posForm.collectAsState()
    val activeWorkers by viewModel.activeWorkers.collectAsState()
    val sites by viewModel.allSites.collectAsState()
    val markedWorkerIds by viewModel.markedWorkerIdsForSelectedDate.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    var showSiteDropdown by remember { mutableStateOf(false) }
    var showWorkerDialog by remember { mutableStateOf(false) }
    var workerSearchQuery by remember { mutableStateOf("") }
    var filterUnmarkedOnly by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val datePickerDialog = remember {
        val parts = form.selectedDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.selectDate(formatted)
        }, y, m, d)
    }

    val isDuplicate = form.selectedWorker != null &&
            form.editingRecordId == null &&
            markedWorkerIds.contains(form.selectedWorker?.workerId)

    val canSubmit = form.selectedWorker != null && !isDuplicate

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_attendance_form_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (form.editingRecordId != null) {
                            if (currentLanguage == AppLanguage.MYANMAR) "တက်ရောက်မှု ပြင်ဆင်ရန်" else "Edit Daily Attendance"
                        } else {
                            if (currentLanguage == AppLanguage.MYANMAR) "နေ့စဉ်တက်ရောက်မှု စာရင်းသွင်းပုံစံ" else "Daily Attendance Form"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.MYANMAR) "အချိန်နှင့်တပြေးညီ လုပ်အားခ၊ OT နှင့် ကျန်ငွေတွက်ချက်မှု" else "Real-time wage, OT & balance calculation",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (form.editingRecordId != null) {
                    FilledTonalButton(
                        onClick = { viewModel.resetForm() },
                        modifier = Modifier.testTag("cancel_edit_mode_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (currentLanguage == AppLanguage.MYANMAR) "အသစ်စရန်" else "New Form")
                    }
                }
            }
        }

        // Error / Alert Banner
        if (form.errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_error_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdvanceRedLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AdvanceRed))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(24.dp))
                        Text(
                            text = form.errorMessage ?: "",
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearFormMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AdvanceRed)
                        }
                    }
                }
            }
        }

        // Success Banner
        if (form.successMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_success_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WagesGreenLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WagesGreenDark, modifier = Modifier.size(24.dp))
                        Text(
                            text = form.successMessage ?: "",
                            color = WagesGreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearFormMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = WagesGreenDark)
                        }
                    }
                }
            }
        }

        // 1. SITE & DATE SELECTION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("site_date_selection_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_1"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            color = PrimaryContainerLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "${sites.size} ဆိုဒ်ရှိသည်" else "${sites.size} Sites",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Site Selection Dropdown Field
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSiteDropdown = true }
                                .testTag("site_dropdown_selector"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(OtBlueLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Apartment, contentDescription = null, tint = OtBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "ဆောက်လုပ်ရေးဆိုဒ် ရွေးချယ်ပါ *" else "Select Construction Site *",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = form.selectedSite?.siteName ?: (if (currentLanguage == AppLanguage.MYANMAR) "ဆိုဒ်ရွေးချယ်ပါ" else "Select Site"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (form.selectedSite != null) {
                                            Text(
                                                text = "${form.selectedSite?.siteCode} • Supervisor: ${form.selectedSite?.supervisor}",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open Sites")
                            }
                        }

                        DropdownMenu(
                            expanded = showSiteDropdown,
                            onDismissRequest = { showSiteDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(site.siteName, fontWeight = FontWeight.Bold)
                                            Text("${site.siteCode} • ${site.location} • Sup: ${site.supervisor}", fontSize = 11.sp, color = Slate500)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, tint = OtBlue) },
                                    onClick = {
                                        viewModel.selectSite(site)
                                        showSiteDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Selector with Quick Day Nav
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    val parsed = dateFormat.parse(form.selectedDate) ?: Date()
                                    val cal = Calendar.getInstance().apply { time = parsed }
                                    cal.add(Calendar.DAY_OF_MONTH, -1)
                                    viewModel.selectDate(dateFormat.format(cal.time))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.testTag("btn_prev_date")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { datePickerDialog.show() }
                                .testTag("btn_date_picker"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ConstructionGoldDark, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "တက်ရောက်မှု နေ့စွဲ" else "Attendance Date",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(form.selectedDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Text(
                                    text = if (form.selectedDate == viewModel.todayDateString) {
                                        if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့" else "TODAY"
                                    } else {
                                        if (currentLanguage == AppLanguage.MYANMAR) "ရွေးရန်" else "PICK"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryLight
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val parsed = dateFormat.parse(form.selectedDate) ?: Date()
                                    val cal = Calendar.getInstance().apply { time = parsed }
                                    cal.add(Calendar.DAY_OF_MONTH, 1)
                                    viewModel.selectDate(dateFormat.format(cal.time))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.testTag("btn_next_date")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                        }
                    }
                }
            }
        }

        // 2. WORKER SELECTION CARD (WITH DUPLICATE VALIDATION)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("worker_selection_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_2"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.MYANMAR) "${markedWorkerIds.size} ဦး စာရင်းသွင်းပြီး" else "${markedWorkerIds.size} logged today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WagesGreenDark
                        )
                    }

                    // Worker Selector Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                workerSearchQuery = ""
                                showWorkerDialog = true
                            }
                            .testTag("worker_dialog_trigger"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (form.selectedWorker == null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else PrimaryContainerLight.copy(alpha = 0.35f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (form.selectedWorker != null) ConstructionGold else Slate300
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (form.selectedWorker == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ConstructionGold.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = ConstructionGoldDark, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားရွေးချယ်ပါ *" else "Tap to Select Worker *",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့စာရင်းသွင်းပြီးသူများကို သီးခြားခွဲထားပေးပါသည်" else "Filtered to prevent duplicate logging on this date",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TradeIcon(trade = form.selectedWorker?.workerType ?: "")
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = form.selectedWorker?.name ?: "",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                            StatusBadge(status = LanguageManager.getString(form.selectedWorker?.workerType ?: ""))
                                        }
                                        Text(
                                            text = "ID: ${form.selectedWorker?.workerId} • Daily: ${ExcelExportHelper.formatCurrency(form.dailyRate)} • OT: ${ExcelExportHelper.formatCurrency(form.otRate)}/h",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                }
                                TextButton(onClick = { showWorkerDialog = true }) {
                                    Text(if (currentLanguage == AppLanguage.MYANMAR) "ပြောင်းမည်" else "Change")
                                }
                            }
                        }
                    }

                    // Duplicate Alert Banner
                    if (isDuplicate) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("duplicate_worker_alert"),
                            shape = RoundedCornerShape(8.dp),
                            color = AdvanceRedLight
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(20.dp))
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR) {
                                        "သတိပေးချက်- ${form.selectedWorker?.name} သည် ${form.selectedDate} အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!"
                                    } else {
                                        "DUPLICATE PREVENTED: ${form.selectedWorker?.name} already has attendance logged for ${form.selectedDate}!"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdvanceRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. MORNING & EVENING ATTENDANCE SHIFT TOGGLES
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shift_attendance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_3"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            color = ConstructionGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "စုစုပေါင်း: ${form.totalDayUnits} ရက်" else "Total Units: ${form.totalDayUnits}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConstructionGoldDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Quick 1-Tap Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = LanguageManager.getString("preset_full_day"),
                            isSelected = form.morningAttendance == "P" && form.eveningAttendance == "P",
                            onClick = { viewModel.applyPreset(AttendancePreset.FULL_DAY) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = LanguageManager.getString("preset_half_am"),
                            isSelected = form.morningAttendance == "P" && form.eveningAttendance == "A",
                            onClick = { viewModel.applyPreset(AttendancePreset.HALF_DAY_AM) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = LanguageManager.getString("preset_half_pm"),
                            isSelected = form.morningAttendance == "A" && form.eveningAttendance == "P",
                            onClick = { viewModel.applyPreset(AttendancePreset.HALF_DAY_PM) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = LanguageManager.getString("preset_absent"),
                            isSelected = form.morningAttendance == "A" && form.eveningAttendance == "A",
                            onClick = { viewModel.applyPreset(AttendancePreset.ABSENT) },
                            modifier = Modifier.weight(1f),
                            selectedBg = AdvanceRed
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Morning Shift
                    Text(LanguageManager.getString("morning_shift"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosSegmentButton(
                            label = LanguageManager.getString("present_btn"),
                            isSelected = form.morningAttendance == "P",
                            onClick = { viewModel.setMorningAttendance("P") },
                            modifier = Modifier.weight(1f),
                            activeColor = WagesGreenDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("half_btn"),
                            isSelected = form.morningAttendance == "HD",
                            onClick = { viewModel.setMorningAttendance("HD") },
                            modifier = Modifier.weight(1f),
                            activeColor = ConstructionGoldDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("absent_btn"),
                            isSelected = form.morningAttendance == "A",
                            onClick = { viewModel.setMorningAttendance("A") },
                            modifier = Modifier.weight(1f),
                            activeColor = AdvanceRed
                        )
                    }

                    // Evening Shift
                    Text(LanguageManager.getString("evening_shift"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosSegmentButton(
                            label = LanguageManager.getString("present_btn"),
                            isSelected = form.eveningAttendance == "P",
                            onClick = { viewModel.setEveningAttendance("P") },
                            modifier = Modifier.weight(1f),
                            activeColor = WagesGreenDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("half_btn"),
                            isSelected = form.eveningAttendance == "HD",
                            onClick = { viewModel.setEveningAttendance("HD") },
                            modifier = Modifier.weight(1f),
                            activeColor = ConstructionGoldDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("absent_btn"),
                            isSelected = form.eveningAttendance == "A",
                            onClick = { viewModel.setEveningAttendance("A") },
                            modifier = Modifier.weight(1f),
                            activeColor = AdvanceRed
                        )
                    }
                }
            }
        }

        // 4. OVERTIME (OT) & WAGES
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("ot_wages_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_4"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.MYANMAR) "OT ရငွေ: ${ExcelExportHelper.formatCurrency(form.otAmount)}" else "OT Amount: ${ExcelExportHelper.formatCurrency(form.otAmount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OtBlue
                        )
                    }

                    // OT Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(-1.0) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("-1h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(-0.5) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("-½", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedTextField(
                            value = if (form.otHours == 0.0) "" else form.otHours.toString(),
                            onValueChange = { str ->
                                val num = str.toDoubleOrNull() ?: 0.0
                                viewModel.setOtHours(num)
                            },
                            modifier = Modifier.weight(1f).testTag("input_ot_hours"),
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "OT နာရီ" else "OT Hours") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(0.5) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+½", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(1.0) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+1h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(2.0) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+2h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Rates row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (form.dailyRate == 0.0) "" else form.dailyRate.toInt().toString(),
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.setDailyRate(it) }
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "နေ့တွက်ခ (ကျပ်)" else "Daily Rate (Ks)") },
                            modifier = Modifier.weight(1f).testTag("input_daily_rate"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = if (form.otRate == 0.0) "" else form.otRate.toInt().toString(),
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.setOtRate(it) }
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "OT ကြေး (ကျပ်/နာရီ)" else "OT Rate (Ks/hr)") },
                            modifier = Modifier.weight(1f).testTag("input_ot_rate"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // 5. ADVANCE & PAID TODAY
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("advance_paid_inputs_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = LanguageManager.getString("section_5"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConstructionGoldDark,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (form.advanceToday == 0.0) "" else form.advanceToday.toInt().toString(),
                            onValueChange = { str ->
                                viewModel.setAdvanceToday(str.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့ကြိုထုတ်ငွေ (ကျပ်)" else "Advance Today (Ks)") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("input_advance_today"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = if (form.paidToday == 0.0) "" else form.paidToday.toInt().toString(),
                            onValueChange = { str ->
                                viewModel.setPaidToday(str.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့ပေးငွေ (ကျပ်)" else "Paid Today (Ks)") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("input_paid_today"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Payment shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.setAdvanceToday(5000.0) },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "+၅,၀၀၀" else "+5,000 Ks") },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setAdvanceToday(10000.0) },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "+၁၀,၀၀၀" else "+10,000 Ks") },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setPaidToday(form.totalEarned) },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အပြည့်ရှင်း (${ExcelExportHelper.formatCurrency(form.totalEarned)})" else "Pay Full") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = form.remark,
                        onValueChange = { viewModel.setRemark(it) },
                        label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "လုပ်ငန်းမှတ်ချက်" else "Work Remark / Notes") },
                        placeholder = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ဥပမာ- အမိုးလောင်းခြင်း၊ အုတ်စီခြင်း" else "e.g. Slab casting, Column shuttering") },
                        modifier = Modifier.fillMaxWidth().testTag("input_remark"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 6. REAL-TIME CALCULATION SUMMARY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("real_time_wage_calculation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WagesGreen))
                            Text(
                                text = LanguageManager.getString("section_6"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConstructionGold,
                                letterSpacing = 1.sp
                            )
                        }
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "အလိုအလျောက်တွက်သည်" else "Auto Computed",
                                fontSize = 10.sp,
                                color = Slate300,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Divider(color = Slate700)

                    CalculationLine(
                        label = "${LanguageManager.getString("calc_daily_wage")} (${form.totalDayUnits} × ${ExcelExportHelper.formatCurrency(form.dailyRate)})",
                        value = ExcelExportHelper.formatCurrency(form.dailyAmount),
                        color = Color.White
                    )

                    CalculationLine(
                        label = "${LanguageManager.getString("calc_ot_wage")} (${form.otHours}h × ${ExcelExportHelper.formatCurrency(form.otRate)})",
                        value = ExcelExportHelper.formatCurrency(form.otAmount),
                        color = OtBlueLight
                    )

                    CalculationLine(
                        label = LanguageManager.getString("calc_total_earned"),
                        value = ExcelExportHelper.formatCurrency(form.totalEarned),
                        color = WagesGreen,
                        isBold = true
                    )

                    if (form.advanceToday > 0) {
                        CalculationLine(
                            label = LanguageManager.getString("calc_less_adv"),
                            value = "- " + ExcelExportHelper.formatCurrency(form.advanceToday),
                            color = AdvanceRedLight
                        )
                    }

                    if (form.paidToday > 0) {
                        CalculationLine(
                            label = LanguageManager.getString("calc_less_paid"),
                            value = "- " + ExcelExportHelper.formatCurrency(form.paidToday),
                            color = Slate300
                        )
                    }

                    Divider(color = Slate700)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LanguageManager.getString("calc_balance_due"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConstructionGold
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "= ရငွေ - ကြိုထုတ်ငွေ - ပေးငွေ" else "= Total Earned - Advance - Paid",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                        Text(
                            text = ExcelExportHelper.formatCurrency(form.balance),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (form.balance > 0) ConstructionGold else WagesGreen
                        )
                    }
                }
            }
        }

        // 7. ACTION BUTTONS
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.saveAttendance(advanceToNextWorker = true) },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_save_and_next"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Slate900, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageManager.getString("save_and_next"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.saveAttendance(advanceToNextWorker = false) },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("btn_save_record"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("save_only"), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetForm() },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("btn_reset_form"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("reset"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // WORKER SELECTION MODAL DIALOG
    if (showWorkerDialog) {
        AlertDialog(
            onDismissRequest = { showWorkerDialog = false },
            title = {
                Column {
                    Text(
                        text = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားရွေးချယ်ပါ" else "Select Worker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text("Date: ${form.selectedDate} (${markedWorkerIds.size} logged)", fontSize = 12.sp, color = Slate500)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                ) {
                    OutlinedTextField(
                        value = workerSearchQuery,
                        onValueChange = { workerSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("worker_search_input"),
                        placeholder = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အမည်၊ ID၊ ကျွမ်းကျင်မှုဖြင့် ရှာဖွေပါ..." else "Search worker name, ID, trade...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Filter chips: All vs Unmarked Only
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val unmarkedCount = activeWorkers.count { !markedWorkerIds.contains(it.workerId) }
                        FilterChip(
                            selected = !filterUnmarkedOnly,
                            onClick = { filterUnmarkedOnly = false },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အားလုံး (${activeWorkers.size})" else "All (${activeWorkers.size})") },
                            modifier = Modifier.testTag("filter_all_workers")
                        )
                        FilterChip(
                            selected = filterUnmarkedOnly,
                            onClick = { filterUnmarkedOnly = true },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "မသွင်းရသေးသူများ ($unmarkedCount)" else "Unmarked Only ($unmarkedCount)") },
                            modifier = Modifier.testTag("filter_unmarked_workers"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ConstructionGold,
                                selectedLabelColor = Slate900
                            )
                        )
                    }

                    val filteredWorkers = activeWorkers.filter { worker ->
                        val matchesSearch = worker.name.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.workerId.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.workerType.contains(workerSearchQuery, ignoreCase = true)
                        val matchesUnmarked = !filterUnmarkedOnly || !markedWorkerIds.contains(worker.workerId) || (form.editingRecordId != null && form.selectedWorker?.workerId == worker.workerId)
                        matchesSearch && matchesUnmarked
                    }

                    if (filteredWorkers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) {
                                    if (filterUnmarkedOnly) "ယနေ့အတွက် အားလုံး စာရင်းသွင်းပြီးဖြစ်ပါသည်!" else "လုပ်သား မတွေ့ရှိပါ"
                                } else {
                                    if (filterUnmarkedOnly) "All workers are already marked for today!" else "No matching workers found"
                                },
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredWorkers) { worker ->
                                val isMarked = markedWorkerIds.contains(worker.workerId)
                                val isCurrentEditing = form.editingRecordId != null && form.selectedWorker?.workerId == worker.workerId
                                val isSelectable = !isMarked || isCurrentEditing

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isSelectable) {
                                            viewModel.selectWorker(worker)
                                            showWorkerDialog = false
                                        }
                                        .testTag("worker_select_item_${worker.workerId}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        !isSelectable -> Slate200.copy(alpha = 0.6f)
                                        form.selectedWorker?.workerId == worker.workerId -> ConstructionGold.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    },
                                    border = if (form.selectedWorker?.workerId == worker.workerId) {
                                        CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ConstructionGold))
                                    } else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TradeIcon(trade = worker.workerType)
                                            Column {
                                                Text(
                                                    text = worker.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelectable) MaterialTheme.colorScheme.onSurface else Slate500
                                                )
                                                Text(
                                                    "${worker.workerId} • ${LanguageManager.getString(worker.workerType)} • ${ExcelExportHelper.formatCurrency(worker.dailyRate)}/ရက်",
                                                    fontSize = 11.sp,
                                                    color = if (isSelectable) Slate600 else Slate400
                                                )
                                            }
                                        }

                                        if (!isSelectable) {
                                            Surface(
                                                color = AdvanceRed.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        text = if (currentLanguage == AppLanguage.MYANMAR) "စာရင်းပြီး" else "DISABLED",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                color = WagesGreenLight,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (currentLanguage == AppLanguage.MYANMAR) "ရွေးချယ်နိုင်" else "SELECT",
                                                    color = WagesGreenDark,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkerDialog = false }) {
                    Text(if (currentLanguage == AppLanguage.MYANMAR) "ပိတ်မည်" else "Close")
                }
            }
        )
    }
}

@Composable
fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedBg: Color = ConstructionGold
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("preset_chip_${label.replace("\n", "_").lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedBg else Slate100,
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Slate900 else Slate700,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun PosSegmentButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = ConstructionGold
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("pos_segment_${label.take(2).trim()}"),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else Slate100,
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Slate700,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CalculationLine(
    label: String,
    value: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Slate300,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isBold) 15.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
