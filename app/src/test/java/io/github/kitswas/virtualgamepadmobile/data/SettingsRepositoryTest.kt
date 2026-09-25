package io.github.kitswas.virtualgamepadmobile.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() = scope.cancel()

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb") }
    }

    private fun repository() = SettingsRepository(dataStore)

    @Test
    fun `saving under an existing name replaces that connection`() = runBlocking {
        val repository = repository()
        repository.saveConnection(SavedConnection("Desktop", "192.168.1.20", "7878"))
        repository.saveConnection(SavedConnection("Desktop", "192.168.1.99", "7878"))

        assertEquals(listOf("192.168.1.99"), repository.savedConnections.first().map { it.ipAddress })
    }

    @Test
    fun `resetting settings keeps saved connections`() = runBlocking {
        val repository = repository()
        repository.saveConnection(SavedConnection("Desktop", "192.168.1.20", "7878"))
        repository.setHapticFeedbackEnabled(!defaultHapticFeedbackEnabled)
        assertEquals(!defaultHapticFeedbackEnabled, repository.hapticFeedbackEnabled.first())

        repository.resetAllSettings()

        assertEquals(defaultHapticFeedbackEnabled, repository.hapticFeedbackEnabled.first())
        assertEquals(listOf("Desktop"), repository.savedConnections.first().map { it.name })
    }

    @Test
    fun `saved connections it can't read are kept aside instead of lost`() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("saved_connections")] = "written by a newer version" }

        repository().saveConnection(SavedConnection("Desktop", "192.168.1.20", "7878"))

        val kept = dataStore.data.first()[stringPreferencesKey("saved_connections_unreadable")]
        assertEquals("written by a newer version", kept)
    }
}
