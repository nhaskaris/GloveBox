package com.eliteonetube.glovebox.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eliteonetube.glovebox.data.dao.*
import com.eliteonetube.glovebox.data.entity.*
import org.json.JSONArray

class VehicleDatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        populateCatalog(db)
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        populateCatalog(db)
    }

    private fun populateCatalog(db: SupportSQLiteDatabase) {
        // Read JSON from assets
        val jsonString = try {
            context.assets.open("vehicles.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return
        
        val jsonArray = JSONArray(jsonString)

        db.beginTransaction()
        try {
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val make = item.getString("make")
                val models = item.getJSONArray("models")

                for (j in 0 until models.length()) {
                    val cv = ContentValues().apply {
                        put("make", make)
                        put("model", models.getString(j))
                    }
                    // Insert directly into the SQLite database
                    db.insert("vehicle_catalog", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

@Database(
    entities = [Vehicle::class, ServiceRecord::class, Reminder::class, VehicleCatalog::class, FuelLog::class, VehicleDocument::class],
    version = 6,
    exportSchema = false
)
abstract class GloveboxDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun reminderDao(): ReminderDao
    abstract fun vehicleCatalogDao(): VehicleCatalogDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun vehicleDocumentDao(): VehicleDocumentDao

    companion object {
        @Volatile
        private var INSTANCE: GloveboxDatabase? = null

        fun getDatabase(context: Context): GloveboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GloveboxDatabase::class.java,
                    "glovebox_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(VehicleDatabaseCallback(context))
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
