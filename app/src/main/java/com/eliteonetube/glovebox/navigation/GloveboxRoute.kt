package com.eliteonetube.glovebox.navigation

import kotlinx.serialization.Serializable

sealed interface GloveboxRoute {
    @Serializable
    data object VehicleList : GloveboxRoute

    @Serializable
    data class VehicleProfile(val vehicleId: Long = 0L) : GloveboxRoute

    @Serializable
    data class ServiceHistory(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data class AddServiceLog(val vehicleId: Long, val recordId: Long = 0L) : GloveboxRoute

    @Serializable
    data class Reminders(val vehicleId: Long) : GloveboxRoute

    @Serializable
    data object Settings : GloveboxRoute
}
