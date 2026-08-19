package com.eliteonetube.glovebox.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.eliteonetube.glovebox.ui.viewmodels.VehicleViewModel
import com.eliteonetube.glovebox.ui.viewmodels.VinDecodingState
import java.io.File

import androidx.compose.ui.tooling.preview.Preview
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme

@Preview(showBackground = true)
@Composable
fun VehicleProfileContentPreview() {
    GloveboxTheme {
        VehicleProfileContent(
            vehicle = null,
            makes = listOf("Toyota", "Honda", "Ford"),
            availableModels = listOf("Camry", "Corolla", "RAV4"),
            vinState = VinDecodingState.Idle,
            isVinEnabled = true,
            onMakeSelected = {},
            onMakeQueryChange = {},
            onModelQueryChange = {},
            onDecodeVin = {},
            onResetVinState = {},
            onSaveVehicle = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onNavigateBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleProfileScreen(
    vehicleId: Long,
    onNavigateBack: () -> Unit,
    viewModel: VehicleViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val app = com.eliteonetube.glovebox.GloveboxApplication.instance
                return VehicleViewModel(app, vehicleId) as T
            }
        }
    )
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val filteredMakes by viewModel.filteredMakes.collectAsStateWithLifecycle()
    val filteredModels by viewModel.filteredModels.collectAsStateWithLifecycle()
    val vinState by viewModel.vinDecodingState.collectAsStateWithLifecycle()
    val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(LocalContext.current)
    val isVinEnabled by userPrefs.isVinFeatureEnabled.collectAsState(initial = true)

    VehicleProfileContent(
        vehicle = vehicle,
        makes = filteredMakes,
        availableModels = filteredModels,
        vinState = vinState,
        isVinEnabled = isVinEnabled,
        onMakeSelected = viewModel::onMakeSelected,
        onMakeQueryChange = viewModel::updateMakeQuery,
        onModelQueryChange = viewModel::updateModelQuery,
        onDecodeVin = viewModel::decodeVin,
        onResetVinState = viewModel::resetVinState,
        onSaveVehicle = { make, model, year, odometer, trim, vin, nickname, licensePlate, color, fuelType, unit, photoUri, onComplete ->
            viewModel.saveVehicle(make, model, year, odometer, trim, vin, nickname, licensePlate, color, fuelType, unit, photoUri, onComplete)
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleProfileContent(
    vehicle: com.eliteonetube.glovebox.data.entity.Vehicle?,
    makes: List<String>,
    availableModels: List<String>,
    vinState: VinDecodingState,
    isVinEnabled: Boolean,
    onMakeSelected: (String) -> Unit,
    onMakeQueryChange: (String) -> Unit,
    onModelQueryChange: (String) -> Unit,
    onDecodeVin: (String) -> Unit,
    onResetVinState: () -> Unit,
    onSaveVehicle: (String, String, Int, Int, String?, String?, String?, String?, String?, String?, String, String?, () -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val fuelTypes = listOf("Gasoline", "Diesel", "Electric", "Hybrid", "Plug-in Hybrid", "LPG", "CNG", "Hydrogen")

    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var trim by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("") }
    var odometerUnit by remember { mutableStateOf("km") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Image capture setup
    val tempUri = remember {
        val file = File(context.cacheDir, "temp_car_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri = tempUri.toString()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { photoUri = it.toString() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(tempUri)
    }

    var modelExpanded by remember { mutableStateOf(false) }
    var makeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var fuelExpanded by remember { mutableStateOf(false) }

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

    // Handle VIN decoding success
    LaunchedEffect(vinState) {
        if (vinState is VinDecodingState.Success) {
            make = vinState.make
            model = vinState.model
            year = vinState.year.toString()
            fuelType = vinState.fuelType
            trim = vinState.trim
            onMakeQueryChange(make)
            onMakeSelected(make)
            onModelQueryChange(model)
            onResetVinState()
        }
    }

    LaunchedEffect(vehicle) {
        vehicle?.let {
            make = it.make
            model = it.model
            year = it.year.toString()
            odometer = it.odometer.toString()
            trim = it.trim ?: ""
            vin = it.vin ?: ""
            nickname = it.nickname ?: ""
            licensePlate = it.licensePlate ?: ""
            color = it.color ?: ""
            fuelType = it.fuelType ?: ""
            odometerUnit = it.odometerUnit
            photoUri = it.photoUri
            onMakeSelected(it.make)
            onMakeQueryChange(it.make)
            onModelQueryChange(it.model)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (vehicle?.id == 0L || vehicle == null) "Add to Garage" else "Edit Vehicle") },
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
            val isFormValid = make.isNotBlank() && model.isNotBlank()
            ExtendedFloatingActionButton(
                onClick = {
                    if (isFormValid) {
                        onSaveVehicle(
                            make,
                            model,
                            year.toIntOrNull() ?: 0,
                            odometer.toIntOrNull() ?: 0,
                            trim.takeIf { it.isNotBlank() },
                            vin.takeIf { it.isNotBlank() },
                            nickname.takeIf { it.isNotBlank() },
                            licensePlate.takeIf { it.isNotBlank() },
                            color.takeIf { it.isNotBlank() },
                            fuelType.takeIf { it.isNotBlank() },
                            odometerUnit,
                            photoUri,
                            onNavigateBack
                        )
                    }
                },
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                text = { Text("Save Vehicle") },
                expanded = isFormValid,
                containerColor = if (isFormValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isFormValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Vehicle photo",
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
                            "Add Photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "Enter your vehicle details or use the VIN to auto-fill.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- VIN Input with Auto-Fill ---
            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it.uppercase() },
                label = { Text("VIN") },
                leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                trailingIcon = {
                    if (isVinEnabled) {
                        if (vinState is VinDecodingState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = { onDecodeVin(vin) },
                                enabled = vin.length >= 11
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Auto-fill from VIN")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Vehicle Identification Number") },
                shape = MaterialTheme.shapes.large,
                supportingText = if (vinState is VinDecodingState.Error) {
                    { Text(vinState.message, color = MaterialTheme.colorScheme.error) }
                } else null
            )

            // --- Nickname Input ---
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname (Optional)") },
                leadingIcon = { Icon(Icons.Rounded.Face, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. My Daily Driver") },
                shape = MaterialTheme.shapes.large
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
                        onMakeQueryChange(it)
                        onMakeSelected(it)
                        makeExpanded = true
                    },
                    label = { Text("Make *") },
                    leadingIcon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    placeholder = { Text("Search or select Make") },
                    shape = MaterialTheme.shapes.large,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true,
                    isError = make.isBlank() && vehicle != null // Show error if empty during edit
                )

                if (makes.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = makeExpanded,
                        onDismissRequest = { makeExpanded = false }
                    ) {
                        makes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    make = item
                                    model = ""
                                    onMakeQueryChange(item)
                                    onMakeSelected(item)
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
                        onModelQueryChange(it)
                        modelExpanded = true
                    },
                    enabled = make.isNotEmpty(),
                    label = { Text("Model *") },
                    leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    placeholder = { Text(if (make.isEmpty()) "Select Make first" else "Search or select Model") },
                    shape = MaterialTheme.shapes.large,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true,
                    isError = model.isBlank() && make.isNotBlank() && vehicle != null
                )

                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = modelExpanded && make.isNotEmpty(),
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        availableModels.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    model = item
                                    onModelQueryChange(item)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Odometer Input ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { if (it.all { char -> char.isDigit() }) odometer = it },
                    label = { Text("Current Odometer") },
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large
                )

                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.width(100.dp)
                ) {
                    OutlinedTextField(
                        value = odometerUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                        shape = MaterialTheme.shapes.large,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("km") },
                            onClick = {
                                odometerUnit = "km"
                                unitExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("mi") },
                            onClick = {
                                odometerUnit = "mi"
                                unitExpanded = false
                            }
                        )
                    }
                }
            }

            // --- Additional Info Section ---
            Text(
                text = "Additional Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            OutlinedTextField(
                value = trim,
                onValueChange = { trim = it },
                label = { Text("Trim / Variant") },
                leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Sport, EX-L") },
                shape = MaterialTheme.shapes.large
            )

            OutlinedTextField(
                value = licensePlate,
                onValueChange = { licensePlate = it.uppercase() },
                label = { Text("License Plate") },
                leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color") },
                leadingIcon = { Icon(Icons.Rounded.ColorLens, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            ExposedDropdownMenuBox(
                expanded = fuelExpanded,
                onExpandedChange = { fuelExpanded = it }
            ) {
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text("Fuel Type") },
                    leadingIcon = { Icon(Icons.Rounded.LocalGasStation, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    placeholder = { Text("Select or type Fuel Type") },
                    shape = MaterialTheme.shapes.large,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = fuelExpanded,
                    onDismissRequest = { fuelExpanded = false }
                ) {
                    fuelTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                fuelType = type
                                fuelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
