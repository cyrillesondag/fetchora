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

        // Ignore all GET requests (in this case url can be passed as parameter)
        if (originalRequest.method == "GET"){
            return chain.proceed(originalRequest)

        }

        // Get the latest URL from DataStore synchronously on the network thread
        val cobaltUrlString = runBlocking { settings.cobaltUrl.first() }
        val cobaltHttpUrl = cobaltUrlString.toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        // Build the new URL by replacing the scheme, host and port from settings
        val newUrl = originalRequest.url.newBuilder()
            .scheme(cobaltHttpUrl.scheme)
            .host(cobaltHttpUrl.host)
            .port(cobaltHttpUrl.port)
            .build()

        val requestBuilder = originalRequest.newBuilder()
            .url(newUrl)

        // Add Authorization header if API key is configured
        val apiKey = runBlocking { settings.cobaltApiKey.first() }
        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Api-Key $apiKey")
        }

        return chain.proceed(requestBuilder.build())
    }
}
