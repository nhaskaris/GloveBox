package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.ui.viewmodels.FuelLogFormViewModel
import com.eliteonetube.glovebox.navigation.LocalBackButtonVisibility
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelLogScreen(
    vehicleId: Long,
    logId: Long = 0L,
    onNavigateBack: () -> Unit,
    viewModel: FuelLogFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(vehicleId, logId) {
        viewModel.loadData(vehicleId, logId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (logId == 0L) stringResource(R.string.add_fuel_log) else stringResource(R.string.edit_fuel_log)) },
                navigationIcon = {
                    if (LocalBackButtonVisibility.current) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveLog(vehicleId, onNavigateBack) }
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save_log))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.odometer,
                onValueChange = viewModel::onOdometerChange,
                label = { Text(stringResource(R.string.odometer_label, uiState.unit)) },
                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val volumeLabel = if (uiState.unit == "mi") stringResource(R.string.gallons) else stringResource(R.string.liters)
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(stringResource(R.string.amount_label, volumeLabel)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                var currencyExpanded by remember { mutableStateOf(false) }
                val currencies = com.eliteonetube.glovebox.util.CurrencyUtility.supportedCurrencies

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencies.forEach { code ->
                            DropdownMenuItem(
                                text = { Text("$code (${com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(code)})") },
                                onClick = {
                                    viewModel.onCurrencyChange(code)
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.totalCost,
                onValueChange = viewModel::onTotalCostChange,
                label = { Text(stringResource(R.string.total_cost_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text(com.eliteonetube.glovebox.util.CurrencyUtility.getCurrencySymbol(uiState.currency)) }
            )

            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text(stringResource(R.string.gas_station_label)) },
                leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
