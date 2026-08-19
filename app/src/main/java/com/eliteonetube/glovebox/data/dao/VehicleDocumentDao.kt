package com.eliteonetube.glovebox.data.dao

import androidx.room.*
import com.eliteonetube.glovebox.data.entity.VehicleDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDocumentDao {
    @Query("""
        SELECT * FROM vehicle_documents 
        WHERE (:vehicleId = 0 OR vehicleId = :vehicleId OR vehicleId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getDocumentsForVehicle(vehicleId: Long): Flow<List<VehicleDocument>>

    @Query("SELECT * FROM vehicle_documents WHERE vehicleId IS NULL ORDER BY createdAt DESC")
    fun getUniversalDocuments(): Flow<List<VehicleDocument>>

    @Query("SELECT * FROM vehicle_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): VehicleDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: VehicleDocument): Long

    @Update
    suspend fun updateDocument(document: VehicleDocument)

    @Delete
    suspend fun deleteDocument(document: VehicleDocument)
}
