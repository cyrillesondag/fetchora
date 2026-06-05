package org.mediadownloader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.local.network.HostSettingInterceptor
import org.mediadownloader.data.remote.CobaltApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // For large file downloads
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideCobaltApi(okHttpClient: OkHttpClient, settings: SettingsDataStore): CobaltApi {
        // Use a placeholder URL. HostSettingInterceptor will replace it with the real one from settings.
        val placeholderUrl = "https://cobalt.example.com/"

        val client = okHttpClient.newBuilder()
            .addInterceptor(HostSettingInterceptor(settings))
            .build()


        return Retrofit.Builder()
            .baseUrl(placeholderUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CobaltApi::class.java)
    }
}
