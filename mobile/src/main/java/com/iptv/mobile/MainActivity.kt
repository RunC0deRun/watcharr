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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.Canvas
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
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search



class MainActivity : ComponentActivity() {

    private val viewModel: MobileViewModel by viewModels()
    private val isInPipMode = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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

@Composable
fun IPTVAppTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFDF9A28), // Mustard Yellow
        secondary = Color(0xFFF5C453), // Light Mustard
        background = Color(0xFF062A1F), // Dark Forest Green
        surface = Color(0xFF0F3E30), // Medium Forest Green
        onBackground = Color(0xFFFAF8F5),
        onSurface = Color(0xFFFAF8F5),
        tertiary = Color(0xFF49A752),
        tertiaryContainer = Color(0xFF0F6633),
        error = Color(0xFFFFD700)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@Composable
fun WatcharrLogo(modifier: Modifier = Modifier) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val tertiaryContainerColor = MaterialTheme.colorScheme.tertiaryContainer
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.size(160.dp, 100.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = w * 0.12f

            val greenPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.12f, h * 0.18f)
                cubicTo(
                    w * 0.18f, h * 0.60f,
                    w * 0.25f, h * 0.85f,
                    w * 0.35f, h * 0.85f
                )
                cubicTo(
                    w * 0.46f, h * 0.85f,
                    w * 0.52f, h * 0.30f,
                    w * 0.57f, h * 0.26f
                )
                cubicTo(
                    w * 0.60f, h * 0.24f,
                    w * 0.63f, h * 0.32f,
                    w * 0.61f, h * 0.45f
                )
            }

            val yellowPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.53f, h * 0.45f)
                cubicTo(
                    w * 0.51f, h * 0.32f,
                    w * 0.54f, h * 0.24f,
                    w * 0.57f, h * 0.26f
                )
                cubicTo(
                    w * 0.61f, h * 0.30f,
                    w * 0.67f, h * 0.85f,
                    w * 0.78f, h * 0.85f
                )
                cubicTo(
                    w * 0.88f, h * 0.85f,
                    w * 0.95f, h * 0.60f,
                    w * 1.0f, h * 0.18f
                )
            }

            val greenBrush = Brush.verticalGradient(
                colors = listOf(tertiaryColor, tertiaryContainerColor)
            )

            val yellowBrush = Brush.verticalGradient(
                colors = listOf(secondaryColor, primaryColor)
            )

            drawPath(
                path = greenPath,
                brush = greenBrush,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            drawPath(
                path = yellowPath,
                brush = yellowBrush,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WATCHARR",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Bring your own TV",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun MobileSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WatcharrLogo()
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary
            )
        }
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
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Hide channels list",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Show Channels")
                                }
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
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Favorites")
                    }
                }
            )
        }
        items(uiState.groups, key = { it }) { group ->
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
            items(uiState.channels, key = { it.url }) { channel ->
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
    val context = LocalContext.current
    var isDispatcharrMode by remember { mutableStateOf(uiState.useDispatcharr) }
    var dispatcharrInput by remember { mutableStateOf(uiState.dispatcharrUrl) }
    var m3uInput by remember { mutableStateOf(uiState.playlistUrlInput) }
    var epgInput by remember { mutableStateOf(uiState.epgUrlInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tab choice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isDispatcharrMode = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDispatcharrMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Dispatcharr")
                    }
                    Button(
                        onClick = { isDispatcharrMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDispatcharrMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Custom")
                    }
                }

                if (isDispatcharrMode) {
                    OutlinedTextField(
                        value = dispatcharrInput,
                        onValueChange = { dispatcharrInput = it },
                        label = { Text("Dispatcharr Server URL") },
                        placeholder = { Text("http://192.168.1.100:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = m3uInput,
                        onValueChange = { m3uInput = it },
                        label = { Text("M3U Playlist URL") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = epgInput,
                        onValueChange = { epgInput = it },
                        label = { Text("EPG XMLTV URL") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Scan TV QR Button
                Button(
                    onClick = {
                        try {
                            val scanner = GmsBarcodeScanning.getClient(context)
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val tvUrl = barcode.rawValue
                                    if (tvUrl != null && tvUrl.startsWith("http")) {
                                        viewModel.sendConfigToTv(
                                            tvSetupUrl = tvUrl,
                                            onSuccess = {
                                                Toast.makeText(context, "TV Client paired successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Pairing failed: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, "Invalid QR code format", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Scan failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Scanner unavailable: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Scan TV Setup QR Code", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isDispatcharrMode) {
                        if (dispatcharrInput.isNotEmpty()) {
                            val m3u = "$dispatcharrInput/output/m3u"
                            val epg = "$dispatcharrInput/output/epg"
                            viewModel.completeOnboarding(m3u, epg, dispatcharrInput, true)
                        }
                    } else {
                        if (m3uInput.isNotEmpty()) {
                            viewModel.completeOnboarding(m3uInput, epgInput, null, false)
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
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
                    .background(MaterialTheme.colorScheme.background),
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Safety Mode: Video stream is hidden while the vehicle is in motion. Audio continues playing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
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
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
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
            
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play channel",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
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
                items(programs, key = { it.channelId + "_" + it.start }) { program ->
                    EpgProgramTimelineItem(program = program, onClick = { onProgramClick(program) })
                }
            }
        }
    }
}

private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.getDefault())
    .withZone(java.time.ZoneId.systemDefault())

fun formatTimeRange(startMs: Long, stopMs: Long): String {
    val startStr = timeFormatter.format(java.time.Instant.ofEpochMilli(startMs))
    val stopStr = timeFormatter.format(java.time.Instant.ofEpochMilli(stopMs))
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

@Composable
fun MobileOnboardingWizard(viewModel: MobileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(0) }
    var isDispatcharrMode by remember { mutableStateOf(true) }
    var dispatcharrInput by remember { mutableStateOf("") }
    var m3uInput by remember { mutableStateOf("") }
    var epgInput by remember { mutableStateOf("") }

    if (uiState.isLoadingPlaylist || uiState.isLoadingEpg) {
        // Step 3: Loading Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Configuring Client...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.isLoadingPlaylist) "Downloading and parsing M3U playlist..." else "Syncing EPG data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (currentStep) {
            0 -> {
                // Welcome Screen
                Text(
                    text = "Watcharr IPTV",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Privacy-first, zero cloud tracking IPTV client.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { currentStep = 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Get Started")
                }
            }

            1 -> {
                // Choice Selection
                Text(
                    text = "Server Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDispatcharrMode = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDispatcharrMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isDispatcharrMode) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dispatcharr Server (Recommended)", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Automatically configures your playlist and guide using a Dispatcharr server URL.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDispatcharrMode = false },
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isDispatcharrMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (!isDispatcharrMode) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom Setup", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Manually enter custom URLs for your M3U playlist and EPG XMLTV source.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { currentStep = 0 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("Back")
                    }
                    Button(onClick = { currentStep = 2 }, modifier = Modifier.weight(1f)) {
                        Text("Next")
                    }
                }
            }

            2 -> {
                // Form input
                Text(
                    text = if (isDispatcharrMode) "Dispatcharr URL" else "Custom URLs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isDispatcharrMode) {
                    OutlinedTextField(
                        value = dispatcharrInput,
                        onValueChange = { dispatcharrInput = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://192.168.1.100:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = m3uInput,
                        onValueChange = { m3uInput = it },
                        label = { Text("M3U Playlist URL") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = epgInput,
                        onValueChange = { epgInput = it },
                        label = { Text("EPG XMLTV URL (Optional)") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { currentStep = 1 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            if (isDispatcharrMode) {
                                if (dispatcharrInput.isNotEmpty()) {
                                    val m3u = "$dispatcharrInput/output/m3u"
                                    val epg = "$dispatcharrInput/output/epg"
                                    viewModel.completeOnboarding(m3u, epg, dispatcharrInput, true)
                                }
                            } else {
                                if (m3uInput.isNotEmpty()) {
                                    viewModel.completeOnboarding(m3uInput, epgInput, null, false)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = if (isDispatcharrMode) dispatcharrInput.isNotEmpty() else m3uInput.isNotEmpty()
                    ) {
                        Text("Load")
                    }
                }
            }
        }
    }
}

