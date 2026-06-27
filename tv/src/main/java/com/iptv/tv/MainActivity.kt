package com.iptv.tv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.iptv.tv.ui.TvViewModel
import com.iptv.tv.ui.TvUiState
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.db.ProgramEntity
import com.iptv.shared.mvi.PlaybackIntent
import com.iptv.shared.mvi.PlaybackSideEffect
import com.iptv.shared.mvi.PlaybackState
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            
            LaunchedEffect(key1 = Unit) {
                viewModel.sideEffects.collectLatest { effect ->
                    when (effect) {
                        is PlaybackSideEffect.ShowToast -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            TvAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    TvMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAppTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFF9D4EDD),
        secondary = Color(0xFFE0AAFF),
        background = Color(0xFF070714),
        surface = Color(0xFF13132B),
        onBackground = Color(0xFFF7F5FA),
        onSurface = Color(0xFFF7F5FA)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMainScreen(viewModel: TvViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (!uiState.isOnboardingCompleted) {
        TvOnboardingScreen(uiState = uiState, viewModel = viewModel)
        return
    }
    var showUrlDialog by remember { mutableStateOf(false) }
    
    val setupPlaylistFocusRequester = remember { FocusRequester() }
    val playerFocusRequester = remember { FocusRequester() }
    
    var showSidebar by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val playerActive = uiState.playbackState !is PlaybackState.Idle

    LaunchedEffect(key1 = Unit) {
        if (!playerActive) {
            setupPlaylistFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (playerActive) {
            // Full Screen Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(playerFocusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (showSidebar) {
                                if (keyEvent.key == Key.Back || keyEvent.key == Key.DirectionRight) {
                                    showSidebar = false
                                    true
                                } else false
                            } else if (showGuide) {
                                if (keyEvent.key == Key.Back || keyEvent.key == Key.DirectionUp) {
                                    showGuide = false
                                    true
                                } else false
                            } else {
                                when (keyEvent.key) {
                                    Key.DirectionUp -> {
                                        viewModel.playPreviousChannel()
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        viewModel.playNextChannel()
                                        true
                                    }
                                    Key.DirectionLeft -> {
                                        showSidebar = true
                                        true
                                    }
                                    Key.DirectionRight, Key.DirectionCenter, Key.Enter -> {
                                        showGuide = true
                                        true
                                    }
                                    Key.Back -> {
                                        viewModel.playerEngine.release()
                                        true
                                    }
                                    else -> false
                                }
                            }
                        } else false
                    }
            ) {
                TvVideoPlayer(viewModel = viewModel, state = uiState.playbackState)
            }
            
            // Sidebar Swapper Overlay
            if (showSidebar) {
                TvSidebarSwapper(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectChannel = {
                        viewModel.handleIntent(PlaybackIntent.SelectChannel(it))
                        showSidebar = false
                    },
                    onClose = { showSidebar = false }
                )
            }
            
            // TV Guide timeline overlay
            if (showGuide) {
                TvEpgGuideOverlay(
                    uiState = uiState,
                    onSelectChannel = {
                        viewModel.handleIntent(PlaybackIntent.SelectChannel(it))
                        showGuide = false
                    },
                    onClose = { showGuide = false }
                )
            }
            
            // Auto-focus the player Box when overlays are closed
            LaunchedEffect(showSidebar, showGuide) {
                if (!showSidebar && !showGuide) {
                    playerFocusRequester.requestFocus()
                }
            }
        } else {
            // Dashboard Layout (No Video Active)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Channel list and playlist config
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(420.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Watcharr TV",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Privacy-first IPTV Client",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isLoadingEpg) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Button(
                            onClick = { showUrlDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(setupPlaylistFocusRequester)
                        ) {
                            Text("Setup Playlist & EPG")
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search channels...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // Categories horizontal row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedGroup == null,
                                onClick = { viewModel.selectGroup(null) }
                            ) {
                                Text("All")
                            }
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedGroup == "Favorites",
                                onClick = { viewModel.selectGroup("Favorites") }
                            ) {
                                Text("★ Favorites")
                            }
                        }
                        items(uiState.groups) { group ->
                            FilterChip(
                                selected = uiState.selectedGroup == group,
                                onClick = { viewModel.selectGroup(group) }
                            ) {
                                Text(group)
                            }
                        }
                    }

                    // Channels List
                    if (uiState.isLoadingPlaylist) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (uiState.channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No matching channels." else "No channels. Select setup above to load a playlist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.channels) { channel ->
                                val programs = uiState.epgData[channel.url] ?: emptyList()
                                val current = programs.firstOrNull()
                                val next = programs.firstOrNull { it.start > System.currentTimeMillis() }
                                val isFav = uiState.favoriteUrls.contains(channel.url)
                                
                                TvChannelItem(
                                    channel = channel,
                                    currentProgram = current,
                                    nextProgram = next,
                                    isFavorite = isFav,
                                    onLongClick = { viewModel.toggleFavorite(channel.url) },
                                    onClick = {
                                        viewModel.handleIntent(PlaybackIntent.SelectChannel(channel))
                                    }
                                )
                            }
                        }
                    }
                }

                // Right Panel: Help/Info Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Select a channel to play",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Controls: D-pad Left (Swapper), D-pad Center/Right (Guide), Long press card to favorite",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    if (showUrlDialog) {
        var isDispatcharrMode by remember { mutableStateOf(uiState.useDispatcharr) }
        var dispatcharrInput by remember { mutableStateOf(uiState.dispatcharrUrl) }
        var m3uInput by remember { mutableStateOf(uiState.playlistUrlInput) }
        var epgInput by remember { mutableStateOf(uiState.epgUrlInput) }

        val dialogFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            dialogFocusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = { showUrlDialog = false }),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(550.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configure Playlist & EPG",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Tab selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isDispatcharrMode = true },
                            modifier = Modifier.weight(1f).focusRequester(dialogFocusRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = if (isDispatcharrMode) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                focusedContainerColor = if (isDispatcharrMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        ) {
                            Text("Dispatcharr Server")
                        }
                        Button(
                            onClick = { isDispatcharrMode = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.colors(
                                containerColor = if (!isDispatcharrMode) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                focusedContainerColor = if (!isDispatcharrMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        ) {
                            Text("Custom URLs")
                        }
                    }

                    if (isDispatcharrMode) {
                        OutlinedTextField(
                            value = dispatcharrInput,
                            onValueChange = { dispatcharrInput = it },
                            label = { Text("Dispatcharr Server URL") },
                            placeholder = { Text("http://192.168.1.100:8080") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = m3uInput,
                            onValueChange = { m3uInput = it },
                            label = { Text("M3U Playlist URL") },
                            placeholder = { Text("http://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        OutlinedTextField(
                            value = epgInput,
                            onValueChange = { epgInput = it },
                            label = { Text("EPG XMLTV URL") },
                            placeholder = { Text("http://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isDispatcharrMode) {
                                    if (dispatcharrInput.isNotEmpty()) {
                                        val m3u = "$dispatcharrInput/output/m3u"
                                        val epg = "$dispatcharrInput/output/epg"
                                        viewModel.saveConfigAndCompleteOnboarding(m3u, epg, dispatcharrInput, true)
                                    }
                                } else {
                                    if (m3uInput.isNotEmpty()) {
                                        viewModel.saveConfigAndCompleteOnboarding(m3uInput, epgInput, null, false)
                                    }
                                }
                                showUrlDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }

                        Button(
                            onClick = { showUrlDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.colors(
                                containerColor = Color.Gray.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Gray
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TvChannelItem(
    channel: ChannelEntity,
    currentProgram: ProgramEntity?,
    nextProgram: ProgramEntity?,
    isFavorite: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Text(
                            text = "★",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                
                val groupTitle = channel.groupTitle
                if (!groupTitle.isNullOrEmpty()) {
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }

                if (currentProgram != null) {
                    Text(
                        text = "Now: ${currentProgram.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE0AAFF),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (nextProgram != null) {
                    Text(
                        text = "Next: ${nextProgram.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvVideoPlayer(viewModel: TvViewModel, state: PlaybackState) {
    val player = remember(viewModel) { viewModel.playerEngine.getPlayer() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay status on loading or error states
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSidebarSwapper(
    uiState: TvUiState,
    viewModel: TvViewModel,
    onSelectChannel: (ChannelEntity) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClose() },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(Color(0xFF0F0F1E).copy(alpha = 0.95f))
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Text(
                text = "Channel Swapper",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            // Category rows
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedGroup == null,
                        onClick = { viewModel.selectGroup(null) }
                    ) {
                        Text("All")
                    }
                }
                item {
                    FilterChip(
                        selected = uiState.selectedGroup == "Favorites",
                        onClick = { viewModel.selectGroup("Favorites") }
                    ) {
                        Text("★ Favorites")
                    }
                }
                items(uiState.groups) { group ->
                    FilterChip(
                        selected = uiState.selectedGroup == group,
                        onClick = { viewModel.selectGroup(group) }
                    ) {
                        Text(group)
                    }
                }
            }

            // Channel items
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.channels) { index, channel ->
                    val programs = uiState.epgData[channel.url] ?: emptyList()
                    val current = programs.firstOrNull()
                    val isFav = uiState.favoriteUrls.contains(channel.url)

                    Card(
                        onClick = { onSelectChannel(channel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                        colors = CardDefaults.colors(
                            containerColor = Color(0xFF1E1E38),
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isFav) "★ ${channel.name}" else channel.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            if (current != null) {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvEpgGuideOverlay(
    uiState: TvUiState,
    onSelectChannel: (ChannelEntity) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    color = Color(0xFF13132B).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TV Guide Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Press Back to Close Guide",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }

                if (uiState.channels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels loaded.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.channels) { index, channel ->
                            val programs = uiState.epgData[channel.url] ?: emptyList()
                            val current = programs.firstOrNull()
                            val upcoming = programs.filter { it.start > System.currentTimeMillis() }
                            
                            val isFav = uiState.favoriteUrls.contains(channel.url)

                            Card(
                                onClick = { onSelectChannel(channel) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                                colors = CardDefaults.colors(
                                    containerColor = Color(0xFF1E1E38).copy(alpha = 0.8f),
                                    focusedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Cell 1: Channel Name
                                    Column(modifier = Modifier.width(130.dp)) {
                                        Text(
                                            text = if (isFav) "★ ${channel.name}" else channel.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        channel.groupTitle?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray.copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Cell 2: Now Playing with progress bar
                                    Column(modifier = Modifier.width(220.dp)) {
                                        if (current != null) {
                                            Text(
                                                text = "Now: ${current.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            val now = System.currentTimeMillis()
                                            val total = current.stop - current.start
                                            val progress = if (total > 0) (now - current.start).toFloat() / total else 0f
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = progress.coerceIn(0f, 1f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = Color(0xFFE0AAFF),
                                                trackColor = Color.Gray.copy(alpha = 0.3f)
                                            )
                                        } else {
                                            Text("No info available", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Cell 3: Next Program
                                    Column(modifier = Modifier.width(220.dp)) {
                                        val nextProg = upcoming.getOrNull(0)
                                        if (nextProg != null) {
                                            Text(
                                                text = "Next: ${nextProg.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.LightGray,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = formatTimeRange(nextProg.start, nextProg.stop),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        } else {
                                            Text("-", color = Color.Gray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Cell 4: Later Program
                                    Column(modifier = Modifier.weight(1f)) {
                                        val laterProg = upcoming.getOrNull(1)
                                        if (laterProg != null) {
                                            Text(
                                                text = "Later: ${laterProg.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.LightGray,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = formatTimeRange(laterProg.start, laterProg.stop),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        } else {
                                            Text("-", color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

private fun formatTimeRange(startMs: Long, stopMs: Long): String {
    val startStr = timeFormatter.format(java.util.Date(startMs))
    val stopStr = timeFormatter.format(java.util.Date(stopMs))
    return "$startStr - $stopStr"
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvOnboardingScreen(uiState: TvUiState, viewModel: TvViewModel) {
    var manualMode by remember { mutableStateOf<String?>(null) } // "dispatcharr" or "custom"
    var dispatcharrUrlInput by remember { mutableStateOf("") }
    var playlistUrlInput by remember { mutableStateOf("") }
    var epgUrlInput by remember { mutableStateOf("") }

    val defaultFocusRequester = remember { FocusRequester() }
    val dispatcharrFocusRequester = remember { FocusRequester() }
    val customFocusRequester = remember { FocusRequester() }

    LaunchedEffect(manualMode) {
        if (manualMode == null) {
            defaultFocusRequester.requestFocus()
        } else if (manualMode == "dispatcharr") {
            dispatcharrFocusRequester.requestFocus()
        } else if (manualMode == "custom") {
            customFocusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Pane: QR Code Setup (Default)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Easy Setup with Mobile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Scan this QR code using the Watcharr Mobile App (Settings -> Scan TV QR) to pair and sync your playlist.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                QrCodeImage(
                    content = uiState.setupQrUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.setupStatus,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Right Pane: Manual Options
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (manualMode == null) {
                Text(
                    text = "Manual Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure your server connection or URLs directly on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { manualMode = "dispatcharr" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(defaultFocusRequester)
                ) {
                    Text("Use Dispatcharr Server")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { manualMode = "custom" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter Custom URLs")
                }
            } else if (manualMode == "dispatcharr") {
                Text(
                    text = "Dispatcharr Server",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dispatcharrUrlInput,
                    onValueChange = { dispatcharrUrlInput = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.100:8080") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(dispatcharrFocusRequester),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (dispatcharrUrlInput.isNotEmpty()) {
                                val m3u = "$dispatcharrUrlInput/output/m3u"
                                val epg = "$dispatcharrUrlInput/output/epg"
                                viewModel.saveConfigAndCompleteOnboarding(m3u, epg, dispatcharrUrlInput, true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect")
                    }
                    Button(
                        onClick = { manualMode = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Gray.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Gray
                        )
                    ) {
                        Text("Back")
                    }
                }
            } else if (manualMode == "custom") {
                Text(
                    text = "Custom URLs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = playlistUrlInput,
                    onValueChange = { playlistUrlInput = it },
                    label = { Text("M3U Playlist URL") },
                    placeholder = { Text("http://...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(customFocusRequester),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = epgUrlInput,
                    onValueChange = { epgUrlInput = it },
                    label = { Text("EPG XMLTV URL (Optional)") },
                    placeholder = { Text("http://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (playlistUrlInput.isNotEmpty()) {
                                viewModel.saveConfigAndCompleteOnboarding(playlistUrlInput, epgUrlInput, null, false)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save & Load")
                    }
                    Button(
                        onClick = { manualMode = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Gray.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Gray
                        )
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeImage(content: String, modifier: Modifier = Modifier) {
    if (content.isEmpty()) {
        Box(modifier = modifier.background(Color.White))
        return
    }
    val matrix = remember(content) {
        try {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 256, 256)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (matrix != null) {
        Canvas(modifier = modifier) {
            // Draw background white
            drawRect(color = Color.White, size = size)

            val width = matrix.width
            val height = matrix.height
            val sizeX = size.width / width
            val sizeY = size.height / height

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(x * sizeX, y * sizeY),
                            size = Size(sizeX + 0.5f, sizeY + 0.5f)
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.background(Color.White))
    }
}

