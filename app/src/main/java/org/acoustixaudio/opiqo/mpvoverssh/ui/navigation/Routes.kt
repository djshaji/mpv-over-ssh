package org.acoustixaudio.opiqo.mpvoverssh.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Profiles : Route
    
    @Serializable
    data class Dashboard(
        val profileId: Long,
        val sharedUri: String? = null
    ) : Route
}

sealed interface ShareIntentRouteDecision {
    data object WaitForProfiles : ShareIntentRouteDecision
    data class ReuseActiveDashboard(val profileId: Long) : ShareIntentRouteDecision
    data class UseOnlyProfile(val profileId: Long) : ShareIntentRouteDecision
    data object ShowProfilePicker : ShareIntentRouteDecision
}

fun resolveShareIntentRouteDecision(
    currentTopRoute: Route?,
    profileIds: List<Long>
): ShareIntentRouteDecision {
    if (profileIds.isEmpty()) return ShareIntentRouteDecision.WaitForProfiles
    if (currentTopRoute is Route.Dashboard) {
        return ShareIntentRouteDecision.ReuseActiveDashboard(currentTopRoute.profileId)
    }
    if (profileIds.size == 1) {
        return ShareIntentRouteDecision.UseOnlyProfile(profileIds.first())
    }
    return ShareIntentRouteDecision.ShowProfilePicker
}

