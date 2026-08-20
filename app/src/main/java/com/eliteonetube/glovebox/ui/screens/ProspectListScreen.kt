package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.data.entity.ProspectVehicle
import com.eliteonetube.glovebox.ui.viewmodels.ProspectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProspectListScreen(
    onAddProspect: () -> Unit,
    onViewProspect: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: ProspectViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return ProspectViewModel(app) as T
        }
    })
) {
    val prospects by viewModel.allProspects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buying Guide") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProspect) {
                Icon(Icons.Rounded.Add, contentDescription = "New Inquiry")
            }
        }
    ) { innerPadding ->
        if (prospects.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Text("Looking for a new car?", style = MaterialTheme.typography.titleLarge)
                    Text("Add a car you're interested in to start a checklist.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Button(onClick = onAddProspect) {
                        Text("Add New Inquiry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(prospects) { prospect ->
                    ProspectItem(
                        prospect = prospect,
                        onClick = { onViewProspect(prospect.id) },
                        onDelete = { viewModel.deleteProspect(prospect) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProspectItem(
    prospect: ProspectVehicle,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${prospect.year} ${prospect.make} ${prospect.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (prospect.askedPrice != null) {
                    Text("$${"%,.0f".format(prospect.askedPrice)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (prospect.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(prospect.location, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}
