package org.mediadownloader.data.remote

import org.mediadownloader.data.remote.model.CobaltInfo
import org.mediadownloader.data.remote.model.CobaltRequest
import org.mediadownloader.data.remote.model.VideoVariant
import org.mediadownloader.util.QualityParser
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

    suspend fun getInfo(cobaltUrl: String): Result<CobaltInfo> = runCatching {
        val response = api.info(cobaltUrl)
        response.cobalt
    }
}
