package org.mediadownloader.data.local.network

import okhttp3.Interceptor
import okhttp3.Response
import org.mediadownloader.data.local.datastore.SettingsDataStore

class HostSettingInterceptor(
    private val settings: SettingsDataStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().method == "GET") {
            return chain.proceed(chain.request())
        }
        val cobaltUrl = settings.getCobaltUrl()
        return chain.proceed(chain.request().newBuilder().url(cobaltUrl).build())
    }
}
