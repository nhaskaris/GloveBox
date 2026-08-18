package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int,
    val trim: String? = null,
    val vin: String? = null,
    val nickname: String? = null,
    val licensePlate: String? = null,
    val color: String? = null,
    val fuelType: String? = null,
    val odometer: Int,
    val odometerUnit: String = "mi",
    val photoUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)