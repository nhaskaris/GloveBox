package com.eliteonetube.glovebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowWidthSizeClass
import com.eliteonetube.glovebox.navigation.GloveboxRoute
import com.eliteonetube.glovebox.ui.screens.AddServiceLogScreen
import com.eliteonetube.glovebox.ui.screens.RemindersScreen
import com.eliteonetube.glovebox.ui.screens.ServiceHistoryScreen
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
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()

            GloveboxTheme(themePreference = themePreference) {
                GloveboxApp(viewModel)
            }
        }
    }
}

@Composable
fun GloveboxApp(viewModel: MainViewModel) {
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<GloveboxRoute>(GloveboxRoute.VehicleList) }
    val currentRoute = backStack.lastOrNull() ?: GloveboxRoute.VehicleList

    val effectiveVehicleId = activeVehicleId?.takeIf { it != 0L } ?: vehicles.firstOrNull()?.id ?: 0L

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navigationItems = listOf(
        NavigationItem(
            label = "Vehicles",
            icon = Icons.AutoMirrored.Rounded.List,
            route = GloveboxRoute.VehicleList,
            onClick = {
                backStack.clear()
                backStack.add(GloveboxRoute.VehicleList)
            }
        ),
        NavigationItem(
            label = "History",
            icon = Icons.Rounded.History,
            route = GloveboxRoute.ServiceHistory(effectiveVehicleId),
            onClick = {
                if (effectiveVehicleId != 0L) {
                    backStack.clear()
                    backStack.add(GloveboxRoute.ServiceHistory(effectiveVehicleId))
                }
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
            label = "Settings",
            icon = Icons.Rounded.Settings,
            route = GloveboxRoute.Settings,
            onClick = {
                backStack.clear()
                backStack.add(GloveboxRoute.Settings)
            }
        )
    )

    if (isExpanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                    Spacer(Modifier.height(12.dp))
                    navigationItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            selected = when (item.route) {
                                is GloveboxRoute.VehicleList -> currentRoute is GloveboxRoute.VehicleList
                                is GloveboxRoute.ServiceHistory -> currentRoute is GloveboxRoute.ServiceHistory
                                is GloveboxRoute.Reminders -> currentRoute is GloveboxRoute.Reminders
                                is GloveboxRoute.Settings -> currentRoute is GloveboxRoute.Settings
                                else -> false
                            },
                            onClick = item.onClick,
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            MainContent(backStack, onOpenDrawer = null)
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    navigationItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            selected = when (item.route) {
                                is GloveboxRoute.VehicleList -> currentRoute is GloveboxRoute.VehicleList
                                is GloveboxRoute.ServiceHistory -> currentRoute is GloveboxRoute.ServiceHistory
                                is GloveboxRoute.Reminders -> currentRoute is GloveboxRoute.Reminders
                                is GloveboxRoute.Settings -> currentRoute is GloveboxRoute.Settings
                                else -> false
                            },
                            onClick = {
                                item.onClick()
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            MainContent(
                backStack = backStack,
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: GloveboxRoute,
    val onClick: () -> Unit
)

@Composable
fun MainContent(
    backStack: MutableList<GloveboxRoute>,
    onOpenDrawer: (() -> Unit)?
) {
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is GloveboxRoute.VehicleList -> NavEntry(key) {
                    VehicleListScreen(
                        onAddVehicle = { backStack.add(GloveboxRoute.VehicleProfile(0L)) },
                        onEditVehicle = { id -> backStack.add(GloveboxRoute.VehicleProfile(id)) },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.VehicleProfile -> NavEntry(key) {
                    VehicleProfileScreen(
                        vehicleId = key.vehicleId,
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }

                is GloveboxRoute.ServiceHistory -> NavEntry(key) {
                    ServiceHistoryScreen(
                        vehicleId = key.vehicleId,
                        onAddRecord = { backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId)) },
                        onEditRecord = { recordId ->
                            backStack.add(
                                GloveboxRoute.AddServiceLog(
                                    key.vehicleId,
                                    recordId
                                )
                            )
                        },
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.AddServiceLog -> NavEntry(key) {
                    AddServiceLogScreen(
                        vehicleId = key.vehicleId,
                        recordId = key.recordId,
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }

                is GloveboxRoute.Reminders -> NavEntry(key) {
                    RemindersScreen(
                        vehicleId = key.vehicleId,
                        onOpenDrawer = onOpenDrawer
                    )
                }

                is GloveboxRoute.Settings -> NavEntry(key) {
                    SettingsScreen(onOpenDrawer = onOpenDrawer)
                }
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun GloveboxAppCompactPreview() {
    GloveboxTheme {
        GloveboxApp(viewModel())
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun GloveboxAppExpandedPreview() {
    GloveboxTheme {
        GloveboxApp(viewModel())
    }
}
