package com.eliteonetube.glovebox.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
sealed interface GloveboxRoute : NavKey {
    @Serializable
    data object Onboarding : GloveboxRoute

    @Serializable
    data object Home : GloveboxRoute

    @Serializable
    data object VehicleList : GloveboxRoute

    @Serializable
    data class VehicleProfile(val vehicleId: Long = 0L) : GloveboxRoute

    @Serializable
    data class History(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddServiceLog(
        val vehicleId: Long, 
        val recordId: Long = 0L, 
        val prefilledType: String? = null
    ) : GloveboxRoute

    @Serializable
    data class Reminders(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddFuelLog(val vehicleId: Long, val logId: Long = 0L) : GloveboxRoute

    @Serializable
    data class DigitalGlovebox(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class MyParts(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class Insights(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddDocument(val vehicleId: Long, val docId: Long = 0L) : GloveboxRoute

    @Serializable
    data object BuyChecklist : GloveboxRoute

    @Serializable
    data class ProspectForm(val prospectId: Long = 0L) : GloveboxRoute

    @Serializable
    data class ProspectComparison(val prospectIds: List<Long>) : GloveboxRoute

    @Serializable
    data object Settings : GloveboxRoute
}
