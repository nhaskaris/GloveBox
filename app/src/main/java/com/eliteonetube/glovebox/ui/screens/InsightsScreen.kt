package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.ui.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    vehicleId: Long,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: InsightsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return InsightsViewModel(app, vehicleId) as T
        }
    })
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(modifier = Modifier.clickable { showPicker = true }) {
                        Text(
                            text = if (selectedId == 0L) "Fleet Insights" else "Insights: ${selectedVehicle?.nickname ?: selectedVehicle?.model}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap to switch vehicle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open Drawer")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (showPicker) {
                VehiclePicker(
                    vehicles = vehicles,
                    selectedId = selectedId,
                    onSelect = { 
                        viewModel.selectVehicle(it)
                        showPicker = false
                    },
                    onDismiss = { showPicker = false }
                )
            }

            // 1. Overview Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Spending", style = MaterialTheme.typography.labelMedium)
                        Text("$${"%.2f".format(uiState.totalCost)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg Efficiency", style = MaterialTheme.typography.labelMedium)
                        Text("${"%.1f".format(uiState.averageEfficiency)} L/100", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // 2. Efficiency Graph
            if (uiState.efficiencyHistory.isNotEmpty()) {
                ChartCard(title = "Fuel Efficiency (L/100km)") {
                    EfficiencyLineChart(points = uiState.efficiencyHistory)
                }
            }

            // 3. Category Breakdown
            if (uiState.spendingByCategory.isNotEmpty()) {
                ChartCard(title = "Spending Breakdown") {
                    DonutChart(items = uiState.spendingByCategory)
                }
            }

            // 4. Monthly Spend
            if (uiState.monthlySpending.isNotEmpty()) {
                ChartCard(title = "Monthly Spend") {
                    SimpleBarChart(data = uiState.monthlySpending)
                }
            }

            if (uiState.totalCost == 0.0) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No data yet. Log fuel or service to see insights.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun VehiclePicker(
    vehicles: List<com.eliteonetube.glovebox.data.entity.Vehicle>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Dashboard View") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // "All Vehicles" Option
                ListItem(
                    headlineContent = { Text("All Vehicles (Global View)") },
                    leadingContent = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { RadioButton(selected = selectedId == 0L, onClick = null) },
                    modifier = Modifier.clickable { onSelect(0L) }
                )
                HorizontalDivider()
                // Specific Car Options
                vehicles.forEach { car ->
                    ListItem(
                        headlineContent = { Text("${car.year} ${car.make} ${car.model}") },
                        supportingContent = { Text(car.nickname ?: "No nickname") },
                        trailingContent = { RadioButton(selected = selectedId == car.id, onClick = null) },
                        modifier = Modifier.clickable { onSelect(car.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                content()
            }
        }
    }
}

@Composable
fun EfficiencyLineChart(points: List<EfficiencyPoint>) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas
        
        val maxVal = points.maxOf { it.value }.toFloat().coerceAtLeast(1f)
        val minVal = points.minOf { it.value }.toFloat().coerceAtMost(maxVal - 1)
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = height - ((point.value.toFloat() - minVal) / range * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, primaryColor, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun DonutChart(items: List<CategorySpending>) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    val total = items.sumOf { it.amount }

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        items.forEachIndexed { index, item ->
            val sweepAngle = (item.amount / total * 360).toFloat()
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun SimpleBarChart(data: List<MonthlySpending>) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxVal = data.maxOf { it.amount }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidth = (width / data.size) * 0.7f
        val gap = (width / data.size) * 0.3f

        data.forEachIndexed { index, item ->
            val barHeight = (item.amount.toFloat() / maxVal) * height
            drawRect(
                color = barColor,
                topLeft = Offset(index * (barWidth + gap), height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}
