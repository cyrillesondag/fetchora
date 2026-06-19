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
