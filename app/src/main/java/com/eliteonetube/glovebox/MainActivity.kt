package com.eliteonetube.glovebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.eliteonetube.glovebox.navigation.GloveboxRoute
import com.eliteonetube.glovebox.ui.screens.AddServiceLogScreen
import com.eliteonetube.glovebox.ui.screens.RemindersScreen
import com.eliteonetube.glovebox.ui.screens.ServiceHistoryScreen
import com.eliteonetube.glovebox.ui.screens.VehicleProfileScreen
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GloveboxTheme {
                GloveboxApp()
            }
        }
    }
}

@Composable
fun GloveboxApp() {
    val backStack = remember { mutableStateListOf<GloveboxRoute>(GloveboxRoute.VehicleProfile) }
    val currentRoute = backStack.lastOrNull() ?: GloveboxRoute.VehicleProfile

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = currentRoute is GloveboxRoute.VehicleProfile,
                onClick = {
                    if (currentRoute !is GloveboxRoute.VehicleProfile) {
                        backStack.clear()
                        backStack.add(GloveboxRoute.VehicleProfile)
                    }
                },
                icon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
                label = { Text("Profile") }
            )
            item(
                selected = currentRoute is GloveboxRoute.ServiceHistory,
                onClick = {
                    if (currentRoute !is GloveboxRoute.ServiceHistory) {
                        backStack.clear()
                        // For simplicity in the shell, we'll assume vehicleId 1
                        backStack.add(GloveboxRoute.ServiceHistory(1))
                    }
                },
                icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                label = { Text("History") }
            )
            item(
                selected = currentRoute is GloveboxRoute.Reminders,
                onClick = {
                    if (currentRoute !is GloveboxRoute.Reminders) {
                        backStack.clear()
                        backStack.add(GloveboxRoute.Reminders)
                    }
                },
                icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                label = { Text("Reminders") }
            )
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is GloveboxRoute.VehicleProfile -> NavEntry(key) {
                        VehicleProfileScreen()
                    }

                    is GloveboxRoute.ServiceHistory -> NavEntry(key) {
                        ServiceHistoryScreen(
                            vehicleId = key.vehicleId,
                            onAddRecord = { backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId)) },
                            onEditRecord = { recordId -> backStack.add(GloveboxRoute.AddServiceLog(key.vehicleId, recordId)) }
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
                        RemindersScreen()
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GloveboxAppPreview() {
    GloveboxTheme {
        GloveboxApp()
    }
}
