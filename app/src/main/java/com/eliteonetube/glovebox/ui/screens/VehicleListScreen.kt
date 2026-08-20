package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.ui.viewmodels.VehicleListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onSelectVehicle: (Long) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: VehicleListViewModel = viewModel()
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()

    var vehiclePendingDelete by rememberSaveable { mutableStateOf<Vehicle?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.garage)) },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_drawer))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.select_car))
            }
        }
    ) { innerPadding ->
        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_vehicles_added))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehicles) { vehicle ->
                    VehicleItem(
                        vehicle = vehicle,
                        isActive = vehicle.id == activeVehicleId,
                        onSelect = {
                            viewModel.setActiveVehicle(vehicle.id)
                            onSelectVehicle(vehicle.id)
                        },
                        onEdit = { onEditVehicle(vehicle.id) },
                        onDelete = { vehiclePendingDelete = vehicle }
                    )
                }
            }
        }
    }

    if (vehiclePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { vehiclePendingDelete = null },
            title = { Text(stringResource(R.string.delete_vehicle_title)) },
            text = { Text(stringResource(R.string.delete_vehicle_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vehiclePendingDelete?.let { viewModel.deleteVehicle(it) }
                        vehiclePendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { vehiclePendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun VehicleItem(
    vehicle: Vehicle,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (vehicle.photoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vehicle.photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = vehicle.nickname ?: "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (vehicle.nickname != null) {
                    Text(
                        text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${vehicle.odometer} ${vehicle.odometerUnit}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}