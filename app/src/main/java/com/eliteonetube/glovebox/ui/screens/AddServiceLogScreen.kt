package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.ui.viewmodels.ServiceLogFormViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme

@Preview(showBackground = true)
@Composable
fun AddServiceLogScreenPreview() {
    GloveboxTheme {
        AddServiceLogScreen(
            vehicleId = 1L,
            onNavigateBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceLogScreen(
    vehicleId: Long,
    recordId: Long = 0L,
    onNavigateBack: () -> Unit,
    viewModel: ServiceLogFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(recordId) {
        if (recordId != 0L) {
            viewModel.loadRecord(recordId)
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    val dateString = Instant.ofEpochMilli(uiState.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(dateFormatter)

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(if (recordId == 0L) "Add Service Log" else "Edit Service Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.saveRecord(vehicleId, onNavigateBack)
                },
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                text = { Text("Save Record") }
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
            OutlinedTextField(
                value = uiState.serviceType,
                onValueChange = viewModel::onServiceTypeChange,
                label = { Text("Service Type") },
                leadingIcon = { Icon(Icons.Rounded.Build, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Oil Change") },
                shape = MaterialTheme.shapes.large
            )

            OutlinedTextField(
                value = uiState.mileage,
                onValueChange = viewModel::onMileageChange,
                label = { Text("Mileage") },
                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large
            )

            OutlinedTextField(
                value = uiState.cost,
                onValueChange = viewModel::onCostChange,
                label = { Text("Cost") },
                leadingIcon = { Icon(Icons.Rounded.AttachMoney, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large
            )

            // Simplistic date picker - in real app would use DatePicker
            OutlinedTextField(
                value = dateString,
                onValueChange = {},
                label = { Text("Date") },
                leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = MaterialTheme.shapes.large
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
