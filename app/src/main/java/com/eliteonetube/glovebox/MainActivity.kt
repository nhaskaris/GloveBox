package com.eliteonetube.glovebox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.eliteonetube.glovebox.navigation.GloveboxRoute
import com.eliteonetube.glovebox.ui.screens.*
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme
import com.eliteonetube.glovebox.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GloveboxTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel = viewModel()) {
    val backStack = rememberNavBackStack(GloveboxRoute.VehicleList)
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.windowWidthSizeClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(isOnboardingCompleted) {
        if (isOnboardingCompleted == false) {
            backStack.add(GloveboxRoute.Onboarding)
        }
    }

    if (isOnboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentRoute = backStack.last() as GloveboxRoute
    val effectiveVehicleId = activeVehicleId ?: 0L
    val hasVehicles = vehicles.isNotEmpty()

    val navigationItems = listOfNotNull(
        NavigationItem(
            label = "Garage",
            icon = Icons.Rounded.DirectionsCar,
            route = GloveboxRoute.VehicleList,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.VehicleList)
            }
        ),
        if (hasVehicles) NavigationItem(
            label = "History",
            icon = Icons.Rounded.History,
            route = GloveboxRoute.History(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.History(effectiveVehicleId))
                }
            }
        ) else null,
        if (hasVehicles) NavigationItem(
            label = "Reminders",
            icon = Icons.Rounded.Notifications,
            route = GloveboxRoute.Reminders(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.Reminders(effectiveVehicleId))
                }
            }
        ) else null,
        if (hasVehicles) NavigationItem(
            label = "Insights",
            icon = Icons.Rounded.BarChart,
            route = GloveboxRoute.Insights(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.Insights(effectiveVehicleId))
                }
            }
        ) else null,
        if (hasVehicles) NavigationItem(
            label = "Digital Glovebox",
            icon = Icons.Rounded.Folder,
            route = GloveboxRoute.DigitalGlovebox(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    backStack.add(GloveboxRoute.DigitalGlovebox(effectiveVehicleId))
                }
            }
        ) else null,
        NavigationItem(
            label = "Buy Checklist",
            icon = Icons.Rounded.Checklist,
            route = GloveboxRoute.BuyChecklist,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.BuyChecklist)
            }
        ),
        NavigationItem(
            label = "Settings",
            icon = Icons.Rounded.Settings,
            route = GloveboxRoute.Settings,
            onClick = {
                scope.launch { drawerState.close() }
                backStack.clear()
                backStack.add(GloveboxRoute.Settings)
            }
        )
    )

    val onOpenDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val onNavigateBack: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }

    NavigationWrapper(
        isExpanded = isExpanded,
        drawerState = drawerState,
        navigationItems = navigationItems,
        currentRoute = currentRoute
    ) {
        NavDisplay(
            backStack = backStack
        ) { key ->
            when (key) {
                is GloveboxRoute.Onboarding -> NavEntry(key) {
                    OnboardingScreen(onComplete = {
                        viewModel.setOnboardingCompleted()
                        backStack.removeAt(backStack.size - 1)
                    })
                }

                is GloveboxRoute.VehicleList -> NavEntry(key) {
                    VehicleListScreen(
                        onSelectVehicle = { id ->
                            viewModel.setActiveVehicleId(id)
                            backStack.add(GloveboxRoute.History(id))
                        },
                        onAddVehicle = { backStack.add(GloveboxRoute.VehicleProfile()) },
                        onEditVehicle = { id -> backStack.add(GloveboxRoute.VehicleProfile(id)) },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.VehicleProfile -> NavEntry(key) {
                    VehicleProfileScreen(
                        vehicleId = key.vehicleId,
                        onNavigateBack = onNavigateBack
                    )
                }

                is GloveboxRoute.History -> NavEntry(key) {
                    HistoryScreen(
                        vehicleId = key.vehicleId,
                        onAddRecord = { backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId)) },
                        onEditRecord = { id -> backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId, id)) },
                        onAddFuel = { backStack.add(GloveboxRoute.AddFuelLog(key.vehicleId)) },
                        onEditFuel = { id -> backStack.add(GloveboxRoute.AddFuelLog(key.vehicleId, id)) },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.AddServiceLog -> NavEntry(key) {
                    AddServiceLogScreen(
                        vehicleId = key.vehicleId,
                        recordId = key.recordId,
                        prefilledType = key.prefilledType,
                        onNavigateBack = onNavigateBack
                    )
                }

                is GloveboxRoute.Reminders -> NavEntry(key) {
                    RemindersScreen(
                        vehicleId = key.vehicleId,
                        onLogService = { type ->
                            backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId, prefilledType = type))
                        },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.Insights -> NavEntry(key) {
                    InsightsScreen(
                        vehicleId = key.vehicleId,
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.AddFuelLog -> NavEntry(key) {
                    AddFuelLogScreen(
                        vehicleId = key.vehicleId,
                        logId = key.logId,
                        onNavigateBack = onNavigateBack
                    )
                }

                is GloveboxRoute.DigitalGlovebox -> NavEntry(key) {
                    DigitalGloveboxScreen(
                        vehicleId = key.vehicleId,
                        onAddDocument = { backStack.add(GloveboxRoute.AddDocument(key.vehicleId)) },
                        onViewDocument = { id -> backStack.add(GloveboxRoute.AddDocument(key.vehicleId, id)) },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.AddDocument -> NavEntry(key) {
                    AddDocumentScreen(
                        vehicleId = key.vehicleId,
                        docId = key.docId,
                        onNavigateBack = onNavigateBack
                    )
                }

                is GloveboxRoute.BuyChecklist -> NavEntry(key) {
                    PrePurchaseChecklistScreen(
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.Settings -> NavEntry(key) {
                    SettingsScreen(onOpenDrawer = onOpenDrawer)
                }
                
                else -> error("Unknown route")
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.size - 1)
    }
}

@Composable
fun NavigationWrapper(
    isExpanded: Boolean,
    drawerState: DrawerState,
    navigationItems: List<NavigationItem>,
    currentRoute: GloveboxRoute,
    content: @Composable () -> Unit
) {
    if (isExpanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                    Spacer(Modifier.height(12.dp))
                    navigationItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            selected = item.isRouteSelected(currentRoute),
                            onClick = item.onClick,
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
            content = content
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    navigationItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            selected = item.isRouteSelected(currentRoute),
                            onClick = item.onClick,
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
            content = content
        )
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: GloveboxRoute,
    val onClick: () -> Unit
) {
    fun isRouteSelected(currentRoute: GloveboxRoute): Boolean {
        return when (route) {
            is GloveboxRoute.VehicleList -> currentRoute is GloveboxRoute.VehicleList || currentRoute is GloveboxRoute.VehicleProfile
            is GloveboxRoute.History -> currentRoute is GloveboxRoute.History || currentRoute is GloveboxRoute.AddServiceLog || currentRoute is GloveboxRoute.AddFuelLog
            is GloveboxRoute.Reminders -> currentRoute is GloveboxRoute.Reminders
            is GloveboxRoute.Insights -> currentRoute is GloveboxRoute.Insights
            is GloveboxRoute.DigitalGlovebox -> currentRoute is GloveboxRoute.DigitalGlovebox || currentRoute is GloveboxRoute.AddDocument
            is GloveboxRoute.BuyChecklist -> currentRoute is GloveboxRoute.BuyChecklist
            is GloveboxRoute.Settings -> currentRoute is GloveboxRoute.Settings
            else -> false
        }
    }
}
