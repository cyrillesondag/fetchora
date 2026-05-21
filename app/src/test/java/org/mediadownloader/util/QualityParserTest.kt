package org.mediadownloader.util

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
    fun `square url returns dimension as quality`() {
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
    fun `fallback index is used in label`() {
        val url = "https://cdn.example.com/video.mp4"
        assertEquals("Quality 3", QualityParser.parseQuality(url, fallbackIndex = 3))
    }
}
