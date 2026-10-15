package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceRecord
import com.example.util.AppLanguage
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LabourMaster", appName)
    }

    @Test
    fun `verify attendance wage calculations in myanmar kyats`() {
        val record = AttendanceRecord(
            date = "2026-10-14",
            siteCode = "S-01",
            siteName = "Metro Tower Phase 2 (ရန်ကုန်)",
            workerId = "W-101",
            workerName = "Ko Rajesh (လက်သမား)",
            workerType = "Carpenter",
            morningAttendance = "P",   // 0.5
            eveningAttendance = "HD",  // 0.25 -> 0.75 day units
            dailyRate = 28000.0,       // daily wage: 0.75 * 28,000 = 21,000 Ks
            otHours = 2.0,             // ot rate: 4,000 -> ot amount: 8,000 Ks
            otRate = 4000.0,           // total earned: 21,000 + 8,000 = 29,000 Ks
            advanceToday = 5000.0,
            paidToday = 10000.0        // balance: 29,000 - 5,000 - 10,000 = 14,000 Ks
        )

        assertEquals(0.75, record.totalDayUnits, 0.001)
        assertEquals(21000.0, record.dailyAmount, 0.001)
        assertEquals(8000.0, record.otAmount, 0.001)
        assertEquals(29000.0, record.totalEarned, 0.001)
        assertEquals(14000.0, record.balance, 0.001)

        LanguageManager.setLanguage(AppLanguage.ENGLISH)
        assertEquals("29,000 Kyats", ExcelExportHelper.formatCurrency(record.totalEarned))

        LanguageManager.setLanguage(AppLanguage.MYANMAR)
        assertEquals("29,000 ကျပ်", ExcelExportHelper.formatCurrency(record.totalEarned))
    }

    @Test
    fun `verify export reports include date and multiple formats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val record = AttendanceRecord(
            date = "2026-10-14",
            siteCode = "S-01",
            siteName = "Metro Tower Phase 2",
            workerId = "W-101",
            workerName = "Ko Rajesh",
            workerType = "Carpenter",
            morningAttendance = "P",
            eveningAttendance = "P",
            dailyRate = 30000.0,
            otHours = 1.0,
            otRate = 4500.0,
            advanceToday = 5000.0,
            paidToday = 0.0
        )
        val records = listOf(record)

        // Test 1: CSV format includes Date period and record date
        val csv = ExcelExportHelper.generateAttendanceCsv(records, "2026-10-14", "Metro Tower Phase 2")
        assert(csv.contains("Date Period: 2026-10-14"))
        assert(csv.contains("2026-10-14"))
        assert(csv.contains("Ko Rajesh"))

        // Test 2: Excel XML format includes Date period and spreadsheet structure
        val excelXml = ExcelExportHelper.generateAttendanceExcelXml(records, "2026-10-14", "Metro Tower Phase 2")
        assert(excelXml.contains("urn:schemas-microsoft-com:office:spreadsheet"))
        assert(excelXml.contains("2026-10-14"))
        assert(excelXml.contains("Ko Rajesh"))

        // Test 3: PDF Generation produces non-empty file
        val pdfFile = ExcelExportHelper.generateAttendancePdf(context, records, "2026-10-14", "Metro Tower Phase 2")
        assert(pdfFile.exists())
        assert(pdfFile.length() > 0)
    }
}
