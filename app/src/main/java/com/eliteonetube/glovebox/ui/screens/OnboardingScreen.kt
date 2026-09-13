package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                    .navigationBarsPadding()
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
    var showSheet by remember { mutableStateOf(false) }
    val currencies = com.eliteonetube.glovebox.util.CurrencyUtility.supportedCurrencies
    val currentLabel = "$preferredCurrency (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(preferredCurrency)})"

    Box {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.width(240.dp),
            shape = MaterialTheme.shapes.medium
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true }
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(currencies) { code ->
                    DropdownMenuItem(
                        text = { Text("$code (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(code)})") },
                        onClick = {
                            onCurrencyChange(code)
                            showSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
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
    var showSheet by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    
    val regions = remember(currentLanguage) {
        val displayLocale = currentLanguage?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
        val isoCountries = Locale.getISOCountries()
        val countryList = isoCountries.map { code ->
            code to Locale.Builder().setRegion(code).build().getDisplayCountry(displayLocale)
        }.sortedBy { it.second }
        
        listOf("Global" to "Global / Other") + countryList
    }

    val currentLabel = regions.find { it.first == userCountry }?.second ?: userCountry

    Box {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_region)) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.width(240.dp),
            shape = MaterialTheme.shapes.medium
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true }
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { 
            showSheet = false
            query = ""
        }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search country...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                val filtered = remember(regions, query) {
                    if (query.isBlank()) regions
                    else regions.filter { it.second.contains(query, ignoreCase = true) }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(filtered, key = { it.first }) { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onCountryChange(code)
                                showSheet = false
                                query = ""
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelector(
    unitSystem: String,
    onUnitChange: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val units = listOf(
        "km" to "Kilometers (km, L/100km)",
        "mi" to "Miles (mi, MPG)"
    )

    val currentLabel = units.find { it.first == unitSystem }?.second ?: units[0].second

    Box {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Measurement System") },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.width(240.dp),
            shape = MaterialTheme.shapes.medium
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true }
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                units.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onUnitChange(code)
                            showSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    currentLanguage: String?,
    onLanguageChange: (String?) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val languages = listOf(
        null to stringResource(R.string.system_language),
        "en" to stringResource(R.string.language_english),
        "el" to stringResource(R.string.language_greek),
        "de" to stringResource(R.string.language_german),
        "es" to stringResource(R.string.language_spanish),
        "fr" to stringResource(R.string.language_french),
        "it" to stringResource(R.string.language_italian)
    )

    val currentLabel = languages.find { it.first == currentLanguage }?.second ?: stringResource(R.string.system_language)

    Box {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.select_language)) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.width(240.dp),
            shape = MaterialTheme.shapes.medium
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true }
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(languages) { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onLanguageChange(code)
                            showSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val showSettings: Boolean = false
)
