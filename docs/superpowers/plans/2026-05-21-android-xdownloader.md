# X Video Downloader — Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app (Kotlin + Compose, min SDK 29) that receives X/Twitter links via share intent, fetches video quality options from the Cobalt API, and downloads the chosen quality to a user-configured folder.

**Architecture:** MVVM with Hilt DI. ShareReceiverActivity (transparent) handles the share intent and shows a BottomSheet for quality selection. WorkManager runs the download in the background. Room stores download history. DataStore persists settings (folder URI + Cobalt URL).

**Tech Stack:** Kotlin 2.0, Jetpack Compose BOM 2024.11, Hilt 2.52, Retrofit 2.11, OkHttp 4.12, Room 2.6, DataStore 1.1, WorkManager 2.10, Coil 2.7, KSP 2.0.21, Robolectric 4.13

---

## File Map

```
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/cyrillesondag/xdownloader/
│   │       ├── XDownloaderApp.kt
│   │       ├── data/
│   │       │   ├── remote/
│   │       │   │   ├── model/CobaltRequest.kt
│   │       │   │   ├── model/CobaltResponse.kt
│   │       │   │   ├── CobaltApi.kt
│   │       │   │   └── CobaltRepository.kt
│   │       │   └── local/
│   │       │       ├── db/DownloadEntity.kt
│   │       │       ├── db/DownloadDao.kt
│   │       │       ├── db/AppDatabase.kt
│   │       │       └── datastore/SettingsDataStore.kt
│   │       ├── di/
│   │       │   ├── AppModule.kt
│   │       │   ├── DatabaseModule.kt
│   │       │   ├── NetworkModule.kt
│   │       │   └── WorkerModule.kt
│   │       ├── util/
│   │       │   ├── QualityParser.kt
│   │       │   └── NotificationHelper.kt
│   │       ├── worker/
│   │       │   └── DownloadWorker.kt
│   │       └── ui/
│   │           ├── theme/Theme.kt
│   │           ├── share/
│   │           │   ├── ShareReceiverActivity.kt
│   │           │   ├── ShareViewModel.kt
│   │           │   └── QualityBottomSheet.kt
│   │           └── main/
│   │               ├── MainActivity.kt
│   │               ├── history/HistoryScreen.kt
│   │               ├── history/HistoryViewModel.kt
│   │               ├── settings/SettingsScreen.kt
│   │               └── settings/SettingsViewModel.kt
│   └── test/
│       └── java/com/cyrillesondag/xdownloader/
│           ├── util/QualityParserTest.kt
│           ├── data/remote/CobaltRepositoryTest.kt
│           ├── data/local/DownloadDaoTest.kt
│           ├── data/local/SettingsDataStoreTest.kt
│           ├── worker/DownloadWorkerTest.kt
│           └── ui/share/ShareViewModelTest.kt
gradle/
└── libs.versions.toml
build.gradle.kts          (root)
settings.gradle.kts
app/build.gradle.kts
```

---

## Task 1: Project Setup & Build Configuration

**Goal:** Create the Android project skeleton with all dependencies correctly wired.

**Files:**
- Create: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/XDownloaderApp.kt`

**Acceptance Criteria:**
- [ ] `./gradlew assembleDebug` completes without errors
- [ ] Hilt, Room, Retrofit, WorkManager, Compose dependencies resolve
- [ ] KSP annotation processing runs (no missing generated code errors)

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Create the Android Studio project**

  In Android Studio: File → New → New Project → **Empty Activity** (Compose template).
  - Name: `X Downloader`
  - Package name: `com.cyrillesondag.xdownloader`
  - Save location: `/home/cyrille/IdeaProjects/twitterDownloadVid`
  - Language: Kotlin
  - Minimum SDK: API 29 (Android 10)
  - Build configuration language: Kotlin DSL

  Click Finish. Wait for Gradle sync to complete.

- [ ] **Step 2: Replace `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
compose-bom = "2024.11.00"
activity-compose = "1.9.3"
navigation-compose = "2.8.4"
hilt = "2.52"
hilt-androidx = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.6.1"
datastore = "1.1.1"
workmanager = "2.10.0"
coil = "2.7.0"
junit = "4.13.2"
robolectric = "4.13"
mockk = "1.13.13"
coroutines-test = "1.9.0"
mockwebserver = "4.12.0"

[libraries]
# Compose
compose-bom            = { group = "androidx.compose",            name = "compose-bom",              version.ref = "compose-bom" }
compose-ui             = { group = "androidx.compose.ui",         name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui",    name = "ui-tooling-preview" }
compose-ui-tooling     = { group = "androidx.compose.ui",         name = "ui-tooling" }
compose-ui-test-junit4 = { group = "androidx.compose.ui",         name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui",       name = "ui-test-manifest" }
compose-material3      = { group = "androidx.compose.material3",  name = "material3" }
compose-material-icons = { group = "androidx.compose.material",   name = "material-icons-extended" }
activity-compose       = { group = "androidx.activity",           name = "activity-compose",         version.ref = "activity-compose" }
navigation-compose     = { group = "androidx.navigation",         name = "navigation-compose",       version.ref = "navigation-compose" }
# Hilt
hilt-android           = { group = "com.google.dagger",           name = "hilt-android",             version.ref = "hilt" }
hilt-compiler          = { group = "com.google.dagger",           name = "hilt-android-compiler",    version.ref = "hilt" }
hilt-navigation        = { group = "androidx.hilt",               name = "hilt-navigation-compose",  version.ref = "hilt-androidx" }
hilt-work              = { group = "androidx.hilt",               name = "hilt-work",                version.ref = "hilt-androidx" }
hilt-work-compiler     = { group = "androidx.hilt",               name = "hilt-compiler",            version.ref = "hilt-androidx" }
hilt-testing           = { group = "com.google.dagger",           name = "hilt-android-testing",     version.ref = "hilt" }
# Retrofit / OkHttp
retrofit               = { group = "com.squareup.retrofit2",      name = "retrofit",                 version.ref = "retrofit" }
retrofit-gson          = { group = "com.squareup.retrofit2",      name = "converter-gson",           version.ref = "retrofit" }
okhttp-logging         = { group = "com.squareup.okhttp3",        name = "logging-interceptor",      version.ref = "okhttp" }
mockwebserver          = { group = "com.squareup.okhttp3",        name = "mockwebserver",            version.ref = "mockwebserver" }
# Room
room-runtime           = { group = "androidx.room",               name = "room-runtime",             version.ref = "room" }
room-compiler          = { group = "androidx.room",               name = "room-compiler",            version.ref = "room" }
room-ktx               = { group = "androidx.room",               name = "room-ktx",                 version.ref = "room" }
room-testing           = { group = "androidx.room",               name = "room-testing",             version.ref = "room" }
# DataStore
datastore-preferences  = { group = "androidx.datastore",          name = "datastore-preferences",    version.ref = "datastore" }
# WorkManager
work-runtime           = { group = "androidx.work",               name = "work-runtime-ktx",         version.ref = "workmanager" }
work-testing           = { group = "androidx.work",               name = "work-testing",             version.ref = "workmanager" }
# Coil
coil-compose           = { group = "io.coil-kt",                  name = "coil-compose",             version.ref = "coil" }
# Testing
junit                  = { group = "junit",                       name = "junit",                    version.ref = "junit" }
robolectric            = { group = "org.robolectric",             name = "robolectric",              version.ref = "robolectric" }
mockk                  = { group = "io.mockk",                    name = "mockk",                    version.ref = "mockk" }
coroutines-test        = { group = "org.jetbrains.kotlinx",       name = "kotlinx-coroutines-test",  version.ref = "coroutines-test" }
androidx-test-core     = { group = "androidx.test",               name = "core-ktx",                 version = "1.6.1" }

[plugins]
android-application = { id = "com.android.application",            version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android",       version.ref = "kotlin" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt                = { id = "com.google.dagger.hilt.android",     version.ref = "hilt" }
ksp                 = { id = "com.google.devtools.ksp",            version.ref = "ksp" }
room                = { id = "androidx.room",                      version.ref = "room" }
```

- [ ] **Step 3: Replace root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.room)                apply false
}
```

- [ ] **Step 4: Replace `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.cyrillesondag.xdownloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cyrillesondag.xdownloader"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime)

    // Coil
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.room.testing)
    testImplementation(libs.work.testing)
    testImplementation(libs.hilt.testing)
    testImplementation(libs.androidx.test.core)
    kspTest(libs.hilt.compiler)
}
```

- [ ] **Step 5: Create `XDownloaderApp.kt`**

```kotlin
package com.cyrillesondag.xdownloader

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class XDownloaderApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

- [ ] **Step 6: Sync and verify**

  In Android Studio: File → Sync Project with Gradle Files.
  Run `./gradlew assembleDebug` from the terminal tab.
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts \
        settings.gradle.kts \
        app/src/main/java/com/cyrillesondag/xdownloader/XDownloaderApp.kt
git commit -m "chore: project setup with all dependencies"
```

---

## Task 2: Quality Parser Utility

**Goal:** A pure function that extracts a human-readable quality label (e.g. `"720p"`) from a Twitter video URL, with full unit-test coverage before any implementation.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/util/QualityParser.kt`
- Create: `app/src/test/java/com/cyrillesondag/xdownloader/util/QualityParserTest.kt`

**Acceptance Criteria:**
- [ ] `parseQuality("https://video.twimg.com/.../1280x720/v.mp4")` → `"720p"`
- [ ] `parseQuality("https://video.twimg.com/.../720x1280/v.mp4")` → `"720p"` (portrait — use smaller dimension)
- [ ] `parseQuality("https://other-cdn.example.com/video.mp4")` → `"Quality 1"` (fallback)
- [ ] All tests pass

**Verify:** `./gradlew test --tests "*.QualityParserTest"` → `BUILD SUCCESSFUL` (all green)

**Steps:**

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/cyrillesondag/xdownloader/util/QualityParserTest.kt
package com.cyrillesondag.xdownloader.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QualityParserTest {

    @Test
    fun `landscape url returns height as quality`() {
        val url = "https://video.twimg.com/ext_tw_video/123/pu/vid/avc1/1280x720/video.mp4"
        assertEquals("720p", QualityParser.parseQuality(url))
    }

    @Test
    fun `portrait url returns smaller dimension as quality`() {
        val url = "https://video.twimg.com/ext_tw_video/123/pu/vid/avc1/720x1280/video.mp4"
        assertEquals("720p", QualityParser.parseQuality(url))
    }

    @Test
    fun `720p variant url`() {
        val url = "https://video.twimg.com/amplify_video/123/vid/avc1/720x720/video.mp4"
        assertEquals("720p", QualityParser.parseQuality(url))
    }

    @Test
    fun `480p variant url`() {
        val url = "https://video.twimg.com/ext_tw_video/123/pu/vid/avc1/854x480/video.mp4"
        assertEquals("480p", QualityParser.parseQuality(url))
    }

    @Test
    fun `url without resolution returns fallback`() {
        val url = "https://other-cdn.example.com/video.mp4"
        assertEquals("Quality 1", QualityParser.parseQuality(url, fallbackIndex = 1))
    }

    @Test
    fun `fallback index is 1-based in label`() {
        val url = "https://cdn.example.com/video.mp4"
        assertEquals("Quality 3", QualityParser.parseQuality(url, fallbackIndex = 3))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "*.QualityParserTest"
```
Expected: `FAILED` — `Unresolved reference: QualityParser`

- [ ] **Step 3: Write the implementation**

```kotlin
// app/src/main/java/com/cyrillesondag/xdownloader/util/QualityParser.kt
package com.cyrillesondag.xdownloader.util

object QualityParser {

    private val resolutionRegex = Regex("""(\d+)x(\d+)""")

    fun parseQuality(url: String, fallbackIndex: Int = 1): String {
        val match = resolutionRegex.find(url) ?: return "Quality $fallbackIndex"
        val w = match.groupValues[1].toInt()
        val h = match.groupValues[2].toInt()
        return "${minOf(w, h)}p"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew test --tests "*.QualityParserTest"
```
Expected: `BUILD SUCCESSFUL` — 6 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/util/QualityParser.kt \
        app/src/test/java/com/cyrillesondag/xdownloader/util/QualityParserTest.kt
git commit -m "feat: add QualityParser utility"
```

---

## Task 3: Cobalt API Client

**Goal:** Retrofit interface + response models + repository that fetches video variants from the Cobalt API, with MockWebServer tests covering all response types.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/remote/model/CobaltRequest.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/remote/model/CobaltResponse.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/remote/CobaltApi.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/remote/CobaltRepository.kt`
- Create: `app/src/test/java/com/cyrillesondag/xdownloader/data/remote/CobaltRepositoryTest.kt`

**Acceptance Criteria:**
- [ ] `picker` response → list of `VideoVariant` with parsed quality labels
- [ ] `stream` / `redirect` response → single-item list
- [ ] `error` response → `Result.failure` with descriptive message
- [ ] Network error → `Result.failure`

**Verify:** `./gradlew test --tests "*.CobaltRepositoryTest"` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write models**

```kotlin
// data/remote/model/CobaltRequest.kt
package com.cyrillesondag.xdownloader.data.remote.model

import com.google.gson.annotations.SerializedName

data class CobaltRequest(@SerializedName("url") val url: String)
```

```kotlin
// data/remote/model/CobaltResponse.kt
package com.cyrillesondag.xdownloader.data.remote.model

import com.google.gson.annotations.SerializedName

data class CobaltResponse(
    @SerializedName("status") val status: String,
    @SerializedName("url")    val url: String?,
    @SerializedName("picker") val picker: List<PickerItem>?,
    @SerializedName("error")  val error: CobaltError?
)

data class PickerItem(
    @SerializedName("url")  val url: String,
    @SerializedName("type") val type: String
)

data class CobaltError(
    @SerializedName("code") val code: String
)

data class VideoVariant(val url: String, val qualityLabel: String)
```

- [ ] **Step 2: Write Retrofit interface**

```kotlin
// data/remote/CobaltApi.kt
package com.cyrillesondag.xdownloader.data.remote

import com.cyrillesondag.xdownloader.data.remote.model.CobaltRequest
import com.cyrillesondag.xdownloader.data.remote.model.CobaltResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CobaltApi {
    @POST("/")
    suspend fun resolve(@Body request: CobaltRequest): CobaltResponse
}
```

- [ ] **Step 3: Write failing tests**

```kotlin
// test/data/remote/CobaltRepositoryTest.kt
package com.cyrillesondag.xdownloader.data.remote

import com.cyrillesondag.xdownloader.data.remote.model.VideoVariant
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CobaltRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: CobaltRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CobaltApi::class.java)
        repository = CobaltRepository(api)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `picker response returns multiple variants with quality labels`() = runTest {
        server.enqueue(MockResponse().setBody("""
            {
              "status": "picker",
              "picker": [
                {"url": "https://video.twimg.com/ext_tw_video/1/pu/vid/avc1/1280x720/v.mp4", "type": "video"},
                {"url": "https://video.twimg.com/ext_tw_video/1/pu/vid/avc1/854x480/v.mp4",  "type": "video"}
              ]
            }
        """.trimIndent()).addHeader("Content-Type", "application/json"))

        val result = repository.getVariants("https://x.com/user/status/1")

        assertTrue(result.isSuccess)
        val variants = result.getOrThrow()
        assertEquals(2, variants.size)
        assertEquals("720p", variants[0].qualityLabel)
        assertEquals("480p", variants[1].qualityLabel)
    }

    @Test
    fun `stream response returns single variant`() = runTest {
        server.enqueue(MockResponse().setBody("""
            {"status": "stream", "url": "https://video.twimg.com/ext_tw_video/1/pu/vid/avc1/1280x720/v.mp4"}
        """.trimIndent()).addHeader("Content-Type", "application/json"))

        val result = repository.getVariants("https://x.com/user/status/2")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("720p", result.getOrThrow()[0].qualityLabel)
    }

    @Test
    fun `redirect response returns single variant`() = runTest {
        server.enqueue(MockResponse().setBody("""
            {"status": "redirect", "url": "https://video.twimg.com/ext_tw_video/1/pu/vid/avc1/854x480/v.mp4"}
        """.trimIndent()).addHeader("Content-Type", "application/json"))

        val result = repository.getVariants("https://x.com/user/status/3")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `error response returns failure`() = runTest {
        server.enqueue(MockResponse().setBody("""
            {"status": "error", "error": {"code": "error.api.link.invalid"}}
        """.trimIndent()).addHeader("Content-Type", "application/json"))

        val result = repository.getVariants("https://not-a-tweet.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("error.api.link.invalid"))
    }

    @Test
    fun `network error returns failure`() = runTest {
        server.shutdown()
        val result = repository.getVariants("https://x.com/user/status/4")
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

```
./gradlew test --tests "*.CobaltRepositoryTest"
```
Expected: `FAILED` — `Unresolved reference: CobaltRepository`

- [ ] **Step 5: Write the repository**

```kotlin
// data/remote/CobaltRepository.kt
package com.cyrillesondag.xdownloader.data.remote

import com.cyrillesondag.xdownloader.data.remote.model.CobaltRequest
import com.cyrillesondag.xdownloader.data.remote.model.VideoVariant
import com.cyrillesondag.xdownloader.util.QualityParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CobaltRepository @Inject constructor(private val api: CobaltApi) {

    suspend fun getVariants(tweetUrl: String): Result<List<VideoVariant>> = runCatching {
        val response = api.resolve(CobaltRequest(tweetUrl))
        when (response.status) {
            "picker" -> {
                val items = response.picker.orEmpty().filter { it.type == "video" }
                items.mapIndexed { i, item ->
                    VideoVariant(
                        url = item.url,
                        qualityLabel = QualityParser.parseQuality(item.url, fallbackIndex = i + 1)
                    )
                }
            }
            "stream", "redirect", "tunnel" -> {
                val url = response.url ?: error("Missing URL in ${response.status} response")
                listOf(VideoVariant(url, QualityParser.parseQuality(url)))
            }
            "error" -> error(response.error?.code ?: "Unknown Cobalt error")
            else -> error("Unexpected Cobalt status: ${response.status}")
        }
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```
./gradlew test --tests "*.CobaltRepositoryTest"
```
Expected: `BUILD SUCCESSFUL` — 5 tests passed.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/data/remote/ \
        app/src/test/java/com/cyrillesondag/xdownloader/data/remote/
git commit -m "feat: Cobalt API client with repository"
```

---

## Task 4: Room Database

**Goal:** `DownloadEntity`, `DownloadDao`, and `AppDatabase` with an in-memory Room test suite.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/local/db/DownloadEntity.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/local/db/DownloadDao.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/local/db/AppDatabase.kt`
- Create: `app/src/test/java/com/cyrillesondag/xdownloader/data/local/DownloadDaoTest.kt`

**Acceptance Criteria:**
- [ ] Insert a download row and retrieve it by id
- [ ] Update status from DOWNLOADING → COMPLETED
- [ ] `flowAll()` emits updated list after insert
- [ ] Delete a row by id

**Verify:** `./gradlew test --tests "*.DownloadDaoTest"` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write entity and DAO**

```kotlin
// data/local/db/DownloadEntity.kt
package com.cyrillesondag.xdownloader.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED }

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val tweetUrl: String,
    val videoUrl: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// data/local/db/DownloadDao.kt
package com.cyrillesondag.xdownloader.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun flowAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun findById(id: String): DownloadEntity?

    @Query("UPDATE downloads SET status = :status, fileSizeBytes = :size WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus, size: Long = 0L)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

```kotlin
// data/local/db/AppDatabase.kt
package com.cyrillesondag.xdownloader.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
```

- [ ] **Step 2: Write failing tests**

```kotlin
// test/data/local/DownloadDaoTest.kt
package com.cyrillesondag.xdownloader.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cyrillesondag.xdownloader.data.local.db.AppDatabase
import com.cyrillesondag.xdownloader.data.local.db.DownloadDao
import com.cyrillesondag.xdownloader.data.local.db.DownloadEntity
import com.cyrillesondag.xdownloader.data.local.db.DownloadStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DownloadDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.downloadDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(id: String = "1") = DownloadEntity(
        id = id,
        tweetUrl = "https://x.com/user/status/$id",
        videoUrl = "https://video.twimg.com/video.mp4",
        fileName = "video_$id.mp4",
        filePath = "/Downloads/video_$id.mp4"
    )

    @Test
    fun `insert and retrieve by id`() = runTest {
        dao.insert(entity("abc"))
        val found = dao.findById("abc")
        assertNotNull(found)
        assertEquals("abc", found!!.id)
        assertEquals(DownloadStatus.DOWNLOADING, found.status)
    }

    @Test
    fun `update status to completed`() = runTest {
        dao.insert(entity("x"))
        dao.updateStatus("x", DownloadStatus.COMPLETED, 1_234_567L)
        val updated = dao.findById("x")!!
        assertEquals(DownloadStatus.COMPLETED, updated.status)
        assertEquals(1_234_567L, updated.fileSizeBytes)
    }

    @Test
    fun `flowAll emits inserted items`() = runTest {
        dao.insert(entity("1"))
        dao.insert(entity("2"))
        val list = dao.flowAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun `deleteById removes the row`() = runTest {
        dao.insert(entity("del"))
        dao.deleteById("del")
        assertNull(dao.findById("del"))
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```
./gradlew test --tests "*.DownloadDaoTest"
```
Expected: `FAILED` — compilation errors (classes not yet created)

- [ ] **Step 4: Run tests to verify they pass** (after writing the files above)

```
./gradlew test --tests "*.DownloadDaoTest"
```
Expected: `BUILD SUCCESSFUL` — 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/data/local/db/ \
        app/src/test/java/com/cyrillesondag/xdownloader/data/local/
git commit -m "feat: Room database with DownloadDao"
```

---

## Task 5: Settings DataStore

**Goal:** `SettingsDataStore` wraps DataStore<Preferences> to persist the download folder URI and Cobalt instance URL, with a unit test using a real DataStore in a temp directory.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/data/local/datastore/SettingsDataStore.kt`
- Create: `app/src/test/java/com/cyrillesondag/xdownloader/data/local/SettingsDataStoreTest.kt`

**Acceptance Criteria:**
- [ ] `cobaltUrl` defaults to `https://api.cobalt.tools/`
- [ ] `folderUri` defaults to `null`
- [ ] Persisted values survive a DataStore re-read
- [ ] Setting `cobaltUrl` to blank resets it to the default

**Verify:** `./gradlew test --tests "*.SettingsDataStoreTest"` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write failing tests**

```kotlin
// test/data/local/SettingsDataStoreTest.kt
package com.cyrillesondag.xdownloader.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import com.cyrillesondag.xdownloader.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {

    private lateinit var store: SettingsDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = SettingsDataStore(context)
    }

    @Test
    fun `cobaltUrl default is api cobalt tools`() = runTest {
        assertEquals("https://api.cobalt.tools/", store.cobaltUrl.first())
    }

    @Test
    fun `folderUri default is null`() = runTest {
        assertNull(store.folderUri.first())
    }

    @Test
    fun `saved cobaltUrl is retrieved`() = runTest {
        store.setCobaltUrl("https://my.instance.example.com/")
        assertEquals("https://my.instance.example.com/", store.cobaltUrl.first())
    }

    @Test
    fun `blank cobaltUrl resets to default`() = runTest {
        store.setCobaltUrl("   ")
        assertEquals("https://api.cobalt.tools/", store.cobaltUrl.first())
    }

    @Test
    fun `saved folderUri is retrieved`() = runTest {
        store.setFolderUri("content://com.android.externalstorage.documents/tree/primary%3ADownloads")
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3ADownloads",
            store.folderUri.first()
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "*.SettingsDataStoreTest"
```
Expected: `FAILED` — `Unresolved reference: SettingsDataStore`

- [ ] **Step 3: Write the implementation**

```kotlin
// data/local/datastore/SettingsDataStore.kt
package com.cyrillesondag.xdownloader.data.local.datastore

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

    private val keyCobaltUrl = stringPreferencesKey("cobalt_url")
    private val keyFolderUri = stringPreferencesKey("folder_uri")

    companion object {
        const val DEFAULT_COBALT_URL = "https://api.cobalt.tools/"
    }

    val cobaltUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyCobaltUrl]?.takeIf { it.isNotBlank() } ?: DEFAULT_COBALT_URL
    }

    val folderUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[keyFolderUri]
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
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew test --tests "*.SettingsDataStoreTest"
```
Expected: `BUILD SUCCESSFUL` — 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/data/local/datastore/ \
        app/src/test/java/com/cyrillesondag/xdownloader/data/local/SettingsDataStoreTest.kt
git commit -m "feat: SettingsDataStore for folder URI and Cobalt URL"
```

---

## Task 6: Notification Helper

**Goal:** `NotificationHelper` creates the notification channel and builds progress/success/failure notifications. No automated tests — verified visually in Task 11.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/util/NotificationHelper.kt`

**Acceptance Criteria:**
- [ ] Channel `downloads` created on first call to `createChannel()`
- [ ] `buildProgress(id, percent)` returns a valid `Notification`
- [ ] `buildSuccess(id, folderName)` returns a notification with an Open action
- [ ] `buildFailure(id, reason)` returns a notification with a Retry action

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write `NotificationHelper.kt`**

```kotlin
// util/NotificationHelper.kt
package com.cyrillesondag.xdownloader.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.cyrillesondag.xdownloader.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val CHANNEL_ID = "downloads"
        const val CHANNEL_NAME = "Downloads"
    }

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
    }

    fun buildProgress(notificationId: Int, progressPercent: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading video…")
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun buildSuccess(notificationId: Int, filePath: String): Notification {
        val openIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(filePath), "video/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText("Tap to open")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
    }

    fun buildFailure(notificationId: Int, reason: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()
}
```

- [ ] **Step 2: Verify it compiles**

```
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/util/NotificationHelper.kt
git commit -m "feat: NotificationHelper for download notifications"
```

---

## Task 7: Download Worker

**Goal:** `DownloadWorker` (WorkManager) downloads a video via OkHttp, writes it to the SAF folder URI, updates progress notifications, and updates the Room row on completion/failure.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/worker/DownloadWorker.kt`
- Create: `app/src/test/java/com/cyrillesondag/xdownloader/worker/DownloadWorkerTest.kt`

**Acceptance Criteria:**
- [ ] Worker reads `videoUrl`, `fileName`, `downloadId`, `folderUriStr` from input data
- [ ] On success: Room row updated to `COMPLETED`, success notification shown
- [ ] On HTTP failure: Room row updated to `FAILED`, failure notification shown
- [ ] Retry once on transient network error (WorkManager policy)

**Verify:** `./gradlew test --tests "*.DownloadWorkerTest"` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write failing tests**

```kotlin
// test/worker/DownloadWorkerTest.kt
package com.cyrillesondag.xdownloader.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.testing.TestWorkerBuilder
import com.cyrillesondag.xdownloader.data.local.db.DownloadDao
import com.cyrillesondag.xdownloader.data.local.db.DownloadStatus
import com.cyrillesondag.xdownloader.util.NotificationHelper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class DownloadWorkerTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var dao: DownloadDao
    private lateinit var notificationHelper: NotificationHelper

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = ApplicationProvider.getApplicationContext()
        dao = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun buildWorker(videoUrl: String, folderUri: String = "file:///tmp"): DownloadWorker {
        return TestWorkerBuilder<DownloadWorker>(
            context = context,
            executor = Executors.newSingleThreadExecutor(),
            inputData = androidx.work.workDataOf(
                DownloadWorker.KEY_VIDEO_URL   to videoUrl,
                DownloadWorker.KEY_FILE_NAME   to "test.mp4",
                DownloadWorker.KEY_DOWNLOAD_ID to "test-id",
                DownloadWorker.KEY_FOLDER_URI  to folderUri
            )
        ).build().also { worker ->
            worker.dao = dao
            worker.notificationHelper = notificationHelper
        }
    }

    @Test
    fun `success response marks download as completed`() = runTest {
        server.enqueue(MockResponse().setBody("fake-video-bytes").setResponseCode(200))
        val worker = buildWorker(server.url("/video.mp4").toString())
        val result = worker.doWork()
        assertEquals(Result.success(), result)
        coVerify { dao.updateStatus("test-id", DownloadStatus.COMPLETED, any()) }
    }

    @Test
    fun `server error returns retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val worker = buildWorker(server.url("/video.mp4").toString())
        val result = worker.doWork()
        assertEquals(Result.retry(), result)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "*.DownloadWorkerTest"
```
Expected: `FAILED` — `Unresolved reference: DownloadWorker`

- [ ] **Step 3: Write the worker**

```kotlin
// worker/DownloadWorker.kt
package com.cyrillesondag.xdownloader.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cyrillesondag.xdownloader.data.local.db.DownloadDao
import com.cyrillesondag.xdownloader.data.local.db.DownloadStatus
import com.cyrillesondag.xdownloader.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    var dao: DownloadDao,
    var notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_VIDEO_URL   = "videoUrl"
        const val KEY_TWEET_URL   = "tweetUrl"
        const val KEY_FILE_NAME   = "fileName"
        const val KEY_DOWNLOAD_ID = "downloadId"
        const val KEY_FOLDER_URI  = "folderUri"

        fun buildRequest(
            videoUrl: String,
            tweetUrl: String,
            fileName: String,
            downloadId: String,
            folderUri: String
        ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                KEY_VIDEO_URL   to videoUrl,
                KEY_TWEET_URL   to tweetUrl,
                KEY_FILE_NAME   to fileName,
                KEY_DOWNLOAD_ID to downloadId,
                KEY_FOLDER_URI  to folderUri
            ))
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
    }

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoUrl   = inputData.getString(KEY_VIDEO_URL)   ?: return@withContext Result.failure()
        val tweetUrl   = inputData.getString(KEY_TWEET_URL)   ?: return@withContext Result.failure()
        val fileName   = inputData.getString(KEY_FILE_NAME)   ?: return@withContext Result.failure()
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val folderUri  = inputData.getString(KEY_FOLDER_URI)  ?: return@withContext Result.failure()

        dao.insert(DownloadEntity(
            id = downloadId, tweetUrl = tweetUrl, videoUrl = videoUrl,
            fileName = fileName, filePath = folderUri
        ))
        notificationHelper.createChannel()

        runCatching {
            val response = client.newCall(Request.Builder().url(videoUrl).build()).execute()
            if (!response.isSuccessful) return@withContext Result.retry()

            val body = response.body ?: return@withContext Result.retry()
            val totalBytes = body.contentLength()
            var downloaded = 0L

            val folderDocUri = Uri.parse(folderUri)
            val outputUri = androidx.documentfile.provider.DocumentFile
                .fromTreeUri(applicationContext, folderDocUri)
                ?.createFile("video/mp4", fileName)
                ?.uri ?: return@withContext Result.failure()

            applicationContext.contentResolver.openOutputStream(outputUri)?.use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val percent = (downloaded * 100 / totalBytes).toInt()
                            setForeground(ForegroundInfo(downloadId.hashCode(),
                                notificationHelper.buildProgress(downloadId.hashCode(), percent)))
                        }
                    }
                }
            }

            dao.updateStatus(downloadId, DownloadStatus.COMPLETED, downloaded)
            notificationHelper.buildSuccess(downloadId.hashCode(), outputUri.toString())
        }.getOrElse {
            dao.updateStatus(downloadId, DownloadStatus.FAILED)
            notificationHelper.buildFailure(downloadId.hashCode(), it.message ?: "Unknown error")
            return@withContext Result.failure()
        }

        Result.success()
    }
}
```

- [ ] **Step 4: Add `androidx.documentfile` dependency**

  In `app/build.gradle.kts`, add to the `dependencies` block:
```kotlin
implementation("androidx.documentfile:documentfile:1.0.1")
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew test --tests "*.DownloadWorkerTest"
```
Expected: `BUILD SUCCESSFUL` — 2 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/worker/ \
        app/src/test/java/com/cyrillesondag/xdownloader/worker/ \
        app/build.gradle.kts
git commit -m "feat: DownloadWorker with OkHttp and progress notifications"
```

---

## Task 8: Dependency Injection Modules

**Goal:** Wire all Hilt modules so the entire object graph compiles and injects correctly.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/di/NetworkModule.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/di/AppModule.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/di/WorkerModule.kt`

**Acceptance Criteria:**
- [ ] `./gradlew assembleDebug` succeeds (Hilt generates all factories)
- [ ] No `MissingBinding` errors in Hilt component

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write `NetworkModule.kt`**

```kotlin
// di/NetworkModule.kt
package com.cyrillesondag.xdownloader.di

import com.cyrillesondag.xdownloader.data.local.datastore.SettingsDataStore
import com.cyrillesondag.xdownloader.data.remote.CobaltApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideCobaltApi(
        okHttpClient: OkHttpClient,
        settings: SettingsDataStore
    ): CobaltApi {
        val baseUrl = runBlocking { settings.cobaltUrl.first() }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CobaltApi::class.java)
    }
}
```

> **Note:** `CobaltApi` is provided as a singleton with the URL read once at startup. If the user changes the Cobalt URL in settings, the app must be restarted for it to take effect. This is acceptable for v1.

- [ ] **Step 2: Write `DatabaseModule.kt`**

```kotlin
// di/DatabaseModule.kt
package com.cyrillesondag.xdownloader.di

import android.content.Context
import androidx.room.Room
import com.cyrillesondag.xdownloader.data.local.db.AppDatabase
import com.cyrillesondag.xdownloader.data.local.db.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "xdownloader.db").build()

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}
```

- [ ] **Step 3: Write `AppModule.kt`**

```kotlin
// di/AppModule.kt
package com.cyrillesondag.xdownloader.di

import android.content.Context
import com.cyrillesondag.xdownloader.data.local.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)
}
```

- [ ] **Step 4: Write `WorkerModule.kt`**

```kotlin
// di/WorkerModule.kt
package com.cyrillesondag.xdownloader.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {
    @Binds
    abstract fun bindWorkerFactory(factory: HiltWorkerFactory): WorkerFactory
}
```

- [ ] **Step 5: Verify full build**

```
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/di/
git commit -m "feat: Hilt dependency injection modules"
```

---

## Task 9: Share UI — BottomSheet + ViewModel

**Goal:** `ShareReceiverActivity` (transparent) + `ShareViewModel` + `QualityBottomSheet` Compose composable — the core user-facing feature.

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/share/ShareViewModel.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/share/QualityBottomSheet.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/share/ShareReceiverActivity.kt`
- Create: `app/src/test/java/com/cyrillesondag/xdownloader/ui/share/ShareViewModelTest.kt`

**Acceptance Criteria:**
- [ ] `ShareViewModel.loadVariants(url)` → `UiState.Loaded` with variants on success
- [ ] `ShareViewModel.loadVariants(url)` → `UiState.Error` on repository failure
- [ ] `ShareViewModel.download(variant)` enqueues a WorkManager request
- [ ] BottomSheet shows loading spinner, quality list, and error states

**Verify:** `./gradlew test --tests "*.ShareViewModelTest"` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write failing ViewModel tests**

```kotlin
// test/ui/share/ShareViewModelTest.kt
package com.cyrillesondag.xdownloader.ui.share

import com.cyrillesondag.xdownloader.data.local.datastore.SettingsDataStore
import com.cyrillesondag.xdownloader.data.remote.CobaltRepository
import com.cyrillesondag.xdownloader.data.remote.model.VideoVariant
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CobaltRepository
    private lateinit var settings: SettingsDataStore
    private lateinit var viewModel: ShareViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        settings = mockk()
        coEvery { settings.folderUri } returns flowOf("file:///tmp")
        viewModel = ShareViewModel(repository, settings)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.uiState.value is ShareUiState.Idle)
    }

    @Test
    fun `loadVariants emits Loading then Loaded`() = runTest {
        val variants = listOf(
            VideoVariant("https://video.twimg.com/1280x720/v.mp4", "720p"),
            VideoVariant("https://video.twimg.com/854x480/v.mp4",  "480p")
        )
        coEvery { repository.getVariants(any()) } returns Result.success(variants)

        viewModel.loadVariants("https://x.com/user/status/1")

        val state = viewModel.uiState.value
        assertTrue(state is ShareUiState.Loaded)
        assertEquals(2, (state as ShareUiState.Loaded).variants.size)
    }

    @Test
    fun `loadVariants emits Error on failure`() = runTest {
        coEvery { repository.getVariants(any()) } returns Result.failure(Exception("error.api.link.invalid"))

        viewModel.loadVariants("https://x.com/user/status/2")

        val state = viewModel.uiState.value
        assertTrue(state is ShareUiState.Error)
        assertTrue((state as ShareUiState.Error).message.contains("error.api.link.invalid"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "*.ShareViewModelTest"
```
Expected: `FAILED` — `Unresolved reference: ShareViewModel`

- [ ] **Step 3: Write `ShareViewModel.kt`**

```kotlin
// ui/share/ShareViewModel.kt
package com.cyrillesondag.xdownloader.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.cyrillesondag.xdownloader.data.local.datastore.SettingsDataStore
import com.cyrillesondag.xdownloader.data.remote.CobaltRepository
import com.cyrillesondag.xdownloader.data.remote.model.VideoVariant
import com.cyrillesondag.xdownloader.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Loading : ShareUiState()
    data class Loaded(val variants: List<VideoVariant>) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
    object Downloading : ShareUiState()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: CobaltRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState

    fun loadVariants(tweetUrl: String) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            repository.getVariants(tweetUrl).fold(
                onSuccess = { variants ->
                    if (variants.isEmpty()) _uiState.value = ShareUiState.Error("No video found in this tweet")
                    else _uiState.value = ShareUiState.Loaded(variants)
                },
                onFailure = { _uiState.value = ShareUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun download(context: Context, tweetUrl: String, variant: VideoVariant) {
        viewModelScope.launch {
            val folderUri = settings.folderUri.first()
                ?: run { _uiState.value = ShareUiState.Error("No download folder set. Open Settings."); return@launch }

            val downloadId = UUID.randomUUID().toString()
            val fileName = "xvideo_${System.currentTimeMillis()}.mp4"

            WorkManager.getInstance(context).enqueue(
                DownloadWorker.buildRequest(variant.url, tweetUrl, fileName, downloadId, folderUri)
            )
            _uiState.value = ShareUiState.Downloading
        }
    }
}
```

- [ ] **Step 4: Write `QualityBottomSheet.kt`**

```kotlin
// ui/share/QualityBottomSheet.kt
package com.cyrillesondag.xdownloader.ui.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyrillesondag.xdownloader.data.remote.model.VideoVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    state: ShareUiState,
    onDismiss: () -> Unit,
    onDownload: (VideoVariant) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Download Video", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when (state) {
                is ShareUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ShareUiState.Loaded -> {
                    var selected by remember { mutableStateOf(state.variants.first()) }

                    state.variants.forEach { variant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = variant == selected, onClick = { selected = variant })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = variant == selected, onClick = { selected = variant })
                            Spacer(Modifier.width(8.dp))
                            Text(variant.qualityLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onDownload(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Download")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
                is ShareUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
                is ShareUiState.Downloading -> {
                    Text("Download started! Check your notifications.")
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 5: Write `ShareReceiverActivity.kt`**

```kotlin
// ui/share/ShareReceiverActivity.kt
package com.cyrillesondag.xdownloader.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cyrillesondag.xdownloader.ui.theme.XDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tweetUrl = intent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { extractUrl(it) }

        if (tweetUrl == null) { finish(); return }

        viewModel.loadVariants(tweetUrl)

        setContent {
            XDownloaderTheme {
                val state by viewModel.uiState.collectAsState()
                QualityBottomSheet(
                    state = state,
                    onDismiss = { finish() },
                    onDownload = { variant ->
                        viewModel.download(this, tweetUrl, variant)
                    }
                )
            }
        }
    }

    private fun extractUrl(text: String): String? =
        Regex("""https?://[^\s]+""").find(text)?.value
}
```

- [ ] **Step 6: Run tests to verify they pass**

```
./gradlew test --tests "*.ShareViewModelTest"
```
Expected: `BUILD SUCCESSFUL` — 3 tests passed.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/ui/share/ \
        app/src/test/java/com/cyrillesondag/xdownloader/ui/share/
git commit -m "feat: ShareReceiverActivity with quality selection BottomSheet"
```

---

## Task 10: Main UI — History & Settings

**Goal:** `MainActivity` with two tabs: History (download list from Room) and Settings (folder picker + Cobalt URL).

**Files:**
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/main/history/HistoryViewModel.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/main/history/HistoryScreen.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/main/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/main/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/cyrillesondag/xdownloader/ui/main/MainActivity.kt`

**Acceptance Criteria:**
- [ ] History tab shows a `LazyColumn` of `DownloadEntity` from Room
- [ ] Settings tab shows current folder path and Cobalt URL
- [ ] Tapping "Change Folder" launches `ACTION_OPEN_DOCUMENT_TREE`
- [ ] Saving a custom Cobalt URL persists it via `SettingsDataStore`

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Write `Theme.kt`**

```kotlin
// ui/theme/Theme.kt
package com.cyrillesondag.xdownloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun XDownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
```

- [ ] **Step 2: Write `HistoryViewModel.kt`**

```kotlin
// ui/main/history/HistoryViewModel.kt
package com.cyrillesondag.xdownloader.ui.main.history

import androidx.lifecycle.ViewModel
import com.cyrillesondag.xdownloader.data.local.db.DownloadDao
import com.cyrillesondag.xdownloader.data.local.db.DownloadEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(dao: DownloadDao) : ViewModel() {
    val downloads: Flow<List<DownloadEntity>> = dao.flowAll()
}
```

- [ ] **Step 3: Write `HistoryScreen.kt`**

```kotlin
// ui/main/history/HistoryScreen.kt
package com.cyrillesondag.xdownloader.ui.main.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cyrillesondag.xdownloader.data.local.db.DownloadEntity
import com.cyrillesondag.xdownloader.data.local.db.DownloadStatus

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())

    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads yet. Share a video from X to get started.")
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(downloads) { item -> DownloadCard(item) }
    }
}

@Composable
private fun DownloadCard(item: DownloadEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.fileName, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            val statusText = when (item.status) {
                DownloadStatus.DOWNLOADING -> "Downloading…"
                DownloadStatus.COMPLETED   -> "Done · ${item.fileSizeBytes / 1024} KB"
                DownloadStatus.FAILED      -> "Failed"
            }
            Text(statusText, style = MaterialTheme.typography.bodySmall,
                color = if (item.status == DownloadStatus.FAILED)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 4: Write `SettingsViewModel.kt`**

```kotlin
// ui/main/settings/SettingsViewModel.kt
package com.cyrillesondag.xdownloader.ui.main.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyrillesondag.xdownloader.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: SettingsDataStore) : ViewModel() {

    val cobaltUrl = settings.cobaltUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val folderUri = settings.folderUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}
```

- [ ] **Step 5: Write `SettingsScreen.kt`**

```kotlin
// ui/main/settings/SettingsScreen.kt
package com.cyrillesondag.xdownloader.ui.main.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val cobaltUrl by viewModel.cobaltUrl.collectAsState()
    val folderUri by viewModel.folderUri.collectAsState()
    var cobaltUrlDraft by remember(cobaltUrl) { mutableStateOf(cobaltUrl) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { viewModel.onFolderSelected(context, it) } }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("Download folder", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            text = folderUri ?: "No folder selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Change Folder") }

        Spacer(Modifier.height(24.dp))
        Text("Cobalt instance URL", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = cobaltUrlDraft,
            onValueChange = { cobaltUrlDraft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.saveCobaltUrl(cobaltUrlDraft) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}
```

- [ ] **Step 6: Write `MainActivity.kt`**

```kotlin
// ui/main/MainActivity.kt
package com.cyrillesondag.xdownloader.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyrillesondag.xdownloader.ui.main.history.HistoryScreen
import com.cyrillesondag.xdownloader.ui.main.settings.SettingsScreen
import com.cyrillesondag.xdownloader.ui.theme.XDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { XDownloaderTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val tabs = listOf("history" to Icons.Default.History, "settings" to Icons.Default.Settings)
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index; navController.navigate(label) { launchSingleTop = true } },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label.replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "history", modifier = Modifier.padding(padding)) {
            composable("history")  { HistoryScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 7: Verify it compiles**

```
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/cyrillesondag/xdownloader/ui/
git commit -m "feat: MainActivity with History and Settings screens"
```

---

## Task 11: AndroidManifest & Final Integration

**Goal:** Wire the share intent filter, declare all permissions and activities, and do a final end-to-end smoke test on a physical device or emulator.

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Acceptance Criteria:**
- [ ] Sharing an X link shows `ShareReceiverActivity` in the Android share sheet
- [ ] Quality BottomSheet appears and lists video qualities
- [ ] Selecting a quality and tapping Download triggers a foreground notification
- [ ] Video file appears in the configured folder
- [ ] Opening the app directly shows the History and Settings tabs

**Verify:** Install on device/emulator → share an X tweet with video → download completes successfully.

**Steps:**

- [ ] **Step 1: Write `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application
        android:name=".XDownloaderApp"
        android:label="X Downloader"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.XDownloader">

        <!-- Main launcher activity -->
        <activity
            android:name=".ui.main.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Share receiver: transparent activity, no taskbar entry -->
        <activity
            android:name=".ui.share.ShareReceiverActivity"
            android:exported="true"
            android:theme="@style/Theme.Transparent"
            android:excludeFromRecents="true">
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>

        <!-- WorkManager initialization is provided by XDownloaderApp.workManagerConfiguration -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
    </application>

</manifest>
```

- [ ] **Step 2: Add transparent theme to `res/values/themes.xml`**

```xml
<style name="Theme.Transparent" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:backgroundDimEnabled">true</item>
</style>
```

- [ ] **Step 3: Request POST_NOTIFICATIONS permission at runtime in `ShareReceiverActivity`**

  Add to `ShareReceiverActivity.onCreate()`, before `setContent`:
```kotlin
if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
    }
}
```

- [ ] **Step 4: Final build**

```
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Install and smoke-test on device/emulator**

```bash
./gradlew installDebug
```

  Manual test steps:
  1. Open X app → find a tweet with a video → tap Share → select **X Downloader**
  2. Verify the BottomSheet appears with a spinner, then shows quality options
  3. Select a quality → tap Download
  4. Verify a notification appears with download progress
  5. Verify the video appears in the configured folder
  6. Open X Downloader directly → verify the History tab shows the completed download
  7. Go to Settings → change the Cobalt URL → save → verify it persists after reopening

- [ ] **Step 6: Final commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/values/themes.xml \
        app/src/main/java/com/cyrillesondag/xdownloader/ui/share/ShareReceiverActivity.kt
git commit -m "feat: AndroidManifest with share intent filter and final integration"
```
