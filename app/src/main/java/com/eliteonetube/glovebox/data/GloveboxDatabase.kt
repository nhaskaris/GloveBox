package com.eliteonetube.glovebox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eliteonetube.glovebox.data.dao.ReminderDao
import com.eliteonetube.glovebox.data.dao.ServiceRecordDao
import com.eliteonetube.glovebox.data.dao.VehicleDao
import com.eliteonetube.glovebox.data.entity.Reminder
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.data.entity.Vehicle

@Database(
    entities = [Vehicle::class, ServiceRecord::class, Reminder::class],
    version = 1,
    exportSchema = false
)
abstract class GloveboxDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: GloveboxDatabase? = null

        fun getDatabase(context: Context): GloveboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GloveboxDatabase::class.java,
                    "glovebox_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
