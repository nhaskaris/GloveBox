package com.eliteonetube.glovebox.data.backup

import android.content.Context
import android.net.Uri
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.GloveboxDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.database.sqlite.SQLiteDatabase
import com.eliteonetube.glovebox.data.DATABASE_VERSION
import com.eliteonetube.glovebox.util.NotificationHelper
import com.eliteonetube.glovebox.util.WidgetHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LocalBackupManager(private val context: Context) {

    suspend fun exportDatabase(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Force WAL Checkpoint to ensure main DB file is up to date
            try {
                val db = GloveboxDatabase.getDatabase(context)
                db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            } catch (e: Exception) {
                android.util.Log.e("Backup", "Checkpoint failed", e)
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext BackupResult.Error(context.getString(R.string.backup_err_db_missing))
            
            val fileSize = dbFile.length()
            if (fileSize == 0L) return@withContext BackupResult.Error(context.getString(R.string.backup_err_db_empty))

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    if (bytesCopied == 0L) return@withContext BackupResult.Error(context.getString(R.string.backup_err_no_data))
                }
            } ?: return@withContext BackupResult.Error(context.getString(R.string.backup_err_out_stream))

            BackupResult.Success
        } catch (e: Exception) {
            android.util.Log.e("Backup", "Export failed", e)
            BackupResult.Error(e.message ?: context.getString(R.string.backup_err_unknown))
        }
    }

    suspend fun importDatabase(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val importTemp = File(dbFile.parentFile, "$DB_NAME.import")
        val rollbackTemp = File(dbFile.parentFile, "$DB_NAME.rollback")
        try {
            // 1. Copy the chosen file somewhere private and validate it BEFORE touching the live DB.
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(importTemp).use { output -> input.copyTo(output) }
            } ?: return@withContext BackupResult.Error(context.getString(R.string.backup_err_open_file))

            validateBackup(importTemp)?.let { return@withContext BackupResult.Error(it) }

            // 2. Snapshot the current DB (checkpointed so the main file is complete) for rollback.
            try {
                GloveboxDatabase.getDatabase(context).openHelper.writableDatabase
                    .execSQL("PRAGMA wal_checkpoint(FULL)")
            } catch (e: Exception) {
                android.util.Log.w("Backup", "Checkpoint before import failed", e)
            }
            // Alarms scheduled for the old data must go before it does.
            try {
                NotificationHelper.cancelAll(context)
            } catch (e: Exception) {
                android.util.Log.w("Backup", "Could not cancel old alarms", e)
            }
            if (dbFile.exists()) dbFile.copyTo(rollbackTemp, overwrite = true)

            try {
                // 3. Swap the files. Stale WAL/SHM files belong to the old database and would
                //    corrupt the imported one if left behind.
                GloveboxDatabase.resetDatabase()
                deleteSidecars(dbFile)
                importTemp.copyTo(dbFile, overwrite = true)

                // 4. Open it through Room; this runs migrations and fails if the schema is wrong.
                GloveboxDatabase.getDatabase(context).openHelper.writableDatabase
            } catch (e: Exception) {
                android.util.Log.e("Backup", "Import failed, rolling back", e)
                GloveboxDatabase.resetDatabase()
                deleteSidecars(dbFile)
                if (rollbackTemp.exists()) rollbackTemp.copyTo(dbFile, overwrite = true)
                throw e
            }

            // 5. Everything derived from the old data must be rebuilt.
            try {
                NotificationHelper.rescheduleAll(context)
            } catch (e: Exception) {
                android.util.Log.w("Backup", "Could not reschedule alarms after import", e)
            }
            WidgetHelper.updateAllWidgets(context)
            BackupResult.Success
        } catch (e: Exception) {
            android.util.Log.e("Backup", "Import failed", e)
            BackupResult.Error(e.message ?: context.getString(R.string.backup_err_unknown))
        } finally {
            importTemp.delete()
            rollbackTemp.delete()
        }
    }

    /** Returns a user-facing error message, or null if [file] looks like a usable backup. */
    private fun validateBackup(file: File): String? {
        if (file.length() < SQLITE_HEADER.size) return context.getString(R.string.backup_err_too_small)

        val header = ByteArray(SQLITE_HEADER.size)
        file.inputStream().use { it.read(header) }
        if (!header.contentEquals(SQLITE_HEADER)) return context.getString(R.string.backup_err_not_backup)

        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val hasVehicles = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='vehicles'", null
                ).use { it.moveToFirst() }
                when {
                    !hasVehicles -> context.getString(R.string.backup_err_not_backup)
                    db.version > DATABASE_VERSION ->
                        context.getString(R.string.backup_err_newer)
                    else -> null
                }
            }
        } catch (e: Exception) {
            context.getString(R.string.backup_err_corrupt)
        }
    }

    private fun deleteSidecars(dbFile: File) {
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }

    private companion object {
        const val DB_NAME = "glovebox_database"
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
