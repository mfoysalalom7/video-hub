package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.VideoEntity
import com.example.data.model.FormatUtils
import com.example.data.model.VideoCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    videos: List<VideoEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: VideoCategory,
    onSelectCategory: (VideoCategory) -> Unit,
    onVideoClick: (VideoEntity) -> Unit,
    onDownloadClick: (VideoEntity) -> Unit,
    onToggleFavorite: (VideoEntity) -> Unit,
    onAddCustomVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredVideos = remember(videos, searchQuery, selectedCategory) {
        val query = searchQuery.trim()
        videos.filter { video ->
            // Category chip matching
            val matchesCategoryChip = selectedCategory == VideoCategory.ALL ||
                    video.category.equals(selectedCategory.englishName, ignoreCase = true) ||
                    video.category.equals(selectedCategory.bengaliName, ignoreCase = true)

            // Search query matching (matches Title, Category in Bengali or English, or Author)
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                // 1. Check title match
                val matchesTitle = video.title.contains(query, ignoreCase = true)

                // 2. Check category match (both stored string and localized category names)
                val matchesCategoryDirect = video.category.contains(query, ignoreCase = true)
                val matchesCategoryName = VideoCategory.values().filter { it != VideoCategory.ALL }.any { cat ->
                    val queryMatchesCategory = cat.bengaliName.contains(query, ignoreCase = true) ||
                            cat.englishName.contains(query, ignoreCase = true)
                    val videoBelongsToCategory = video.category.equals(cat.englishName, ignoreCase = true) ||
                            video.category.equals(cat.bengaliName, ignoreCase = true)
                    queryMatchesCategory && videoBelongsToCategory
                }

                // 3. Check author or description
                val matchesAuthor = video.author.contains(query, ignoreCase = true)
                val matchesDesc = video.description.contains(query, ignoreCase = true)

                matchesTitle || matchesCategoryDirect || matchesCategoryName || matchesAuthor || matchesDesc
            }

            matchesCategoryChip && matchesSearch
        }
    }

    // Featured Hero Video (first video)
    val heroVideo = remember(videos) { videos.firstOrNull() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VideoHub",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onAddCustomVideoClick,
                        modifier = Modifier.testTag("add_custom_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "লিঙ্ক যোগ করুন",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pinned Search Bar & Category Header at the top of the video list
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "শিরোনাম বা ক্যাটাগরি দিয়ে খুঁজুন...",
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "অনুসন্ধান",
                                tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "মুছুন",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_search_bar")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories horizontal row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_filter_row")
                    ) {
                        items(VideoCategory.values()) { category ->
                            val isSelected = category == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCategory(category) },
                                label = {
                                    Text(
                                        text = category.bengaliName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("category_chip_${category.name}")
                            )
                        }
                    }

                    // Active Search & Filter Status
                    if (searchQuery.isNotBlank() || selectedCategory != VideoCategory.ALL) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildString {
                                    if (searchQuery.isNotBlank()) {
                                        append("\"${searchQuery.trim()}\" ")
                                    }
                                    if (selectedCategory != VideoCategory.ALL) {
                                        append("[${selectedCategory.bengaliName}] ")
                                    }
                                    append("• ${filteredVideos.size} টি ভিডিও")
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(
                                onClick = {
                                    onSearchQueryChange("")
                                    onSelectCategory(VideoCategory.ALL)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("reset_all_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ফিল্টার রিসেট",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Video List Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_video_list"),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Hero Featured Video Card (Show when no search or category filter is active)
                if (searchQuery.isBlank() && selectedCategory == VideoCategory.ALL && heroVideo != null) {
                    item {
                        FeaturedHeroCard(
                            video = heroVideo,
                            onPlayClick = { onVideoClick(heroVideo) },
                            onDownloadClick = { onDownloadClick(heroVideo) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != VideoCategory.ALL)
                                "অনুসন্ধান ফলাফল (${filteredVideos.size})"
                            else
                                "জনপ্রিয় ভিডিও সমূহ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Video Cards List
                if (filteredVideos.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                                .testTag("no_search_results_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "কোনো ভিডিও পাওয়া যায়নি",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank())
                                        "\"$searchQuery\" এর সাথে কোনো ভিডিওর শিরোনাম বা ক্যাটাগরি মেলেনি। প্রকৃতি, প্রযুক্তি, সিনেমা, টিউটোরিয়াল বা গান লিখে অনুসন্ধান করুন।"
                                    else
                                        "এই ক্যাটাগরিতে বর্তমানে কোনো ভিডিও পাওয়া যায়নি।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        onSearchQueryChange("")
                                        onSelectCategory(VideoCategory.ALL)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("reset_search_empty_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("সব ভিডিও দেখুন")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredVideos, key = { it.id }) { video ->
                        OnlineVideoCard(
                            video = video,
                            onVideoClick = { onVideoClick(video) },
                            onDownloadClick = { onDownloadClick(video) },
                            onFavoriteClick = { onToggleFavorite(video) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedHeroCard(
    video: VideoEntity,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(210.dp)
            .clickable { onPlayClick() }
            .testTag("hero_video_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Tag badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "ফিচার্ড ভিডিও",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${video.author} • ${FormatUtils.formatDuration(video.durationSeconds)}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("এখনই চালান", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (video.isOfflineReady) {
                        FilledTonalButton(
                            onClick = onPlayClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ডাউনলোড সম্পন্ন", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ডাউনলোড", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineVideoCard(
    video: VideoEntity,
    onVideoClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable { onVideoClick() }
            .testTag("video_card_${video.id}")
    ) {
        Column {
            // Thumbnail Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon circle in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "চালান",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Duration Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(video.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Download status pill
                if (video.isOfflineReady) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "অফলাইন প্রস্তুত",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Channel Avatar icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.author.take(1),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = getCategoryLabel(video.category),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${video.author} • ${FormatUtils.formatFileSize(video.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Downloading progress indicator if active
                    if (video.downloadStatus == VideoEntity.DOWNLOAD_STATUS_DOWNLOADING) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ডাউনলোড হচ্ছে...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${video.downloadProgress}%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { video.downloadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Actions: Download button and Favorite toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (video.isOfflineReady) {
                        IconButton(onClick = onVideoClick) {
                            Icon(
                                imageVector = Icons.Default.FileDownloadDone,
                                contentDescription = "অফলাইনে দেখুন",
                                tint = Color(0xFF10B981)
                            )
                        }
                    } else if (video.downloadStatus == VideoEntity.DOWNLOAD_STATUS_DOWNLOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.testTag("download_button_${video.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "ডাউনলোড করুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "সংরক্ষণ",
                            tint = if (video.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dedicated Action Row with prominent buttons
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Action
                TextButton(
                    onClick = onVideoClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (video.isOfflineReady) "অফলাইনে চালান" else "অনলাইনে দেখুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Dedicated Download Action Button
                if (video.isOfflineReady) {
                    FilledTonalButton(
                        onClick = onVideoClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("downloaded_indicator_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ডাউনলোড সম্পন্ন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else if (video.downloadStatus == VideoEntity.DOWNLOAD_STATUS_DOWNLOADING) {
                    FilledTonalButton(
                        onClick = onDownloadClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ডাউনলোড হচ্ছে (${video.downloadProgress}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onDownloadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("prominent_download_button_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ডাউনলোড করুন (${FormatUtils.formatFileSize(video.fileSizeBytes)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryLabel(categoryStr: String): String {
    val matched = VideoCategory.values().find {
        it.englishName.equals(categoryStr, ignoreCase = true) ||
        it.bengaliName.equals(categoryStr, ignoreCase = true)
    }
    return matched?.bengaliName ?: categoryStr
}
