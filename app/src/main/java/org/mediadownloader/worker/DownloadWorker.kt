package org.mediadownloader.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.*
import org.mediadownloader.data.local.db.DownloadDao
import org.mediadownloader.data.local.db.DownloadEntity
import org.mediadownloader.data.local.db.DownloadStatus
import org.mediadownloader.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: DownloadDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_VIDEO_URL   = "videoUrl"
        const val KEY_TWEET_URL   = "tweetUrl"
        const val KEY_FILE_NAME   = "fileName"
        const val KEY_DOWNLOAD_ID = "downloadId"
        const val KEY_FOLDER_URI  = "folderUri"
        const val KEY_PERCENT     = "percent"

        fun buildRequest(
            videoUrl: String,
            tweetUrl: String,
            fileName: String,
            downloadId: String,
            folderUri: String
        ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                KEY_VIDEO_URL   to videoUrl,
                KEY_TWEET_URL   to tweetUrl,
                KEY_FILE_NAME   to fileName,
                KEY_DOWNLOAD_ID to downloadId,
                KEY_FOLDER_URI  to folderUri
            ))
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
    }

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoUrl   = inputData.getString(KEY_VIDEO_URL)   ?: return@withContext Result.failure()
        val tweetUrl   = inputData.getString(KEY_TWEET_URL)   ?: return@withContext Result.failure()
        val fileName   = inputData.getString(KEY_FILE_NAME)   ?: return@withContext Result.failure()
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val folderUri  = inputData.getString(KEY_FOLDER_URI)  ?: return@withContext Result.failure()

        dao.insert(DownloadEntity(
            id = downloadId, tweetUrl = tweetUrl, videoUrl = videoUrl,
            fileName = fileName, filePath = folderUri
        ))
        notificationHelper.createChannel()

        runCatching {
            val response = client.newCall(Request.Builder().url(videoUrl).build()).execute()
            if (!response.isSuccessful) return@withContext Result.retry()

            val body = response.body ?: return@withContext Result.retry()
            val totalBytes = body.contentLength()
            var downloaded = 0L

            val folderDocUri = Uri.parse(folderUri)
            val outputUri = DocumentFile
                .fromTreeUri(applicationContext, folderDocUri)
                ?.createFile("video/mp4", fileName)
                ?.uri ?: return@withContext Result.failure()

            applicationContext.contentResolver.openOutputStream(outputUri)?.use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val percent = (downloaded * 100 / totalBytes).toInt()
                            setProgress(workDataOf(KEY_PERCENT to percent))
                            setForeground(ForegroundInfo(
                                downloadId.hashCode(),
                                notificationHelper.buildProgress(downloadId.hashCode(), percent)
                            ))
                        }
                    }
                }
            }

            dao.updateStatus(downloadId, DownloadStatus.COMPLETED, downloaded)
            notificationHelper.buildSuccess(downloadId.hashCode(), outputUri.toString())
        }.getOrElse {
            dao.updateStatus(downloadId, DownloadStatus.FAILED)
            notificationHelper.buildFailure(downloadId.hashCode(), it.message ?: "Unknown error")
            return@withContext Result.failure()
        }

        Result.success()
    }
}
