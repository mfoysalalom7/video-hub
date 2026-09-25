package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE downloadStatus = 'COMPLETED' ORDER BY downloadDate DESC")
    fun getOfflineVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE downloadStatus IN ('DOWNLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    fun getVideoById(id: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoByIdDirect(id: String): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideosIfNotExist(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET downloadProgress = :progress, downloadedBytes = :downloadedBytes, downloadStatus = :status WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, progress: Int, downloadedBytes: Long, status: String)

    @Query("UPDATE videos SET downloadStatus = 'COMPLETED', downloadProgress = 100, localFilePath = :localPath, downloadedBytes = :fileSizeBytes, downloadDate = :downloadDate WHERE id = :id")
    suspend fun updateDownloadComplete(id: String, localPath: String, fileSizeBytes: Long, downloadDate: Long)

    @Query("UPDATE videos SET downloadStatus = 'NOT_DOWNLOADED', downloadProgress = 0, downloadedBytes = 0, localFilePath = NULL, downloadDate = NULL WHERE id = :id")
    suspend fun resetDownload(id: String)

    @Query("UPDATE videos SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("UPDATE videos SET lastWatchedPositionMs = :positionMs WHERE id = :id")
    suspend fun updateWatchPosition(id: String, positionMs: Long)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    // Watch History
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 50")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteHistoryByVideoId(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}
