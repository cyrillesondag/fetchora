package org.mediadownloader.worker

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mediadownloader.data.local.db.DownloadDao
import org.mediadownloader.data.local.db.DownloadEntity
import org.mediadownloader.data.local.db.DownloadStatus
import org.mediadownloader.util.NotificationHelper

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: DownloadDao,
    private val notificationHelper: NotificationHelper,
    private val client: OkHttpClient
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

        var outputDocumentFile: DocumentFile? = null

        runCatching {
            val response = client.newCall(Request.Builder().url(videoUrl).build()).execute()
            if (!response.isSuccessful) return@withContext Result.retry()

            val body = response.body ?: return@withContext Result.retry()
            val totalBytes = body.contentLength()
            var downloaded = 0L

            val folderDocUri = folderUri.toUri()
            val docFile = if (folderDocUri.scheme == "file") {
                val dir = java.io.File(folderDocUri.path!!)
                dir.mkdirs()
                DocumentFile.fromFile(dir).createFile("video/mp4", fileName)
            } else {
                DocumentFile.fromTreeUri(applicationContext, folderDocUri)
                    ?.createFile("video/mp4", fileName)
            } ?: throw IllegalStateException("Could not create file in target folder")
            
            outputDocumentFile = docFile
            val outputUri = docFile.uri

            dao.updateFilePath(downloadId, outputUri.toString())

            applicationContext.contentResolver.openOutputStream(outputUri)?.use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var lastPercent = -1
                    
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val percent = (downloaded * 100 / totalBytes).toInt()
                            if (percent > lastPercent) {
                                lastPercent = percent
                                setProgress(workDataOf(KEY_PERCENT to percent))
                                setForeground(ForegroundInfo(
                                    downloadId.hashCode(),
                                    notificationHelper.buildProgress(percent)
                                ))
                            }
                        }
                    }
                }
            }

            dao.updateStatus(downloadId, DownloadStatus.COMPLETED, downloaded)
            val successNotification = notificationHelper.buildSuccess(downloadId.hashCode(), outputUri.toString())
            notificationHelper.show(downloadId.hashCode(), successNotification)
        }.getOrElse {
            outputDocumentFile?.delete()
            dao.updateStatus(downloadId, DownloadStatus.FAILED)
            val failureNotification = notificationHelper.buildFailure(it.message ?: "Unknown error")
            notificationHelper.show(downloadId.hashCode(), failureNotification)
            return@withContext Result.failure()
        }

        Result.success()
    }
}
