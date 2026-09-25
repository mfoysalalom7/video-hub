package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val category: String,
    val fileSizeBytes: Long,
    val downloadStatus: String = DOWNLOAD_STATUS_NOT_DOWNLOADED,
    val downloadProgress: Int = 0,
    val downloadedBytes: Long = 0L,
    val localFilePath: String? = null,
    val downloadDate: Long? = null,
    val lastWatchedPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isCustomImported: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DOWNLOAD_STATUS_NOT_DOWNLOADED = "NOT_DOWNLOADED"
        const val DOWNLOAD_STATUS_DOWNLOADING = "DOWNLOADING"
        const val DOWNLOAD_STATUS_PAUSED = "PAUSED"
        const val DOWNLOAD_STATUS_COMPLETED = "COMPLETED"
        const val DOWNLOAD_STATUS_FAILED = "FAILED"
    }

    val isOfflineReady: Boolean
        get() = downloadStatus == DOWNLOAD_STATUS_COMPLETED && !localFilePath.isNullOrEmpty()
}

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
