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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.border
import com.google.zxing.qrcode.QRCodeWriter
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp



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
        primary = Color(0xFFDF9A28), // Mustard Yellow
        secondary = Color(0xFFF5C453), // Light Mustard
        background = Color(0xFF062A1F), // Dark Forest Green
        surface = Color(0xFF0F3E30), // Medium Forest Green
        onBackground = Color(0xFFFAF8F5),
        onSurface = Color(0xFFFAF8F5)
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
                                        viewModel.playerEngine.release()
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

            Row(modifier = Modifier.fillMaxSize()) {
                // Left Docked Navigation Sidebar
                TvSidebar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    firstItemFocusRequester = setupPlaylistFocusRequester
                )

                // Right Main Content Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        TvTab.CHANNELS -> {
                            TvChannelsGrid(
                                uiState = uiState,
                                viewModel = viewModel,
                                onSelectChannel = {
                                    viewModel.handleIntent(PlaybackIntent.SelectChannel(it))
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
                        color = if (isFocused) Color(0xFF062A1F) else Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Text(
                            text = "★",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isFocused) Color(0xFF062A1F) else Color(0xFFFFD700),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                
                val groupTitle = channel.groupTitle
                if (!groupTitle.isNullOrEmpty()) {
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) Color(0xFF062A1F).copy(alpha = 0.8f) else Color.LightGray.copy(alpha = 0.8f)
                    )
                }

                if (currentProgram != null) {
                    Text(
                        text = "Now: ${currentProgram.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFocused) Color(0xFF062A1F) else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (nextProgram != null) {
                    Text(
                        text = "Next: ${nextProgram.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) Color(0xFF062A1F).copy(alpha = 0.7f) else Color.LightGray.copy(alpha = 0.6f)
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
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    isClickable = false
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
                .background(Color(0xFF062A1F).copy(alpha = 0.95f))
                .padding(16.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight) {
                        onClose()
                        true
                    } else {
                        false
                    }
                }
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

                    var isChCardFocused by remember { mutableStateOf(false) }

                    Card(
                        onClick = { onSelectChannel(channel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                            .onFocusChanged { isChCardFocused = it.isFocused },
                        colors = CardDefaults.colors(
                            containerColor = Color(0xFF0F3E30),
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isFav) "★ ${channel.name}" else channel.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isChCardFocused) Color(0xFF062A1F) else Color.White,
                                maxLines = 1
                            )
                            if (current != null) {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChCardFocused) Color(0xFF062A1F).copy(alpha = 0.8f) else Color.LightGray.copy(alpha = 0.8f),
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
    onClose: () -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit
) {
    val activeChannel = (uiState.playbackState as? PlaybackState.Playing)?.channel
    val activeChannelIndex = remember(uiState.channels, activeChannel) {
        if (activeChannel != null) {
            val idx = uiState.channels.indexOfFirst { it.url == activeChannel.url }
            if (idx != -1) idx else 0
        } else {
            0
        }
    }

    val focusRequester = remember { FocusRequester() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(activeChannelIndex, uiState.channels) {
        if (uiState.channels.isNotEmpty() && activeChannelIndex in uiState.channels.indices) {
            listState.scrollToItem(activeChannelIndex)
        }
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
                    color = Color(0xFF062A1F).copy(alpha = 0.9f),
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
                        text = "Press Back or Right to Close",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                if (uiState.channels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels loaded.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.channels) { index, channel ->
                            val programs = uiState.epgData[channel.url] ?: emptyList()
                            val current = programs.getOrNull(0)
                            val upcoming = programs.drop(1)
                            
                            val isFav = uiState.favoriteUrls.contains(channel.url)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F3E30).copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cell 1: Channel Info (Focusable, Click switches channel)
                                var isChFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(72.dp)
                                        .then(if (index == activeChannelIndex) Modifier.focusRequester(focusRequester) else Modifier)
                                        .onFocusChanged { isChFocused = it.isFocused }
                                        .clickable { onSelectChannel(channel) }
                                        .background(
                                            color = if (isChFocused) MaterialTheme.colorScheme.primary else Color(0xFF0F3E30).copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isChFocused) 2.dp else 0.dp,
                                            color = if (isChFocused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = if (isFav) "★ ${channel.name}" else channel.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChFocused) Color(0xFF062A1F) else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        channel.groupTitle?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChFocused) Color(0xFF062A1F).copy(alpha = 0.8f) else Color.LightGray.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Cell 2: Now Playing (Focusable, Click opens details)
                                var isCell2Focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(72.dp)
                                        .onFocusChanged { isCell2Focused = it.isFocused }
                                        .clickable(enabled = current != null) { current?.let { onSelectProgramDetail(it, channel) } }
                                        .background(
                                            color = if (isCell2Focused) MaterialTheme.colorScheme.primary else Color(0xFF0F3E30).copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isCell2Focused) 2.dp else 0.dp,
                                            color = if (isCell2Focused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (current != null) {
                                        Column {
                                            Text(
                                                text = "Now: ${current.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCell2Focused) Color(0xFF062A1F) else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val now = System.currentTimeMillis()
                                            val total = current.stop - current.start
                                            val progress = if (total > 0) (now - current.start).toFloat() / total else 0f
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = progress.coerceIn(0f, 1f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = if (isCell2Focused) Color(0xFF062A1F) else MaterialTheme.colorScheme.primary,
                                                trackColor = if (isCell2Focused) Color(0xFF062A1F).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "No info available",
                                            color = if (isCell2Focused) Color(0xFF062A1F).copy(alpha = 0.8f) else Color.Gray,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Cell 3: Next Program (Focusable, Click opens details)
                                val nextProg = upcoming.getOrNull(0)
                                var isCell3Focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(72.dp)
                                        .onFocusChanged { isCell3Focused = it.isFocused }
                                        .clickable(enabled = nextProg != null) { nextProg?.let { onSelectProgramDetail(it, channel) } }
                                        .background(
                                            color = if (isCell3Focused) MaterialTheme.colorScheme.primary else Color(0xFF0F3E30).copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isCell3Focused) 2.dp else 0.dp,
                                            color = if (isCell3Focused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (nextProg != null) {
                                        Column {
                                            Text(
                                                text = "Next: ${nextProg.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCell3Focused) Color(0xFF062A1F).copy(alpha = 0.9f) else Color.LightGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatTimeRange(nextProg.start, nextProg.stop),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCell3Focused) Color(0xFF062A1F).copy(alpha = 0.7f) else Color.Gray
                                            )
                                        }
                                    } else {
                                        Text("-", color = if (isCell3Focused) Color(0xFF062A1F).copy(alpha = 0.7f) else Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Cell 4: Later Program (Focusable, Click opens details)
                                val laterProg = upcoming.getOrNull(1)
                                var isCell4Focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .onFocusChanged { isCell4Focused = it.isFocused }
                                        .clickable(enabled = laterProg != null) { laterProg?.let { onSelectProgramDetail(it, channel) } }
                                        .background(
                                            color = if (isCell4Focused) MaterialTheme.colorScheme.primary else Color(0xFF0F3E30).copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isCell4Focused) 2.dp else 0.dp,
                                            color = if (isCell4Focused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (laterProg != null) {
                                        Column {
                                            Text(
                                                text = "Later: ${laterProg.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCell4Focused) Color(0xFF062A1F).copy(alpha = 0.9f) else Color.LightGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatTimeRange(laterProg.start, laterProg.stop),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCell4Focused) Color(0xFF062A1F).copy(alpha = 0.7f) else Color.Gray
                                            )
                                        }
                                    } else {
                                        Text("-", color = if (isCell4Focused) Color(0xFF062A1F).copy(alpha = 0.7f) else Color.Gray)
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

enum class TvTab {
    CHANNELS, EPG, SETTINGS
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSidebar(
    selectedTab: TvTab,
    onTabSelected: (TvTab) -> Unit,
    firstItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp)
            .background(Color(0xFF062A1F))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Logo
        Text(
            text = "W",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TvSidebarItem(
            icon = "📺",
            label = "Channels",
            isSelected = selectedTab == TvTab.CHANNELS,
            onClick = { onTabSelected(TvTab.CHANNELS) },
            modifier = Modifier.focusRequester(firstItemFocusRequester)
        )

        TvSidebarItem(
            icon = "📅",
            label = "TV Guide",
            isSelected = selectedTab == TvTab.EPG,
            onClick = { onTabSelected(TvTab.EPG) }
        )

        TvSidebarItem(
            icon = "⚙️",
            label = "Setup",
            isSelected = selectedTab == TvTab.SETTINGS,
            onClick = { onTabSelected(TvTab.SETTINGS) }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSidebarItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(64.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                color = if (isFocused) MaterialTheme.colorScheme.primary 
                        else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                        else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.secondary else if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvChannelsGrid(
    uiState: TvUiState,
    viewModel: TvViewModel,
    onSelectChannel: (ChannelEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Watcharr TV Channels",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Select a channel to start watching",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search channels...") },
                singleLine = true,
                modifier = Modifier.width(300.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(20.dp))

        // Grid Content
        if (uiState.isLoadingPlaylist) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.searchQuery.isNotEmpty()) "No matching channels." else "No channels loaded.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        } else {
            val columns = 5
            val rows = (uiState.channels.size + columns - 1) / columns
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(rows) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (colIndex in 0 until columns) {
                            val itemIndex = rowIndex * columns + colIndex
                            if (itemIndex < uiState.channels.size) {
                                val channel = uiState.channels[itemIndex]
                                Box(modifier = Modifier.weight(1f)) {
                                    TvChannelGridItem(
                                        channel = channel,
                                        onClick = { onSelectChannel(channel) },
                                        onLongClick = { 
                                            viewModel.toggleFavorite(channel.url)
                                            Toast.makeText(viewModel.getApplication(), "Favorite toggled for ${channel.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TvChannelGridItem(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                color = if (isFocused) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color(0xFF062A1F) else Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!channel.logoUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color(0xFF062A1F) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFullEpgGuide(
    uiState: TvUiState,
    viewModel: TvViewModel,
    onSelectChannel: (ChannelEntity) -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "TV Program Guide",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Browse current and upcoming programs across all channels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        
        items(uiState.channels) { channel ->
            val programs = uiState.epgData[channel.url] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var isChFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .onFocusChanged { isChFocused = it.isFocused }
                        .clickable { onSelectChannel(channel) }
                        .background(
                            color = if (isChFocused) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isChFocused) 2.dp else 0.dp,
                            color = if (isChFocused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        SubcomposeAsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            error = {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChFocused) Color(0xFF062A1F) else Color.White,
                                    maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    } else {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isChFocused) Color(0xFF062A1F) else Color.White,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (programs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No program data available", color = Color.Gray)
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(programs) { program ->
                            val durationMin = (program.stop - program.start) / 60000
                            val cardWidth = (durationMin * 5).coerceIn(160L, 600L).toInt().dp
                            
                            var isProgFocused by remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .fillMaxHeight()
                                    .onFocusChanged { isProgFocused = it.isFocused }
                                    .clickable { onSelectProgramDetail(program, channel) }
                                    .background(
                                        color = if (isProgFocused) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isProgFocused) 2.dp else 0.dp,
                                        color = if (isProgFocused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = program.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProgFocused) Color(0xFF062A1F) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatTimeRange(program.start, program.stop),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isProgFocused) Color(0xFF062A1F) else MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
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
fun TvSettingsPanel(
    uiState: TvUiState,
    viewModel: TvViewModel
) {
    var isDispatcharrMode by remember { mutableStateOf(uiState.useDispatcharr) }
    var dispatcharrInput by remember { mutableStateOf(uiState.dispatcharrUrl) }
    var m3uInput by remember { mutableStateOf(uiState.playlistUrlInput) }
    var epgInput by remember { mutableStateOf(uiState.epgUrlInput) }

    LaunchedEffect(Unit) {
        viewModel.startSetupServer()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSetupServer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Watcharr TV Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Configure your connection URLs or pair with a mobile device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Left Pane: Setup Form
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Connection Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isDispatcharrMode = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = if (isDispatcharrMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Dispatcharr Server")
                    }
                    Button(
                        onClick = { isDispatcharrMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = if (!isDispatcharrMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Custom URLs")
                    }
                }

                if (isDispatcharrMode) {
                    OutlinedTextField(
                        value = dispatcharrInput,
                        onValueChange = { dispatcharrInput = it },
                        label = { Text("Server URL") },
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
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply & Reload")
                }
            }

            // Right Pane: QR Pairing Code
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Pair Mobile Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan this QR code using the mobile app Settings to pair.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.setupQrUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeImage(content = uiState.setupQrUrl, modifier = Modifier.fillMaxSize())
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.DarkGray, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.setupStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvProgramDetailScreen(
    program: ProgramEntity,
    channel: ChannelEntity?,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF062A1F))
    ) {
        // Hero Background Image
        if (!program.iconUrl.isNullOrEmpty()) {
            AsyncImage(
                model = program.iconUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient Overlay (Dark forest green to transparent across whole screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF062A1F),
                            Color(0xFF062A1F).copy(alpha = 0.95f),
                            Color(0xFF062A1F).copy(alpha = 0.85f),
                            Color(0xFF062A1F).copy(alpha = 0.6f),
                            Color(0xFF062A1F).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp)
                .padding(start = 48.dp, top = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pill back button
            var isBackFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .onPreviewKeyEvent { keyEvent ->
                        val isScrollable = scrollState.maxValue > 0
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            if (isScrollable) {
                                when (keyEvent.key) {
                                    Key.DirectionDown -> {
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(120f)
                                        }
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(-120f)
                                        }
                                        true
                                    }
                                    Key.DirectionLeft, Key.DirectionRight -> true
                                    else -> false
                                }
                            } else {
                                when (keyEvent.key) {
                                    Key.DirectionDown, Key.DirectionUp, Key.DirectionLeft, Key.DirectionRight -> true
                                    else -> false
                                }
                            }
                        } else if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionDown, Key.DirectionUp, Key.DirectionLeft, Key.DirectionRight -> true
                                else -> false
                            }
                        } else false
                    }
                    .clickable { onDismiss() }
                    .background(
                        color = if (isBackFocused) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isBackFocused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "< Back",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBackFocused) Color(0xFF062A1F) else Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel name / Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (channel != null && !channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    text = channel?.name ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Title
            Text(
                text = program.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Subtitle (Time duration info)
            val durationMin = (program.stop - program.start) / 60000
            val dateStr = remember(program.start) {
                try {
                    val sdf = java.text.SimpleDateFormat("EEE d MMM", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(program.start))
                } catch (e: Exception) {
                    ""
                }
            }
            Text(
                text = "$dateStr ${formatTimeRange(program.start, program.stop)}, $durationMin min.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            // Status tag
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Live / Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            program.desc?.let { description ->
                Box(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


