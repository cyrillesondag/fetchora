package org.mediadownloader.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mediadownloader.data.local.db.AppDatabase
import org.mediadownloader.data.local.db.DownloadDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "fetchora.db").build()

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}
