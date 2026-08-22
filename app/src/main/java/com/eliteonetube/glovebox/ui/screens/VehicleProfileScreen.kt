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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.R
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
            vinValidationErrorResId = null,
            isVinEnabled = true,
            onMakeSelected = {},
            onMakeQueryChange = {},
            onModelQueryChange = {},
            onVinChange = { null },
            onDecodeVin = {},
            onResetVinState = {},
            unitSystem = "km",
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
    val vinValidationErrorResId by viewModel.vinValidationErrorResId.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(LocalContext.current)
    val isVinEnabled by userPrefs.isVinFeatureEnabled.collectAsState(initial = true)

    VehicleProfileContent(
        vehicle = vehicle,
        makes = filteredMakes,
        availableModels = filteredModels,
        vinState = vinState,
        vinValidationErrorResId = vinValidationErrorResId,
        isVinEnabled = isVinEnabled,
        unitSystem = unitSystem,
        onMakeSelected = viewModel::onMakeSelected,
        onMakeQueryChange = viewModel::updateMakeQuery,
        onModelQueryChange = viewModel::updateModelQuery,
        onVinChange = viewModel::validateVin,
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
    vinValidationErrorResId: Int?,
    isVinEnabled: Boolean,
    unitSystem: String,
    onMakeSelected: (String) -> Unit,
    onMakeQueryChange: (String) -> Unit,
    onModelQueryChange: (String) -> Unit,
    onVinChange: (String) -> Int?,
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
    var photoUri by remember { mutableStateOf<String?>(null) }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showVinScanner by remember { mutableStateOf(false) }

    if (showVinScanner) {
        VinScannerDialog(
            onDismiss = { showVinScanner = false },
            onVinScanned = { scannedVin ->
                vin = scannedVin
                onVinChange(scannedVin)
                onDecodeVin(scannedVin)
                showVinScanner = false
            }
        )
    }

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
    var fuelExpanded by remember { mutableStateOf(false) }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text(stringResource(R.string.select_image_source)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.camera)) },
                        leadingContent = { Icon(Icons.Rounded.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.gallery)) },
                        leadingContent = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

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
            photoUri = it.photoUri
            onMakeSelected(it.make)
            onMakeQueryChange(it.make)
            onModelQueryChange(it.model)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (vehicle?.id == 0L || vehicle == null) stringResource(R.string.add_to_garage) 
                        else stringResource(R.string.edit_vehicle),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            val isFormValid = make.isNotBlank() && model.isNotBlank()
            ExtendedFloatingActionButton(
                onClick = {
                    if (isFormValid) {
                        onSaveVehicle(make, model, year.toIntOrNull() ?: 0, odometer.toIntOrNull() ?: 0, trim.takeIf { it.isNotBlank() }, vin.takeIf { it.isNotBlank() }, nickname.takeIf { it.isNotBlank() }, licensePlate.takeIf { it.isNotBlank() }, color.takeIf { it.isNotBlank() }, fuelType.takeIf { it.isNotBlank() }, unitSystem, photoUri, onNavigateBack)
                    }
                },
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                text = { Text(stringResource(R.string.save_vehicle)) },
                containerColor = if (isFormValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced from 24.dp
        ) {
            // --- Photo Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp) // Reduced from 180.dp
                    .clip(MaterialTheme.shapes.large) // Reduced from extraLarge
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
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            stringResource(R.string.change_photo),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.add_photo), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // --- Section: Identification ---
            ProfileSection(title = "Identification", icon = Icons.Rounded.Badge) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text(stringResource(R.string.nickname_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.nickname_placeholder)) },
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase() },
                        label = { Text(stringResource(R.string.license_plate)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            // --- Section: Smart VIN & Basic Info ---
            ProfileSection(title = "Vehicle Details", icon = Icons.Rounded.DirectionsCar) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = vin,
                        onValueChange = { 
                            vin = it.uppercase()
                            onVinChange(vin)
                        },
                        label = { Text(stringResource(R.string.vin)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = vinValidationErrorResId != null || vinState is VinDecodingState.Error,
                        supportingText = {
                            when {
                                vinState is VinDecodingState.Error -> Text(stringResource(vinState.messageResId), color = MaterialTheme.colorScheme.error)
                                vinValidationErrorResId != null -> Text(stringResource(vinValidationErrorResId), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (vin.length == 17 && vinValidationErrorResId == null && vinState !is VinDecodingState.Error) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                }
                                IconButton(onClick = { showVinScanner = true }) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                                }
                                if (isVinEnabled) {
                                    if (vinState is VinDecodingState.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        IconButton(onClick = { onDecodeVin(vin) }, enabled = vin.length >= 11) {
                                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) year = it },
                            label = { Text(stringResource(R.string.year)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = odometer,
                            onValueChange = { if (it.all { char -> char.isDigit() }) odometer = it },
                            label = { Text(stringResource(R.string.current_odometer)) },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text(unitSystem) },
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    ExposedDropdownMenuBox(expanded = makeExpanded, onExpandedChange = { makeExpanded = it }) {
                        OutlinedTextField(
                            value = make,
                            onValueChange = { make = it; model = ""; onMakeQueryChange(it); onMakeSelected(it); makeExpanded = true },
                            label = { Text(stringResource(R.string.make_required)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded) },
                            shape = MaterialTheme.shapes.medium
                        )
                        if (makes.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = makeExpanded, onDismissRequest = { makeExpanded = false }) {
                                makes.forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { make = item; model = ""; onMakeQueryChange(item); onMakeSelected(item); makeExpanded = false })
                                }
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = modelExpanded && make.isNotEmpty(), onExpandedChange = { if (make.isNotEmpty()) modelExpanded = it }) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it; onModelQueryChange(it); modelExpanded = true },
                            enabled = make.isNotEmpty(),
                            label = { Text(stringResource(R.string.model_required)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            shape = MaterialTheme.shapes.medium
                        )
                        if (availableModels.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = modelExpanded && make.isNotEmpty(), onDismissRequest = { modelExpanded = false }) {
                                availableModels.forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { model = item; onModelQueryChange(item); modelExpanded = false })
                                }
                            }
                        }
                    }
                }
            }

            // --- Section: Technical Specs ---
            ProfileSection(title = "Specifications", icon = Icons.Rounded.Tune) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = trim,
                        onValueChange = { trim = it },
                        label = { Text(stringResource(R.string.trim_variant)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.trim_placeholder)) },
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text(stringResource(R.string.color)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = it }) {
                        OutlinedTextField(
                            value = fuelType,
                            onValueChange = { fuelType = it },
                            label = { Text(stringResource(R.string.fuel_type)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                            fuelTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { fuelType = type; fuelExpanded = false })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProfileSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp) // Reduced from 12.dp
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) // Reduced from 20.dp
            Spacer(Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium, // Reduced from large
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp) // Reduced from 2.dp
        ) {
            Box(modifier = Modifier.padding(12.dp)) { // Reduced from 16.dp
                content()
            }
        }
    }
}
