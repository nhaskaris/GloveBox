package com.eliteonetube.glovebox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.entity.ProspectVehicle
import com.eliteonetube.glovebox.ui.viewmodels.ProspectViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProspectComparisonScreen(
    prospectIds: List<Long>,
    onNavigateBack: () -> Unit,
    viewModel: ProspectViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = com.eliteonetube.glovebox.GloveboxApplication.instance
            return ProspectViewModel(app) as T
        }
    })
) {
    val allProspects by viewModel.allProspects.collectAsStateWithLifecycle()
    val prospectsToCompare = remember(allProspects, prospectIds) {
        allProspects.filter { it.id in prospectIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comparison)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (prospectsToCompare.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_prospects_to_compare))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                ComparisonContent(prospects = prospectsToCompare)
            }
        }
    }
}

@Composable
fun ComparisonContent(prospects: List<ProspectVehicle>) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    val labelWidth = 100.dp
    val columnWidth = 180.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        // Headers Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // Spacer for the label column
            Spacer(modifier = Modifier.width(labelWidth + 32.dp)) 
            
            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScrollState)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                prospects.forEach { prospect ->
                    ComparisonCard(prospect = prospect, width = columnWidth)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        // Attribute Rows
        val attributes = listOf(
            ComparisonAttribute(stringResource(R.string.price)) { p -> p.askedPrice?.let { "$${"%,.0f".format(it)}" } ?: "-" },
            ComparisonAttribute(stringResource(R.string.year)) { p -> p.year.toString() },
            ComparisonAttribute(stringResource(R.string.location)) { p -> p.location.takeIf { it.isNotBlank() } ?: "-" },
            ComparisonAttribute(stringResource(R.string.inspection_readiness)) { p -> "${getChecklistCount(p.checklistJson)} items" },
            ComparisonAttribute(stringResource(R.string.notes)) { p -> p.sellerNotes.takeIf { it.isNotBlank() } ?: "-" }
        )

        attributes.forEach { attr ->
            ComparisonAttributeRow(
                attribute = attr,
                prospects = prospects,
                labelWidth = labelWidth,
                columnWidth = columnWidth,
                horizontalScrollState = horizontalScrollState
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

data class ComparisonAttribute(
    val label: String,
    val valueProvider: (ProspectVehicle) -> String
)

@Composable
fun ComparisonAttributeRow(
    attribute: ComparisonAttribute,
    prospects: List<ProspectVehicle>,
    labelWidth: androidx.compose.ui.unit.Dp,
    columnWidth: androidx.compose.ui.unit.Dp,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = attribute.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(labelWidth)
        )
        
        Spacer(Modifier.width(16.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            prospects.forEach { prospect ->
                Text(
                    text = attribute.valueProvider(prospect),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(columnWidth)
                )
            }
        }
    }
}

@Composable
fun ComparisonCard(prospect: ProspectVehicle, width: androidx.compose.ui.unit.Dp) {
    Card(
        modifier = Modifier.width(width),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (prospect.photoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(prospect.photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn, 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(8.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${prospect.make} ${prospect.model}",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    minLines = 2
                )
            }
        }
    }
}

private fun getChecklistCount(json: String): Int {
    return try {
        val obj = JSONObject(json)
        obj.length()
    } catch (_: Exception) {
        0
    }
}
