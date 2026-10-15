package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    MYANMAR("my", "Burmese", "မြန်မာ")
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.MYANMAR)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.ENGLISH) {
            AppLanguage.MYANMAR
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun getString(key: String): String {
        val isMy = _currentLanguage.value == AppLanguage.MYANMAR
        return when (key) {
            "app_title" -> if (isMy) "လုပ်အားစီမံ" else "LabourMaster"
            "dashboard_subtitle" -> if (isMy) "ဆိုဒ်နှင့် လုပ်အားခ စီမံခန့်ခွဲမှု" else "Site & Wages Dashboard"
            "pos_subtitle" -> if (isMy) "နေ့စဉ် အမြန်စာရင်းသွင်း" else "POS Attendance Entry"
            "log_subtitle" -> if (isMy) "တက်ရောက်မှု မှတ်တမ်း" else "Daily Attendance Log"
            "worker_subtitle" -> if (isMy) "လုပ်သား မှတ်ပုံတင်စာရင်း" else "Worker Master Directory"
            "site_subtitle" -> if (isMy) "ဆောက်လုပ်ရေးဆိုဒ် စာရင်း" else "Construction Site Master"
            "ledger_subtitle" -> if (isMy) "ကြိုထုတ်ငွေနှင့် လက်ကျန်စာရင်း" else "Advance & Wage Ledger"
            "export_subtitle" -> if (isMy) "အစီရင်ခံစာ (PDF, Excel, CSV) ထုတ်ယူရန်" else "Report (PDF, Excel, CSV) Export"

            // Bottom Nav
            "nav_home" -> if (isMy) "ပင်မ" else "Home"
            "nav_pos" -> if (isMy) "အမြန်သွင်း" else "POS Entry"
            "nav_log" -> if (isMy) "မှတ်တမ်း" else "Log"
            "nav_ledger" -> if (isMy) "ငွေစာရင်း" else "Ledger"
            "nav_excel" -> if (isMy) "အစီရင်ခံစာ" else "Report"

            // POS Form Headers
            "section_1" -> if (isMy) "၁။ ဆိုဒ်နှင့် နေ့စွဲ ရွေးချယ်ခြင်း" else "1. CONSTRUCTION SITE & DATE"
            "section_2" -> if (isMy) "၂။ လုပ်သား ရွေးချယ်ခြင်း" else "2. WORKER SELECTION"
            "section_3" -> if (isMy) "၃။ အလုပ်ချိန် တက်ရောက်မှု" else "3. SHIFT ATTENDANCE"
            "section_4" -> if (isMy) "၄။ အချိန်ပို (OT) နာရီနှင့် နှုန်းထား" else "4. OVERTIME (OT) & RATES"
            "section_5" -> if (isMy) "၅။ ကြိုထုတ်ငွေ၊ ပေးငွေနှင့် မှတ်ချက်" else "5. ADVANCES, PAYMENTS & REMARK"
            "section_6" -> if (isMy) "အချိန်နှင့်တပြေးညီ တွက်ချက်မှု" else "REAL-TIME CALCULATION"

            // Presets
            "preset_full_day" -> if (isMy) "တစ်နေ့လုံး\n(၁.၀)" else "Full Day\n(1.0)"
            "preset_half_am" -> if (isMy) "မနက်ဝက်\n(၀.၅)" else "AM Half\n(0.5)"
            "preset_half_pm" -> if (isMy) "ညနေဝက်\n(၀.၅)" else "PM Half\n(0.5)"
            "preset_absent" -> if (isMy) "ပျက်ကွက်\n(၀.၀)" else "Absent\n(0.0)"

            // Shift labels
            "morning_shift" -> if (isMy) "နံနက်ပိုင်း- P (၀.၅) • HD (၀.၂၅) • A (၀)" else "Morning Shift: P (0.5) • HD (0.25) • A (0.0)"
            "evening_shift" -> if (isMy) "ညနေပိုင်း- P (၀.၅) • HD (၀.၂၅) • A (၀)" else "Evening Shift: P (0.5) • HD (0.25) • A (0.0)"
            "present_btn" -> if (isMy) "P - တက်ရောက် (၀.၅)" else "P - Present (0.5)"
            "half_btn" -> if (isMy) "HD - တစ်ဝက် (၀.၂၅)" else "HD - Half Day (0.25)"
            "absent_btn" -> if (isMy) "A - ပျက်ကွက် (၀)" else "A - Absent (0)"

            // Calculation Lines
            "calc_daily_wage" -> if (isMy) "နေ့တွက်လုပ်အားခ" else "Daily Wages"
            "calc_ot_wage" -> if (isMy) "အချိန်ပိုလုပ်အားခ" else "OT Wages"
            "calc_total_earned" -> if (isMy) "စုစုပေါင်းရငွေ (နေ့တွက်ခ + OT)" else "Total Earned (Daily Wages + OT)"
            "calc_less_adv" -> if (isMy) "နုတ် - ယနေ့ကြိုထုတ်ငွေ (-)" else "Less Advance Today (-)"
            "calc_less_paid" -> if (isMy) "နုတ် - ယနေ့ပေးငွေ (-)" else "Less Paid Today (-)"
            "calc_balance_due" -> if (isMy) "ပေးရန်ကျန်ငွေ" else "REMAINING BALANCE DUE"

            // Actions
            "save_and_next" -> if (isMy) "သိမ်းဆည်းပြီး နောက်တစ်ဦး" else "SAVE & NEXT WORKER"
            "save_only" -> if (isMy) "သိမ်းဆည်းမည်" else "Save Only"
            "reset" -> if (isMy) "အသစ်ပြန်စ" else "Reset"
            "duplicate_warning" -> if (isMy) "သတိပေးချက်- ဤလုပ်သားသည် ယနေ့အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!" else "DUPLICATE PREVENTED: Worker is already marked for this date!"

            // Trades in Myanmar
            "Carpenter" -> if (isMy) "လက်သမား" else "Carpenter"
            "Mason" -> if (isMy) "ပန်းရံ" else "Mason"
            "Helper" -> if (isMy) "အကူ/အလုပ်ကြမ်း" else "Helper"
            "Electrician" -> if (isMy) "လျှပ်စစ်ကျွမ်းကျင်" else "Electrician"
            "Plumber" -> if (isMy) "ရေပိုက်ကျွမ်းကျင်" else "Plumber"
            "Painter" -> if (isMy) "ဆေးသုတ်" else "Painter"
            "Welder" -> if (isMy) "ဝဟ်ဆော်" else "Welder"
            "Bar Bender" -> if (isMy) "သံချည်သံကွေး" else "Bar Bender"
            "Foreman" -> if (isMy) "ဖိုမင်/ခေါင်းဆောင်" else "Foreman"

            // Statuses
            "Active" -> if (isMy) "လက်ရှိ" else "Active"
            "Inactive" -> if (isMy) "ရပ်နား" else "Inactive"
            "Ongoing" -> if (isMy) "လုပ်ဆောင်ဆဲ" else "Ongoing"
            "Completed" -> if (isMy) "ပြီးစီး" else "Completed"
            "On Hold" -> if (isMy) "ဆိုင်းငံ့" else "On Hold"

            // Report and Export strings
            "export_title" -> if (isMy) "အစီရင်ခံစာ ထုတ်ယူခြင်းနှင့် တိုက်ရိုက်ကြည့်ရှုခြင်း" else "Report Export & Direct Viewer"
            "export_description" -> if (isMy) "ရက်စွဲအလိုက် စိစစ်ပြီး PDF, Excel (XLSX) နှင့် CSV ပုံစံများဖြင့် တိုက်ရိုက်ကြည့်ရှုခြင်း သို့မဟုတ် ထုတ်ယူမျှဝေခြင်း ပြုလုပ်နိုင်ပါသည်။" else "Filter by date and export or directly view reports in PDF, Excel (XLSX), and CSV formats."
            "filter_date_heading" -> if (isMy) "ရက်စွဲ ရွေးချယ်မှု (Date Filter)" else "Filter by Date"
            "date_all" -> if (isMy) "ရက်စွဲအားလုံး" else "All Dates"
            "date_today" -> if (isMy) "ယနေ့" else "Today"
            "date_custom_day" -> if (isMy) "သတ်မှတ်ရက်စွဲ" else "Specific Date"
            "date_range" -> if (isMy) "ရက်စွဲအပိုင်းအခြား" else "Date Range"
            "from_date" -> if (isMy) "စတင်ရက်" else "From Date"
            "to_date" -> if (isMy) "ပြီးဆုံးရက်" else "To Date"
            "format_pdf" -> if (isMy) "PDF ဖိုင် (.pdf)" else "PDF Report (.pdf)"
            "format_excel" -> if (isMy) "Excel ဖိုင် (.xlsx)" else "Excel (.xlsx)"
            "format_csv" -> if (isMy) "CSV ဖိုင် (.csv)" else "CSV File (.csv)"
            "action_open_direct" -> if (isMy) "တိုက်ရိုက် ဖွင့်ကြည့်မည်" else "OPEN & VIEW DIRECTLY"
            "action_share" -> if (isMy) "မျှဝေမည်" else "Share Report"
            "action_copy" -> if (isMy) "ကူးယူမည်" else "Copy CSV"
            "view_table" -> if (isMy) "ဇယားပုံစံ တိုက်ရိုက်ကြည့်ရန်" else "Interactive Table View"
            "view_raw" -> if (isMy) "ဖိုင်စာသား နမူနာ" else "Raw File Preview"
            "report_date" -> if (isMy) "ထုတ်သည့်ရက်စွဲ" else "Report Date"
            "period_label" -> if (isMy) "ရက်စွဲကာလ" else "Date Period"
            "records_found" -> if (isMy) "မှတ်တမ်းပေါင်း" else "Total Records"
            "total_mandays" -> if (isMy) "လူအင်အားရက်" else "Man-Days"
            "total_earned_mmk" -> if (isMy) "စုစုပေါင်းရငွေ (ကျပ်)" else "Total Earned (MMK)"
            "total_advance_mmk" -> if (isMy) "ကြိုထုတ်ငွေ (ကျပ်)" else "Advance (MMK)"
            "total_paid_mmk" -> if (isMy) "ပေးငွေ (ကျပ်)" else "Paid Today (MMK)"
            "net_balance_mmk" -> if (isMy) "လက်ကျန်ကျပ်ငွေ" else "Balance Due (MMK)"

            // Defaults
            else -> key
        }
    }
}
