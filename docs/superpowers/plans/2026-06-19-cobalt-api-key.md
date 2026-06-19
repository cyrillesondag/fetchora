# API Key Cobalt optionnelle — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permettre à l'utilisateur de saisir une API key optionnelle dans les settings, injectée en header `Authorization: Api-Key <key>` sur chaque requête POST vers le serveur Cobalt.

**Architecture:** La clé est persistée dans `SettingsDataStore` (DataStore Preferences). `HostSettingInterceptor` lit la clé et l'injecte sur les POST (les GET sont déjà ignorés). L'UI ajoute un champ masqué dans la section "Service Configuration" avec son propre bouton Save.

**Tech Stack:** Kotlin, Jetpack DataStore, OkHttp Interceptor, Jetpack Compose, Hilt, JUnit + Robolectric + MockK + MockWebServer.

**User decisions (already made):**
- L'API key n'est envoyée que sur les requêtes POST (pas les GET info).
- Champ indépendant du flow Test/Save de l'URL (bouton Save dédié, toujours actif).
- Stockage en clair dans DataStore (cohérent avec l'URL).
- Logique injectée dans `HostSettingInterceptor` existant (pas de nouvel intercepteur).

---

## Structure des fichiers

| Action | Fichier |
|--------|---------|
| Modifier | `app/src/main/java/org/mediadownloader/data/local/datastore/SettingsDataStore.kt` |
| Modifier | `app/src/main/java/org/mediadownloader/data/local/network/HostSettingInterceptor.kt` |
| Modifier | `app/src/main/java/org/mediadownloader/ui/main/settings/SettingsViewModel.kt` |
| Modifier | `app/src/main/java/org/mediadownloader/ui/main/settings/SettingsScreen.kt` |
| Modifier | `app/src/test/java/org/mediadownloader/data/local/SettingsDataStoreTest.kt` |
| Créer   | `app/src/test/java/org/mediadownloader/data/local/network/HostSettingInterceptorTest.kt` |
| Créer   | `app/src/test/java/org/mediadownloader/ui/main/settings/SettingsViewModelTest.kt` |

---

### Task 1 : SettingsDataStore — stocker `cobaltApiKey`

**Goal:** Ajouter `cobaltApiKey: Flow<String?>` et `setCobaltApiKey(key: String)` au DataStore, avec tests.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/data/local/datastore/SettingsDataStore.kt`
- Modify: `app/src/test/java/org/mediadownloader/data/local/SettingsDataStoreTest.kt`

**Acceptance Criteria:**
- [ ] `cobaltApiKey` émet `null` si la clé n'a jamais été sauvegardée
- [ ] `cobaltApiKey` émet la valeur après `setCobaltApiKey("abc")`
- [ ] `setCobaltApiKey("")` ou `setCobaltApiKey("  ")` supprime la préférence et réémet `null`
- [ ] Les tests passent avec `./gradlew :app:testDebugUnitTest --tests "*.SettingsDataStoreTest"`

**Verify:** `./gradlew :app:testDebugUnitTest --tests "*.SettingsDataStoreTest"` → BUILD SUCCESSFUL, 8 tests passés

**Steps:**

- [ ] **Step 1 : Ajouter les tests pour `cobaltApiKey` dans `SettingsDataStoreTest`**

Ouvrir `app/src/test/java/org/mediadownloader/data/local/SettingsDataStoreTest.kt` et ajouter, après la méthode `setFolderUri` existante et avant la première annotation `@Test` :

```kotlin
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
```

Puis ajouter en fin de classe (avant la dernière accolade `}`) :

```kotlin
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
```

- [ ] **Step 2 : Vérifier que les tests échouent (clé absente)**

```
./gradlew :app:testDebugUnitTest --tests "*.SettingsDataStoreTest.cobaltApiKey*"
```

Attendu : FAILED — les méthodes helper `getCobaltApiKey`/`setCobaltApiKey` compilent mais les tests doivent passer car ils testent la logique raw, pas `SettingsDataStore` directement. En réalité les 3 tests doivent PASSER dès maintenant (ils testent le DataStore brut). Si PASS → continuer.

- [ ] **Step 3 : Modifier `SettingsDataStore` pour ajouter `cobaltApiKey`**

Remplacer le contenu de `app/src/main/java/org/mediadownloader/data/local/datastore/SettingsDataStore.kt` par :

```kotlin
package org.mediadownloader.data.local.datastore

import android.content.Context
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
        prefs[keyFolderUri]
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
```

- [ ] **Step 4 : Lancer tous les tests `SettingsDataStoreTest`**

```
./gradlew :app:testDebugUnitTest --tests "*.SettingsDataStoreTest"
```

Attendu : BUILD SUCCESSFUL, 8 tests passed.

- [ ] **Step 5 : Commit**

```bash
git add app/src/main/java/org/mediadownloader/data/local/datastore/SettingsDataStore.kt \
        app/src/test/java/org/mediadownloader/data/local/SettingsDataStoreTest.kt
git commit -m "feat: add cobaltApiKey to SettingsDataStore"
```

---

### Task 2 : HostSettingInterceptor — injecter le header Authorization sur POST

**Goal:** Modifier `HostSettingInterceptor` pour ajouter `Authorization: Api-Key <key>` sur les POST quand la clé est définie, et couvrir le comportement par des tests.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/data/local/network/HostSettingInterceptor.kt`
- Create: `app/src/test/java/org/mediadownloader/data/local/network/HostSettingInterceptorTest.kt`

**Acceptance Criteria:**
- [ ] Un POST avec une clé configurée reçoit `Authorization: Api-Key <key>` dans les headers envoyés
- [ ] Un POST sans clé configurée (null) n'envoie pas de header `Authorization`
- [ ] Un GET n'est pas modifié (pas de header `Authorization`, même si une clé est définie)
- [ ] Les tests passent avec `./gradlew :app:testDebugUnitTest --tests "*.HostSettingInterceptorTest"`

**Verify:** `./gradlew :app:testDebugUnitTest --tests "*.HostSettingInterceptorTest"` → BUILD SUCCESSFUL, 3 tests passés

**Steps:**

- [ ] **Step 1 : Créer le fichier de test `HostSettingInterceptorTest.kt`**

Créer `app/src/test/java/org/mediadownloader/data/local/network/HostSettingInterceptorTest.kt` :

```kotlin
package org.mediadownloader.data.local.network

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mediadownloader.data.local.datastore.SettingsDataStore

class HostSettingInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var settings: SettingsDataStore
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        settings = mockk()
        every { settings.cobaltUrl } returns flowOf(server.url("/").toString())
        client = OkHttpClient.Builder()
            .addInterceptor(HostSettingInterceptor(settings))
            .build()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `POST with api key sends Authorization header`() {
        every { settings.cobaltApiKey } returns flowOf("my-secret-key")
        server.enqueue(MockResponse().setBody("{}"))

        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(server.url("/"))
            .post(body)
            .build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("Api-Key my-secret-key", recorded.getHeader("Authorization"))
    }

    @Test
    fun `POST without api key omits Authorization header`() {
        every { settings.cobaltApiKey } returns flowOf(null)
        server.enqueue(MockResponse().setBody("{}"))

        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(server.url("/"))
            .post(body)
            .build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `GET request is not modified`() {
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/info"))
            .get()
            .build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }
}
```

- [ ] **Step 2 : Vérifier que les tests échouent avant implémentation**

```
./gradlew :app:testDebugUnitTest --tests "*.HostSettingInterceptorTest"
```

Attendu : FAILED — `POST with api key sends Authorization header` échoue car le header n'est pas encore injecté.

- [ ] **Step 3 : Modifier `HostSettingInterceptor`**

Remplacer le contenu de `app/src/main/java/org/mediadownloader/data/local/network/HostSettingInterceptor.kt` par :

```kotlin
package org.mediadownloader.data.local.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import org.mediadownloader.data.local.datastore.SettingsDataStore

class HostSettingInterceptor(
    private val settings: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.method == "GET") {
            return chain.proceed(originalRequest)
        }

        val cobaltUrlString = runBlocking { settings.cobaltUrl.first() }
        val cobaltHttpUrl = cobaltUrlString.toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        val newUrl = originalRequest.url.newBuilder()
            .scheme(cobaltHttpUrl.scheme)
            .host(cobaltHttpUrl.host)
            .port(cobaltHttpUrl.port)
            .build()

        val requestBuilder = originalRequest.newBuilder().url(newUrl)

        val apiKey = runBlocking { settings.cobaltApiKey.first() }
        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Api-Key $apiKey")
        }

        return chain.proceed(requestBuilder.build())
    }
}
```

- [ ] **Step 4 : Lancer les tests**

```
./gradlew :app:testDebugUnitTest --tests "*.HostSettingInterceptorTest"
```

Attendu : BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5 : Commit**

```bash
git add app/src/main/java/org/mediadownloader/data/local/network/HostSettingInterceptor.kt \
        app/src/test/java/org/mediadownloader/data/local/network/HostSettingInterceptorTest.kt
git commit -m "feat: inject Authorization header on POST when cobaltApiKey is set"
```

---

### Task 3 : SettingsViewModel — exposer `cobaltApiKey` et `saveApiKey`

**Goal:** Ajouter `cobaltApiKey: StateFlow<String?>` et `saveApiKey(key: String)` au ViewModel, avec tests.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/ui/main/settings/SettingsViewModel.kt`
- Create: `app/src/test/java/org/mediadownloader/ui/main/settings/SettingsViewModelTest.kt`

**Acceptance Criteria:**
- [ ] `viewModel.cobaltApiKey.value` retourne la valeur émise par `settings.cobaltApiKey`
- [ ] `viewModel.saveApiKey("new-key")` appelle `settings.setCobaltApiKey("new-key")`
- [ ] Les tests passent avec `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"`

**Verify:** `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` → BUILD SUCCESSFUL, 2 tests passés

**Steps:**

- [ ] **Step 1 : Créer `SettingsViewModelTest.kt`**

Créer `app/src/test/java/org/mediadownloader/ui/main/settings/SettingsViewModelTest.kt` :

```kotlin
package org.mediadownloader.ui.main.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import org.mediadownloader.data.remote.model.CobaltInfo
import org.mediadownloader.data.remote.model.GitInfo

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settings: SettingsDataStore
    private lateinit var repository: CobaltRepository

    private val fakeCobaltInfo = CobaltInfo(
        version = "10.0",
        url = "https://api.cobalt.tools/",
        startTime = 0L,
        services = emptyArray(),
        git = GitInfo(branch = "main", commit = "abc", remote = "origin")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settings = mockk()
        repository = mockk()
        every { settings.cobaltUrl } returns flowOf("https://api.cobalt.tools/")
        every { settings.cobaltApiKey } returns flowOf(null)
        every { settings.folderUri } returns flowOf(null)
        coEvery { repository.getInfo(any()) } returns Result.success(fakeCobaltInfo)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `cobaltApiKey StateFlow reflects settings value`() = runTest {
        every { settings.cobaltApiKey } returns flowOf("my-key")
        val viewModel = SettingsViewModel(settings, repository)
        advanceUntilIdle()
        assertEquals("my-key", viewModel.cobaltApiKey.value)
    }

    @Test
    fun `saveApiKey delegates to settings`() = runTest {
        coEvery { settings.setCobaltApiKey(any()) } just Runs
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.saveApiKey("new-key")
        advanceUntilIdle()
        coVerify { settings.setCobaltApiKey("new-key") }
    }
}
```

- [ ] **Step 2 : Vérifier que les tests échouent (méthode absente)**

```
./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"
```

Attendu : FAILED — erreur de compilation sur `viewModel.cobaltApiKey` et `viewModel.saveApiKey`.

- [ ] **Step 3 : Modifier `SettingsViewModel`**

Remplacer le contenu de `app/src/main/java/org/mediadownloader/ui/main/settings/SettingsViewModel.kt` par :

```kotlin
package org.mediadownloader.ui.main.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val cobaltRepository: CobaltRepository) : ViewModel() {

    val cobaltUrl = settings.cobaltUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val folderUri = settings.folderUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cobaltApiKey = settings.cobaltApiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _serverInfoState = MutableStateFlow<ServerInfoState>(ServerInfoState.Idle)
    val serverInfoState = _serverInfoState.asStateFlow()

    init {
        viewModelScope.launch {
            val url = settings.cobaltUrl.first()
            _serverInfoState.value = cobaltRepository.getInfo(url).fold(
                onSuccess = { ServerInfoState.Success(it) },
                onFailure = { ServerInfoState.Error(it.message ?: "Connection failed") }
            )
        }
    }

    fun onFolderSelected(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch { settings.setFolderUri(uri.toString()) }
    }

    fun saveCobaltUrl(url: String) {
        viewModelScope.launch { settings.setCobaltUrl(url) }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch { settings.setCobaltApiKey(key) }
    }

    fun testCobaltUrl(url: String) {
        viewModelScope.launch {
            _serverInfoState.value = ServerInfoState.Loading
            val result = cobaltRepository.getInfo(url)
            _serverInfoState.value = result.fold(
                onSuccess = { ServerInfoState.Success(it) },
                onFailure = { ServerInfoState.Error(it.message ?: "Connection failed") }
            )
        }
    }
}
```

- [ ] **Step 4 : Lancer les tests**

```
./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"
```

Attendu : BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5 : Commit**

```bash
git add app/src/main/java/org/mediadownloader/ui/main/settings/SettingsViewModel.kt \
        app/src/test/java/org/mediadownloader/ui/main/settings/SettingsViewModelTest.kt
git commit -m "feat: expose cobaltApiKey StateFlow and saveApiKey in SettingsViewModel"
```

---

### Task 4 : SettingsScreen — champ API key avec bouton Save

**Goal:** Ajouter un `OutlinedTextField` masqué "API Key (optionnel)" et son bouton "Save" dans la section "Service Configuration".

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/ui/main/settings/SettingsScreen.kt`

**Acceptance Criteria:**
- [ ] `SettingsContent` accepte `cobaltApiKey: String?` et `onSaveApiKey: (String) -> Unit` en paramètres
- [ ] Le champ affiche les caractères masqués (`PasswordVisualTransformation`)
- [ ] Le champ est pré-rempli avec la valeur sauvegardée (ou vide si null)
- [ ] Le bouton "Save" du champ API key est toujours actif et indépendant du bouton Save de l'URL
- [ ] La preview Compose compile sans erreur
- [ ] `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL

**Verify:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL sans erreur

**Steps:**

- [ ] **Step 1 : Mettre à jour `SettingsScreen`**

Remplacer le contenu de `app/src/main/java/org/mediadownloader/ui/main/settings/SettingsScreen.kt` par :

```kotlin
package org.mediadownloader.ui.main.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()) {
    val context = LocalContext.current
    val cobaltUrl by viewModel.cobaltUrl.collectAsState()
    val cobaltApiKey by viewModel.cobaltApiKey.collectAsState()
    val folderUri by viewModel.folderUri.collectAsState()
    val serverInfoState by viewModel.serverInfoState.collectAsState()
    SettingsContent(
        modifier = Modifier.fillMaxSize(),
        cobaltUrl = cobaltUrl,
        cobaltApiKey = cobaltApiKey,
        folderUri = folderUri,
        serverInfoState = serverInfoState,
        onFolderSelected = { uri -> viewModel.onFolderSelected(context, uri) },
        onSaveCobaltUrl = { url -> viewModel.saveCobaltUrl(url) },
        onTestCobaltUrl = { url -> viewModel.testCobaltUrl(url) },
        onSaveApiKey = { key -> viewModel.saveApiKey(key) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    cobaltUrl: String,
    cobaltApiKey: String?,
    folderUri: String?,
    serverInfoState: ServerInfoState,
    onFolderSelected: (Uri) -> Unit,
    onSaveCobaltUrl: (String) -> Unit,
    onTestCobaltUrl: (String) -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    var cobaltUrlDraft by remember(cobaltUrl) { mutableStateOf(cobaltUrl) }
    var apiKeyDraft by remember(cobaltApiKey) { mutableStateOf(cobaltApiKey ?: "") }
    val scrollState = rememberScrollState()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { onFolderSelected(it) } }

    val folderName = remember(folderUri) {
        folderUri?.let { uriString ->
            runCatching {
                val uri = uriString.toUri()
                val path = uri.path ?: ""
                path.substringAfterLast(':').substringAfterLast('/')
            }.getOrDefault("Selected Folder")
        } ?: "Not selected"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section: Storage
        SettingsSection(title = "Storage", icon = Icons.Default.Folder) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { /* Read-only */ },
                    modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
                    label = { Text("Download location") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    readOnly = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { folderPicker.launch(folderUri?.toUri()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select")
                    }
                }
            }
        }

        // Section: API Configuration
        SettingsSection(title = "Service Configuration", icon = Icons.Default.Link) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cobaltUrlDraft,
                    onValueChange = { cobaltUrlDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cobalt Instance URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isSuccess = serverInfoState is ServerInfoState.Success
                    val isLoading = serverInfoState is ServerInfoState.Loading

                    if (isSuccess) {
                        OutlinedButton(
                            onClick = { onTestCobaltUrl(cobaltUrlDraft) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Connection")
                        }
                    } else {
                        Button(
                            onClick = { onTestCobaltUrl(cobaltUrlDraft) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Test Connection")
                        }
                    }

                    Button(
                        onClick = { onSaveCobaltUrl(cobaltUrlDraft) },
                        modifier = Modifier.weight(1f),
                        enabled = isSuccess
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                }

                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key (optionnel)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = PasswordVisualTransformation(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onSaveApiKey(apiKeyDraft) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }

        // Section: Server Info
        SettingsSection(title = "Information", icon = Icons.Default.Settings) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    when (serverInfoState) {
                        is ServerInfoState.Success -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InfoColumn(
                                    modifier = Modifier.weight(1f),
                                    label = "Version",
                                    value = serverInfoState.info.version
                                )
                                InfoColumn(
                                    modifier = Modifier.weight(1f),
                                    label = "Status",
                                    value = "Connected"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val servicesBuffer = StringBuilder()
                                serverInfoState.info.services.joinTo(buffer = servicesBuffer, separator = ", ")
                                InfoColumn(
                                    modifier = Modifier.weight(1f),
                                    label = "services",
                                    value = servicesBuffer.toString()
                                )
                            }
                        }

                        is ServerInfoState.Error -> {
                            Text(
                                text = serverInfoState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        else -> {
                            Text(
                                text = "No information available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    SettingsContent(
        cobaltUrl = "https://cobalt.example.com",
        cobaltApiKey = null,
        folderUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload",
        serverInfoState = ServerInfoState.Idle,
        onFolderSelected = {},
        onSaveCobaltUrl = {},
        onTestCobaltUrl = {},
        onSaveApiKey = {}
    )
}
```

- [ ] **Step 2 : Vérifier la compilation**

```
./gradlew :app:compileDebugKotlin
```

Attendu : BUILD SUCCESSFUL sans erreur.

- [ ] **Step 3 : Lancer tous les tests unitaires**

```
./gradlew :app:testDebugUnitTest
```

Attendu : BUILD SUCCESSFUL, tous les tests passent.

- [ ] **Step 4 : Commit**

```bash
git add app/src/main/java/org/mediadownloader/ui/main/settings/SettingsScreen.kt
git commit -m "feat: add optional API key field in Service Configuration settings"
```
