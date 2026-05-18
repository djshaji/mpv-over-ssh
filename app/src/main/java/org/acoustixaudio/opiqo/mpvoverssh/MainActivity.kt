package org.acoustixaudio.opiqo.mpvoverssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.mpvoverssh.settings.ThemeMode
import org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard.DashboardScreen
import org.acoustixaudio.opiqo.mpvoverssh.ui.navigation.Route
import org.acoustixaudio.opiqo.mpvoverssh.ui.profiles.ProfilesScreen
import org.acoustixaudio.opiqo.mpvoverssh.ui.theme.MpvOverSshTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = LocalContext.current.applicationContext as MpvOverSshApplication
            val backStack = rememberNavBackStack(Route.Profiles as NavKey)
            val coroutineScope = rememberCoroutineScope()
            val themeMode = application.themePreferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

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
                                        themeMode = themeMode.value,
                                        onThemeModeChange = { mode ->
                                            coroutineScope.launch {
                                                application.themePreferencesRepository.setThemeMode(mode)
                                            }
                                        },
                                        onBack = { backStack.removeAt(backStack.size - 1) },
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
}
