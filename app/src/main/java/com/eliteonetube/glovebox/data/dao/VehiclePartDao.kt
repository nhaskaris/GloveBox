package com.eliteonetube.glovebox.data.dao

import androidx.room.*
import com.eliteonetube.glovebox.data.entity.VehiclePart
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiclePartDao {
    @Query("SELECT * FROM vehicle_parts WHERE vehicleId = :vehicleId ORDER BY name ASC")
    fun getPartsForVehicle(vehicleId: Long): Flow<List<VehiclePart>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: VehiclePart): Long

    @Update
    suspend fun updatePart(part: VehiclePart)

    @Delete
    suspend fun deletePart(part: VehiclePart)
}
