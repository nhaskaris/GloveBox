package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.ui.viewmodels.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleProfileScreen(
    vehicleId: Long,
    onNavigateBack: () -> Unit,
    viewModel: VehicleViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return VehicleViewModel(app, vehicleId) as T
        }
    })
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val makes by viewModel.makes.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }

    var makeExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    // Filtered lists based on user search input
    val filteredMakes by remember(make, makes) {
        derivedStateOf {
            if (make.isBlank()) makes
            else makes.filter { it.contains(make, ignoreCase = true) }
        }
    }

    val filteredModels by remember(model, availableModels) {
        derivedStateOf {
            if (model.isBlank()) availableModels
            else availableModels.filter { it.contains(model, ignoreCase = true) }
        }
    }

    LaunchedEffect(vehicle) {
        vehicle?.let {
            make = it.make
            model = it.model
            year = it.year.toString()
            odometer = it.odometer.toString()
            viewModel.onMakeSelected(it.make)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (vehicleId == 0L) "Add Vehicle" else "Edit Vehicle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.saveVehicle(
                        make = make,
                        model = model,
                        year = year.toIntOrNull() ?: 0,
                        odometer = odometer.toIntOrNull() ?: 0
                    )
                    onNavigateBack()
                },
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                text = { Text("Save Vehicle") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Enter your vehicle details to keep track of maintenance and records.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- Year Input ---
            OutlinedTextField(
                value = year,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) year = it },
                label = { Text("Year") },
                leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large
            )

            // --- Searchable Make Dropdown ---
            ExposedDropdownMenuBox(
                expanded = makeExpanded,
                onExpandedChange = { makeExpanded = it }
            ) {
                OutlinedTextField(
                    value = make,
                    onValueChange = {
                        make = it
                        model = "" // Reset model when make query changes
                        viewModel.onMakeSelected(it)
                        makeExpanded = true
                    },
                    label = { Text("Make") },
                    leadingIcon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text("Search or select Make") },
                    shape = MaterialTheme.shapes.large,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true
                )

                if (filteredMakes.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = makeExpanded,
                        onDismissRequest = { makeExpanded = false }
                    ) {
                        filteredMakes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    make = item
                                    model = ""
                                    viewModel.onMakeSelected(item)
                                    makeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Searchable Model Dropdown ---
            ExposedDropdownMenuBox(
                expanded = modelExpanded && make.isNotEmpty(),
                onExpandedChange = { if (make.isNotEmpty()) modelExpanded = it }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = {
                        model = it
                        modelExpanded = true
                    },
                    enabled = make.isNotEmpty(),
                    label = { Text("Model") },
                    leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text(if (make.isEmpty()) "Select Make first" else "Search or select Model") },
                    shape = MaterialTheme.shapes.large,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true
                )

                if (filteredModels.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = modelExpanded && make.isNotEmpty(),
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        filteredModels.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    model = item
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Odometer Input ---
            OutlinedTextField(
                value = odometer,
                onValueChange = { if (it.all { char -> char.isDigit() }) odometer = it },
                label = { Text("Current Odometer") },
                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}