package com.eliteonetube.glovebox.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.data.entity.ProspectVehicle
import com.eliteonetube.glovebox.ui.viewmodels.ProspectViewModel
import org.json.JSONObject

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
    val tabs = listOf("Details", "Checklist")

    LaunchedEffect(prospectId) {
        viewModel.loadProspect(prospectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (prospectId == 0L) "New Car Inquiry" else "${formState.make} ${formState.model}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                            Text("I bought it!")
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
                    text = { Text("Save Details") }
                )
            }
        }
    ) { innerPadding ->
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

@Composable
fun ProspectDetailsForm(viewModel: ProspectViewModel, state: com.eliteonetube.glovebox.ui.viewmodels.ProspectFormState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.vin,
                onValueChange = { viewModel.onVinChange(it.uppercase()) },
                label = { Text("VIN (17 characters)") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else IconButton(onClick = { viewModel.decodeVin() }) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Decode VIN")
                    }
                }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.year,
                    onValueChange = viewModel::onYearChange,
                    label = { Text("Year") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.askedPrice,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text("Asked Price") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("$") }
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.make,
                onValueChange = viewModel::onMakeChange,
                label = { Text("Make") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = state.model,
                onValueChange = viewModel::onModelChange,
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Seller Location") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = state.sellerNotes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Seller Notes") },
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
