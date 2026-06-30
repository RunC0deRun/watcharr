package com.iptv.mobile.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.db.ProgramEntity
import com.iptv.shared.mvi.*

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

@Composable
fun WelcomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Welcome to Watcharr",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enjoy your high-fidelity, privacy-focused IPTV streams.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun CategoryGroupsRow(uiState: IptvUiState, viewModel: MobileViewModel) {
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
fun ChannelsList(uiState: IptvUiState, viewModel: MobileViewModel, modifier: Modifier = Modifier) {
    if (uiState.isLoadingPlaylist) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (uiState.channels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (uiState.searchQuery.isNotEmpty()) "No matching channels." else "No channels loaded. Use the configuration setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.channels, key = { it.url }) { channel ->
                val programs = uiState.epgData[channel.url] ?: emptyList()
                val current = programs.firstOrNull()
                val next = if (programs.size > 1) programs[1] else null
                val isFav = uiState.favoriteUrls.contains(channel.url)

                ChannelListItem(
                    channel = channel,
                    currentProgram = current,
                    nextProgram = next,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(channel.url) },
                    onClick = { viewModel.handleIntent(PlaybackIntent.SelectChannel(channel)) }
                )
            }
        }
    }
}

@Composable
fun ConfigureUrlsDialog(uiState: IptvUiState, viewModel: MobileViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isDispatcharrMode by remember { mutableStateOf(uiState.useDispatcharr) }
    var dispatcharrInput by remember { mutableStateOf(uiState.dispatcharrUrl) }
    var m3uInput by remember { mutableStateOf(uiState.playlistUrlInput) }
    var epgInput by remember { mutableStateOf(uiState.epgUrlInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Client Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text("Dispatcharr Server Mode", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = isDispatcharrMode, onCheckedChange = { isDispatcharrMode = it })
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
                        label = { Text("EPG XMLTV URL (Optional)") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        try {
                            val scanner = GmsBarcodeScanning.getClient(context)
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val raw = barcode.rawValue ?: ""
                                    if (raw.isNotEmpty()) {
                                        if (raw.startsWith("http") && raw.contains("/setup")) {
                                            viewModel.sendConfigToTv(
                                                tvSetupUrl = raw,
                                                onSuccess = {
                                                    Toast.makeText(context, "Configuration successfully sent to TV paired screen!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Pairing server rejected pairing: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            if (isDispatcharrMode) {
                                                dispatcharrInput = raw
                                            } else {
                                                if (raw.contains(".m3u")) {
                                                    m3uInput = raw
                                                } else {
                                                    epgInput = raw
                                                }
                                            }
                                        }
                                    }
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
    uiState: IptvUiState,
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
