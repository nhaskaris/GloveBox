package com.eliteonetube.glovebox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.entity.ProspectVehicle
import com.eliteonetube.glovebox.ui.viewmodels.ProspectViewModel
import org.json.JSONObject
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class ChecklistCategory(val title: String, val items: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProspectFormScreen(
    prospectId: Long,
    onNavigateBack: () -> Unit,
    onPromoted: (Long) -> Unit,
    viewModel: ProspectViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return ProspectViewModel(app) as T
        }
    })
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val prospects by viewModel.allProspects.collectAsStateWithLifecycle()
    val prospect = prospects.find { it.id == prospectId }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.details_tab), stringResource(R.string.checklist_tab))

    LaunchedEffect(prospectId) {
        viewModel.loadProspect(prospectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (prospectId == 0L) stringResource(R.string.new_car_inquiry) else "${formState.make} ${formState.model}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (prospectId != 0L) {
                        Button(
                            onClick = { 
                                prospect?.let { 
                                    viewModel.promoteToGarage(it, onPromoted) 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.i_bought_it))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (tabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveProspect(onNavigateBack) },
                    icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                    text = { Text(stringResource(R.string.save_details)) }
                )
            }
        }
    )
{ innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (prospectId != 0L) {
                TabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            if (tabIndex == 0) {
                ProspectDetailsForm(viewModel, formState)
            } else {
                prospect?.let {
                    PrePurchaseChecklistContent(
                        initialCheckedJson = it.checklistJson,
                        onCheckedChange = { checkedItems ->
                            viewModel.updateChecklist(it.id, checkedItems)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProspectDetailsForm(viewModel: ProspectViewModel, state: com.eliteonetube.glovebox.ui.viewmodels.ProspectFormState) {
    val filteredMakes by viewModel.filteredMakes.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var makeExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showVinScanner by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Image capture setup
    val tempUri = remember {
        val file = File(context.cacheDir, "temp_prospect_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.onPhotoChange(tempUri.toString())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(tempUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onPhotoChange(it.toString()) }
    }

    if (showVinScanner) {
        VinScannerDialog(
            onDismiss = { showVinScanner = false },
            onVinScanned = { vin ->
                viewModel.onVinChange(vin)
                viewModel.decodeVin()
                showVinScanner = false
            }
        )
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
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(tempUri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (state.photoUri != null) {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Car Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            stringResource(R.string.change_photo),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.add_photo), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        item {
            Column {
                OutlinedTextField(
                    value = state.vin,
                    onValueChange = { 
                        viewModel.onVinChange(it)
                    },
                    label = { Text(stringResource(R.string.vin_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.vinValidationErrorResId != null,
                    supportingText = {
                        state.vinValidationErrorResId?.let { Text(stringResource(it)) }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else if (state.vin.length == 17 && state.vinValidationErrorResId == null) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.valid_vin),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(onClick = { showVinScanner = true }) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = stringResource(R.string.scan_vin))
                            }
                            
                            IconButton(onClick = { viewModel.decodeVin() }) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = stringResource(R.string.decode_vin))
                            }
                        }
                    }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.year,
                    onValueChange = viewModel::onYearChange,
                    label = { Text(stringResource(R.string.year)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.askedPrice,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text(stringResource(R.string.asked_price)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text(stringResource(R.string.currency_symbol)) }
                )
            }
        }

        item {
            // --- Searchable Make Dropdown ---
            ExposedDropdownMenuBox(
                expanded = makeExpanded,
                onExpandedChange = { makeExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.make,
                    onValueChange = {
                        viewModel.onMakeChange(it)
                        viewModel.onModelChange("") // Reset model when make changes
                        makeExpanded = true
                    },
                    label = { Text(stringResource(R.string.make)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    placeholder = { Text(stringResource(R.string.search_select_make)) },
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
                                    viewModel.onMakeSelected(item)
                                    viewModel.onModelChange("")
                                    makeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            // --- Searchable Model Dropdown ---
            ExposedDropdownMenuBox(
                expanded = modelExpanded && state.make.isNotEmpty(),
                onExpandedChange = { if (state.make.isNotEmpty()) modelExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.model,
                    onValueChange = {
                        viewModel.onModelChange(it)
                        modelExpanded = true
                    },
                    enabled = state.make.isNotEmpty(),
                    label = { Text(stringResource(R.string.model)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    placeholder = { Text(if (state.make.isEmpty()) stringResource(R.string.select_make_first) else stringResource(R.string.search_select_model)) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true
                )

                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = modelExpanded && state.make.isNotEmpty(),
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        availableModels.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    viewModel.onModelChange(item)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text(stringResource(R.string.seller_location)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = state.sellerNotes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.seller_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}

@Composable
fun PrePurchaseChecklistContent(
    initialCheckedJson: String,
    onCheckedChange: (Set<String>) -> Unit
) {
    val categories = remember {
        listOf(
            ChecklistCategory("Under the Hood", listOf(
                "Check oil level and color", "Look for fluid leaks", "Inspect battery terminals", "Check belt condition"
            )),
            ChecklistCategory("Exterior", listOf(
                "Check for panel gaps", "Inspect paint condition", "Check tire tread depth", "Look for rust"
            )),
            ChecklistCategory("Interior", listOf(
                "Check upholstery wear", "Test A/C and heater", "Test window switches", "Check dashboard lights"
            )),
            ChecklistCategory("The Test Drive", listOf(
                "Listen for engine knocking", "Check steering vibration", "Ensure brakes feel solid", "Test gear shifting"
            ))
        )
    }

    var checkedItems by remember(initialCheckedJson) {
        mutableStateOf(
            try {
                if (initialCheckedJson.isBlank()) emptySet<String>()
                else {
                    val json = JSONObject(initialCheckedJson)
                    val set = mutableSetOf<String>()
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        set.add(keys.next())
                    }
                    set
                }
            } catch (e: Exception) {
                emptySet<String>()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            item {
                Text(category.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            }
            items(category.items) { itemText ->
                val isChecked = checkedItems.contains(itemText)
                ListItem(
                    headlineContent = {
                        Text(itemText, textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    },
                    leadingContent = {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val newSet = if (checked) checkedItems + itemText else checkedItems - itemText
                                checkedItems = newSet
                                onCheckedChange(newSet)
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        val newSet = if (isChecked) checkedItems - itemText else checkedItems + itemText
                        checkedItems = newSet
                        onCheckedChange(newSet)
                    }
                )
            }
        }
    }
}
