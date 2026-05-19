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
