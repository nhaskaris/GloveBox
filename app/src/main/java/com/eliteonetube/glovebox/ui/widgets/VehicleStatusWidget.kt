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
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.data.entity.Reminder
import kotlinx.coroutines.flow.first

class VehicleStatusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = try {
            val userPrefs = UserPreferencesRepository(context)
            val db = GloveboxDatabase.getDatabase(context)
            val activeId = userPrefs.activeVehicleId.first()
            val vehicle = activeId?.let { db.vehicleDao().getVehicleById(it) }
            val reminders = activeId?.let { db.reminderDao().getRemindersForVehicle(it).first() }
            val nextReminder = reminders?.filter { !it.isCompleted }?.minByOrNull { 
                it.targetDate ?: Long.MAX_VALUE 
            }
            WidgetData(vehicle, nextReminder)
        } catch (e: Exception) {
            e.printStackTrace()
            WidgetData(null, null)
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(20.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    if (data.vehicle != null) {
                        Text(
                            text = data.vehicle.nickname ?: "${data.vehicle.make} ${data.vehicle.model}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 1
                        )
                        
                        Spacer(GlanceModifier.height(12.dp))
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusItem(
                                iconRes = R.drawable.ic_widget_speedometer,
                                label = "${data.vehicle.odometer} ${data.vehicle.odometerUnit}",
                                color = GlanceTheme.colors.onSurfaceVariant,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            
                            if (data.nextReminder != null) {
                                Spacer(GlanceModifier.width(8.dp))
                                StatusItem(
                                    iconRes = R.drawable.ic_widget_calendar,
                                    label = data.nextReminder.description,
                                    color = GlanceTheme.colors.primary,
                                    modifier = GlanceModifier.defaultWeight(),
                                    isUrgent = true
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = context.getString(R.string.no_vehicle_selected_widget),
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusItem(
        iconRes: Int,
        label: String,
        color: ColorProvider,
        modifier: GlanceModifier,
        isUrgent: Boolean = false
    ) {
        Row(
            modifier = modifier
                .background(if (isUrgent) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.secondaryContainer)
                .cornerRadius(12.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(if (isUrgent) GlanceTheme.colors.onPrimaryContainer else color)
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isUrgent) GlanceTheme.colors.onPrimaryContainer else color
                ),
                maxLines = 1
            )
        }
    }

    private data class WidgetData(
        val vehicle: Vehicle?,
        val nextReminder: Reminder?
    )
}
