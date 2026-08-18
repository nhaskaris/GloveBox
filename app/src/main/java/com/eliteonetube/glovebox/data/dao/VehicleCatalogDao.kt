package com.eliteonetube.glovebox.data.dao

import androidx.room.*
import com.eliteonetube.glovebox.data.entity.VehicleCatalog
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleCatalogDao {
    @Query("SELECT * FROM vehicle_catalog")
    fun getAllVehicles(): Flow<List<VehicleCatalog>>

    @Query("SELECT DISTINCT make FROM vehicle_catalog ORDER BY make ASC")
    fun getMakes(): Flow<List<String>>

    @Query("SELECT model FROM vehicle_catalog WHERE make = :make ORDER BY model ASC")
    fun getModelsForMake(make: String): Flow<List<String>>
}