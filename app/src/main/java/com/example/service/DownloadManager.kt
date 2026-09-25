package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class DownloadEvent {
    data class Progress(val videoId: String, val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadEvent()
    data class Completed(val videoId: String, val filePath: String) : DownloadEvent()
    data class Failed(val videoId: String, val reason: String) : DownloadEvent()
    data class Cancelled(val videoId: String) : DownloadEvent()
}

class VideoDownloadManager(
    private val context: Context,
    private val videoDao: VideoDao,
    private val repository: VideoRepository,
    private val scope: CoroutineScope
) {
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<DownloadEvent>()
    val events: SharedFlow<DownloadEvent> = _events

    fun isDownloading(videoId: String): Boolean {
        return activeJobs[videoId]?.isActive == true
    }

    fun startDownload(video: VideoEntity) {
        if (isDownloading(video.id)) return

        val job = scope.launch(Dispatchers.IO) {
            val destFile = repository.getDownloadFileForVideo(video.id)
            val tempFile = File(destFile.parentFile, "temp_${video.id}.tmp")

            try {
                videoDao.updateDownloadProgress(
                    id = video.id,
                    progress = 0,
                    downloadedBytes = 0L,
                    status = VideoEntity.DOWNLOAD_STATUS_DOWNLOADING
                )

                val request = Request.Builder()
                    .url(video.videoUrl)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP Error: ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val totalBytes = if (contentLength > 0) contentLength else video.fileSizeBytes

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloaded = 0L
                        var lastProgressUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val now = System.currentTimeMillis()
                            // Update progress periodically or on percentage change
                            if (now - lastProgressUpdate > 300) {
                                val progress = if (totalBytes > 0) {
                                    ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
                                } else 0

                                videoDao.updateDownloadProgress(
                                    id = video.id,
                                    progress = progress,
                                    downloadedBytes = downloaded,
                                    status = VideoEntity.DOWNLOAD_STATUS_DOWNLOADING
                                )
                                _events.emit(DownloadEvent.Progress(video.id, progress, downloaded, totalBytes))
                                lastProgressUpdate = now
                            }
                        }
                        output.flush()
                    }
                }

                // Rename temp file to final destination
                if (destFile.exists()) {
                    destFile.delete()
                }
                tempFile.renameTo(destFile)

                val finalSize = destFile.length()
                val downloadDate = System.currentTimeMillis()

                videoDao.updateDownloadComplete(
                    id = video.id,
                    localPath = destFile.absolutePath,
                    fileSizeBytes = finalSize,
                    downloadDate = downloadDate
                )

                _events.emit(DownloadEvent.Completed(video.id, destFile.absolutePath))
                Log.d("DownloadManager", "Download complete for ${video.id}, size: $finalSize")

            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed for ${video.id}", e)
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                if (e is kotlinx.coroutines.CancellationException) {
                    videoDao.resetDownload(video.id)
                    _events.emit(DownloadEvent.Cancelled(video.id))
                } else {
                    videoDao.updateDownloadProgress(
                        id = video.id,
                        progress = 0,
                        downloadedBytes = 0L,
                        status = VideoEntity.DOWNLOAD_STATUS_FAILED
                    )
                    _events.emit(DownloadEvent.Failed(video.id, e.message ?: "ডাউনলোড ব্যর্থ হয়েছে"))
                }
            } finally {
                activeJobs.remove(video.id)
            }
        }

        activeJobs[video.id] = job
    }

    fun pauseOrCancelDownload(videoId: String) {
        val job = activeJobs[videoId]
        if (job != null && job.isActive) {
            job.cancel()
            activeJobs.remove(videoId)
        }
        scope.launch(Dispatchers.IO) {
            val destFile = repository.getDownloadFileForVideo(videoId)
            val tempFile = File(destFile.parentFile, "temp_${videoId}.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            videoDao.resetDownload(videoId)
            _events.emit(DownloadEvent.Cancelled(videoId))
        }
    }
}
