package com.eliteonetube.glovebox.navigation

import kotlinx.serialization.Serializable

sealed interface GloveboxRoute {
    @Serializable
    data object Onboarding : GloveboxRoute

    @Serializable
    data object VehicleList : GloveboxRoute

    @Serializable
    data class VehicleProfile(val vehicleId: Long = 0L) : GloveboxRoute

    @Serializable
    data class History(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddServiceLog(val vehicleId: Long, val recordId: Long = 0L) : GloveboxRoute

    @Serializable
    data class Reminders(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddFuelLog(val vehicleId: Long, val logId: Long = 0L) : GloveboxRoute

    @Serializable
    data class DigitalGlovebox(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddDocument(val vehicleId: Long, val docId: Long = 0L) : GloveboxRoute

    @Serializable
    data object Settings : GloveboxRoute
}
