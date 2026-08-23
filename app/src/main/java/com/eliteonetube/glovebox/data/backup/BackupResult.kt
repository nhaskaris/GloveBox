package com.eliteonetube.glovebox.data.backup

sealed class BackupResult {
    data object Success : BackupResult()
    data class Error(val message: String) : BackupResult()
}