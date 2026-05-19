package org.acoustixaudio.opiqo.mpvoverssh.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareIntentRouteDecisionTest {

    @Test
    fun waitsWhenNoProfilesAvailable() {
        val decision = resolveShareIntentRouteDecision(
            currentTopRoute = Route.Profiles,
            profileIds = emptyList()
        )

        assertEquals(ShareIntentRouteDecision.WaitForProfiles, decision)
    }

    @Test
    fun reusesDashboardWhenAlreadyActive() {
        val decision = resolveShareIntentRouteDecision(
            currentTopRoute = Route.Dashboard(profileId = 7L),
            profileIds = listOf(1L, 7L, 9L)
        )

        assertEquals(ShareIntentRouteDecision.ReuseActiveDashboard(7L), decision)
    }

    @Test
    fun usesOnlyProfileWhenSingleProfileExists() {
        val decision = resolveShareIntentRouteDecision(
            currentTopRoute = Route.Profiles,
            profileIds = listOf(42L)
        )

        assertEquals(ShareIntentRouteDecision.UseOnlyProfile(42L), decision)
    }

    @Test
    fun showsPickerWhenMultipleProfilesAndNoDashboard() {
        val decision = resolveShareIntentRouteDecision(
            currentTopRoute = Route.Profiles,
            profileIds = listOf(1L, 2L)
        )

        assertEquals(ShareIntentRouteDecision.ShowProfilePicker, decision)
    }
}

