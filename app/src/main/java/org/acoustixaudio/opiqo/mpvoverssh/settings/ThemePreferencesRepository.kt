package org.acoustixaudio.opiqo.mpvoverssh.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class ThemePreferencesRepository(
    private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.entries.firstOrNull { mode ->
                mode.storageValue == preferences[themeModeKey]
            } ?: ThemeMode.System
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.storageValue
        }
    }
}


