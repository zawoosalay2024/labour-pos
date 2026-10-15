package com.example.data.local

import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SampleDataProvider {

    fun getInitialWorkers(): List<Worker> {
        return listOf(
            Worker("W-101", "Ko Rajesh (လက်သမား)", "Male", "Carpenter", 28000.0, 4000.0, "Active", "+95 9789 12345"),
            Worker("W-102", "U Amit (ပန်းရံခေါင်းဆောင်)", "Male", "Mason", 30000.0, 4500.0, "Active", "+95 9789 12346"),
            Worker("W-103", "Ko Sunil (အကူ)", "Male", "Helper", 18000.0, 2500.0, "Active", "+95 9789 12347"),
            Worker("W-104", "Ko Ramesh (အကူ)", "Male", "Helper", 18000.0, 2500.0, "Active", "+95 9789 12348"),
            Worker("W-105", "U Dinesh (လက်သမား)", "Male", "Carpenter", 28000.0, 4000.0, "Active", "+95 9789 12349"),
            Worker("W-106", "Daw Pooja (အကူ)", "Female", "Helper", 18000.0, 2500.0, "Active", "+95 9789 12350"),
            Worker("W-107", "Daw Kiran (အကူ)", "Female", "Helper", 18000.0, 2500.0, "Active", "+95 9789 12351"),
            Worker("W-108", "U Aslam (လျှပ်စစ်ကျွမ်းကျင်)", "Male", "Electrician", 35000.0, 5000.0, "Active", "+95 9789 12352"),
            Worker("W-109", "Ko Vikram (ပန်းရံ)", "Male", "Mason", 30000.0, 4500.0, "Active", "+95 9789 12353"),
            Worker("W-110", "Ko Sanjay (ဆေးသုတ်)", "Male", "Painter", 25000.0, 3500.0, "Active", "+95 9789 12354"),
            Worker("W-111", "Ko Deepak (ရေပိုက်)", "Male", "Plumber", 30000.0, 4500.0, "Active", "+95 9789 12355"),
            Worker("W-112", "U Manoj (သံချည်သံကွေး)", "Male", "Bar Bender", 28000.0, 4000.0, "Inactive", "+95 9789 12356")
        )
    }

    fun getInitialSites(): List<Site> {
        return listOf(
            Site("S-01", "Metro Tower Phase 2 (ရန်ကုန်)", "Expressway, Sector 62", "U John Miller", "Ongoing"),
            Site("S-02", "Green Valley Luxury Villas", "North Dagon Plot 14", "U Rajesh Kumar", "Ongoing"),
            Site("S-03", "City Center Commercial Hub", "Pyay Road Junction", "Ko Vikram", "Ongoing"),
            Site("S-04", "Sunrise Residential Towers", "Hlaing Hillview", "U Sunil", "On Hold")
        )
    }

    fun getInitialAttendance(): List<AttendanceRecord> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val today = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgo = dateFormat.format(calendar.time)

        return listOf(
            // Today's records (S-01)
            AttendanceRecord(
                date = today,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-101",
                workerName = "Ko Rajesh (လက်သမား)",
                workerType = "Carpenter",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 28000.0,
                otHours = 2.0,
                otRate = 4000.0,
                advanceToday = 5000.0,
                paidToday = 0.0,
                remark = "Roof formwork framing (ခေါင်မိုးပုံစံ)"
            ),
            AttendanceRecord(
                date = today,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-102",
                workerName = "U Amit (ပန်းရံခေါင်းဆောင်)",
                workerType = "Mason",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 30000.0,
                otHours = 1.5,
                otRate = 4500.0,
                advanceToday = 0.0,
                paidToday = 20000.0,
                remark = "Main entrance brickwork (အုတ်စီခြင်း)"
            ),
            AttendanceRecord(
                date = today,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-103",
                workerName = "Ko Sunil (အကူ)",
                workerType = "Helper",
                morningAttendance = "P",
                eveningAttendance = "HD",
                dailyRate = 18000.0,
                otHours = 0.0,
                otRate = 2500.0,
                advanceToday = 3000.0,
                paidToday = 0.0,
                remark = "Mortar mixing & carrying (သရွတ်ဖျော်)"
            ),
            AttendanceRecord(
                date = today,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-106",
                workerName = "Daw Pooja (အကူ)",
                workerType = "Helper",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 18000.0,
                otHours = 1.0,
                otRate = 2500.0,
                advanceToday = 0.0,
                paidToday = 0.0,
                remark = "Scaffolding shifting (ငြမ်းရွှေ့ခြင်း)"
            ),
            // Yesterday's records
            AttendanceRecord(
                date = yesterday,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-101",
                workerName = "Ko Rajesh (လက်သမား)",
                workerType = "Carpenter",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 28000.0,
                otHours = 3.0,
                otRate = 4000.0,
                advanceToday = 0.0,
                paidToday = 28000.0,
                remark = "Column shuttering (တိုင်ပုံစံခတ်)"
            ),
            AttendanceRecord(
                date = yesterday,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-102",
                workerName = "U Amit (ပန်းရံခေါင်းဆောင်)",
                workerType = "Mason",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 30000.0,
                otHours = 2.0,
                otRate = 4500.0,
                advanceToday = 10000.0,
                paidToday = 0.0,
                remark = "Plaster work corridor (အင်္ဂတေချော)"
            ),
            AttendanceRecord(
                date = yesterday,
                siteCode = "S-02",
                siteName = "Green Valley Luxury Villas",
                workerId = "W-108",
                workerName = "U Aslam (လျှပ်စစ်ကျွမ်းကျင်)",
                workerType = "Electrician",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 35000.0,
                otHours = 2.0,
                otRate = 5000.0,
                advanceToday = 15000.0,
                paidToday = 0.0,
                remark = "Conduit wiring Villa 4 (ဝါယာလိုင်းသွင်း)"
            ),
            AttendanceRecord(
                date = yesterday,
                siteCode = "S-02",
                siteName = "Green Valley Luxury Villas",
                workerId = "W-104",
                workerName = "Ko Ramesh (အကူ)",
                workerType = "Helper",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 18000.0,
                otHours = 0.0,
                otRate = 2500.0,
                advanceToday = 0.0,
                paidToday = 18000.0,
                remark = "Chasing trench work"
            ),
            // 2 Days ago records
            AttendanceRecord(
                date = twoDaysAgo,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-101",
                workerName = "Ko Rajesh (လက်သမား)",
                workerType = "Carpenter",
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = 28000.0,
                otHours = 2.0,
                otRate = 4000.0,
                advanceToday = 5000.0,
                paidToday = 0.0,
                remark = "Beam framing"
            ),
            AttendanceRecord(
                date = twoDaysAgo,
                siteCode = "S-01",
                siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
                workerId = "W-109",
                workerName = "Ko Vikram (ပန်းရံ)",
                workerType = "Mason",
                morningAttendance = "P",
                eveningAttendance = "HD",
                dailyRate = 30000.0,
                otHours = 0.0,
                otRate = 4500.0,
                advanceToday = 0.0,
                paidToday = 20000.0,
                remark = "Boundary wall (ခြံစည်းရိုးအုတ်တံတိုင်း)"
            )
        )
    }
}
