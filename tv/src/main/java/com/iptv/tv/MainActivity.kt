package com.iptv.tv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.iptv.tv.ui.TvViewModel
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.mvi.PlaybackIntent
import com.iptv.shared.mvi.PlaybackSideEffect
import com.iptv.shared.mvi.PlaybackState
import kotlinx.coroutines.flow.collectLatest

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
    var showUrlDialog by remember { mutableStateOf(false) }
    
    val setupPlaylistFocusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        setupPlaylistFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Playlist load button
            Button(
                onClick = { showUrlDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(setupPlaylistFocusRequester)
                    .padding(bottom = 12.dp)
            ) {
                Text("Setup Master M3U URL")
            }

            // Categories horizontal row
            if (uiState.groups.isNotEmpty()) {
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
                    items(uiState.groups) { group ->
                        FilterChip(
                            selected = uiState.selectedGroup == group,
                            onClick = { viewModel.selectGroup(group) }
                        ) {
                            Text(group)
                        }
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
                        text = "No channels. Select setup above to load a playlist.",
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
                        TvChannelItem(
                            channel = channel,
                            onClick = {
                                viewModel.handleIntent(PlaybackIntent.SelectChannel(channel))
                            }
                        )
                    }
                }
            }
        }

        // Right Panel: Playback Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.playbackState !is PlaybackState.Idle) {
                TvVideoPlayer(viewModel = viewModel, state = uiState.playbackState)
            } else {
                Text(
                    text = "Select a channel to play",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    // Playlist URL Setup Dialog (standard material dialog, tv navigable)
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Configure M3U URL", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.playlistUrlInput,
                        onValueChange = { viewModel.updateUrlInput(it) },
                        label = { Text("M3U URL") },
                        placeholder = { Text("http://192.168.1.100/playlist.m3u") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.handleIntent(PlaybackIntent.LoadPlaylist(uiState.playlistUrlInput))
                        showUrlDialog = false
                    }
                ) {
                    Text("Load")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvChannelItem(channel: ChannelEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
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
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val groupTitle = channel.groupTitle
                if (!groupTitle.isNullOrEmpty()) {
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvVideoPlayer(viewModel: TvViewModel, state: PlaybackState) {
    val context = LocalContext.current
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
