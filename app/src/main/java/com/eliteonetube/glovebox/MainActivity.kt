package com.eliteonetube.glovebox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowWidthSizeClass
import com.eliteonetube.glovebox.navigation.GloveboxRoute
import com.eliteonetube.glovebox.ui.screens.AddServiceLogScreen
import com.eliteonetube.glovebox.ui.screens.OnboardingScreen
import com.eliteonetube.glovebox.ui.screens.AddDocumentScreen
import com.eliteonetube.glovebox.ui.screens.AddFuelLogScreen
import com.eliteonetube.glovebox.ui.screens.DigitalGloveboxScreen
import com.eliteonetube.glovebox.ui.screens.HistoryScreen
import com.eliteonetube.glovebox.ui.screens.InsightsScreen
import com.eliteonetube.glovebox.ui.screens.InsightsScreen
import com.eliteonetube.glovebox.ui.screens.RemindersScreen
import com.eliteonetube.glovebox.ui.screens.SettingsScreen
import com.eliteonetube.glovebox.ui.screens.VehicleListScreen
import com.eliteonetube.glovebox.ui.screens.VehicleProfileScreen
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme
import com.eliteonetube.glovebox.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        com.eliteonetube.glovebox.util.NotificationHelper.createNotificationChannel(this)

        setContent {
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

            val viewModel: MainViewModel = viewModel()
            val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()

            GloveboxTheme(themePreference = themePreference) {
                GloveboxApp(viewModel)
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: GloveboxRoute,
    val onClick: () -> Unit
)

@Composable
fun GloveboxApp(viewModel: MainViewModel) {
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<GloveboxRoute>() }
    
    // Auto-navigate when onboarding state changes or initialized
    LaunchedEffect(isOnboardingCompleted) {
        if (isOnboardingCompleted != null && backStack.isEmpty()) {
            if (isOnboardingCompleted == false) {
                backStack.add(GloveboxRoute.Onboarding)
            } else {
                backStack.add(GloveboxRoute.VehicleList)
            }
        } else if (isOnboardingCompleted == true && backStack.any { it is GloveboxRoute.Onboarding }) {
            backStack.clear()
            backStack.add(GloveboxRoute.VehicleList)
        }
    }

    if (isOnboardingCompleted == null || backStack.isEmpty()) {
        // Show a splash or loading state while preferences load
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentRoute = backStack.lastOrNull() ?: GloveboxRoute.VehicleList

    val effectiveVehicleId = activeVehicleId?.takeIf { it != 0L } ?: vehicles.firstOrNull()?.id ?: 0L

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Safely pop elements off the backStack
    val safePopBackStack: () -> Unit = remember(backStack) {
        {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }

    // Intercept hardware/gesture system back actions
    BackHandler(enabled = backStack.size > 1) {
        safePopBackStack()
    }

    val navigationItems = listOf(
        NavigationItem(
            label = "Garage",
            icon = Icons.Rounded.DirectionsCar,
            route = GloveboxRoute.VehicleList,
            onClick = {
                backStack.clear()
                backStack.add(GloveboxRoute.VehicleList)
            }
        ),
        NavigationItem(
            label = "Reminders",
            icon = Icons.Rounded.Notifications,
            route = GloveboxRoute.Reminders(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    backStack.clear()
                    backStack.add(GloveboxRoute.Reminders(effectiveVehicleId))
                }
            }
        ),
        NavigationItem(
            label = "Insights",
            icon = Icons.Rounded.BarChart,
            route = GloveboxRoute.Insights(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    backStack.clear()
                    backStack.add(GloveboxRoute.Insights(effectiveVehicleId))
                }
            }
        ),
        NavigationItem(
            label = "Digital Glovebox",
            icon = Icons.Rounded.Folder,
            route = GloveboxRoute.DigitalGlovebox(0L),
            onClick = {
                backStack.clear()
                backStack.add(GloveboxRoute.DigitalGlovebox(0L))
            }
        ),
        NavigationItem(
            label = "Settings",
            icon = Icons.Rounded.Settings,
            route = GloveboxRoute.Settings,
            onClick = {
                backStack.clear()
                backStack.add(GloveboxRoute.Settings)
            }
        )
    )

    @Composable
    fun DrawerSheetContent(onItemClick: (NavigationItem) -> Unit) {
        Spacer(Modifier.height(12.dp))
        navigationItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = when (item.route) {
                    is GloveboxRoute.VehicleList -> currentRoute is GloveboxRoute.VehicleList
                    is GloveboxRoute.Reminders -> currentRoute is GloveboxRoute.Reminders
                    is GloveboxRoute.Insights -> currentRoute is GloveboxRoute.Insights
                    is GloveboxRoute.DigitalGlovebox -> currentRoute is GloveboxRoute.DigitalGlovebox
                    is GloveboxRoute.Settings -> currentRoute is GloveboxRoute.Settings
                    else -> false
                },
                onClick = { onItemClick(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    if (isExpanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                    DrawerSheetContent(onItemClick = { item -> item.onClick() })
                }
            }
        ) {
            MainContent(
                backStack = backStack,
                onNavigateBack = safePopBackStack,
                onOpenDrawer = null,
                onOnboardingComplete = viewModel::setOnboardingCompleted
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerSheetContent(onItemClick = { item ->
                        item.onClick()
                        scope.launch { drawerState.close() }
                    })
                }
            }
        ) {
            MainContent(
                backStack = backStack,
                onNavigateBack = safePopBackStack,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOnboardingComplete = viewModel::setOnboardingCompleted
            )
        }
    }
}

@Composable
fun MainContent(
    backStack: MutableList<GloveboxRoute>,
    onNavigateBack: () -> Unit,
    onOpenDrawer: (() -> Unit)?,
    onOnboardingComplete: () -> Unit
) {
    NavDisplay(
        backStack = backStack,
        onBack = onNavigateBack,
        entryProvider = { key ->
            when (key) {
                is GloveboxRoute.Onboarding -> NavEntry(key) {
                    OnboardingScreen(
                        onComplete = onOnboardingComplete
                    )
                }

                is GloveboxRoute.VehicleList -> NavEntry(key) {
                    VehicleListScreen(
                        onAddVehicle = { backStack.add(GloveboxRoute.VehicleProfile(0L)) },
                        onEditVehicle = { id -> backStack.add(GloveboxRoute.VehicleProfile(id)) },
                        onSelectVehicle = { id -> backStack.add(GloveboxRoute.History(id)) },
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
                        onEditRecord = { recordId ->
                            backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId, recordId))
                        },
                        onAddFuel = { backStack.add(GloveboxRoute.AddFuelLog(key.vehicleId)) },
                        onEditFuel = { logId ->
                            backStack.add(GloveboxRoute.AddFuelLog(key.vehicleId, logId))
                        },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.AddServiceLog -> NavEntry(key) {
                    AddServiceLogScreen(
                        vehicleId = key.vehicleId,
                        recordId = key.recordId,
                        onNavigateBack = onNavigateBack
                    )
                }

                is GloveboxRoute.Reminders -> NavEntry(key) {
                    RemindersScreen(
                        vehicleId = key.vehicleId,
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
                        onViewDocument = { docId: Long -> /* Handle view or edit */ },
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

                is GloveboxRoute.Settings -> NavEntry(key) {
                    SettingsScreen(onOpenDrawer = onOpenDrawer)
                }
            }
        }
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun GloveboxAppCompactPreview() {
    GloveboxTheme {
        GloveboxApp(viewModel())
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun GloveboxAppExpandedPreview() {
    GloveboxTheme {
        GloveboxApp(viewModel())
    }
}