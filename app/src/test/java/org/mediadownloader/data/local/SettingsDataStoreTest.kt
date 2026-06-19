package org.mediadownloader.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import org.mediadownloader.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testDataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        val testFile = tempFolder.newFile("settings_test.preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(produceFile = { testFile })
    }

    private suspend fun getCobaltUrl(): String {
        val keyCobaltUrl = stringPreferencesKey("cobalt_url")
        return testDataStore.data.map { prefs ->
            prefs[keyCobaltUrl]?.takeIf { it.isNotBlank() } ?: "https://api.cobalt.tools/"
        }.first()
    }

    private suspend fun getFolderUri(): String? {
        val keyFolderUri = stringPreferencesKey("folder_uri")
        return testDataStore.data.map { prefs ->
            prefs[keyFolderUri]
        }.first()
    }

    private suspend fun setCobaltUrl(url: String) {
        val keyCobaltUrl = stringPreferencesKey("cobalt_url")
        testDataStore.edit { prefs ->
            if (url.isBlank()) prefs.remove(keyCobaltUrl)
            else prefs[keyCobaltUrl] = url
        }
    }

    private suspend fun setFolderUri(uri: String) {
        val keyFolderUri = stringPreferencesKey("folder_uri")
        testDataStore.edit { it[keyFolderUri] = uri }
    }

    private suspend fun getCobaltApiKey(): String? {
        val keyCobaltApiKey = stringPreferencesKey("cobalt_api_key")
        return testDataStore.data.map { prefs -> prefs[keyCobaltApiKey] }.first()
    }

    private suspend fun setCobaltApiKey(key: String) {
        val keyCobaltApiKey = stringPreferencesKey("cobalt_api_key")
        testDataStore.edit { prefs ->
            if (key.isBlank()) prefs.remove(keyCobaltApiKey)
            else prefs[keyCobaltApiKey] = key
        }
    }

    @Test
    fun `cobaltUrl default is api cobalt tools`() = runTest {
        assertEquals("https://api.cobalt.tools/", getCobaltUrl())
    }

    @Test
    fun `folderUri default is null`() = runTest {
        assertNull(getFolderUri())
    }

    @Test
    fun `saved cobaltUrl is retrieved`() = runTest {
        setCobaltUrl("https://my.instance.example.com/")
        assertEquals("https://my.instance.example.com/", getCobaltUrl())
    }

    @Test
    fun `blank cobaltUrl resets to default`() = runTest {
        setCobaltUrl("   ")
        assertEquals("https://api.cobalt.tools/", getCobaltUrl())
    }

    @Test
    fun `saved folderUri is retrieved`() = runTest {
        setFolderUri("content://com.android.externalstorage.documents/tree/primary%3ADownloads")
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3ADownloads",
            getFolderUri()
        )
    }

    @Test
    fun `cobaltApiKey default is null`() = runTest {
        assertNull(getCobaltApiKey())
    }

    @Test
    fun `saved cobaltApiKey is retrieved`() = runTest {
        setCobaltApiKey("my-api-key")
        assertEquals("my-api-key", getCobaltApiKey())
    }

    @Test
    fun `blank cobaltApiKey removes the key`() = runTest {
        setCobaltApiKey("my-api-key")
        setCobaltApiKey("")
        assertNull(getCobaltApiKey())
    }
}
