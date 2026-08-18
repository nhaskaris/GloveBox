package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_catalog")
data class VehicleCatalog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val make: String,
    val model: String
)