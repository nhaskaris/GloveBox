package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.data.entity.Reminder
import com.eliteonetube.glovebox.ui.viewmodels.RemindersViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.eliteonetube.glovebox.ui.theme.GloveboxTheme

@Preview(showBackground = true)
@Composable
fun RemindersScreenPreview() {
    GloveboxTheme {
        RemindersContent(
            reminders = listOf(
                Reminder(1, 1, "Oil Change", 10000, null, false),
                Reminder(2, 1, "Tire Rotation", null, System.currentTimeMillis() + 86400000, false)
            ),
            currentOdometer = 5000,
            odometerUnit = "km",
            onToggleCompletion = {},
            onDelete = {},
            onAddReminder = { _, _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    vehicleId: Long,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: RemindersViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return RemindersViewModel(app, vehicleId) as T
        }
    })
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val currentOdometer by viewModel.currentOdometer.collectAsStateWithLifecycle()
    val odometerUnit by viewModel.odometerUnit.collectAsStateWithLifecycle()

    RemindersContent(
        reminders = reminders,
        currentOdometer = currentOdometer,
        odometerUnit = odometerUnit,
        onToggleCompletion = { viewModel.toggleReminderCompletion(it) },
        onDelete = { viewModel.deleteReminder(it) },
        onAddReminder = { desc, mileage, date -> viewModel.addReminder(desc, mileage, date) },
        onOpenDrawer = onOpenDrawer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersContent(
    reminders: List<Reminder>,
    currentOdometer: Int,
    odometerUnit: String,
    onToggleCompletion: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onAddReminder: (String, Int?, Long?) -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Reminder")
            }
        }
    ) { innerPadding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reminders yet.\nTap + to set a maintenance reminder.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        currentOdometer = currentOdometer,
                        odometerUnit = odometerUnit,
                        onToggleCompletion = { onToggleCompletion(reminder) },
                        onDelete = { onDelete(reminder) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddReminderDialog(
                odometerUnit = odometerUnit,
                onDismiss = { showAddDialog = false },
                onConfirm = { desc, mileage, date ->
                    onAddReminder(desc, mileage, date)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    currentOdometer: Int,
    odometerUnit: String,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val isDue = remember(reminder, currentOdometer) {
        val mileageDue = reminder.targetMileage?.let { it <= currentOdometer } ?: false
        val dateDue = reminder.targetDate?.let { it <= System.currentTimeMillis() } ?: false
        (mileageDue || dateDue) && !reminder.isCompleted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDue) MaterialTheme.colorScheme.errorContainer 
                            else if (reminder.isCompleted) MaterialTheme.colorScheme.surfaceContainerLow
                            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onToggleCompletion,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Complete",
                    tint = if (reminder.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (reminder.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (reminder.targetMileage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${reminder.targetMileage} $odometerUnit",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (reminder.targetDate != null) {
                        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
                        val dateString = Instant.ofEpochMilli(reminder.targetDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(dateFormatter)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (isDue) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = " DUE NOW ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    odometerUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, Long?) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var useDate by remember { mutableStateOf(false) }
    // Date would normally use a DatePicker, but for brevity we'll mock it or use current + some time
    val mockDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // +1 week

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Maintenance Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Tire Rotation") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mileage,
                    onValueChange = { if (it.all { c -> c.isDigit() }) mileage = it },
                    label = { Text("Target $odometerUnit (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useDate, onCheckedChange = { useDate = it })
                    Text("Remind by date (next week)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        description,
                        mileage.toIntOrNull(),
                        if (useDate) mockDate else null
                    )
                },
                enabled = description.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
