package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.LabourBackupData
import com.example.data.model.Site
import com.example.data.model.Worker
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    fun serializeToJson(backup: LabourBackupData): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("appName", backup.appName)
        root.put("timestamp", backup.timestamp)
        root.put("exportedDate", backup.exportedDate)

        // Sites
        val sitesArray = JSONArray()
        for (site in backup.sites) {
            val obj = JSONObject()
            obj.put("siteCode", site.siteCode)
            obj.put("siteName", site.siteName)
            obj.put("location", site.location)
            obj.put("supervisor", site.supervisor)
            obj.put("status", site.status)
            sitesArray.put(obj)
        }
        root.put("sites", sitesArray)

        // Workers
        val workersArray = JSONArray()
        for (worker in backup.workers) {
            val obj = JSONObject()
            obj.put("workerId", worker.workerId)
            obj.put("name", worker.name)
            obj.put("gender", worker.gender)
            obj.put("workerType", worker.workerType)
            obj.put("dailyRate", worker.dailyRate)
            obj.put("otRate", worker.otRate)
            obj.put("status", worker.status)
            obj.put("phone", worker.phone)
            obj.put("joinedDate", worker.joinedDate)
            workersArray.put(obj)
        }
        root.put("workers", workersArray)

        // Attendance
        val attendanceArray = JSONArray()
        for (rec in backup.attendance) {
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("date", rec.date)
            obj.put("siteCode", rec.siteCode)
            obj.put("siteName", rec.siteName)
            obj.put("workerId", rec.workerId)
            obj.put("workerName", rec.workerName)
            obj.put("workerType", rec.workerType)
            obj.put("morningAttendance", rec.morningAttendance)
            obj.put("eveningAttendance", rec.eveningAttendance)
            obj.put("dailyRate", rec.dailyRate)
            obj.put("otHours", rec.otHours)
            obj.put("otRate", rec.otRate)
            obj.put("advanceToday", rec.advanceToday)
            obj.put("paidToday", rec.paidToday)
            obj.put("remark", rec.remark)
            obj.put("createdAt", rec.createdAt)
            attendanceArray.put(obj)
        }
        root.put("attendance", attendanceArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): LabourBackupData {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val appName = root.optString("appName", "LabourMaster")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())
        val exportedDate = root.optString("exportedDate", "")

        val sites = mutableListOf<Site>()
        val sitesArray = root.optJSONArray("sites")
        if (sitesArray != null) {
            for (i in 0 until sitesArray.length()) {
                val obj = sitesArray.getJSONObject(i)
                sites.add(
                    Site(
                        siteCode = obj.optString("siteCode", "S-01"),
                        siteName = obj.optString("siteName", "Site"),
                        location = obj.optString("location", ""),
                        supervisor = obj.optString("supervisor", ""),
                        status = obj.optString("status", "Ongoing")
                    )
                )
            }
        }

        val workers = mutableListOf<Worker>()
        val workersArray = root.optJSONArray("workers")
        if (workersArray != null) {
            for (i in 0 until workersArray.length()) {
                val obj = workersArray.getJSONObject(i)
                workers.add(
                    Worker(
                        workerId = obj.optString("workerId", "W-100"),
                        name = obj.optString("name", "Worker"),
                        gender = obj.optString("gender", "Male"),
                        workerType = obj.optString("workerType", "Helper"),
                        dailyRate = obj.optDouble("dailyRate", 20000.0),
                        otRate = obj.optDouble("otRate", 3000.0),
                        status = obj.optString("status", "Active"),
                        phone = obj.optString("phone", ""),
                        joinedDate = obj.optString("joinedDate", "")
                    )
                )
            }
        }

        val attendance = mutableListOf<AttendanceRecord>()
        val attendanceArray = root.optJSONArray("attendance")
        if (attendanceArray != null) {
            for (i in 0 until attendanceArray.length()) {
                val obj = attendanceArray.getJSONObject(i)
                attendance.add(
                    AttendanceRecord(
                        id = 0, // Auto-increment or let database generate cleanly
                        date = obj.optString("date", "2026-01-01"),
                        siteCode = obj.optString("siteCode", ""),
                        siteName = obj.optString("siteName", ""),
                        workerId = obj.optString("workerId", ""),
                        workerName = obj.optString("workerName", ""),
                        workerType = obj.optString("workerType", "Helper"),
                        morningAttendance = obj.optString("morningAttendance", "P"),
                        eveningAttendance = obj.optString("eveningAttendance", "P"),
                        dailyRate = obj.optDouble("dailyRate", 20000.0),
                        otHours = obj.optDouble("otHours", 0.0),
                        otRate = obj.optDouble("otRate", 3000.0),
                        advanceToday = obj.optDouble("advanceToday", 0.0),
                        paidToday = obj.optDouble("paidToday", 0.0),
                        remark = obj.optString("remark", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return LabourBackupData(
            version = version,
            appName = appName,
            timestamp = timestamp,
            exportedDate = exportedDate,
            sites = sites,
            workers = workers,
            attendance = attendance
        )
    }

    fun createBackupFile(context: Context, backup: LabourBackupData): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LabourMaster_Backup_$timeStamp.json"
        val file = File(exportDir, fileName)

        val json = serializeToJson(backup)
        FileOutputStream(file).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }

        // Also save a copy to persistent internal files directory
        val internalDir = File(context.filesDir, "backups")
        if (!internalDir.exists()) internalDir.mkdirs()
        val internalFile = File(internalDir, fileName)
        FileOutputStream(internalFile).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }

        return file
    }

    fun shareBackupToDriveOrApps(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LabourMaster Backup - ${file.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "LabourMaster Construction Database Backup File. Save this to Google Drive or your secure storage for easy restore."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Backup to Google Drive / Share File")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): LabourBackupData? {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            parseFromJson(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
