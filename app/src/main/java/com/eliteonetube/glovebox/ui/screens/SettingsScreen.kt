package com.eliteonetube.glovebox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.util.Locale
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: MainViewModel = viewModel()
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isVinEnabled by viewModel.isVinFeatureEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val lastBackup by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(backupStatus) {
        if (backupStatus != null) {
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
                headlineContent = { Text("Measurement System") },
                supportingContent = { Text("Choose between Metric and Imperial") },
                leadingContent = { Icon(Icons.Rounded.Speed, contentDescription = null) }
            )

            val units = viewModel.unitSystem.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier.padding(start = 56.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    text = "Kilometers (km, L/100km)",
                    selected = units.value == "km",
                    onClick = { viewModel.setUnitSystem("km") }
                )
                ThemeOption(
                    text = "Miles (mi, MPG)",
                    selected = units.value == "mi",
                    onClick = { viewModel.setUnitSystem("mi") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Preferred Currency") },
                supportingContent = { Text("Used for fleet-wide cost summaries") },
                leadingContent = { Icon(Icons.Rounded.AttachMoney, contentDescription = null) }
            )

            val prefCurrency by viewModel.preferredCurrency.collectAsStateWithLifecycle()
            var showCurrencySheet by remember { mutableStateOf(false) }
            val currencies = com.eliteonetube.glovebox.util.CurrencyUtility.supportedCurrencies

            Box(modifier = Modifier.padding(start = 56.dp)) {
                Box {
                    OutlinedTextField(
                        value = "$prefCurrency (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(prefCurrency)})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCurrencySheet = true }
                    )
                }
            }

            if (showCurrencySheet) {
                ModalBottomSheet(onDismissRequest = { showCurrencySheet = false }) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(currencies) { code ->
                            DropdownMenuItem(
                                text = { Text("$code (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(code)})") },
                                onClick = {
                                    viewModel.setPreferredCurrency(code)
                                    showCurrencySheet = false
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_region)) },
                leadingContent = { Icon(Icons.Rounded.Public, contentDescription = null) }
            )

            val countryState = viewModel.userCountry.collectAsStateWithLifecycle()
            var showRegionSheet by remember { mutableStateOf(false) }
            var regionQuery by remember { mutableStateOf("") }

            val regions = remember(appLanguage) {
                val displayLocale = appLanguage?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
                val isoCountries = Locale.getISOCountries()
                val countryList = isoCountries.map { code ->
                    code to Locale.Builder().setRegion(code).build().getDisplayCountry(displayLocale)
                }.sortedBy { it.second }

                listOf("Global" to "Global / Other") + countryList
            }

            val currentRegionLabel = regions.find { it.first == countryState.value }?.second ?: countryState.value

            Box(modifier = Modifier.padding(start = 56.dp)) {
                Box {
                    OutlinedTextField(
                        value = currentRegionLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showRegionSheet = true }
                    )
                }
            }

            if (showRegionSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showRegionSheet = false
                        regionQuery = ""
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = regionQuery,
                            onValueChange = { regionQuery = it },
                            placeholder = { Text("Search country...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        val filtered = remember(regions, regionQuery) {
                            if (regionQuery.isBlank()) regions
                            else regions.filter { it.second.contains(regionQuery, ignoreCase = true) }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 500.dp)
                        ) {
                            items(filtered, key = { it.first }) { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setUserCountry(code)
                                        regionQuery = ""
                                        showRegionSheet = false
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
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
                headlineContent = { Text(stringResource(R.string.local_backup)) },
                supportingContent = { Text(stringResource(R.string.local_backup_desc)) },
                leadingContent = { Icon(Icons.Rounded.Backup, contentDescription = null) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch("glovebox_backup_${System.currentTimeMillis()}.db") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_now))
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.restore_now))
                }
            }

            if (lastBackup != null) {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                Text(
                    text = stringResource(R.string.last_backup, sdf.format(java.util.Date(lastBackup!!))),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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