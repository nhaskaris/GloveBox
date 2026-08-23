package com.eliteonetube.glovebox.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.ui.viewmodels.DocumentFormViewModel
import com.eliteonetube.glovebox.navigation.LocalBackButtonVisibility
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    vehicleId: Long,
    docId: Long = 0L,
    onNavigateBack: () -> Unit,
    viewModel: DocumentFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val userCountry by viewModel.userCountry.collectAsStateWithLifecycle()
    val isGreece = userCountry == "GR"

    val categories = remember(isGreece) {
        val base = listOf(
            R.string.cat_insurance,
            R.string.cat_registration,
            R.string.cat_warranty,
            R.string.cat_invoice
        )
        val greekSpecific = if (isGreece) listOf(R.string.cat_kteo, R.string.cat_emissions) else emptyList()
        (base + greekSpecific + listOf(R.string.cat_other))
    }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Image capture setup
    val tempUri = remember {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
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
        if (isGranted) {
            cameraLauncher.launch(tempUri)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onExpiryDateChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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

    LaunchedEffect(vehicleId) {
        if (docId == 0L) {
            viewModel.initialize(vehicleId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_document)) },
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
                onClick = { if (uiState.photoUri != null) viewModel.saveDocument(onNavigateBack) },
                containerColor = if (uiState.photoUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (uiState.photoUri != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save_document))
            }
        }
    )
{ innerPadding ->
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
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Document photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(stringResource(R.string.scan_or_upload), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (uiState.photoUri != null) {
                Button(
                    onClick = viewModel::scanDocument,
                    enabled = !uiState.isOcrLoading,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    if (uiState.isOcrLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.auto_extract_expiry))
                    }
                }
                
                uiState.ocrResult?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.contains("Error", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.document_name)) },
                placeholder = { Text(stringResource(R.string.document_name_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { catResId ->
                        val catName = stringResource(catResId)
                        DropdownMenuItem(
                            text = { Text(catName) },
                            onClick = {
                                viewModel.onCategoryChange(catName)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // --- Link to Car Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.link_to_specific_car), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (uiState.isUniversal) stringResource(R.string.universal_document) else stringResource(R.string.linked_to_car),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = !uiState.isUniversal,
                    onCheckedChange = { viewModel.onUniversalToggle(!it) }
                )
            }

            // --- Vehicle Selection (Only shown if not universal) ---
            if (!uiState.isUniversal) {
                ExposedDropdownMenuBox(
                    expanded = vehicleExpanded,
                    onExpandedChange = { vehicleExpanded = it }
                ) {
                    val selectedVehicle = vehicles.find { it.id == uiState.linkedVehicleId }
                    val vehicleText = selectedVehicle?.let { "${it.year} ${it.make} ${it.model}" } ?: stringResource(R.string.select_car)
                    
                    OutlinedTextField(
                        value = vehicleText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_car)) },
                        leadingIcon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleExpanded,
                        onDismissRequest = { vehicleExpanded = false }
                    ) {
                        vehicles.forEach { car ->
                            DropdownMenuItem(
                                text = { Text("${car.year} ${car.make} ${car.model}") },
                                onClick = {
                                    viewModel.onVehicleSelect(car.id)
                                    vehicleExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            val expiryDateStr = uiState.expiryDate?.let {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
            } ?: stringResource(R.string.no_expiry_date)

            OutlinedTextField(
                value = expiryDateStr,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.expiry_date)) },
                leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Text(
                text = stringResource(R.string.expiry_notification_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
