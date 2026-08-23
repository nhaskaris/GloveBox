package com.eliteonetube.glovebox.util

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.eliteonetube.glovebox.ui.widgets.VehicleStatusWidget
import com.eliteonetube.glovebox.ui.widgets.QuickActionsWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetHelper {
    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            VehicleStatusWidget().updateAll(context)
            QuickActionsWidget().updateAll(context)
        }
    }
}
