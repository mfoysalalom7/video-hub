package com.example

import com.example.data.local.VideoEntity
import com.example.data.model.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFormatDuration() {
        assertEquals("00:45", FormatUtils.formatDuration(45))
        assertEquals("01:30", FormatUtils.formatDuration(90))
        assertEquals("1:05:00", FormatUtils.formatDuration(3900))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("500 KB", FormatUtils.formatFileSize(512_000L))
        assertEquals("35.5 MB", FormatUtils.formatFileSize(37_224_448L))
        assertEquals("1.5 GB", FormatUtils.formatFileSize(1_610_612_736L))
    }

    @Test
    fun testVideoOfflineReadyState() {
        val nonDownloaded = VideoEntity(
            id = "test_1",
            title = "Test",
            description = "Desc",
            author = "Author",
            videoUrl = "https://example.com/test.mp4",
            thumbnailUrl = "https://example.com/thumb.jpg",
            durationSeconds = 120,
            category = "Tech",
            fileSizeBytes = 10_000_000L,
            downloadStatus = VideoEntity.DOWNLOAD_STATUS_NOT_DOWNLOADED,
            localFilePath = null
        )
        assertFalse(nonDownloaded.isOfflineReady)

        val downloaded = nonDownloaded.copy(
            downloadStatus = VideoEntity.DOWNLOAD_STATUS_COMPLETED,
            localFilePath = "/data/user/0/app/files/downloads/video_test_1.mp4"
        )
        assertTrue(downloaded.isOfflineReady)
    }
}
