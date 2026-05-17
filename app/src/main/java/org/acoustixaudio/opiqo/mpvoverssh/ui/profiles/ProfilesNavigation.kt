package org.acoustixaudio.opiqo.mpvoverssh.ui.profiles

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProfileRoute {
    @Serializable
    data object List : ProfileRoute
    
    @Serializable
    data class Detail(val profileId: Long?) : ProfileRoute
}
