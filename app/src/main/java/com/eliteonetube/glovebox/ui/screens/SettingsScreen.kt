package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text("Choose how the app looks") },
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) }
            )

            Column(
                modifier = Modifier.padding(start = 56.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    text = "System Default",
                    selected = themePreference == ThemePreference.SYSTEM,
                    onClick = { viewModel.setThemePreference(ThemePreference.SYSTEM) }
                )
                ThemeOption(
                    text = "Light",
                    selected = themePreference == ThemePreference.LIGHT,
                    onClick = { viewModel.setThemePreference(ThemePreference.LIGHT) }
                )
                ThemeOption(
                    text = "Dark",
                    selected = themePreference == ThemePreference.DARK,
                    onClick = { viewModel.setThemePreference(ThemePreference.DARK) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Vehicle Tools") },
                supportingContent = { Text("Manage special features") },
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
                    Text("VIN Decoding", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Automatically fill car details from VIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isVinEnabled,
                    onCheckedChange = { viewModel.setVinFeatureEnabled(it) }
                )
            }
        }
    }
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
