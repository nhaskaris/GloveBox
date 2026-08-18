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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.ui.viewmodels.VehicleListViewModel

import androidx.compose.material.icons.rounded.Menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: VehicleListViewModel = viewModel()
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("My Vehicles") },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Vehicle")
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
                Text("No vehicles added yet.")
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
                        onSelect = { viewModel.setActiveVehicle(vehicle.id) },
                        onEdit = { onEditVehicle(vehicle.id) },
                        onDelete = { viewModel.deleteVehicle(vehicle) }
                    )
                }
            }
        }
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
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${vehicle.odometer} km",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete")
            }
        }
    }
}
