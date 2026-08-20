package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        val title = if (selectedId == 0L) {
                            stringResource(R.string.fleet_insights)
                        } else {
                            stringResource(R.string.insights_title, selectedVehicle?.nickname ?: selectedVehicle?.model ?: "")
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.tap_to_switch),
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
                        Text(stringResource(R.string.total_spending), style = MaterialTheme.typography.labelMedium)
                        Text("$${"%.2f".format(uiState.totalCost)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.avg_efficiency), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.efficiency_unit_label, "%.1f".format(uiState.averageEfficiency)), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // 2. Smart Maintenance Predictions
            if (uiState.predictions.isNotEmpty()) {
                Text(stringResource(R.string.predicted_maintenance), style = MaterialTheme.typography.titleMedium)
                uiState.predictions.take(3).forEach { prediction ->
                    PredictionCard(prediction, uiState.odometerUnit)
                }
            }

            // 3. Efficiency Graph
            if (uiState.efficiencyHistory.isNotEmpty()) {
                val chartTitle = if (uiState.odometerUnit == "mi") stringResource(R.string.fuel_efficiency_mpg) else stringResource(R.string.fuel_efficiency_l100)
                ChartCard(title = chartTitle) {
                    EfficiencyLineChart(points = uiState.efficiencyHistory)
                }
            }

            // 3. Category Breakdown
            if (uiState.spendingByCategory.isNotEmpty()) {
                ChartCard(title = stringResource(R.string.spending_breakdown)) {
                    DonutChart(items = uiState.spendingByCategory)
                }
            }

            // 4. Monthly Spend
            if (uiState.monthlySpending.isNotEmpty()) {
                ChartCard(title = stringResource(R.string.monthly_spend)) {
                    SimpleBarChart(data = uiState.monthlySpending)
                }
            }

            if (uiState.totalCost == 0.0) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_data_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun PredictionCard(prediction: MaintenancePrediction, unit: String) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = prediction.descriptionResId?.let { stringResource(it) } ?: prediction.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (prediction.isAutoGenerated) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(10.dp))
                                Text(stringResource(R.string.smart_tag), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.expected_at, prediction.targetMileage.toString(), unit),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.in_days, prediction.daysRemaining),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = dateFormat.format(Date(prediction.predictedDate)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
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
        title = { Text(stringResource(R.string.switch_dashboard_view)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // "All Vehicles" Option
                ListItem(
                    headlineContent = { Text(stringResource(R.string.all_vehicles_global)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
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
