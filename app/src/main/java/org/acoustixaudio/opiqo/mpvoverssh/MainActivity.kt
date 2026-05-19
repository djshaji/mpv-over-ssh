package org.acoustixaudio.opiqo.mpvoverssh

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.mpvoverssh.settings.ThemeMode
import org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard.DashboardScreen
import org.acoustixaudio.opiqo.mpvoverssh.ui.navigation.Route
import org.acoustixaudio.opiqo.mpvoverssh.ui.navigation.ShareIntentRouteDecision
import org.acoustixaudio.opiqo.mpvoverssh.ui.navigation.resolveShareIntentRouteDecision
import org.acoustixaudio.opiqo.mpvoverssh.ui.profiles.ProfilesScreen
import org.acoustixaudio.opiqo.mpvoverssh.ui.theme.MpvOverSshTheme

class MainActivity : ComponentActivity() {
    private val pendingSharedUri = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            val application = LocalContext.current.applicationContext as MpvOverSshApplication
            val backStack = rememberNavBackStack(Route.Profiles as NavKey)
            val coroutineScope = rememberCoroutineScope()
            val themeMode = application.themePreferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val profiles = application.repository.allProfiles
                .collectAsStateWithLifecycle(initialValue = emptyList())
            // Keep dedupe local to current composition lifetime so process restarts can handle
            // the same shared URI again when relaunched from the share sheet.
            var lastHandledSharedUri by remember { mutableStateOf<String?>(null) }
            // URI waiting for a profile to be selected from the picker
            var profilePickerUri by rememberSaveable { mutableStateOf<String?>(null) }

            LaunchedEffect(pendingSharedUri.value, profiles.value) {
                val sharedUri = pendingSharedUri.value ?: return@LaunchedEffect
                if (sharedUri == lastHandledSharedUri) return@LaunchedEffect
                val profileIds = profiles.value.map { it.id }

                when (
                    val routeDecision = resolveShareIntentRouteDecision(
                        currentTopRoute = backStack.lastOrNull() as? Route,
                        profileIds = profileIds
                    )
                ) {
                    ShareIntentRouteDecision.WaitForProfiles -> return@LaunchedEffect
                    is ShareIntentRouteDecision.ReuseActiveDashboard -> {
                        lastHandledSharedUri = sharedUri
                        pendingSharedUri.value = null
                        backStack[backStack.lastIndex] = Route.Dashboard(routeDecision.profileId, sharedUri)
                    }
                    is ShareIntentRouteDecision.UseOnlyProfile -> {
                        lastHandledSharedUri = sharedUri
                        pendingSharedUri.value = null
                        backStack.add(Route.Dashboard(routeDecision.profileId, sharedUri))
                    }
                    ShareIntentRouteDecision.ShowProfilePicker -> {
                        lastHandledSharedUri = sharedUri
                        pendingSharedUri.value = null
                        profilePickerUri = sharedUri
                    }
                }
            }

            // Profile picker dialog shown when a share intent arrives with multiple profiles
            if (profilePickerUri != null && profiles.value.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { profilePickerUri = null },
                    title = { Text("Stream to which profile?") },
                    text = {
                        LazyColumn {
                            items(profiles.value) { profile ->
                                TextButton(onClick = {
                                    val uri = profilePickerUri
                                    profilePickerUri = null
                                    if (uri != null) {
                                        backStack.add(Route.Dashboard(profile.id, uri))
                                    }
                                }) {
                                    Text(profile.name)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { profilePickerUri = null }) { Text("Cancel") }
                    }
                )
            }

            MpvOverSshTheme(themeMode = themeMode.value) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAt(backStack.size - 1) },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = { key ->
                        when (key) {
                            is Route.Profiles -> {
                                NavEntry<NavKey>(key) {
                                    ProfilesScreen(
                                        application = application,
                                        onNavigateToDashboard = { profileId ->
                                            backStack.add(Route.Dashboard(profileId))
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            is Route.Dashboard -> {
                                NavEntry<NavKey>(key) {
                                    DashboardScreen(
                                        application = application,
                                        profileId = key.profileId,
                                        initialSharedUri = key.sharedUri,
                                        themeMode = themeMode.value,
                                        onThemeModeChange = { mode ->
                                            coroutineScope.launch {
                                                application.themePreferencesRepository.setThemeMode(mode)
                                            }
                                        },
                                        onBack = { backStack.removeAt(backStack.size - 1) },
                                        onSwitchProfile = { profileId ->
                                            backStack.removeAt(backStack.size - 1)
                                            backStack.add(Route.Dashboard(profileId))
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            else -> NavEntry<NavKey>(key) { }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        @Suppress("DEPRECATION")
        val sharedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
        runCatching {
            contentResolver.takePersistableUriPermission(
                sharedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pendingSharedUri.value = sharedUri.toString()
    }
}
