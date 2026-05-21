package org.mediadownloader.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = true)
@TypeConverters(DownloadStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
