package com.iptv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.iptv.tv.ui.*
import com.iptv.shared.mvi.IptvUiState
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.db.ProgramEntity
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
            
            // Auto-focus the player Box when overlays are closed
            LaunchedEffect(showSidebar, showGuide) {
                if (!showSidebar && !showGuide) {
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
