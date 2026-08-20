package com.eliteonetube.glovebox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prospect_vehicles")
data class ProspectVehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val vin: String? = null,
    val askedPrice: Double? = null,
    val sellerNotes: String = "",
    val location: String = "",
    val checklistJson: String = "{}", // Stores checked item keys as a JSON map/set
    val createdAt: Long = System.currentTimeMillis()
)
