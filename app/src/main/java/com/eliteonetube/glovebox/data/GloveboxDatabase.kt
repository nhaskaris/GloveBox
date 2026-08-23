package com.eliteonetube.glovebox.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        // Ensure table exists before inserting. 
        // During destructive migration, Room calls this callback after dropping tables but before recreating them.
        db.execSQL("CREATE TABLE IF NOT EXISTS `vehicle_catalog` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `make` TEXT NOT NULL, `model` TEXT NOT NULL)")

        // Read JSON from assets
        val jsonString = try {
            context.assets.open("vehicles.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return
        
        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            return
        }

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
        } catch (e: Exception) {
            // Log or handle error if needed, but don't crash the app initialization
        } finally {
            db.endTransaction()
        }
    }
}

@Database(
    entities = [
        Vehicle::class, 
        ServiceRecord::class, 
        Reminder::class, 
        VehicleCatalog::class, 
        FuelLog::class, 
        VehicleDocument::class,
        ProspectVehicle::class,
        VehiclePart::class
    ],
    version = 15,
    exportSchema = false
)
abstract class GloveboxDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun reminderDao(): ReminderDao
    abstract fun vehicleCatalogDao(): VehicleCatalogDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun vehicleDocumentDao(): VehicleDocumentDao
    abstract fun prospectVehicleDao(): ProspectVehicleDao
    abstract fun vehiclePartDao(): VehiclePartDao

    companion object {
        @Volatile
        private var INSTANCE: GloveboxDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Currently no schema changes, just establishing formal migration path
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix: Ensure table is created with correct schema matching VehiclePart entity
                db.execSQL("DROP TABLE IF EXISTS `vehicle_parts`")
                db.execSQL("""
                    CREATE TABLE `vehicle_parts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `vehicleId` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `partNumber` TEXT NOT NULL, 
                        `brand` TEXT, 
                        `notes` TEXT, 
                        `lastUpdated` INTEGER NOT NULL, 
                        FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_vehicleId` ON `vehicle_parts` (`vehicleId`)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if photoUri column exists to prevent duplicate addition errors
                val cursor = db.query("PRAGMA table_info(`vehicle_parts`)", emptyArray())
                var hasPhotoUri = false
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (name == "photoUri") {
                        hasPhotoUri = true
                        break
                    }
                }
                cursor.close()

                if (!hasPhotoUri) {
                    db.execSQL("ALTER TABLE `vehicle_parts` ADD COLUMN `photoUri` TEXT DEFAULT NULL")
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `isRecurring` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `intervalMileage` INTEGER")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `intervalMonths` INTEGER")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `lastCompletedMileage` INTEGER")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `lastCompletedDate` INTEGER")
            }
        }

        fun getDatabase(context: Context): GloveboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GloveboxDatabase::class.java,
                    "glovebox_database"
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .addCallback(VehicleDatabaseCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun resetDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
