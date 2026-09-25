package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryEntity
import com.example.data.local.VideoEntity
import com.example.data.model.StorageInfo
import com.example.data.model.VideoCategory
import com.example.data.repository.VideoRepository
import com.example.service.DownloadEvent
import com.example.service.VideoDownloadManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = VideoRepository(database.videoDao(), application)
    private val downloadManager = VideoDownloadManager(
        context = application,
        videoDao = database.videoDao(),
        repository = repository,
        scope = viewModelScope
    )

    // State flows from Room
    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineVideos: StateFlow<List<VideoEntity>> = repository.offlineVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<VideoEntity>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteVideos: StateFlow<List<VideoEntity>> = repository.favoriteVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<HistoryEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player State
    private val _currentlyPlayingVideo = MutableStateFlow<VideoEntity?>(null)
    val currentlyPlayingVideo: StateFlow<VideoEntity?> = _currentlyPlayingVideo.asStateFlow()

    // Filter & Search states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow(VideoCategory.ALL)
    val offlineSearchQuery = MutableStateFlow("")
    val isOfflineGridView = MutableStateFlow(false)

    // Storage Info State
    private val _storageInfo = MutableStateFlow(
        StorageInfo(
            downloadedVideosSizeMb = 0.0,
            totalDeviceStorageGb = 32.0,
            freeDeviceStorageGb = 16.0,
            offlineVideoCount = 0
        )
    )
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    // Feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSampleVideosIfNeeded()
            refreshStorageInfo()
        }

        // Listen for download completion or events to refresh storage and show messages
        viewModelScope.launch {
            downloadManager.events.collect { event ->
                when (event) {
                    is DownloadEvent.Completed -> {
                        _userMessage.value = "ডাউনলোড সম্পন্ন হয়েছে! এখন অফলাইনে দেখুন।"
                        refreshStorageInfo()
                    }
                    is DownloadEvent.Failed -> {
                        _userMessage.value = "ডাউনলোড ব্যর্থ: ${event.reason}"
                    }
                    is DownloadEvent.Cancelled -> {
                        _userMessage.value = "ডাউনলোড বাতিল করা হয়েছে।"
                    }
                    is DownloadEvent.Progress -> {
                        // Managed by DB flow
                    }
                }
            }
        }
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            val info = repository.getStorageInfo()
            _storageInfo.value = info
        }
    }

    fun playVideo(video: VideoEntity) {
        _currentlyPlayingVideo.value = video
    }

    fun stopPlayback() {
        _currentlyPlayingVideo.value = null
    }

    fun onPlayerProgress(positionMs: Long, durationMs: Long) {
        val current = _currentlyPlayingVideo.value ?: return
        viewModelScope.launch {
            repository.recordWatchHistory(current, positionMs, durationMs)
        }
    }

    fun startDownload(video: VideoEntity) {
        downloadManager.startDownload(video)
        _userMessage.value = "${video.title} ডাউনলোড শুরু হয়েছে..."
    }

    fun pauseOrCancelDownload(videoId: String) {
        downloadManager.pauseOrCancelDownload(videoId)
        refreshStorageInfo()
    }

    /**
     * Delete an individual downloaded video
     */
    fun deleteDownloadedVideo(videoId: String) {
        viewModelScope.launch {
            val success = repository.deleteDownloadedVideo(videoId)
            if (success) {
                _userMessage.value = "ভিডিওটি অফলাইন মেমোরি থেকে মুছে ফেলা হয়েছে।"
            } else {
                _userMessage.value = "মুছে ফেলতে সমস্যা হয়েছে।"
            }
            refreshStorageInfo()
        }
    }

    /**
     * Clear ALL downloaded videos at once
     */
    fun clearAllDownloadedVideos() {
        viewModelScope.launch {
            val success = repository.clearAllDownloadedVideos()
            if (success) {
                _userMessage.value = "সকল ডাউনলোড করা ভিডিও মুছে মেমোরি খালি করা হয়েছে।"
            } else {
                _userMessage.value = "সকল ভিডিও মুছতে ব্যর্থ হয়েছে।"
            }
            refreshStorageInfo()
        }
    }

    fun toggleFavorite(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(video.id, video.isFavorite)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _userMessage.value = "দেখার ইতিহাস মুছে ফেলা হয়েছে।"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addCustomVideo(title: String, url: String, category: String, description: String) {
        viewModelScope.launch {
            val newVideo = repository.insertCustomVideo(title, url, category, description)
            _userMessage.value = "নতুন ভিডিও যোগ হয়েছে: ${newVideo.title}"
        }
    }

    fun importLocalVideoUri(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            val title = displayName ?: "ডিভাইসের ভিডিও (${System.currentTimeMillis() % 1000})"
            val video = repository.insertCustomVideo(
                title = title,
                videoUrl = uri.toString(),
                category = VideoCategory.CUSTOM.englishName,
                description = "ফোন থেকে যুক্ত করা লোকাল ভিডিও",
                author = "ডিভাইস স্টোরেজ"
            )
            playVideo(video)
            _userMessage.value = "ভিডিও লোড হয়েছে: $title"
        }
    }
}
