package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.data.repository.LabourRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.ExcelExportHelper

enum class AppScreen {
    DASHBOARD,
    POS_ENTRY,
    ATTENDANCE_LIST,
    WORKER_MASTER,
    SITE_MASTER,
    ADVANCE_LEDGER,
    EXCEL_EXPORT
}

enum class AttendancePreset {
    FULL_DAY,       // Morning P (0.5), Evening P (0.5) = 1.0
    HALF_DAY_AM,    // Morning P (0.5), Evening A (0.0) = 0.5
    HALF_DAY_PM,    // Morning A (0.0), Evening P (0.5) = 0.5
    QUARTER_DAY,    // Morning HD (0.25), Evening A (0.0) = 0.25
    ABSENT          // Morning A (0.0), Evening A (0.0) = 0.0
}

data class PosFormState(
    val selectedDate: String,
    val selectedSite: Site? = null,
    val selectedWorker: Worker? = null,
    val morningAttendance: String = "P",
    val eveningAttendance: String = "P",
    val dailyRate: Double = 20000.0,
    val otHours: Double = 0.0,
    val otRate: Double = 3000.0,
    val advanceToday: Double = 0.0,
    val paidToday: Double = 0.0,
    val remark: String = "",
    val editingRecordId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val morningValue: Double
        get() = when (morningAttendance.uppercase()) {
            "P" -> 0.5
            "HD" -> 0.25
            else -> 0.0
        }

    val eveningValue: Double
        get() = when (eveningAttendance.uppercase()) {
            "P" -> 0.5
            "HD" -> 0.25
            else -> 0.0
        }

    val totalDayUnits: Double
        get() = morningValue + eveningValue

    val dailyAmount: Double
        get() = totalDayUnits * dailyRate

    val otAmount: Double
        get() = otHours * otRate

    val totalEarned: Double
        get() = dailyAmount + otAmount

    val balance: Double
        get() = totalEarned - advanceToday - paidToday
}

class LabourViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LabourRepository

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = dateFormat.format(Date())

    private val _currentScreen = MutableStateFlow(AppScreen.POS_ENTRY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val allWorkers: StateFlow<List<Worker>>
    val activeWorkers: StateFlow<List<Worker>>
    val allSites: StateFlow<List<Site>>
    val activeSites: StateFlow<List<Site>>
    val allAttendance: StateFlow<List<AttendanceRecord>>

    // POS Form State
    private val _posForm = MutableStateFlow(PosFormState(selectedDate = todayDateString))
    val posForm: StateFlow<PosFormState> = _posForm.asStateFlow()

    // Filter states for reports & table view
    val filterSiteCode = MutableStateFlow<String?>(null)
    val filterWorkerId = MutableStateFlow<String?>(null)
    val filterDate = MutableStateFlow<String?>(todayDateString)

    // Dynamic marked worker IDs for the selected date in POS
    @OptIn(ExperimentalCoroutinesApi::class)
    val markedWorkerIdsForSelectedDate: StateFlow<List<String>>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = LabourRepository(
            database.workerDao(),
            database.siteDao(),
            database.attendanceDao()
        )

        allWorkers = repository.allWorkers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        activeWorkers = repository.activeWorkers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allSites = repository.allSites.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        activeSites = repository.activeSites.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allAttendance = repository.allAttendance.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        markedWorkerIdsForSelectedDate = _posForm.flatMapLatest { form ->
            repository.getMarkedWorkerIdsForDate(form.selectedDate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Pre-select first site once sites are available
        viewModelScope.launch {
            repository.activeSites.collect { sites ->
                if (_posForm.value.selectedSite == null && sites.isNotEmpty()) {
                    _posForm.value = _posForm.value.copy(selectedSite = sites.first())
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectDate(date: String) {
        _posForm.value = _posForm.value.copy(
            selectedDate = date,
            errorMessage = null
        )
        // Check if selected worker is already marked for this new date
        val currentWorker = _posForm.value.selectedWorker
        if (currentWorker != null && _posForm.value.editingRecordId == null) {
            viewModelScope.launch {
                val marked = repository.isWorkerMarkedForDate(date, currentWorker.workerId)
                if (marked) {
                    _posForm.value = _posForm.value.copy(
                        errorMessage = "Worker ${currentWorker.name} is already logged on $date!"
                    )
                }
            }
        }
    }

    fun selectSite(site: Site) {
        _posForm.value = _posForm.value.copy(selectedSite = site)
    }

    fun selectWorker(worker: Worker) {
        val form = _posForm.value
        viewModelScope.launch {
            val isMarked = repository.isWorkerMarkedForDate(form.selectedDate, worker.workerId)
            val error = if (isMarked && form.editingRecordId == null) {
                "Worker ${worker.name} is ALREADY marked on ${form.selectedDate}!"
            } else null

            _posForm.value = form.copy(
                selectedWorker = worker,
                dailyRate = worker.dailyRate,
                otRate = worker.otRate,
                errorMessage = error
            )
        }
    }

    fun setMorningAttendance(status: String) {
        _posForm.value = _posForm.value.copy(morningAttendance = status)
    }

    fun setEveningAttendance(status: String) {
        _posForm.value = _posForm.value.copy(eveningAttendance = status)
    }

    fun applyPreset(preset: AttendancePreset) {
        when (preset) {
            AttendancePreset.FULL_DAY -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "P", eveningAttendance = "P")
            }
            AttendancePreset.HALF_DAY_AM -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "P", eveningAttendance = "A")
            }
            AttendancePreset.HALF_DAY_PM -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "A", eveningAttendance = "P")
            }
            AttendancePreset.QUARTER_DAY -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "HD", eveningAttendance = "A")
            }
            AttendancePreset.ABSENT -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "A", eveningAttendance = "A")
            }
        }
    }

    fun setOtHours(hours: Double) {
        _posForm.value = _posForm.value.copy(otHours = hours.coerceAtLeast(0.0))
    }

    fun adjustOtHours(delta: Double) {
        val newOt = (_posForm.value.otHours + delta).coerceAtLeast(0.0)
        _posForm.value = _posForm.value.copy(otHours = newOt)
    }

    fun setDailyRate(rate: Double) {
        _posForm.value = _posForm.value.copy(dailyRate = rate.coerceAtLeast(0.0))
    }

    fun setOtRate(rate: Double) {
        _posForm.value = _posForm.value.copy(otRate = rate.coerceAtLeast(0.0))
    }

    fun setAdvanceToday(amount: Double) {
        _posForm.value = _posForm.value.copy(advanceToday = amount.coerceAtLeast(0.0))
    }

    fun setPaidToday(amount: Double) {
        _posForm.value = _posForm.value.copy(paidToday = amount.coerceAtLeast(0.0))
    }

    fun setRemark(text: String) {
        _posForm.value = _posForm.value.copy(remark = text)
    }

    fun clearFormMessage() {
        _posForm.value = _posForm.value.copy(errorMessage = null, successMessage = null)
    }

    fun resetForm() {
        val defaultSite = activeSites.value.firstOrNull() ?: allSites.value.firstOrNull()
        _posForm.value = PosFormState(
            selectedDate = _posForm.value.selectedDate,
            selectedSite = defaultSite ?: _posForm.value.selectedSite,
            selectedWorker = null,
            morningAttendance = "P",
            eveningAttendance = "P",
            otHours = 0.0,
            advanceToday = 0.0,
            paidToday = 0.0,
            remark = "",
            editingRecordId = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun editRecord(record: AttendanceRecord) {
        val worker = allWorkers.value.find { it.workerId == record.workerId }
            ?: Worker(
                workerId = record.workerId,
                name = record.workerName,
                workerType = record.workerType,
                dailyRate = record.dailyRate,
                otRate = record.otRate
            )
        val site = allSites.value.find { it.siteCode == record.siteCode }
            ?: Site(siteCode = record.siteCode, siteName = record.siteName)

        _posForm.value = PosFormState(
            selectedDate = record.date,
            selectedSite = site,
            selectedWorker = worker,
            morningAttendance = record.morningAttendance,
            eveningAttendance = record.eveningAttendance,
            dailyRate = record.dailyRate,
            otHours = record.otHours,
            otRate = record.otRate,
            advanceToday = record.advanceToday,
            paidToday = record.paidToday,
            remark = record.remark,
            editingRecordId = record.id,
            errorMessage = null,
            successMessage = "Editing record for ${record.workerName}"
        )
        _currentScreen.value = AppScreen.POS_ENTRY
    }

    /**
     * Saves attendance. If advanceToNext is true, prepares form for next unmarked worker.
     */
    fun saveAttendance(advanceToNextWorker: Boolean = false) {
        val form = _posForm.value
        val worker = form.selectedWorker
        val site = form.selectedSite
        val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR

        if (worker == null) {
            _posForm.value = form.copy(
                errorMessage = if (isMy) "ကျေးဇူးပြု၍ လုပ်သားကို အရင်ရွေးချယ်ပါ *" else "Please select a worker first."
            )
            return
        }

        if (site == null) {
            _posForm.value = form.copy(
                errorMessage = if (isMy) "ကျေးဇူးပြု၍ ဆောက်လုပ်ရေးဆိုဒ်ကို အရင်ရွေးချယ်ပါ *" else "Please select a site first."
            )
            return
        }

        viewModelScope.launch {
            // Check duplicate validation if not editing
            if (form.editingRecordId == null) {
                val alreadyMarked = repository.isWorkerMarkedForDate(form.selectedDate, worker.workerId)
                if (alreadyMarked) {
                    _posForm.value = form.copy(
                        errorMessage = if (isMy) {
                            "သတိပေးချက်- ${worker.name} (${worker.workerId}) သည် ${form.selectedDate} အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်! စာရင်းနှစ်ခါထပ်၍မရပါ။"
                        } else {
                            "ERROR: ${worker.name} (${worker.workerId}) is ALREADY recorded for ${form.selectedDate}. Duplicates are prohibited!"
                        }
                    )
                    return@launch
                }
            }

            val record = AttendanceRecord(
                id = form.editingRecordId ?: 0,
                date = form.selectedDate,
                siteCode = site.siteCode,
                siteName = site.siteName,
                workerId = worker.workerId,
                workerName = worker.name,
                workerType = worker.workerType,
                morningAttendance = form.morningAttendance,
                eveningAttendance = form.eveningAttendance,
                dailyRate = form.dailyRate,
                otHours = form.otHours,
                otRate = form.otRate,
                advanceToday = form.advanceToday,
                paidToday = form.paidToday,
                remark = form.remark
            )

            if (form.editingRecordId != null) {
                repository.updateAttendance(record)
            } else {
                repository.insertAttendance(record)
            }

            // Find next available active worker if rapid POS entry requested
            val markedIds = repository.getRecordForDateAndWorker(form.selectedDate, worker.workerId)?.let {
                markedWorkerIdsForSelectedDate.value + worker.workerId
            } ?: (markedWorkerIdsForSelectedDate.value + worker.workerId)

            val nextWorker = if (advanceToNextWorker) {
                activeWorkers.value.firstOrNull { it.workerId !in markedIds && it.workerId != worker.workerId }
            } else null

            _posForm.value = PosFormState(
                selectedDate = form.selectedDate,
                selectedSite = site,
                selectedWorker = nextWorker,
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = nextWorker?.dailyRate ?: 20000.0,
                otHours = 0.0,
                otRate = nextWorker?.otRate ?: 3000.0,
                advanceToday = 0.0,
                paidToday = 0.0,
                remark = "",
                editingRecordId = null,
                errorMessage = null,
                successMessage = if (nextWorker != null) {
                    if (isMy) {
                        "${worker.name} ၏ စာရင်းသွင်းပြီးပါပြီ! နောက်တစ်ဦး ${nextWorker.name} အတွက် အဆင်သင့်ဖြစ်ပါပြီ။"
                    } else {
                        "Saved ${worker.name}! Ready for ${nextWorker.name}."
                    }
                } else {
                    if (isMy) {
                        "${worker.name} ၏ စာရင်းသွင်းမှု အောင်မြင်ပါသည် (စုစုပေါင်းရငွေ: ${ExcelExportHelper.formatCurrency(record.totalEarned)})"
                    } else {
                        "Saved attendance for ${worker.name} successfully (Earned: ${ExcelExportHelper.formatCurrency(record.totalEarned)})."
                    }
                }
            )
        }
    }

    fun deleteAttendance(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.deleteAttendance(record)
            if (_posForm.value.editingRecordId == record.id) {
                resetForm()
            }
        }
    }

    // Worker Master actions
    fun addWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertWorker(worker)
            onComplete()
        }
    }

    fun updateWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorker(worker)
            onComplete()
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
        }
    }

    // Site Master actions
    fun addSite(site: Site, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertSite(site)
            onComplete()
        }
    }

    fun updateSite(site: Site, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSite(site)
            onComplete()
        }
    }

    fun deleteSite(site: Site) {
        viewModelScope.launch {
            repository.deleteSite(site)
        }
    }

    fun resetDataWithSamples() {
        viewModelScope.launch {
            repository.resetWithSampleData()
            _posForm.value = _posForm.value.copy(
                successMessage = "Sample construction data restored successfully."
            )
        }
    }
}
