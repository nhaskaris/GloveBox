package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicle_documents",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class VehicleDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long? = null,
    val name: String,
    val category: String, // Insurance, Registration, Warranty, Other
    val photoUri: String,
    val expiryDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
