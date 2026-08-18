package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long, // Epoch millis
    val mileage: Int,
    val serviceType: String,
    val cost: Double,
    val notes: String
)
