package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import com.example.data.local.HistoryEntity
import com.example.data.model.StorageInfo
import com.example.data.model.VideoCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(
    private val videoDao: VideoDao,
    private val context: Context
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val offlineVideos: Flow<List<VideoEntity>> = videoDao.getOfflineVideos()
    val activeDownloads: Flow<List<VideoEntity>> = videoDao.getActiveDownloads()
    val favoriteVideos: Flow<List<VideoEntity>> = videoDao.getFavoriteVideos()
    val watchHistory: Flow<List<HistoryEntity>> = videoDao.getAllHistory()

    fun getVideoById(id: String): Flow<VideoEntity?> = videoDao.getVideoById(id)

    suspend fun getVideoByIdDirect(id: String): VideoEntity? = videoDao.getVideoByIdDirect(id)

    private val downloadsDir: File
        get() {
            val dir = File(context.filesDir, "downloads")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getDownloadFileForVideo(videoId: String): File {
        return File(downloadsDir, "video_$videoId.mp4")
    }

    suspend fun initializeSampleVideosIfNeeded() = withContext(Dispatchers.IO) {
        val count = videoDao.getVideoCount()
        if (count == 0) {
            val sampleVideos = getInitialCuratedVideos()
            videoDao.insertVideosIfNotExist(sampleVideos)
        }
    }

    suspend fun insertCustomVideo(
        title: String,
        videoUrl: String,
        category: String,
        description: String = "",
        author: String = "ইউজার আপলোড"
    ): VideoEntity = withContext(Dispatchers.IO) {
        val videoId = "custom_${System.currentTimeMillis()}"
        val newVideo = VideoEntity(
            id = videoId,
            title = title,
            description = description.ifBlank { "ব্যক্তিগত বা কাস্টম যোগ করা ভিডিও" },
            author = author,
            videoUrl = videoUrl,
            thumbnailUrl = "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=600&auto=format&fit=crop&q=80",
            durationSeconds = 180,
            category = category,
            fileSizeBytes = 15_000_000L,
            isCustomImported = true
        )
        videoDao.insertVideo(newVideo)
        newVideo
    }

    suspend fun toggleFavorite(videoId: String, currentFav: Boolean) = withContext(Dispatchers.IO) {
        videoDao.updateFavorite(videoId, !currentFav)
    }

    suspend fun recordWatchHistory(video: VideoEntity, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updateWatchPosition(video.id, positionMs)
        val history = HistoryEntity(
            videoId = video.id,
            title = video.title,
            thumbnailUrl = video.thumbnailUrl,
            positionMs = positionMs,
            durationMs = durationMs
        )
        videoDao.insertHistory(history)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        videoDao.clearHistory()
    }

    /**
     * Delete an individual downloaded video:
     * - Deletes physical file from device storage
     * - Resets download status in database to NOT_DOWNLOADED
     */
    suspend fun deleteDownloadedVideo(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val video = videoDao.getVideoByIdDirect(videoId)
            if (video != null && !video.localFilePath.isNullOrEmpty()) {
                val file = File(video.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Also check standard downloads directory
            val standardFile = getDownloadFileForVideo(videoId)
            if (standardFile.exists()) {
                standardFile.delete()
            }
            videoDao.resetDownload(videoId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete completely (e.g. for custom imported video)
     */
    suspend fun deleteVideoRecord(video: VideoEntity) = withContext(Dispatchers.IO) {
        deleteDownloadedVideo(video.id)
        videoDao.deleteVideoById(video.id)
    }

    /**
     * Clear ALL downloaded videos:
     * - Deletes all files in the downloads directory
     * - Resets all video entities download status
     */
    suspend fun clearAllDownloadedVideos(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete all files in downloads folder
            val dir = downloadsDir
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }

            // Reset all videos that were marked as downloaded
            val offline = videoDao.getOfflineVideos().first()
            for (vid in offline) {
                videoDao.resetDownload(vid.id)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Calculate storage details:
     * - Downloaded videos size in MB
     * - Total and free device storage in GB
     */
    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        var downloadedBytes = 0L
        val dir = downloadsDir
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    downloadedBytes += file.length()
                }
            }
        }

        val offlineCount = videoDao.getOfflineVideos().first().size

        // Calculate device storage using StatFs
        val stat = StatFs(context.filesDir.absolutePath)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalDeviceBytes = totalBlocks * blockSize
        val freeDeviceBytes = availableBlocks * blockSize

        val downloadedMb = downloadedBytes / (1024.0 * 1024.0)
        val totalGb = totalDeviceBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGb = freeDeviceBytes / (1024.0 * 1024.0 * 1024.0)

        StorageInfo(
            downloadedVideosSizeMb = downloadedMb,
            totalDeviceStorageGb = totalGb,
            freeDeviceStorageGb = freeGb,
            offlineVideoCount = offlineCount
        )
    }

    private fun getInitialCuratedVideos(): List<VideoEntity> {
        return listOf(
            VideoEntity(
                id = "vid_1",
                title = "সুন্দরবনের রহস্য ও রয়েল বেঙ্গল টাইগার",
                description = "ম্যানগ্রোভ বনাঞ্চল সুন্দরবনের অপার সৌন্দর্য, নদীমাতৃক প্রকৃতি এবং বন্যপ্রাণীদের জীবনযাত্রা নিয়ে একটি অসাধারণ প্রামাণ্যচিত্র। অফলাইনে দেখার জন্য এখনই ডাউনলোড করুন।",
                author = "ওয়াইল্ডলাইফ বাংলাদেশ",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 596,
                category = VideoCategory.NATURE.englishName,
                fileSizeBytes = 35_500_000L
            ),
            VideoEntity(
                id = "vid_2",
                title = "মোবাইল দিয়ে ৪K ভিডিও এডিটিং মাস্টারক্লাস",
                description = "স্মার্টফোনে সিনেমাটিক ভিডিও এডিটিং, কালার গ্রেডিং এবং সাউন্ড এফেক্টস শেখার সহজ বাংলা টিউটোরিয়াল। ইন্টারনেট ছাড়াও যেকোনো সময় দেখুন।",
                author = "ক্রিয়েটর হাব বাংলা",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 653,
                category = VideoCategory.TUTORIAL.englishName,
                fileSizeBytes = 48_200_000L
            ),
            VideoEntity(
                id = "vid_3",
                title = "মহাকাশ স্টেশন থেকে পৃথিবীর অদ্ভুত রূপ",
                description = "আন্তর্জাতিক মহাকাশ স্টেশন থেকে ধারণকৃত পৃথিবীর বায়ুমণ্ডল, রাত ও দিনের পরিবর্তন এবং অরোরার মনোমুগ্ধকর হাই ডেফিনিশন দৃশ্য।",
                author = "বিজ্ঞান ও প্রযুক্তি বার্তা",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 15,
                category = VideoCategory.TECH.englishName,
                fileSizeBytes = 15_800_000L
            ),
            VideoEntity(
                id = "vid_4",
                title = "পাহাড়ের বৃষ্টি ও রিলাক্সিং অ্যাম্বিয়েন্স",
                description = "শান্তিময় বৃষ্টির শব্দ, পাহাড়ের কোল ঘেঁষে মেঘের ভেলা এবং পাখির কলকাকলি। কাজ বা পড়ার সময় মানসিক প্রশান্তির জন্য আদর্শ।",
                author = "রিলাক্সেশন জোন",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 15,
                category = VideoCategory.MUSIC.englishName,
                fileSizeBytes = 14_900_000L
            ),
            VideoEntity(
                id = "vid_5",
                title = "সায়েন্স ফিকশন শর্ট ফিল্ম: দ্য লাস্ট ড্রাইভ",
                description = "ভবিষ্যতের এক রোবোটিক পৃথিবীর গল্প নিয়ে নির্মিত পুরষ্কারপ্রাপ্ত শর্ট ফিল্ম। চমৎকার ভিজ্যুয়াল এফেক্টস ও সাউন্ড ডিজাইন।",
                author = "সিনেমাটিকা স্টুডিও",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 734,
                category = VideoCategory.ENTERTAINMENT.englishName,
                fileSizeBytes = 62_100_000L
            ),
            VideoEntity(
                id = "vid_6",
                title = "কৃত্রিম বুদ্ধিমত্তা ও ভবিষ্যতের রোবোটিক্স বিপ্লব",
                description = "এআই কীভাবে আমাদের দৈনন্দিন জীবন ও কর্মসংস্থান বদলে দিচ্ছে তার বিশদ বিশ্লেষণ। আধুনিক প্রযুক্তিপ্রেমীদের জন্য বিশেষ পর্ব।",
                author = "ফিউচার টেক",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 47,
                category = VideoCategory.TECH.englishName,
                fileSizeBytes = 19_400_000L
            )
        )
    }
}
