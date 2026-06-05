package org.mediadownloader.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun flowAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun findById(id: String): DownloadEntity?

    @Query("UPDATE downloads SET status = :status, fileSizeBytes = :size WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus, size: Long = 0L)

    @Query("UPDATE downloads SET status = 'FAILED' WHERE status = 'DOWNLOADING'")
    suspend fun cleanOrphanedDownloads()

    @Query("UPDATE downloads SET filePath = :path WHERE id = :id")
    suspend fun updateFilePath(id: String, path: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
