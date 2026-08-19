package com.eliteonetube.glovebox.data.dao

import androidx.room.*
import com.eliteonetube.glovebox.data.entity.FuelLog
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs ORDER BY date DESC")
    fun getAllFuelLogs(): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelLogsForVehicle(vehicleId: Long): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE id = :id")
    suspend fun getFuelLogById(id: Long): FuelLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(log: FuelLog): Long

    @Update
    suspend fun updateFuelLog(log: FuelLog)

    @Delete
    suspend fun deleteFuelLog(log: FuelLog)
}
