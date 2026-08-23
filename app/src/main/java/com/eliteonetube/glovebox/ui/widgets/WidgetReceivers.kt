package com.eliteonetube.glovebox.ui.widgets

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class VehicleStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = VehicleStatusWidget()
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickActionsWidget()
}
