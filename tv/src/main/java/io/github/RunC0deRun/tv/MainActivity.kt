package io.github.RunC0deRun.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import io.github.RunC0deRun.tv.ui.*
import io.github.RunC0deRun.shared.data.db.ChannelEntity
import io.github.RunC0deRun.shared.data.db.ProgramEntity
import io.github.RunC0deRun.shared.mvi.PlaybackIntent
import io.github.RunC0deRun.shared.mvi.PlaybackSideEffect
import io.github.RunC0deRun.shared.mvi.PlaybackState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.ACCESS_LOCAL_NETWORK"
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf("android.permission.ACCESS_LOCAL_NETWORK"),
                    101
                )
            }
        }
        
        setContent {
            // Listen to MVI Side Effects
            LaunchedEffect(key1 = Unit) {
                viewModel.sideEffects.collectLatest { effect ->
                    when (effect) {
                        is PlaybackSideEffect.ShowToast -> {
                            android.widget.Toast.makeText(this@MainActivity, effect.message, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            TvAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TvMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMainScreen(viewModel: TvViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (!uiState.isInitialized) {
        TvSplashScreen()
        return
    }
    if (!uiState.isOnboardingCompleted) {
        TvOnboardingScreen(uiState = uiState, viewModel = viewModel)
        return
    }
    
    val setupPlaylistFocusRequester = remember { FocusRequester() }
    val playerFocusRequester = remember { FocusRequester() }
    
    var showSidebar by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var selectedProgramForDetail by remember { mutableStateOf<ProgramEntity?>(null) }
    var selectedChannelForDetail by remember { mutableStateOf<ChannelEntity?>(null) }

    val playerActive = uiState.playbackState !is PlaybackState.Idle

    LaunchedEffect(playerActive) {
        if (playerActive) {
            playerFocusRequester.requestFocus()
        } else {
            setupPlaylistFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (playerActive) {
            var timeBehindLive by remember { mutableLongStateOf(0L) }
            var playheadTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            
            val player = remember(viewModel) { viewModel.playerEngine.getPlayer() }
            var isPlaying by remember { mutableStateOf(player.isPlaying) }
            DisposableEffect(player) {
                val listener = object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                }
            }
            
            val isLive = timeBehindLive < 2000L
            
            LaunchedEffect(isPlaying, isLive) {
                while (true) {
                    val now = System.currentTimeMillis()
                    if (isPlaying) {
                        if (isLive) {
                            timeBehindLive = 0L
                            playheadTime = now
                        } else {
                            playheadTime = now - timeBehindLive
                        }
                    } else {
                        timeBehindLive = now - playheadTime
                    }
                    delay(1.seconds)
                }
            }
            
            LaunchedEffect(uiState.playbackState) {
                timeBehindLive = 0L
                playheadTime = System.currentTimeMillis()
            }

            // Full Screen Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(playerFocusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
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
                            } else if (showControls) {
                                if (keyEvent.key == Key.Back) {
                                    showControls = false
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
                                    Key.DirectionCenter, Key.Enter -> {
                                        showControls = true
                                        true
                                    }
                                    Key.Back -> {
                                        viewModel.playerEngine.stop()
                                        true
                                    }
                                    else -> false
                                }
                            }
                        } else if (keyEvent.type == KeyEventType.KeyDown) {
                            // Consume KeyDown to prevent default OS actions/sounds, and prepare for KeyUp
                            when (keyEvent.key) {
                                Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.Back -> true
                                else -> false
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
                    onClose = { showGuide = false },
                    onSelectProgramDetail = { prog, ch ->
                        selectedProgramForDetail = prog
                        selectedChannelForDetail = ch
                    }
                )
            }
            
            // TV player controls overlay
            if (showControls) {
                TvPlayerControlsOverlay(
                    uiState = uiState,
                    viewModel = viewModel,
                    playheadTime = playheadTime,
                    isLive = isLive,
                    onSeekToLive = {
                        player.seekToDefaultPosition()
                        player.play()
                        timeBehindLive = 0L
                        playheadTime = System.currentTimeMillis()
                    },
                    onCloseOverlay = { showControls = false }
                )
            }
            
            // Auto-focus the player Box when overlays are closed
            LaunchedEffect(showSidebar, showGuide, showControls) {
                if (!showSidebar && !showGuide && !showControls) {
                    playerFocusRequester.requestFocus()
                }
            }
        } else {
            // Dashboard Layout (No Video Active)
            var selectedTab by remember { mutableStateOf(TvTab.CHANNELS) }

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Menu Bar
                TvTopBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    firstItemFocusRequester = setupPlaylistFocusRequester,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) }
                )

                // Main Content Panel below Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        TvTab.CHANNELS -> {
                            TvChannelsGrid(
                                uiState = uiState,
                                viewModel = viewModel,
                                onSelectChannel = {
                                    viewModel.handleIntent(PlaybackIntent.SelectChannel(it))
                                },
                                onSelectProgramDetail = { prog, ch ->
                                    selectedProgramForDetail = prog
                                    selectedChannelForDetail = ch
                                }
                            )
                        }
                        TvTab.EPG -> {
                            TvFullEpgGuide(
                                uiState = uiState,
                                onSelectChannel = {
                                    viewModel.handleIntent(PlaybackIntent.SelectChannel(it))
                                },
                                onSelectProgramDetail = { prog, ch ->
                                    selectedProgramForDetail = prog
                                    selectedChannelForDetail = ch
                                }
                            )
                        }
                        TvTab.SETTINGS -> {
                            TvSettingsPanel(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        selectedProgramForDetail?.let { prog ->
            TvProgramDetailScreen(
                program = prog,
                channel = selectedChannelForDetail,
                onDismiss = {
                    selectedProgramForDetail = null
                    selectedChannelForDetail = null
                }
            )
        }
    }
}
