package com.eliteonetube.glovebox.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.alerts.AlertSeverity
import com.eliteonetube.glovebox.data.alerts.VehicleAlert
import com.eliteonetube.glovebox.ui.viewmodels.home.DashboardActivity
import com.eliteonetube.glovebox.ui.viewmodels.home.HomeDashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToAddFuel: (Long) -> Unit,
    onNavigateToAddService: (Long) -> Unit,
    onNavigateToAddDocument: (Long) -> Unit,
    onNavigateToGarage: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: HomeDashboardViewModel = viewModel()
) {
    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!state.hasVehicles) {
            EmptyDashboard(onNavigateToGarage)
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Urgent Alerts Section
                if (state.alerts.isNotEmpty()) {
                    AlertsSection(
                        alerts = state.alerts,
                        onAlertClick = { alert ->
                            when (alert) {
                                is VehicleAlert.DocumentExpiring -> onNavigateToAddDocument(alert.vehicleId)
                                is VehicleAlert.ServiceDue -> onNavigateToAddService(alert.vehicleId)
                            }
                        },
                        onViewAll = { /* Future: alert list */ }
                    )
                }

                // 2. Active Vehicle Summary Card
                state.activeVehicle?.let { vehicle ->
                    VehicleSummaryCard(
                        vehicle = vehicle,
                        hasCriticalAlerts = state.alerts.any { it.vehicleId == vehicle.id && it.severity == AlertSeverity.CRITICAL },
                        onClick = { onNavigateToHistory(vehicle.id) }
                    )
                }

                // 3. Quick Action Bar
                QuickActionsSection(
                    activeVehicleId = state.activeVehicle?.id ?: 0L,
                    onLogFuel = onNavigateToAddFuel,
                    onLogService = onNavigateToAddService
                )

                // 4. Recent Activity Section
                if (state.recentActivity.isNotEmpty()) {
                    RecentActivitySection(
                        activities = state.recentActivity,
                        onActivityClick = { onNavigateToHistory(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertsSection(alerts: List<VehicleAlert>, onAlertClick: (VehicleAlert) -> Unit, onViewAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Priority Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        alerts.take(3).forEach { alert ->
            val containerColor = when (alert.severity) {
                AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                AlertSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                AlertSeverity.INFO -> MaterialTheme.colorScheme.secondaryContainer
            }
            
            val contentColor = when (alert.severity) {
                AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                AlertSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                AlertSeverity.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
            }

            Card(
                onClick = { onAlertClick(alert) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (alert.severity == AlertSeverity.CRITICAL) Icons.Rounded.ReportProblem else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.vehicleName,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = contentColor.copy(alpha = 0.5f))
                }
            }
        }
        
        if (alerts.size > 3) {
            TextButton(
                onClick = onViewAll,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("View all (${alerts.size})")
            }
        }
    }
}

@Composable
fun VehicleSummaryCard(
    vehicle: com.eliteonetube.glovebox.data.entity.Vehicle,
    hasCriticalAlerts: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            if (vehicle.photoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vehicle.photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                }
            }

            // Overlay for info
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = vehicle.nickname ?: "${vehicle.make} ${vehicle.model}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${vehicle.odometer} ${vehicle.odometerUnit}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Surface(
                        color = if (hasCriticalAlerts) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        shape = CircleShape,
                        modifier = Modifier.size(12.dp)
                    ) {}
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    activeVehicleId: Long,
    onLogFuel: (Long) -> Unit,
    onLogService: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickActionButton(
            label = "Log Fuel",
            icon = Icons.Rounded.LocalGasStation,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
            onClick = { onLogFuel(activeVehicleId) }
        )
        QuickActionButton(
            label = "Add Service",
            icon = Icons.Rounded.Build,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            onClick = { onLogService(activeVehicleId) }
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RecentActivitySection(activities: List<DashboardActivity>, onActivityClick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        activities.forEach { activity ->
            val icon = when (activity) {
                is DashboardActivity.Fuel -> Icons.Rounded.LocalGasStation
                is DashboardActivity.Service -> Icons.Rounded.Build
            }
            
            val title = when (activity) {
                is DashboardActivity.Fuel -> "Refilled Fuel"
                is DashboardActivity.Service -> activity.record.serviceType
            }
            
            val details = when (activity) {
                is DashboardActivity.Fuel -> "${activity.log.amount} L @ ${activity.log.location ?: "Station"}"
                is DashboardActivity.Service -> activity.record.serviceLocation ?: "Maintenance"
            }

            val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(activity.date))

            ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("${activity.vehicleName} • $details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                trailingContent = {
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onActivityClick(activity.vehicleId) }
            )
        }
    }
}

@Composable
fun EmptyDashboard(onNavigateToGarage: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Welcome to Glovebox!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Add your first vehicle to see maintenance alerts and spend tracking on this dashboard.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onNavigateToGarage) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Go to Garage")
            }
        }
    }
}
