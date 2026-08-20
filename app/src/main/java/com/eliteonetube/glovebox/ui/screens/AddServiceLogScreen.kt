package com.eliteonetube.glovebox.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.ui.viewmodels.ServiceLogFormViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val context = LocalContext.current

    val serviceCategories = listOf(
        "Oil Change", "Tire Rotation", "Brake Service", "Inspection", 
        "Battery Replacement", "Air Filter", "Spark Plugs", "Transmission", 
        "Coolant Flush", "Wheel Alignment", "Wipers", "Brake Pads", "Brake Rotors",
        "Cabin Air Filter", "Engine Air Filter", "Drive Belt", "Timing Belt",
        "Water Pump", "Alternator", "Starter", "AC Recharge", "Tire Replacement"
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var serviceSearchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val filteredCategories by remember(serviceSearchQuery) {
        derivedStateOf {
            serviceCategories.filter { 
                it.contains(serviceSearchQuery, ignoreCase = true) && 
                !uiState.serviceTypes.contains(it) 
            }
        }
    }

    // Image capture setup
    val tempUri = remember {
        val file = File(context.cacheDir, "temp_receipt_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.onPhotoChange(tempUri.toString())
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onPhotoChange(it.toString()) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(tempUri)
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Camera") },
                        leadingContent = { Icon(Icons.Rounded.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Gallery") },
                        leadingContent = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text("Cancel") }
            }
        )
    }

    LaunchedEffect(vehicleId, recordId) {
        viewModel.loadData(vehicleId, recordId)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    val dateString = Instant.ofEpochMilli(uiState.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(dateFormatter)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
            val isReadyToSave = uiState.serviceTypes.isNotEmpty() || uiState.notes.isNotBlank()
            ExtendedFloatingActionButton(
                onClick = {
                    if (isReadyToSave) {
                        viewModel.saveRecord(vehicleId, onNavigateBack)
                    }
                },
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                text = { Text("Save Record") },
                expanded = isReadyToSave,
                containerColor = if (isReadyToSave) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isReadyToSave) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Receipt Photo ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.receiptPhotoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.receiptPhotoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Receipt photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Add Receipt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- Service Type Selection (Multi-select Searchable Dropdown) ---
            Text(
                text = "Services Performed",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded && filteredCategories.isNotEmpty(),
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = serviceSearchQuery,
                    onValueChange = {
                        serviceSearchQuery = it
                        dropdownExpanded = true
                    },
                    label = { Text("Search Service Type") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    placeholder = { Text("e.g. Oil Change, Brakes...") },
                    shape = MaterialTheme.shapes.large,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true
                )

                if (filteredCategories.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    viewModel.onServiceTypeToggle(category)
                                    serviceSearchQuery = ""
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Display selected services as removable chips
            if (uiState.serviceTypes.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.serviceTypes.forEach { type ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.onServiceTypeToggle(type) },
                            label = { Text(type) },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.mileage,
                    onValueChange = viewModel::onMileageChange,
                    label = { Text("Mileage (${uiState.unit})") },
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = uiState.cost,
                    onValueChange = viewModel::onCostChange,
                    label = { Text("Cost (Optional)") },
                    leadingIcon = { Icon(Icons.Rounded.AttachMoney, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large
                )
            }

            // --- DIY vs Shop Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Service performed at a shop?", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = !uiState.isDiy,
                    onCheckedChange = { viewModel.onDiyToggle(!it) }
                )
            }

            if (!uiState.isDiy) {
                OutlinedTextField(
                    value = uiState.serviceLocation,
                    onValueChange = viewModel::onLocationChange,
                    label = { Text("Shop Name (Optional)") },
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Precision Tune Auto Care") },
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = uiState.mechanicName,
                    onValueChange = viewModel::onMechanicNameChange,
                    label = { Text("Mechanic Name (Optional)") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Who performed the work?") },
                    shape = MaterialTheme.shapes.large
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DIY Service recorded. Location will be saved as 'DIY'.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.laborHours,
                    onValueChange = viewModel::onLaborHoursChange,
                    label = { Text("Labor Hours") },
                    leadingIcon = { Icon(Icons.Rounded.Engineering, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = uiState.partsUsed,
                    onValueChange = viewModel::onPartsUsedChange,
                    label = { Text("Parts Used") },
                    leadingIcon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Filter, Oil, etc.") },
                    shape = MaterialTheme.shapes.large
                )
            }

            OutlinedTextField(
                value = dateString,
                onValueChange = {},
                label = { Text("Date") },
                leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
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
