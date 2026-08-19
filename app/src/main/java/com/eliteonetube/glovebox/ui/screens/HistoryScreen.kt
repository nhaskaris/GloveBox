package com.eliteonetube.glovebox.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme
import com.eliteonetube.glovebox.ui.viewmodels.HistoryFilter
import com.eliteonetube.glovebox.ui.viewmodels.HistoryItem
import com.eliteonetube.glovebox.ui.viewmodels.HistoryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

private fun formatCost(cost: Double?, currency: String = "USD"): String {
    val symbol = when (currency) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currency "
    }
    return "$symbol%.2f".format(cost ?: 0.0)
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    GloveboxTheme {
        HistoryContent(
            items = listOf(
                HistoryItem.Service(
                    ServiceRecord(
                        id = 1L,
                        vehicleId = 1L,
                        date = System.currentTimeMillis(),
                        mileage = 45210,
                        serviceType = "Oil Change",
                        cost = 45.99,
                        notes = "Full synthetic"
                    )
                ),
                HistoryItem.Fuel(
                    FuelLog(
                        id = 1L,
                        vehicleId = 1L,
                        date = System.currentTimeMillis() - 86400000,
                        odometer = 45000,
                        amount = 40.0,
                        totalCost = 60.0,
                        location = "Shell"
                    )
                )
            ),
            vehicle = null,
            currentFilter = HistoryFilter.ALL,
            onFilterChange = {},
            onAddRecord = {},
            onEditRecord = {},
            onAddFuel = {},
            onEditFuel = {},
            onDeleteService = {},
            onDeleteFuel = {},
            onExportPdf = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vehicleId: Long,
    onAddRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (Long) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: HistoryViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val app = com.eliteonetube.glovebox.GloveboxApplication.instance
                return HistoryViewModel(app, vehicleId) as T
            }
        }
    )
) {
    val items by viewModel.historyItems.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }

    HistoryContent(
        items = items,
        vehicle = vehicle,
        currentFilter = currentFilter,
        onFilterChange = { viewModel.setFilter(it) },
        onAddRecord = onAddRecord,
        onEditRecord = onEditRecord,
        onAddFuel = onAddFuel,
        onEditFuel = onEditFuel,
        onExportPdf = { showExportDialog = true },
        onDeleteService = { viewModel.deleteServiceRecord(it) },
        onDeleteFuel = { viewModel.deleteFuelLog(it) },
        onOpenDrawer = onOpenDrawer
    )

    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onExport = { includeCosts, includeShop, includeMechanic, includeFuel, includeSummary ->
                showExportDialog = false
                viewModel.exportHistoryToPdf(includeCosts, includeShop, includeMechanic, includeFuel, includeSummary) { file ->
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share History"))
                    }
                }
            }
        )
    }
}

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExport: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var includeCosts by remember { mutableStateOf(true) }
    var includeShop by remember { mutableStateOf(true) }
    var includeMechanic by remember { mutableStateOf(true) }
    var includeFuel by remember { mutableStateOf(true) }
    var includeSummary by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF Export Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select what to include in the report:", style = MaterialTheme.typography.bodyMedium)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeCosts, onCheckedChange = { includeCosts = it })
                    Text("Include Costs & Financial Summary")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeShop, onCheckedChange = { includeShop = it })
                    Text("Include Shop/Station Locations")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMechanic, onCheckedChange = { includeMechanic = it })
                    Text("Include Mechanic Names")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeFuel, onCheckedChange = { includeFuel = it })
                    Text("Include Fuel History")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeSummary, onCheckedChange = { includeSummary = it })
                    Text("Include Summary Section")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(includeCosts, includeShop, includeMechanic, includeFuel, includeSummary) }) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    items: List<HistoryItem>,
    vehicle: com.eliteonetube.glovebox.data.entity.Vehicle?,
    currentFilter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (Long) -> Unit,
    onDeleteService: (ServiceRecord) -> Unit,
    onDeleteFuel: (FuelLog) -> Unit,
    onExportPdf: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    val mileageUnit = vehicle?.odometerUnit ?: "km"
    var recordPendingDelete by rememberSaveable { mutableStateOf<HistoryItem?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
                        }
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = onExportPdf) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export PDF")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showAddMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            showAddMenu = false
                            onAddFuel()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalGasStation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fuel")
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            showAddMenu = false
                            onAddRecord()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Build, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Service")
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showAddMenu = !showAddMenu },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (showAddMenu) Icons.Rounded.Close else Icons.Rounded.Add,
                        contentDescription = "Add"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HistoryFilter.values().forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = currentFilter == filter,
                        onClick = { onFilterChange(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = HistoryFilter.values().size),
                        label = {
                            Text(
                                text = when (filter) {
                                    HistoryFilter.ALL -> "All"
                                    HistoryFilter.SERVICE -> "Service"
                                    HistoryFilter.FUEL -> "Fuel"
                                }
                            )
                        }
                    )
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history yet.\nTap + to add service or fuel logs.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items, key = { "${it.javaClass.simpleName}_${it.sortId}" }) { item ->
                        when (item) {
                            is HistoryItem.Service -> {
                                ServiceRecordItem(
                                    record = item.record,
                                    mileageUnit = mileageUnit,
                                    onEdit = { onEditRecord(item.record.id) },
                                    onDelete = { recordPendingDelete = item }
                                )
                            }
                            is HistoryItem.Fuel -> {
                                FuelLogHistoryItem(
                                    log = item.log,
                                    mileageUnit = mileageUnit,
                                    onEdit = { onEditFuel(item.log.id) },
                                    onDelete = { recordPendingDelete = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (recordPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("This will permanently remove this record. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = recordPendingDelete
                        if (toDelete is HistoryItem.Service) onDeleteService(toDelete.record)
                        else if (toDelete is HistoryItem.Fuel) onDeleteFuel(toDelete.log)
                        recordPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FuelLogHistoryItem(
    log: FuelLog,
    mileageUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(log.date) {
        Instant.ofEpochMilli(log.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            Icons.Rounded.LocalGasStation,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "Fuel Refill",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = formatCost(log.totalCost),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = dateString, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${log.odometer} $mileageUnit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Text(
                text = "${log.amount} L @ ${log.location ?: "Unknown Station"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun ServiceRecordItem(
    record: ServiceRecord,
    mileageUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(record.date) {
        Instant.ofEpochMilli(record.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                Icons.Rounded.Build,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = record.serviceType,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(if (record.isDiy) "DIY" else "Shop") },
                            icon = {
                                Icon(
                                    imageVector = if (record.isDiy) Icons.Rounded.Build else Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        if (record.serviceLocation != null && record.serviceLocation != "DIY") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record.serviceLocation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (record.mechanicName != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record.mechanicName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = formatCost(cost = record.cost),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${record.mileage} $mileageUnit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.partsUsed != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Parts: ${record.partsUsed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.notes.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
