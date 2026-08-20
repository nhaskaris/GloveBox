package com.eliteonetube.glovebox.data.dao

import androidx.room.*
import com.eliteonetube.glovebox.data.entity.ProspectVehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface ProspectVehicleDao {
    @Query("SELECT * FROM prospect_vehicles ORDER BY createdAt DESC")
    fun getAllProspects(): Flow<List<ProspectVehicle>>

    @Query("SELECT * FROM prospect_vehicles WHERE id = :id")
    suspend fun getProspectById(id: Long): ProspectVehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProspect(prospect: ProspectVehicle): Long

    @Update
    suspend fun updateProspect(prospect: ProspectVehicle)

    @Delete
    suspend fun deleteProspect(prospect: ProspectVehicle)
}
