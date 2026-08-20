package com.eliteonetube.glovebox.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class GoogleDriveBackupManager(private val context: Context) {

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Glovebox")
            .build()
    }

    suspend fun performBackup(): Boolean = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext false
        val driveService = getDriveService(account)

        try {
            val dbFile = context.getDatabasePath("glovebox_database")
            if (!dbFile.exists()) return@withContext false

            val metadata = File().apply {
                name = "glovebox_backup.db"
                parents = listOf("appDataFolder")
            }

            val content = FileContent("application/x-sqlite3", dbFile)
            
            // Check if file already exists in appDataFolder
            val existingFiles = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'glovebox_backup.db'")
                .execute()

            if (existingFiles.files.isNotEmpty()) {
                val fileId = existingFiles.files[0].id
                driveService.files().update(fileId, null, content).execute()
            } else {
                driveService.files().create(metadata, content).execute()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
