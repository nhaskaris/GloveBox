package com.eliteonetube.glovebox.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.ui.viewmodels.ProspectViewModel
import org.json.JSONObject
import java.io.File

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
                title = { 
                    Text(
                        if (prospectId == 0L) stringResource(R.string.new_car_inquiry) 
                        else "${formState.make} ${formState.model}",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.i_bought_it))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (tabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveProspect(onNavigateBack) },
                    icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                    text = { Text(stringResource(R.string.save_details)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (prospectId != 0L) {
                SecondaryTabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = { 
                                Text(
                                    title, 
                                    style = if (tabIndex == index) MaterialTheme.typography.titleSmall 
                                            else MaterialTheme.typography.bodyMedium
                                ) 
                            }
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced from 24.dp
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Reduced from 220.dp
                    .clip(MaterialTheme.shapes.large) // Reduced from extraLarge
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            stringResource(R.string.change_photo),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AddAPhoto, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.add_photo), 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            FormSection(title = stringResource(R.string.vehicle_info)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = state.vin,
                        onValueChange = { viewModel.onVinChange(it) },
                        label = { Text(stringResource(R.string.vin_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.vinValidationErrorResId != null,
                        supportingText = {
                            state.vinValidationErrorResId?.let { Text(stringResource(it)) }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else if (state.vin.length == 17 && state.vinValidationErrorResId == null) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                }
                                IconButton(onClick = { showVinScanner = true }) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                                }
                                IconButton(onClick = { viewModel.decodeVin() }) {
                                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.year,
                            onValueChange = viewModel::onYearChange,
                            label = { Text(stringResource(R.string.year)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = state.askedPrice,
                            onValueChange = viewModel::onPriceChange,
                            label = { Text(stringResource(R.string.asked_price)) },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            prefix = { Text(stringResource(R.string.currency_symbol)) },
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = makeExpanded,
                        onExpandedChange = { makeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.make,
                            onValueChange = {
                                viewModel.onMakeChange(it)
                                viewModel.onModelChange("")
                                makeExpanded = true
                            },
                            label = { Text(stringResource(R.string.make)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            placeholder = { Text(stringResource(R.string.search_select_make)) },
                            shape = MaterialTheme.shapes.medium
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
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            placeholder = { Text(if (state.make.isEmpty()) stringResource(R.string.select_make_first) else stringResource(R.string.search_select_model)) },
                            shape = MaterialTheme.shapes.medium
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
            }
        }

        item {
            FormSection(title = stringResource(R.string.seller_details)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = state.location,
                        onValueChange = viewModel::onLocationChange,
                        label = { Text(stringResource(R.string.seller_location)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = state.sellerNotes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text(stringResource(R.string.seller_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        }
    }
}

@Composable
fun FormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp) // Reduced from 12.dp
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), // Reduced from 2.dp
            shape = MaterialTheme.shapes.medium // Reduced from large
        ) {
            Box(modifier = Modifier.padding(12.dp)) { // Reduced from 16.dp
                content()
            }
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { category ->
            item {
                Text(
                    category.title, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(category.items) { itemText ->
                val isChecked = checkedItems.contains(itemText)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                itemText, 
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                                        else MaterialTheme.colorScheme.onSurface
                            )
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
}
