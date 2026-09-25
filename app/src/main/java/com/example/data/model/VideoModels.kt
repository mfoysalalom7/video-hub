package com.example.data.model

import java.util.Locale

enum class VideoCategory(val bengaliName: String, val englishName: String) {
    ALL("সব ভিডিও", "All Videos"),
    NATURE("প্রকৃতি ও বিশ্ব", "Nature & Wildlife"),
    TECH("প্রযুক্তি ও গ্যাজেট", "Tech & Science"),
    TUTORIAL("টিউটোরিয়াল ও শিক্ষা", "Tutorials & Education"),
    ENTERTAINMENT("সিনেমা ও নাটক", "Cinema & Entertainment"),
    MUSIC("গান ও রিলাক্স", "Music & Relaxation"),
    CUSTOM("আমার ভিডিও", "Custom & Local")
}

data class StorageInfo(
    val downloadedVideosSizeMb: Double,
    val totalDeviceStorageGb: Double,
    val freeDeviceStorageGb: Double,
    val offlineVideoCount: Int
)

object FormatUtils {
    fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationMs(totalMs: Long): String {
        return formatDuration((totalMs / 1000).toInt())
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}
