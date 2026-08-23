package com.eliteonetube.glovebox.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.eliteonetube.glovebox.MainActivity
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class QuickActionsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val activeVehicleName = try {
            val userPrefs = UserPreferencesRepository(context)
            val db = GloveboxDatabase.getDatabase(context)
            val activeId = userPrefs.activeVehicleId.first()
            val vehicle = activeId?.let { db.vehicleDao().getVehicleById(it) }
            vehicle?.let { "${it.make} ${it.model}" } ?: context.getString(R.string.app_name)
        } catch (e: Exception) {
            context.getString(R.string.app_name)
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(20.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = activeVehicleName,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                    
                    Spacer(GlanceModifier.height(10.dp))
                    
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActionTile(
                            iconRes = R.drawable.ic_widget_fuel,
                            label = "Log Fuel",
                            containerColor = GlanceTheme.colors.primaryContainer,
                            contentColor = GlanceTheme.colors.onPrimaryContainer,
                            modifier = GlanceModifier.defaultWeight(),
                            onClick = actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    action = "com.eliteonetube.glovebox.ACTION_ADD_FUEL"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        )
                        
                        Spacer(GlanceModifier.width(8.dp))
                        
                        ActionTile(
                            iconRes = R.drawable.ic_widget_wrench,
                            label = "Log Service",
                            containerColor = GlanceTheme.colors.secondaryContainer,
                            contentColor = GlanceTheme.colors.onSecondaryContainer,
                            modifier = GlanceModifier.defaultWeight(),
                            onClick = actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    action = "com.eliteonetube.glovebox.ACTION_ADD_SERVICE"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionTile(
        iconRes: Int,
        label: String,
        containerColor: ColorProvider,
        contentColor: ColorProvider,
        modifier: GlanceModifier,
        onClick: Action
    ) {
        Column(
            modifier = modifier
                .background(containerColor)
                .cornerRadius(16.dp)
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .clickable(onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                ),
                maxLines = 1
            )
        }
    }
}
