package com.iptv.mobile

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.iptv.mobile.ui.MobileViewModel
import com.iptv.mobile.ui.MobileUiState
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.db.ProgramEntity
import com.iptv.shared.mvi.PlaybackIntent
import com.iptv.shared.mvi.PlaybackSideEffect
import com.iptv.shared.mvi.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MobileViewModel by viewModels()
    private val isInPipMode = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            val pipState by isInPipMode.collectAsStateWithLifecycle()
            
            // Listen to MVI Side Effects
            LaunchedEffect(key1 = Unit) {
                viewModel.sideEffects.collectLatest { effect ->
                    when (effect) {
                        is PlaybackSideEffect.ShowToast -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            IPTVAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel, isInPipMode = pipState)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val uiState = viewModel.uiState.value
        if (uiState.playbackState is PlaybackState.Playing) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
    }
}

@Composable
fun IPTVAppTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFF9D4EDD), // Deep purple
        secondary = Color(0xFFE0AAFF), // Soft lavender
        background = Color(0xFF0F0F1E), // Slate dark
        surface = Color(0xFF1E1E38), // Slightly lighter slate
        onBackground = Color(0xFFF7F5FA),
        onSurface = Color(0xFFF7F5FA)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MobileViewModel, isInPipMode: Boolean) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUrlDialog by remember { mutableStateOf(false) }

    var isPlayerFullscreen by remember { mutableStateOf(false) }
    var showChannelsList by remember { mutableStateOf(true) }
    var detailedProgram by remember { mutableStateOf<ProgramEntity?>(null) }

    if (isInPipMode) {
        // Picture-in-Picture mode: render ONLY the video player
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = viewModel.playerEngine.getPlayer()
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    if (isPlayerFullscreen && uiState.playbackState !is PlaybackState.Idle) {
        // Fullscreen Player view
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayerContainer(
                viewModel = viewModel,
                state = uiState.playbackState,
                isFullscreen = true,
                onToggleFullscreen = { isPlayerFullscreen = false }
            )
        }
        return
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        // Tablet Split-Pane Master-Detail Layout
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Pane (Channels & Search Panel) - hidable!
            if (showChannelsList) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Watcharr IPTV",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (uiState.playbackState !is PlaybackState.Idle) {
                            IconButton(onClick = { showChannelsList = false }, modifier = Modifier.size(28.dp)) {
                                Text("◀", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search channels...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Categories horizontal row
                    CategoryGroupsRow(uiState = uiState, viewModel = viewModel)

                    Spacer(modifier = Modifier.height(8.dp))

                    // List Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Channels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = { showUrlDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Configure")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Channels List
                    ChannelsList(uiState = uiState, viewModel = viewModel, modifier = Modifier.weight(1f))
                }
            }

            // Right Pane (Player & EPG Timeline Details)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                if (uiState.playbackState !is PlaybackState.Idle) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!showChannelsList) {
                            Button(
                                onClick = { showChannelsList = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text("▶ Show Channels")
                            }
                        }
                    }
                    VideoPlayerContainer(
                        viewModel = viewModel,
                        state = uiState.playbackState,
                        isFullscreen = false,
                        onToggleFullscreen = { isPlayerFullscreen = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ActiveChannelEpgGuide(
                        uiState = uiState,
                        modifier = Modifier.weight(1f),
                        onProgramClick = { detailedProgram = it }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Select a channel to start watching",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showUrlDialog = true }) {
                                Text("Set up Playlist / EPG")
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Standard Phone Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Video Player Container at top
            if (uiState.playbackState !is PlaybackState.Idle) {
                VideoPlayerContainer(
                    viewModel = viewModel,
                    state = uiState.playbackState,
                    isFullscreen = false,
                    onToggleFullscreen = { isPlayerFullscreen = true }
                )
                
                if (showChannelsList) {
                    // Show mini EPG drawer under player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    ) {
                        ActiveChannelEpgGuide(
                            uiState = uiState,
                            modifier = Modifier.fillMaxSize(),
                            onProgramClick = { detailedProgram = it }
                        )
                    }
                } else {
                    // Channels list hidden: EPG Guide takes up all remaining screen space!
                    Column(modifier = Modifier.weight(1f)) {
                        ActiveChannelEpgGuide(
                            uiState = uiState,
                            modifier = Modifier.weight(1f),
                            onProgramClick = { detailedProgram = it }
                        )
                        TextButton(
                            onClick = { showChannelsList = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
                        ) {
                            Text("▲ Show Channels List")
                        }
                    }
                }
            } else {
                // Elegant Welcome Header
                WelcomeHeader()
            }

            if (showChannelsList) {
                // Search and Setup Toolbar
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Channels",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (uiState.playbackState !is PlaybackState.Idle) {
                                TextButton(onClick = { showChannelsList = false }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Hide Channels", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Button(
                            onClick = { showUrlDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Configure")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search channels...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Category Chips Row
                CategoryGroupsRow(uiState = uiState, viewModel = viewModel)

                // Channels List
                ChannelsList(uiState = uiState, viewModel = viewModel, modifier = Modifier.weight(1f))
            }
        }
    }

    // Playlist & EPG URL Setup Dialog
    if (showUrlDialog) {
        ConfigureUrlsDialog(uiState = uiState, viewModel = viewModel, onDismiss = { showUrlDialog = false })
    }

    // Detailed Program dialog
    if (detailedProgram != null) {
        val program = detailedProgram!!
        val channelName = (uiState.playbackState as? PlaybackState.Playing)?.channel?.name ?: ""

        AlertDialog(
            onDismissRequest = { detailedProgram = null },
            title = {
                Column {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (channelName.isNotEmpty()) {
                        Text(
                            text = "Channel: $channelName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Time: ${formatTimeRange(program.start, program.stop)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val desc = program.desc
                    if (!desc.isNullOrEmpty()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "No description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { detailedProgram = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun WelcomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Watcharr IPTV",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Privacy-first, zero cloud tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun CategoryGroupsRow(uiState: MobileUiState, viewModel: MobileViewModel) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = uiState.selectedGroup == null,
                onClick = { viewModel.selectGroup(null) },
                label = { Text("All") }
            )
        }
        item {
            FilterChip(
                selected = uiState.selectedGroup == "Favorites",
                onClick = { viewModel.selectGroup("Favorites") },
                label = { Text("★ Favorites") }
            )
        }
        items(uiState.groups) { group ->
            FilterChip(
                selected = uiState.selectedGroup == group,
                onClick = { viewModel.selectGroup(group) },
                label = { Text(group) }
            )
        }
    }
}

@Composable
fun ChannelsList(uiState: MobileUiState, viewModel: MobileViewModel, modifier: Modifier = Modifier) {
    if (uiState.isLoadingPlaylist) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (uiState.channels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (uiState.searchQuery.isNotEmpty()) "No channels match your search query." else "No channels loaded. Set up a master M3U URL.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.channels) { channel ->
                val programs = uiState.epgData[channel.url] ?: emptyList()
                val current = programs.firstOrNull()
                val next = programs.firstOrNull { it.start > System.currentTimeMillis() }
                
                val isFav = uiState.favoriteUrls.contains(channel.url)
                
                ChannelListItem(
                    channel = channel,
                    currentProgram = current,
                    nextProgram = next,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(channel.url) }
                ) {
                    viewModel.handleIntent(PlaybackIntent.SelectChannel(channel))
                }
            }
        }
    }
}

@Composable
fun ConfigureUrlsDialog(uiState: MobileUiState, viewModel: MobileViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Playlist & EPG") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.playlistUrlInput,
                    onValueChange = { viewModel.updateUrlInput(it) },
                    label = { Text("M3U Playlist URL") },
                    placeholder = { Text("http://192.168.1.100/playlist.m3u") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.epgUrlInput,
                    onValueChange = { viewModel.updateEpgUrlInput(it) },
                    label = { Text("EPG XMLTV URL") },
                    placeholder = { Text("http://192.168.1.100/epg.xml.gz") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uiState.playlistUrlInput.isNotEmpty()) {
                        viewModel.handleIntent(PlaybackIntent.LoadPlaylist(uiState.playlistUrlInput))
                    }
                    if (uiState.epgUrlInput.isNotEmpty()) {
                        viewModel.loadEpg(uiState.epgUrlInput)
                    }
                    onDismiss()
                }
            ) {
                Text("Load")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VideoPlayerContainer(
    viewModel: MobileViewModel,
    state: PlaybackState,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    val player = remember(viewModel) { viewModel.playerEngine.getPlayer() }

    val isRestricted = (state is PlaybackState.Playing && state.isVideoRestricted)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.fillMaxHeight() else Modifier.aspectRatio(16 / 9f))
            .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        if (!isRestricted) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setFullscreenButtonClickListener { isFullScreen ->
                            onToggleFullscreen()
                        }
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF151525)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "⚠️ Video Paused While Driving",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD166)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Safety Mode: Video stream is hidden while the vehicle is in motion. Audio continues playing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E2EC),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        when (state) {
            is PlaybackState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is PlaybackState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Playback Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ChannelListItem(
    channel: ChannelEntity,
    currentProgram: ProgramEntity?,
    nextProgram: ProgramEntity?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = if (isFavorite) "★" else "☆",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                val groupTitle = channel.groupTitle
                if (!groupTitle.isNullOrEmpty()) {
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                    )
                }

                if (currentProgram != null) {
                    Text(
                        text = "Now: ${currentProgram.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (nextProgram != null) {
                    Text(
                        text = "Next: ${nextProgram.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Text(
                text = "▶",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ActiveChannelEpgGuide(
    uiState: MobileUiState,
    modifier: Modifier = Modifier,
    onProgramClick: (ProgramEntity) -> Unit
) {
    val activeChannel = (uiState.playbackState as? PlaybackState.Playing)?.channel ?: return
    val programs = uiState.epgData[activeChannel.url] ?: emptyList()

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "EPG Guide: ${activeChannel.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (programs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No upcoming programs found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(programs) { program ->
                    EpgProgramTimelineItem(program = program, onClick = { onProgramClick(program) })
                }
            }
        }
    }
}

private val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

fun formatTimeRange(startMs: Long, stopMs: Long): String {
    val startStr = timeFormatter.format(java.util.Date(startMs))
    val stopStr = timeFormatter.format(java.util.Date(stopMs))
    return "$startStr - $stopStr"
}

@Composable
fun EpgProgramTimelineItem(program: ProgramEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimeRange(program.start, program.stop),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val desc = program.desc
            if (!desc.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
