package org.mediadownloader.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mediadownloader.data.local.datastore.SettingsDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    // WorkManager auto-init is disabled in the manifest; FetchoraApp implements Configuration.Provider
    // and supplies HiltWorkerFactory. getInstance() triggers that deferred init on first call.
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
