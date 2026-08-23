package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eliteonetube.glovebox.R
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    currentLanguage: String?,
    onLanguageChange: (String?) -> Unit,
    userCountry: String,
    onCountryChange: (String) -> Unit,
    onComplete: () -> Unit,
    unitSystem: String,
    onUnitChange: (String) -> Unit,
    preferredCurrency: String,
    onCurrencyChange: (String) -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.welcome_title),
            description = stringResource(R.string.welcome_desc),
            icon = Icons.Rounded.DirectionsCar,
            showSettings = true
        ),
        OnboardingPage(
            title = stringResource(R.string.history_title),
            description = stringResource(R.string.history_desc),
            icon = Icons.Rounded.History
        ),
        OnboardingPage(
            title = stringResource(R.string.glovebox_intro_title),
            description = stringResource(R.string.glovebox_intro_desc),
            icon = Icons.Rounded.Folder
        ),
        OnboardingPage(
            title = stringResource(R.string.vin_title),
            description = stringResource(R.string.vin_desc),
            icon = Icons.Rounded.AutoFixHigh
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = MaterialTheme.shapes.small,
                            color = color
                        ) {}
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text(if (pagerState.currentPage == pages.size - 1) stringResource(R.string.get_started) else stringResource(R.string.next))
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(48.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (page.showSettings) {
                    Spacer(Modifier.height(32.dp))
                    LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange
                    )
                    Spacer(Modifier.height(16.dp))
                    UnitSelector(
                        unitSystem = unitSystem,
                        onUnitChange = onUnitChange
                    )
                    Spacer(Modifier.height(16.dp))
                    RegionSelector(
                        userCountry = userCountry,
                        onCountryChange = onCountryChange,
                        currentLanguage = currentLanguage
                    )
                    Spacer(Modifier.height(16.dp))
                    CurrencySelector(
                        preferredCurrency = preferredCurrency,
                        onCurrencyChange = onCurrencyChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(
    preferredCurrency: String,
    onCurrencyChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = com.eliteonetube.glovebox.util.CurrencyUtility.supportedCurrencies
    
    val currentLabel = "$preferredCurrency (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(preferredCurrency)})"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .width(240.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { code ->
                DropdownMenuItem(
                    text = { Text("$code (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(code)})") },
                    onClick = {
                        onCurrencyChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelector(
    userCountry: String,
    onCountryChange: (String) -> Unit,
    currentLanguage: String?
) {
    var expanded by remember { mutableStateOf(false) }
    
    val regions = remember(currentLanguage) {
        val displayLocale = currentLanguage?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
        val isoCountries = Locale.getISOCountries()
        val countryList = isoCountries.map { code ->
            code to Locale.Builder().setRegion(code).build().getDisplayCountry(displayLocale)
        }.sortedBy { it.second }
        
        listOf("Global" to "Global / Other") + countryList
    }

    val currentLabel = regions.find { it.first == userCountry }?.second ?: userCountry

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_region)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .width(240.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            regions.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onCountryChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelector(
    unitSystem: String,
    onUnitChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val units = listOf(
        "km" to "Kilometers (km, L/100km)",
        "mi" to "Miles (mi, MPG)"
    )

    val currentLabel = units.find { it.first == unitSystem }?.second ?: units[0].second

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Measurement System") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .width(240.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onUnitChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    currentLanguage: String?,
    onLanguageChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf(
        null to stringResource(R.string.system_language),
        "en" to stringResource(R.string.language_english),
        "el" to stringResource(R.string.language_greek)
    )

    val currentLabel = languages.find { it.first == currentLanguage }?.second ?: stringResource(R.string.system_language)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.select_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .width(240.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onLanguageChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val showSettings: Boolean = false
)
