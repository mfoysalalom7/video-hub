package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.player.CustomVideoPlayer
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VideoViewModel

enum class NavigationTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("হোম", Icons.Default.Home),
    OFFLINE("অফলাইন", Icons.Default.DownloadForOffline),
    DOWNLOADS("ম্যানেজার", Icons.Default.Downloading),
    LIBRARY("লাইব্রেরি", Icons.Default.VideoLibrary)
}

class MainActivity : ComponentActivity() {
    private val viewModel: VideoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: VideoViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showAddCustomVideoDialog by remember { mutableStateOf(false) }

    // Collect states
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val offlineVideos by viewModel.offlineVideos.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val playingVideo by viewModel.currentlyPlayingVideo.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val offlineSearchQuery by viewModel.offlineSearchQuery.collectAsStateWithLifecycle()
    val isOfflineGridView by viewModel.isOfflineGridView.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Display messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (tab == NavigationTab.OFFLINE && offlineVideos.isNotEmpty()) {
                                            Badge { Text("${offlineVideos.size}") }
                                        } else if (tab == NavigationTab.DOWNLOADS && activeDownloads.isNotEmpty()) {
                                            Badge { Text("${activeDownloads.size}") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                }
                            },
                            label = { Text(tab.label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            videos = allVideos,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            selectedCategory = selectedCategory,
                            onSelectCategory = { viewModel.selectedCategory.value = it },
                            onVideoClick = { video -> viewModel.playVideo(video) },
                            onDownloadClick = { video -> viewModel.startDownload(video) },
                            onToggleFavorite = { video -> viewModel.toggleFavorite(video) },
                            onAddCustomVideoClick = { showAddCustomVideoDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    NavigationTab.OFFLINE -> {
                        DownloadsScreen(
                            offlineVideos = offlineVideos,
                            storageInfo = storageInfo,
                            searchQuery = offlineSearchQuery,
                            onSearchQueryChange = { viewModel.offlineSearchQuery.value = it },
                            isGridView = isOfflineGridView,
                            onToggleGridView = { viewModel.isOfflineGridView.value = !isOfflineGridView },
                            onVideoClick = { video -> viewModel.playVideo(video) },
                            onDeleteVideo = { videoId -> viewModel.deleteDownloadedVideo(videoId) },
                            onClearAllDownloads = { viewModel.clearAllDownloadedVideos() },
                            onNavigateToExplore = { currentTab = NavigationTab.HOME },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    NavigationTab.DOWNLOADS -> {
                        DownloadManagerScreen(
                            activeDownloads = activeDownloads,
                            completedDownloads = offlineVideos,
                            onCancelDownload = { videoId -> viewModel.pauseOrCancelDownload(videoId) },
                            onPlayVideo = { video -> viewModel.playVideo(video) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    NavigationTab.LIBRARY -> {
                        LibraryScreen(
                            favoriteVideos = favoriteVideos,
                            watchHistory = watchHistory,
                            storageInfo = storageInfo,
                            onPlayVideo = { video -> viewModel.playVideo(video) },
                            onImportLocalVideo = { uri, name -> viewModel.importLocalVideoUri(uri, name) },
                            onClearHistory = { viewModel.clearHistory() },
                            onAddCustomVideoClick = { showAddCustomVideoDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Active Player Overlay
        playingVideo?.let { video ->
            CustomVideoPlayer(
                video = video,
                onClose = { viewModel.stopPlayback() },
                onProgressUpdate = { pos, dur -> viewModel.onPlayerProgress(pos, dur) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom Video Link Dialog
        if (showAddCustomVideoDialog) {
            AddCustomVideoDialog(
                onDismiss = { showAddCustomVideoDialog = false },
                onAddVideo = { title, url, category, desc ->
                    viewModel.addCustomVideo(title, url, category, desc)
                }
            )
        }
    }
}
