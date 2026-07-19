package io.github.runc0derun.watcharr.mobile.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import io.github.runc0derun.watcharr.mobile.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import io.github.runc0derun.watcharr.shared.data.db.ProgramEntity
import io.github.runc0derun.watcharr.shared.mvi.*
import androidx.compose.animation.Crossfade
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer

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

                val isAutomotive = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUTOMOTIVE)
                if (!isAutomotive) {
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
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Scan TV Setup QR Code", color = MaterialTheme.colorScheme.onSecondary)
                        }
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
    val context = LocalContext.current
    val isPlaying = state is PlaybackState.Playing
    val player = remember(viewModel) { viewModel.playerEngine.getPlayer() }
    val isRestricted = (state is PlaybackState.Playing && state.isVideoRestricted)
    var areControlsVisible by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

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
                        keepScreenOn = true
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                            areControlsVisible = visibility == android.view.View.VISIBLE
                        })
                    }
                },
                update = { view ->
                    view.player = player
                    view.keepScreenOn = true
                    view.setFullscreenButtonClickListener { isFullScreen ->
                        onToggleFullscreen()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { layoutCoordinates ->
                        if (isPlaying) {
                            var currentContext = context
                            var activity: android.app.Activity? = null
                            while (currentContext is android.content.ContextWrapper) {
                                if (currentContext is android.app.Activity) {
                                    activity = currentContext
                                    break
                                }
                                currentContext = currentContext.baseContext
                            }
                            activity?.let { act ->
                                val builder = android.app.PictureInPictureParams.Builder()
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    builder.setAutoEnterEnabled(true)
                                }
                                val bounds = layoutCoordinates.boundsInWindow()
                                val sourceRect = android.graphics.Rect(
                                    bounds.left.toInt(),
                                    bounds.top.toInt(),
                                    bounds.right.toInt(),
                                    bounds.bottom.toInt()
                                )
                                builder.setSourceRectHint(sourceRect)
                                builder.setAspectRatio(android.util.Rational(16, 9))
                                try {
                                    act.setPictureInPictureParams(builder.build())
                                } catch (e: Exception) {
                                    android.util.Log.e("Watcharr", "Failed to update setPictureInPictureParams on glob", e)
                                }
                            }
                        }
                    }
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.playerEngine.stop() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                }
            }
            else -> {}
        }

        if (state is PlaybackState.Playing && !isRestricted) {
            if (areControlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { showStats = !showStats },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_stats),
                            contentDescription = "Stats",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (showStats) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    MobilePlayerStatsOverlay(
                        player = player,
                        onClose = { showStats = false }
                    )
                }
            }
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
                            painter = painterResource(id = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
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
                painter = painterResource(id = R.drawable.ic_play_arrow),
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

private val timeFormatter get() = DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.getDefault())
    .withZone(ZoneId.systemDefault())

fun formatTimeRange(startMs: Long, stopMs: Long): String {
    val startStr = timeFormatter.format(Instant.ofEpochMilli(startMs))
    val stopStr = timeFormatter.format(Instant.ofEpochMilli(stopMs))
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
fun TailscaleStatusIndicator(status: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "Pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    val (color, text, isPulsing) = when {
        status.startsWith("Error", ignoreCase = true) -> Triple(Color(0xFFE57373), status, false)
        status == "Authenticating" -> Triple(Color(0xFFFFB74D), "Authenticating with Tailnet...", true)
        status == "Connected" -> Triple(Color(0xFF81C784), "Connected to Tailnet", false)
        status == "Dialing Stream" -> Triple(Color(0xFF4FC3F7), "Streaming via Tailnet...", true)
        status.isEmpty() -> Triple(Color.Gray, "Tailnet Node Stopped", false)
        else -> Triple(Color(0xFFFFB74D), status, true)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    if (isPulsing) {
                        this.alpha = alpha
                    }
                }
                .background(color, shape = CircleShape)
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MobileOnboardingWizard(viewModel: MobileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(0) }
    var manualStep by remember { mutableIntStateOf(0) }
    var isDispatcharrMode by remember { mutableStateOf(true) }
    var dispatcharrInput by remember { mutableStateOf("") }
    var m3uInput by remember { mutableStateOf("") }
    var epgInput by remember { mutableStateOf("") }

    var isTailnetSelected by remember { mutableStateOf(uiState.isTailnetEnabled) }
    var tailscaleAuthKeyInput by remember(uiState.tailscaleAuthKey) { mutableStateOf(uiState.tailscaleAuthKey) }

    val context = LocalContext.current
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val isAutomotive = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUTOMOTIVE)
    val isTablet = with(density) { containerSize.width.toDp() } >= 600.dp && !isAutomotive

    if (isTablet && !uiState.isLoadingPlaylist && !uiState.isLoadingEpg) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                    text = "Pair Android TV",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan the QR code displayed on your Android TV setup screen to automatically pair and transfer configuration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        try {
                            val scanner = GmsBarcodeScanning.getClient(context)
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val raw = barcode.rawValue ?: ""
                                    if (raw.isNotEmpty() && raw.startsWith("http") && raw.contains("/setup")) {
                                        viewModel.sendConfigToTv(
                                            tvSetupUrl = raw,
                                            onSuccess = {
                                                Toast.makeText(context, "Configuration successfully sent to TV paired screen!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Pairing server rejected pairing: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Scanner unavailable: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text("Scan TV Setup QR Code")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (manualStep) {
                    0 -> {
                        Text(
                            text = "Manual Setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure your connection URLs directly on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isDispatcharrMode = true
                                manualStep = 1
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Dispatcharr Server")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isDispatcharrMode = false
                                manualStep = 1
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enter Custom URLs")
                        }
                    }
                    1 -> {
                        Text(
                            text = "Tailscale Tailnet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTailnetSelected = true },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTailnetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isTailnetSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Yes, on a Tailnet", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("My server/sources are hosted on a private Tailscale network.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTailnetSelected = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isTailnetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (!isTailnetSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("No, standard network", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Connect directly via local IP or public internet URL.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { manualStep = 0 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (isTailnetSelected) {
                                        manualStep = 2
                                    } else {
                                        viewModel.setTailnetEnabled(false)
                                        manualStep = 3
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next")
                            }
                        }
                    }
                    2 -> {
                        Text(
                            text = "Tailscale Setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = tailscaleAuthKeyInput,
                            onValueChange = { tailscaleAuthKeyInput = it },
                            label = { Text("Tailscale Auth Key") },
                            placeholder = { Text("tskey-auth-...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.connectTailscale(tailscaleAuthKeyInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TailscaleStatusIndicator(status = uiState.tsnetStatus)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { manualStep = 1 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                Text("Back")
                            }
                            Button(onClick = { manualStep = 3 }, modifier = Modifier.weight(1f)) {
                                Text("Next")
                            }
                        }
                    }
                    3 -> {
                        Text(
                            text = if (isDispatcharrMode) "Dispatcharr Server" else "Custom URLs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    if (isTailnetSelected) {
                                        manualStep = 2
                                    } else {
                                        manualStep = 1
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
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
        return
    }

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
                    text = "Tailscale Tailnet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Is your IPTV source hosted on a Tailscale Tailnet?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTailnetSelected = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTailnetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isTailnetSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Yes, on a Tailnet", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("My server/sources are hosted on a private Tailscale network.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTailnetSelected = false },
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isTailnetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (!isTailnetSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No, standard network", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Connect directly via local IP or public internet URL.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { currentStep = 0 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            if (isTailnetSelected) {
                                currentStep = 2
                            } else {
                                viewModel.setTailnetEnabled(false)
                                currentStep = 3
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }
                }
            }

            2 -> {
                Text(
                    text = "Tailscale Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = tailscaleAuthKeyInput,
                    onValueChange = { tailscaleAuthKeyInput = it },
                    label = { Text("Tailscale Auth Key") },
                    placeholder = { Text("tskey-auth-...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.connectTailscale(tailscaleAuthKeyInput) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Connect to Tailnet")
                }
                Spacer(modifier = Modifier.height(12.dp))
                TailscaleStatusIndicator(status = uiState.tsnetStatus)

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { currentStep = 1 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("Back")
                    }
                    Button(onClick = { currentStep = 3 }, modifier = Modifier.weight(1f)) {
                        Text("Next")
                    }
                }
            }

            3 -> {
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
                    Button(
                        onClick = {
                            if (isTailnetSelected) {
                                currentStep = 2
                            } else {
                                currentStep = 1
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Back")
                    }
                    Button(onClick = { currentStep = 4 }, modifier = Modifier.weight(1f)) {
                        Text("Next")
                    }
                }
            }

            4 -> {
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
                    Button(onClick = { currentStep = 3 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
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

data class CarouselItem(val program: ProgramEntity, val channel: ChannelEntity)

@Composable
fun MobileHeroCarousel(
    carouselItems: List<CarouselItem>,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(carouselItems) {
        activeIndex = 0
        if (carouselItems.size > 1) {
            while (true) {
                delay(5.seconds)
                activeIndex = (activeIndex + 1) % carouselItems.size
            }
        }
    }

    val activeItem = carouselItems.getOrNull(activeIndex) ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable { 
                carouselItems.getOrNull(activeIndex)?.let {
                    onSelectProgramDetail(it.program, it.channel)
                }
            }
            .clip(RoundedCornerShape(12.dp))
    ) {
        Crossfade(
            targetState = activeItem,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
            label = "MobileHeroCarouselTransition"
        ) { item ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (!item.program.iconUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.program.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.66f)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(480.dp)
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Featured",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = item.program.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.channel.name} • ${formatTimeRange(item.program.start, item.program.stop)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    val desc = item.program.desc
                    if (!desc.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MobileNowLiveRow(
    liveItems: List<CarouselItem>,
    onSelectChannel: (ChannelEntity) -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(liveItems, key = { it.channel.url }) { item ->
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .height(110.dp)
                    .clickable { onSelectChannel(item.channel) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!item.channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = item.channel.logoUrl,
                            contentDescription = item.channel.name,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.channel.name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.channel.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.program.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatTimeRange(item.program.start, item.program.stop),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { onSelectProgramDetail(item.program, item.channel) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = "Program detail",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileChannelsGrid(
    uiState: IptvUiState,
    viewModel: MobileViewModel,
    onSelectChannel: (ChannelEntity) -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit
) {
    val carouselItems = remember(uiState.epgData, uiState.channels) {
        val now = System.currentTimeMillis()
        val twoHours = 2 * 60 * 60 * 1000
        val items = mutableListOf<CarouselItem>()
        for (channel in uiState.channels) {
            val programs = uiState.epgData[channel.url] ?: emptyList()
            for (prog in programs) {
                if (prog.start >= now && prog.start <= now + twoHours) {
                    items.add(CarouselItem(prog, channel))
                }
            }
        }
        if (items.size >= 3) {
            items.sortedBy { it.program.start }.take(3)
        } else {
            val fallbackItems = mutableListOf<CarouselItem>()
            for (channel in uiState.channels) {
                val programs = uiState.epgData[channel.url] ?: emptyList()
                for (prog in programs) {
                    if (prog.stop > now) {
                        fallbackItems.add(CarouselItem(prog, channel))
                    }
                }
            }
            if (fallbackItems.size >= 3) {
                fallbackItems.sortedBy { it.program.start }.take(3)
            } else {
                val allItems = mutableListOf<CarouselItem>()
                for (channel in uiState.channels) {
                    val programs = uiState.epgData[channel.url] ?: emptyList()
                    for (prog in programs) {
                        allItems.add(CarouselItem(prog, channel))
                    }
                }
                allItems.sortedBy { it.program.start }.take(3)
            }
        }
    }

    val liveItems = remember(uiState.epgData, uiState.channels) {
        val items = mutableListOf<CarouselItem>()
        for (channel in uiState.channels) {
            val programs = uiState.epgData[channel.url] ?: emptyList()
            val current = programs.getOrNull(0)
            if (current != null) {
                items.add(CarouselItem(current, channel))
            }
        }
        items
    }

    val isFilterActive = uiState.searchQuery.isNotEmpty() || uiState.selectedGroup != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (uiState.isLoadingPlaylist) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.searchQuery.isNotEmpty()) "No matching channels." else "No channels loaded.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (!isFilterActive) {
                    if (carouselItems.isNotEmpty()) {
                        item {
                            MobileHeroCarousel(
                                carouselItems = carouselItems,
                                onSelectProgramDetail = onSelectProgramDetail
                            )
                        }
                    }

                    if (liveItems.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "NOW LIVE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "• All channels",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                MobileNowLiveRow(
                                    liveItems = liveItems,
                                    onSelectChannel = onSelectChannel,
                                    onSelectProgramDetail = onSelectProgramDetail
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "BROWSE ALL CHANNELS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
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
                                            painter = painterResource(id = R.drawable.ic_star),
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

                val columns = 4
                val rows = (uiState.channels.size + columns - 1) / columns
                items(rows, key = { it }) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (colIndex in 0 until columns) {
                            val itemIndex = rowIndex * columns + colIndex
                            if (itemIndex < uiState.channels.size) {
                                val channel = uiState.channels[itemIndex]
                                Box(modifier = Modifier.weight(1f)) {
                                    Card(
                                        modifier = Modifier
                                            .aspectRatio(1.5f)
                                            .fillMaxWidth()
                                            .clickable { onSelectChannel(channel) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!channel.logoUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = channel.logoUrl,
                                                    contentDescription = channel.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text(
                                                    text = channel.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
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

@Composable
fun MobileFullEpgGuide(
    uiState: IptvUiState,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Browse current and upcoming programs across all channels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        items(uiState.channels, key = { it.url }) { channel ->
            val programs = uiState.epgData[channel.url] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .clickable { onSelectChannel(channel) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
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
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                        Text("No program data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(programs, key = { it.channelId + "_" + it.start }) { program ->
                            val durationMin = (program.stop - program.start) / 60000
                            val cardWidth = (durationMin * 5).coerceIn(160L, 600L).toInt().dp

                            Card(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .fillMaxHeight()
                                    .clickable { onSelectProgramDetail(program, channel) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = program.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MobileSettingsPanel(
    uiState: IptvUiState,
    viewModel: MobileViewModel
) {
    val context = LocalContext.current
    val isAutomotive = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUTOMOTIVE)
    var isDispatcharrMode by remember { mutableStateOf(uiState.useDispatcharr) }
    var dispatcharrInput by remember { mutableStateOf(uiState.dispatcharrUrl) }
    var m3uInput by remember { mutableStateOf(uiState.playlistUrlInput) }
    var epgInput by remember { mutableStateOf(uiState.epgUrlInput) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Watcharr Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isAutomotive) "Configure your connection URLs" else "Configure your connection URLs or pair with an Android TV",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(if (isAutomotive) 1f else 1.2f)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDispatcharrMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Dispatcharr Server",
                            color = if (isDispatcharrMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { isDispatcharrMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDispatcharrMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Custom URLs",
                            color = if (!isDispatcharrMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                    OutlinedTextField(
                        value = epgInput,
                        onValueChange = { epgInput = it },
                        label = { Text("EPG XMLTV URL") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        Toast.makeText(context, "Settings reloaded successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply & Reload")
                }
            }

            if (!isAutomotive) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Pair Android TV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan the QR code displayed on your TV's pairing setup screen to pair automatically and sync configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_info), contentDescription = null)
                            Text("Scan TV Setup QR Code")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MobilePlayerStatsOverlay(
    player: androidx.media3.exoplayer.ExoPlayer,
    onClose: () -> Unit
) {
    var stats by remember { mutableStateOf<io.github.runc0derun.watcharr.shared.playback.StreamStats?>(null) }

    LaunchedEffect(player) {
        while (true) {
            stats = io.github.runc0derun.watcharr.shared.playback.StreamStatsHelper.getStreamStats(player)
            delay(1.seconds)
        }
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .width(280.dp)
            .clickable { onClose() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stream Stats",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to close",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            stats?.let { s ->
                MobileStatsRow(label = "Resolution", value = s.resolution)
                MobileStatsRow(label = "Frame Rate", value = s.frameRate)
                MobileStatsRow(label = "Video Codec", value = s.videoCodec)
                MobileStatsRow(label = "Video Bitrate", value = s.videoBitrate)
                MobileStatsRow(label = "Audio Codec", value = s.audioCodec)
                MobileStatsRow(label = "Audio Bitrate", value = s.audioBitrate)
                MobileStatsRow(label = "Audio Channels", value = s.audioChannels)
                MobileStatsRow(label = "Sample Rate", value = s.audioSampleRate)
                MobileStatsRow(label = "Buffer Ahead", value = s.bufferAhead)
            } ?: run {
                Text("Loading stream info...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MobileStatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


