package com.iptv.mobile

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.iptv.mobile.ui.*
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
        enableEdgeToEdge()
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
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MobileViewModel, isInPipMode: Boolean) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (!uiState.isInitialized) {
        MobileSplashScreen()
        return
    }
    if (!uiState.isOnboardingCompleted) {
        MobileOnboardingWizard(viewModel = viewModel)
        return
    }
    var showUrlDialog by remember { mutableStateOf(false) }

    var isPlayerFullscreen by rememberSaveable { mutableStateOf(false) }
    var showChannelsList by rememberSaveable { mutableStateOf(true) }
    var detailedProgram by remember { mutableStateOf<ProgramEntity?>(null) }

    val context = LocalContext.current
    LaunchedEffect(isPlayerFullscreen) {
        var currentContext = context
        var activity: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        val window = activity?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isPlayerFullscreen) {
                // Immersive fullscreen: hide status and navigation bars
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Exit fullscreen: restore system bars
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(uiState.playbackState) {
        if (uiState.playbackState is PlaybackState.Idle) {
            isPlayerFullscreen = false
        }
    }

    if (isInPipMode) {
        // Picture-in-Picture mode: render ONLY the video player
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = viewModel.playerEngine.getPlayer()
                        useController = false
                        keepScreenOn = true
                    }
                },
                update = { view ->
                    view.keepScreenOn = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    android.util.Log.d("Watcharr", "MainScreen composed. isPlayerFullscreen = $isPlayerFullscreen, playbackState = ${uiState.playbackState}")

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.playbackState !is PlaybackState.Idle) {
            if (isPlayerFullscreen) {
                androidx.activity.compose.BackHandler {
                    android.util.Log.d("Watcharr", "Fullscreen BackHandler triggered. Setting isPlayerFullscreen = false")
                    isPlayerFullscreen = false
                }
            } else {
                androidx.activity.compose.BackHandler {
                    android.util.Log.d("Watcharr", "Tablet/Phone inline player back pressed. Stopping player.")
                    viewModel.playerEngine.stop()
                }
            }

            VideoPlayerContainer(
                viewModel = viewModel,
                state = uiState.playbackState,
                isFullscreen = isPlayerFullscreen,
                onToggleFullscreen = {
                    android.util.Log.d("Watcharr", "onToggleFullscreen clicked! Setting isPlayerFullscreen = ${!isPlayerFullscreen}")
                    isPlayerFullscreen = !isPlayerFullscreen
                }
            )
        }

        if (!isPlayerFullscreen) {
            if (isTablet) {
                var activeTab by rememberSaveable { mutableStateOf(0) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Top Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                text = "Watcharr",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val tabs = listOf("Start", "TV Guide", "Setup")
                                tabs.forEachIndexed { index, label ->
                                    val selected = activeTab == index
                                    TextButton(
                                        onClick = { activeTab = index },
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Search input on the right side if Start or TV Guide tabs are active
                        if (activeTab == 0 || activeTab == 1) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search channels...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_search),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .width(300.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    // Main Tab Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activeTab) {
                            0 -> MobileChannelsGrid(
                                uiState = uiState,
                                viewModel = viewModel,
                                onSelectChannel = { channel ->
                                    viewModel.handleIntent(PlaybackIntent.SelectChannel(channel))
                                },
                                onSelectProgramDetail = { program, channel ->
                                    detailedProgram = program
                                }
                            )
                            1 -> MobileFullEpgGuide(
                                uiState = uiState,
                                onSelectChannel = { channel ->
                                    viewModel.handleIntent(PlaybackIntent.SelectChannel(channel))
                                },
                                onSelectProgramDetail = { program, channel ->
                                    detailedProgram = program
                                }
                            )
                            2 -> MobileSettingsPanel(
                                uiState = uiState,
                                viewModel = viewModel
                            )
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
                    if (uiState.playbackState !is PlaybackState.Idle) {
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
        }
    }

    // Playlist & EPG URL Setup Dialog
    if (showUrlDialog) {
        ConfigureUrlsDialog(uiState = uiState, viewModel = viewModel, onDismiss = { showUrlDialog = false })
    }

    // Detailed Program dialog
    detailedProgram?.let { program ->
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
