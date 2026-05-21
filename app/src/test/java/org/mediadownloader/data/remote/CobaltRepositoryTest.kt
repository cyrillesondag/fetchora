package org.mediadownloader.data.remote

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
        assertEquals("480p", result.getOrThrow()[0].qualityLabel)
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
