package org.mediadownloader.data.remote

import org.mediadownloader.data.remote.model.CobaltInfo
import org.mediadownloader.data.remote.model.CobaltRequest
import org.mediadownloader.data.remote.model.VideoVariant
import org.mediadownloader.util.QualityParser
import javax.inject.Inject
import javax.inject.Singleton

class CobaltRepositoryException(message: String) : Exception(message)

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
                val url = response.url ?: throw CobaltRepositoryException("The service failed to provide a download link.")
                listOf(VideoVariant(url, QualityParser.parseQuality(url)))
            }
            "error" -> {
                val code = response.error?.code
                val message = when (code) {
                    "error.api.rate_limit" -> "Too many requests. Please try again later."
                    "error.invalid_url" -> "The provided URL is invalid or not supported."
                    else -> "The download service returned an error: ${code ?: "unknown"}"
                }
                throw CobaltRepositoryException(message)
            }
            else -> throw CobaltRepositoryException("Unexpected response from the service (status: ${response.status}).")
        }
    }

    suspend fun getInfo(cobaltUrl: String): Result<CobaltInfo> = runCatching {
        val response = api.info(cobaltUrl)
        response.cobalt
    }
}
