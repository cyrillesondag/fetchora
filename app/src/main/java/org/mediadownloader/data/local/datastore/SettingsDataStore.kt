package org.mediadownloader.data.local.datastore

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val DEFAULT_COBALT_URL = "https://api.cobalt.tools/"
    }

    private val keyCobaltUrl = stringPreferencesKey("cobalt_url")
    private val keyFolderUri = stringPreferencesKey("folder_uri")
    private val keyCobaltApiKey = stringPreferencesKey("cobalt_api_key")

    val cobaltUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyCobaltUrl]?.takeIf { it.isNotBlank() } ?: DEFAULT_COBALT_URL
    }

    val folderUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[keyFolderUri] ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.let { Uri.fromFile(it).toString() }
    }

    val cobaltApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[keyCobaltApiKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun setCobaltUrl(url: String) {
        context.dataStore.edit { prefs ->
            if (url.isBlank()) prefs.remove(keyCobaltUrl)
            else prefs[keyCobaltUrl] = url
        }
    }

    suspend fun setFolderUri(uri: String) {
        context.dataStore.edit { it[keyFolderUri] = uri }
    }

    suspend fun setCobaltApiKey(key: String) {
        context.dataStore.edit { prefs ->
            if (key.isBlank()) prefs.remove(keyCobaltApiKey)
            else prefs[keyCobaltApiKey] = key
        }
    }
}
