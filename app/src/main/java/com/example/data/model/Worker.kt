package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey
    val workerId: String, // e.g. "W-101"
    val name: String,
    val gender: String = "Male", // "Male", "Female", "Other"
    val workerType: String = "Helper", // "Carpenter", "Mason", "Helper", "Electrician", "Plumber", "Painter", "Welder", "Bar Bender"
    val dailyRate: Double = 20000.0,
    val otRate: Double = 3000.0, // per hour
    val status: String = "Active", // "Active", "Inactive"
    val phone: String = "",
    val joinedDate: String = ""
)
