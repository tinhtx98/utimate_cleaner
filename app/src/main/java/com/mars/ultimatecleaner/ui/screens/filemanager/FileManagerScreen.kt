package com.mars.ultimatecleaner.ui.screens.filemanager

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mars.ultimatecleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(navController: NavController) {
    val hapticFeedback = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val tabs = listOf("Photos", "Videos", "Documents", "Audio", "Downloads")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("File Manager") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { isSearchActive = !isSearchActive }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(
                    onClick = { isGridView = !isGridView }
                ) {
                    Icon(
                        if (isGridView) Icons.Default.List else Icons.Default.Menu,
                        contentDescription = if (isGridView) "List View" else "Grid View"
                    )
                }
            }
        )

        // Search Bar
        AnimatedVisibility(
            visible = isSearchActive,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { isSearchActive = false }
            )
        }

        // Category Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // File Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> PhotosContent(isGridView = isGridView)
                1 -> VideosContent(isGridView = isGridView)
                2 -> DocumentsContent(isGridView = isGridView)
                3 -> AudioContent(isGridView = isGridView)
                4 -> DownloadsContent(isGridView = isGridView)
            }

            // Floating Action Button
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = CleanerBlue
            ) {
                Icon(
                    // Icons.Default.CreateNewFolder,
                    Icons.Default.Add,
                    contentDescription = "Create Folder",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search files...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
fun PhotosContent(isGridView: Boolean) {
    val photos = getPhotoFiles()

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                PhotoGridItem(photo)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                PhotoListItem(photo)
            }
        }
    }
}

@Composable
fun VideosContent(isGridView: Boolean) {
    val videos = getVideoFiles()

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos) { video ->
                VideoGridItem(video)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos) { video ->
                VideoListItem(video)
            }
        }
    }
}

@Composable
fun DocumentsContent(isGridView: Boolean) {
    val documents = getDocumentFiles()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(documents) { document ->
            DocumentListItem(document)
        }
    }
}

@Composable
fun AudioContent(isGridView: Boolean) {
    val audioFiles = getAudioFiles()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(audioFiles) { audio ->
            AudioListItem(audio)
        }
    }
}

@Composable
fun DownloadsContent(isGridView: Boolean) {
    val downloads = getDownloadFiles()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(downloads) { download ->
            DownloadListItem(download)
        }
    }
}

@Composable
fun PhotoGridItem(photo: FileItem) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CleanerBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // Icons.Default.Image,
                Icons.Default.Email,
                contentDescription = photo.name,
                modifier = Modifier.size(48.dp),
                tint = CleanerBlue
            )
        }
    }
}

@Composable
fun PhotoListItem(photo: FileItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CleanerBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // Icons.Default.Image,
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = CleanerBlue
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${photo.size} • ${photo.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
fun VideoGridItem(video: FileItem) {
    Card(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CleanerOrange.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = video.name,
                modifier = Modifier.size(48.dp),
                tint = CleanerOrange
            )
        }
    }
}

@Composable
fun VideoListItem(video: FileItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CleanerOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = CleanerOrange
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.size} • ${video.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
fun DocumentListItem(document: FileItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CleanerPurple.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // Icons.Default.Description,
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = CleanerPurple
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${document.size} • ${document.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
fun AudioListItem(audio: FileItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CleanerGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // Icons.Default.AudioFile,
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = CleanerGreen
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audio.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${audio.size} • ${audio.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
fun DownloadListItem(download: FileItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CleanerBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // Icons.Default.Download,
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = CleanerBlue
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${download.size} • ${download.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

// Data classes and sample data
data class FileItem(
    val name: String,
    val size: String,
    val date: String,
    val type: String
)

fun getPhotoFiles(): List<FileItem> {
    return listOf(
        FileItem("IMG_001.jpg", "2.4 MB", "Today", "image"),
        FileItem("IMG_002.jpg", "3.1 MB", "Yesterday", "image"),
        FileItem("IMG_003.jpg", "1.8 MB", "2 days ago", "image"),
        FileItem("IMG_004.jpg", "2.7 MB", "3 days ago", "image"),
        FileItem("IMG_005.jpg", "1.9 MB", "1 week ago", "image"),
        FileItem("IMG_006.jpg", "2.2 MB", "1 week ago", "image"),
    )
}

fun getVideoFiles(): List<FileItem> {
    return listOf(
        FileItem("VID_001.mp4", "45.2 MB", "Today", "video"),
        FileItem("VID_002.mp4", "78.5 MB", "Yesterday", "video"),
        FileItem("VID_003.mp4", "32.1 MB", "2 days ago", "video"),
        FileItem("VID_004.mp4", "55.8 MB", "3 days ago", "video"),
    )
}

fun getDocumentFiles(): List<FileItem> {
    return listOf(
        FileItem("Resume.pdf", "1.2 MB", "Today", "document"),
        FileItem("Report.docx", "856 KB", "Yesterday", "document"),
        FileItem("Presentation.pptx", "4.5 MB", "2 days ago", "document"),
        FileItem("Spreadsheet.xlsx", "2.1 MB", "3 days ago", "document"),
    )
}

fun getAudioFiles(): List<FileItem> {
    return listOf(
        FileItem("Song1.mp3", "4.2 MB", "Today", "audio"),
        FileItem("Song2.mp3", "3.8 MB", "Yesterday", "audio"),
        FileItem("Podcast.mp3", "25.4 MB", "2 days ago", "audio"),
        FileItem("Recording.m4a", "1.5 MB", "3 days ago", "audio"),
    )
}

fun getDownloadFiles(): List<FileItem> {
    return listOf(
        FileItem("app-release.apk", "12.5 MB", "Today", "application"),
        FileItem("document.zip", "8.7 MB", "Yesterday", "archive"),
        FileItem("image.png", "2.3 MB", "2 days ago", "image"),
        FileItem("video.mp4", "67.2 MB", "3 days ago", "video"),
    )
}