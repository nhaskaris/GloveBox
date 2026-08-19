package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.data.entity.VehicleDocument
import com.eliteonetube.glovebox.ui.viewmodels.DocumentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalGloveboxScreen(
    vehicleId: Long,
    onAddDocument: () -> Unit,
    onViewDocument: (Long) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: DocumentsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return DocumentsViewModel(app, vehicleId) as T
        }
    })
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vehicleDao = remember { com.eliteonetube.glovebox.data.GloveboxDatabase.getDatabase(context).vehicleDao() }
    val vehicle by produceState<com.eliteonetube.glovebox.data.entity.Vehicle?>(null) {
        if (vehicleId != 0L) {
            value = vehicleDao.getVehicleById(vehicleId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vehicle != null) "Glovebox: ${vehicle?.nickname ?: vehicle?.model}" else "Digital Glovebox") },
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
            FloatingActionButton(onClick = onAddDocument) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Document")
            }
        }
    ) { innerPadding ->
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No documents stored. Tap + to scan your insurance or registration.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(documents) { doc ->
                    DocumentItem(
                        document = doc,
                        onClick = { onViewDocument(doc.id) },
                        onDelete = { viewModel.deleteDocument(doc) }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentItem(
    document: VehicleDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(document.photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = document.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = document.name, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (document.vehicleId == null) {
                            Icon(
                                Icons.Rounded.Public,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(text = document.category, style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
