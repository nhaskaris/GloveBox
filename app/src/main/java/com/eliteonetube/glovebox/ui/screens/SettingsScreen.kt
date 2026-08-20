package com.eliteonetube.glovebox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.ThemePreference
import com.eliteonetube.glovebox.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: MainViewModel = viewModel()
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isVinEnabled by viewModel.isVinFeatureEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isDriveEnabled by viewModel.isDriveBackupEnabled.collectAsStateWithLifecycle()
    val lastBackup by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.performBackup()
        }
    }

    LaunchedEffect(backupStatus) {
        if (backupStatus != null) {
            // Show snackbar or toast
            android.widget.Toast.makeText(context, backupStatus, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearBackupStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_drawer))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = { Text(stringResource(R.string.choose_theme)) },
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) }
            )

            Column(
                modifier = Modifier.padding(start = 56.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    text = stringResource(R.string.system_default),
                    selected = themePreference == ThemePreference.SYSTEM,
                    onClick = { viewModel.setThemePreference(ThemePreference.SYSTEM) }
                )
                ThemeOption(
                    text = stringResource(R.string.light),
                    selected = themePreference == ThemePreference.LIGHT,
                    onClick = { viewModel.setThemePreference(ThemePreference.LIGHT) }
                )
                ThemeOption(
                    text = stringResource(R.string.dark),
                    selected = themePreference == ThemePreference.DARK,
                    onClick = { viewModel.setThemePreference(ThemePreference.DARK) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.select_language)) },
                leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) }
            )

            Column(
                modifier = Modifier.padding(start = 56.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageOption(
                    text = stringResource(R.string.system_language),
                    selected = appLanguage == null,
                    onClick = { viewModel.setAppLanguage(null) }
                )
                LanguageOption(
                    text = stringResource(R.string.language_english),
                    selected = appLanguage == "en",
                    onClick = { viewModel.setAppLanguage("en") }
                )
                LanguageOption(
                    text = stringResource(R.string.language_greek),
                    selected = appLanguage == "el",
                    onClick = { viewModel.setAppLanguage("el") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.vehicle_tools)) },
                supportingContent = { Text(stringResource(R.string.manage_special_features)) },
                leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 40.dp)) {
                    Text(stringResource(R.string.vin_decoding), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.vin_decoding_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isVinEnabled,
                    onCheckedChange = { viewModel.setVinFeatureEnabled(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.cloud_backup)) },
                supportingContent = { Text(stringResource(R.string.google_drive_backup_desc)) },
                leadingContent = { Icon(Icons.Rounded.CloudUpload, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isDriveEnabled,
                        onCheckedChange = { viewModel.setDriveBackupEnabled(it) }
                    )
                }
            )

            if (isDriveEnabled) {
                ListItem(
                    headlineContent = {
                        val timeText = if (lastBackup != null) {
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                            stringResource(R.string.last_backup, sdf.format(java.util.Date(lastBackup!!)))
                        } else {
                            stringResource(R.string.never_backed_up)
                        }
                        Text(timeText)
                    },
                    trailingContent = {
                        TextButton(onClick = { 
                            launcher.launch(viewModel.getBackupSignInIntent())
                        }) {
                            Text(stringResource(R.string.backup_now))
                        }
                    },
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ThemeOption(text, selected, onClick)
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
