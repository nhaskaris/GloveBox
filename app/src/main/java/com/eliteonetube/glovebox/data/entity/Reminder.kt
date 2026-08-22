package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val description: String,
    val targetMileage: Int?,
    val targetDate: Long?, // Epoch millis
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val intervalMileage: Int? = null,
    val intervalMonths: Int? = null,
    val lastCompletedMileage: Int? = null,
    val lastCompletedDate: Long? = null
)
