package org.mediadownloader.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED }

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val tweetUrl: String,
    val videoUrl: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val createdAt: Long = System.currentTimeMillis()
)
