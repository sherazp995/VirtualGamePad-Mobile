package io.github.kitswas.virtualgamepadmobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.settingsDataStore)

    val baseColor: Flow<BaseColor> = dataStore.data.map { preferences ->
        BaseColor.fromInt(preferences[BASE_COLOR] ?: defaultBaseColor.ordinal)
    }

    val colorScheme: Flow<ColorScheme> = dataStore.data.map { preferences ->
        ColorScheme.fromInt(preferences[COLOR_SCHEME] ?: defaultColorScheme.ordinal)
    }

    val pollingDelay: Flow<Int> = dataStore.data.map { preferences ->
        preferences[POLLING_DELAY] ?: defaultPollingDelay
    }

    val hapticFeedbackEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK_ENABLED] ?: defaultHapticFeedbackEnabled
    }

    val buttonConfigs: Flow<Map<ButtonComponent, ButtonConfig>> =
        dataStore.data.map { preferences ->
            val jsonString = preferences[BUTTON_CONFIGS]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<Map<ButtonComponent, ButtonConfig>>(jsonString)
                } catch (e: Exception) {
                    defaultButtonConfigs
                }
            } else {
                defaultButtonConfigs
            }
        }

    val lastConnectionIpAddress: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_CONNECTION_IP_ADDRESS] ?: ""
    }

    val lastConnectionPort: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_CONNECTION_PORT] ?: ""
    }

    val saveConnectionCredentials: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SAVE_CONNECTION_CREDENTIALS] ?: defaultSaveConnectionCredentials
    }

    val fullScreenEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FULL_SCREEN_ENABLED] ?: defaultFullScreenEnabled
    }

    val customButtons: Flow<List<CustomButton>> = dataStore.data.map { preferences ->
        preferences[CUSTOM_BUTTONS]?.let {
            try {
                Json.decodeFromString<List<CustomButton>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    val savedConnections: Flow<List<SavedConnection>> = dataStore.data.map { preferences ->
        preferences[SAVED_CONNECTIONS]?.let {
            try {
                Json.decodeFromString<List<SavedConnection>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun setBaseColor(baseColor: BaseColor) {
        dataStore.edit { preferences ->
            preferences[BASE_COLOR] = baseColor.ordinal
        }
    }

    suspend fun setColorScheme(colorScheme: ColorScheme) {
        dataStore.edit { preferences ->
            preferences[COLOR_SCHEME] = colorScheme.ordinal
        }
    }

    suspend fun setPollingDelay(delay: Int) {
        dataStore.edit { preferences ->
            preferences[POLLING_DELAY] = delay
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setButtonConfig(component: ButtonComponent, config: ButtonConfig) {
        dataStore.edit { preferences ->
            val currentJson = preferences[BUTTON_CONFIGS]
            val currentConfigs = if (currentJson != null) {
                try {
                    Json.decodeFromString<Map<ButtonComponent, ButtonConfig>>(currentJson)
                        .toMutableMap()
                } catch (e: Exception) {
                    defaultButtonConfigs.toMutableMap()
                }
            } else {
                defaultButtonConfigs.toMutableMap()
            }
            currentConfigs[component] = config
            preferences[BUTTON_CONFIGS] = Json.encodeToString(currentConfigs)
        }
    }

    suspend fun setAllButtonConfigs(configs: Map<ButtonComponent, ButtonConfig>) {
        dataStore.edit { preferences ->
            preferences[BUTTON_CONFIGS] = Json.encodeToString(configs)
        }
    }

    suspend fun setCustomButtons(buttons: List<CustomButton>) {
        dataStore.edit { preferences ->
            preferences[CUSTOM_BUTTONS] = Json.encodeToString(buttons)
        }
    }

    suspend fun setLastConnectionCredentials(ipAddress: String, port: String) {
        dataStore.edit { preferences ->
            preferences[LAST_CONNECTION_IP_ADDRESS] = ipAddress
            preferences[LAST_CONNECTION_PORT] = port
        }
    }

    suspend fun setSaveConnectionCredentials(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SAVE_CONNECTION_CREDENTIALS] = enabled
        }
    }

    suspend fun setFullScreenEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FULL_SCREEN_ENABLED] = enabled
        }
    }

    /** Saves [connection], replacing any saved connection with the same name. */
    suspend fun saveConnection(connection: SavedConnection) {
        editSavedConnections { saved -> saved.filterNot { it.name == connection.name } + connection }
    }

    suspend fun deleteConnection(id: String) {
        editSavedConnections { saved -> saved.filterNot { it.id == id } }
    }

    private suspend fun editSavedConnections(change: (List<SavedConnection>) -> List<SavedConnection>) {
        dataStore.edit { preferences ->
            val stored = preferences[SAVED_CONNECTIONS]
            val saved = stored?.let {
                try {
                    Json.decodeFromString<List<SavedConnection>>(it)
                } catch (e: Exception) {
                    // Can't read them (e.g. saved by a newer version): keep a copy rather
                    // than silently overwriting them with this change.
                    preferences[UNREADABLE_SAVED_CONNECTIONS] = it
                    emptyList()
                }
            } ?: emptyList()
            preferences[SAVED_CONNECTIONS] = Json.encodeToString(change(saved))
        }
    }

    /** Resets every setting and the layout. Saved connections are kept. */
    suspend fun resetAllSettings() {
        dataStore.edit { preferences ->
            val savedConnections = preferences[SAVED_CONNECTIONS]
            val unreadable = preferences[UNREADABLE_SAVED_CONNECTIONS]
            preferences.clear()
            savedConnections?.let { preferences[SAVED_CONNECTIONS] = it }
            unreadable?.let { preferences[UNREADABLE_SAVED_CONNECTIONS] = it }
        }
    }

    companion object {
        private val COLOR_SCHEME = intPreferencesKey("color_scheme")
        private val BASE_COLOR = intPreferencesKey("base_color")
        private val POLLING_DELAY = intPreferencesKey("polling_delay")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        private val BUTTON_CONFIGS = stringPreferencesKey("button_configs")
        private val LAST_CONNECTION_IP_ADDRESS = stringPreferencesKey("last_connection_ip_address")
        private val LAST_CONNECTION_PORT = stringPreferencesKey("last_connection_port")
        private val SAVE_CONNECTION_CREDENTIALS =
            booleanPreferencesKey("save_connection_credentials")
        private val FULL_SCREEN_ENABLED = booleanPreferencesKey("full_screen_enabled")
        private val SAVED_CONNECTIONS = stringPreferencesKey("saved_connections")
        private val UNREADABLE_SAVED_CONNECTIONS = stringPreferencesKey("saved_connections_unreadable")
        private val CUSTOM_BUTTONS = stringPreferencesKey("custom_buttons")
    }
}
