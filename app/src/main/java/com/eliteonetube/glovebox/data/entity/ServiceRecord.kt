package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_records",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val mileage: Int,
    val serviceType: String,
    val otherDescription: String? = null,
    val serviceLocation: String? = null, // "DIY" or shop name
    val cost: Double? = null,
    val currency: String = "USD",
    val partsUsed: String? = null,
    val laborHours: Double? = null,
    val notes: String = "",
    val receiptPhotoUri: String? = null,
    val nextDueDate: Long? = null,
    val nextDueMileage: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)