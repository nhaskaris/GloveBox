package com.eliteonetube.glovebox.data.backup

import android.content.Context
import android.net.Uri
import com.eliteonetube.glovebox.data.GloveboxDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

            val dbFile = context.getDatabasePath("glovebox_database")
            if (!dbFile.exists()) return@withContext BackupResult.Error("Database file not found")
            
            val fileSize = dbFile.length()
            if (fileSize == 0L) return@withContext BackupResult.Error("Database file is empty")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    if (bytesCopied == 0L) return@withContext BackupResult.Error("No data was copied during export")
                }
            } ?: return@withContext BackupResult.Error("Could not open output stream")

            BackupResult.Success
        } catch (e: Exception) {
            android.util.Log.e("Backup", "Export failed", e)
            BackupResult.Error(e.message ?: "Unknown export error")
        }
    }

    suspend fun importDatabase(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // First, validate the source file is not empty
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            val sourceSize = pfd?.statSize ?: 0L
            pfd?.close()

            if (sourceSize <= 0) {
                return@withContext BackupResult.Error("The selected backup file is empty and cannot be imported.")
            }

            val dbFile = context.getDatabasePath("glovebox_database")
            
            // Backup current DB to a temp file just in case
            val backupTemp = java.io.File(dbFile.absolutePath + ".tmp")
            if (dbFile.exists()) {
                dbFile.copyTo(backupTemp, overwrite = true)
            }

            try {
                // Close and clear the database instance before replacing the file
                GloveboxDatabase.resetDatabase()

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        val bytesCopied = inputStream.copyTo(outputStream)
                        if (bytesCopied <= 0) throw Exception("Import failed: No data copied")
                    }
                } ?: throw Exception("Could not open input stream")

                // Re-open/initialize the database to ensure it's valid
                GloveboxDatabase.getDatabase(context).openHelper.writableDatabase
                
                // If we got here, it succeeded. Delete the temp backup.
                backupTemp.delete()
                BackupResult.Success
            } catch (e: Exception) {
                // Restore from temp if something went wrong
                if (backupTemp.exists()) {
                    backupTemp.copyTo(dbFile, overwrite = true)
                    backupTemp.delete()
                }
                throw e
            }
        } catch (e: Exception) {
            android.util.Log.e("Backup", "Import failed", e)
            BackupResult.Error(e.message ?: "Unknown import error")
        }
    }
}