package com.eliteonetube.glovebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
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

    val backStack = remember { mutableStateListOf<GloveboxRoute>(GloveboxRoute.VehicleList) }
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
            label = "Vehicles",
            icon = Icons.AutoMirrored.Rounded.List,
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
                onOpenDrawer = null
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
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}

@Composable
fun MainContent(
    backStack: MutableList<GloveboxRoute>,
    onNavigateBack: () -> Unit,
    onOpenDrawer: (() -> Unit)?
) {
    NavDisplay(
        backStack = backStack,
        onBack = onNavigateBack,
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
                        onNavigateBack = onNavigateBack
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
                        onNavigateBack = onNavigateBack
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