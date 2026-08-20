package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChecklistCategory(val title: String, val items: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrePurchaseChecklistScreen(
    onOpenDrawer: () -> Unit
) {
    val categories = remember {
        listOf(
            ChecklistCategory(
                "Under the Hood",
                listOf(
                    "Check oil level and color (no milky residue)",
                    "Look for fluid leaks (coolant, brake fluid, oil)",
                    "Inspect battery terminals for corrosion",
                    "Check belt condition (no cracks or fraying)",
                    "Check coolant level and condition"
                )
            ),
            ChecklistCategory(
                "Exterior",
                listOf(
                    "Check for panel gaps (indicates past accidents)",
                    "Inspect paint for bubbles or mismatched colors",
                    "Check tire tread depth and even wear",
                    "Look for rust on wheel arches and sills",
                    "Test all lights (headlights, signals, brake lights)"
                )
            ),
            ChecklistCategory(
                "Interior",
                listOf(
                    "Check upholstery for tears or heavy wear",
                    "Test the air conditioning and heater",
                    "Ensure all window switches and door locks work",
                    "Check the dashboard for warning lights",
                    "Test the infotainment system and speakers"
                )
            ),
            ChecklistCategory(
                "The Test Drive",
                listOf(
                    "Listen for engine knocking or rattling",
                    "Check for vibration in the steering wheel",
                    "Ensure brakes don't squeal or feel spongy",
                    "Test all gears (smooth shifting)",
                    "Check if the car pulls to one side"
                )
            ),
            ChecklistCategory(
                "Paperwork",
                listOf(
                    "Verify the VIN matches the Title/Logbook",
                    "Check the service history records",
                    "Confirm the current owner on the registration",
                    "Ask for latest inspection/emission certificates",
                    "Inquire about remaining warranty"
                )
            )
        )
    }

    // Use a Set of strings to track checked items - much safer for rememberSaveable
    var checkedItems by rememberSaveable { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Purchase Checklist") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Use this list as a guide when inspecting a potential new vehicle. Don't be afraid to take your time and ask questions!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            categories.forEach { category ->
                item {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(category.items) { itemText ->
                    val isChecked = checkedItems.contains(itemText)
                    
                    Surface(
                        onClick = {
                            checkedItems = if (isChecked) {
                                checkedItems - itemText
                            } else {
                                checkedItems + itemText
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    checkedItems = if (checked) {
                                        checkedItems + itemText
                                    } else {
                                        checkedItems - itemText
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = itemText,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
