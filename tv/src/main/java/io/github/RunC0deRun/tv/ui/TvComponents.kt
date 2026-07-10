package io.github.RunC0deRun.tv.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.annotation.SuppressLint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.github.RunC0deRun.shared.data.db.ChannelEntity
import io.github.RunC0deRun.shared.data.db.ProgramEntity
import io.github.RunC0deRun.shared.mvi.*
import io.github.RunC0deRun.tv.R
import kotlinx.coroutines.launch

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
fun TvSplashScreen() {
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

@SuppressLint("InflateParams")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvVideoPlayer(viewModel: TvViewModel, state: PlaybackState) {
    val player = remember(viewModel) { viewModel.playerEngine.getPlayer() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx).inflate(R.layout.player_view, null) as PlayerView
                view.apply {
                    this.player = player
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    isClickable = false
                    keepScreenOn = true
                }
            },
            update = { view ->
                view.keepScreenOn = true
            },
            modifier = Modifier.fillMaxSize()
        )

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
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
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
    uiState: IptvUiState,
    viewModel: TvViewModel,
    onSelectChannel: (ChannelEntity) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            .clickable { onClose() },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

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
                }
                items(uiState.groups, key = { it }) { group ->
                    FilterChip(
                        selected = uiState.selectedGroup == group,
                        onClick = { viewModel.selectGroup(group) }
                    ) {
                        Text(group)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.channels, key = { index, channel -> channel.url }) { index, channel ->
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isFav) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_star),
                                        contentDescription = "Favorite",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChCardFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            if (current != null) {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChCardFocused) MaterialTheme.colorScheme.background.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
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
    uiState: IptvUiState,
    onSelectChannel: (ChannelEntity) -> Unit,
    onClose: () -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit
) {
    BackHandler(onBack = onClose)

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
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10.seconds)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(activeChannelIndex, uiState.channels) {
        if (uiState.channels.isNotEmpty() && activeChannelIndex in uiState.channels.indices) {
            listState.scrollToItem(activeChannelIndex)
        }
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Press Back or Right to Close",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.channels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels loaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.channels, key = { index, channel -> channel.url }) { index, channel ->
                            val programs = uiState.epgData[channel.url] ?: emptyList()
                            val current = programs.getOrNull(0)
                            val upcoming = programs.drop(1)
                            val isFav = uiState.favoriteUrls.contains(channel.url)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var isChFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(72.dp)
                                        .then(if (index == activeChannelIndex) Modifier.focusRequester(focusRequester) else Modifier)
                                        .onFocusChanged { isChFocused = it.isFocused }
                                        .clickable { onSelectChannel(channel) }
                                        .background(
                                            color = if (isChFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isFav) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_star),
                                                    contentDescription = "Favorite",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = channel.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        channel.groupTitle?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChFocused) MaterialTheme.colorScheme.background.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                var isCell2Focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(72.dp)
                                        .onFocusChanged { isCell2Focused = it.isFocused }
                                        .clickable(enabled = current != null) { current?.let { onSelectProgramDetail(it, channel) } }
                                        .background(
                                            color = if (isCell2Focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
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
                                                color = if (isCell2Focused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val total = current.stop - current.start
                                            val progress = if (total > 0) (now - current.start).toFloat() / total else 0f
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = progress.coerceIn(0f, 1f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = if (isCell2Focused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary,
                                                trackColor = if (isCell2Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "No info available",
                                            color = if (isCell2Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                val nextProg = upcoming.getOrNull(0)
                                var isCell3Focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(72.dp)
                                        .onFocusChanged { isCell3Focused = it.isFocused }
                                        .clickable(enabled = nextProg != null) { nextProg?.let { onSelectProgramDetail(it, channel) } }
                                        .background(
                                            color = if (isCell3Focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
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
                                                color = if (isCell3Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatTimeRange(nextProg.start, nextProg.stop),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCell3Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    } else {
                                        Text("-", color = if (isCell3Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                val laterProg = upcoming.getOrNull(1)
                                var isCell4Focused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .onFocusChanged { isCell4Focused = it.isFocused }
                                        .clickable(enabled = laterProg != null) { laterProg?.let { onSelectProgramDetail(it, channel) } }
                                        .background(
                                            color = if (isCell4Focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
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
                                                color = if (isCell4Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatTimeRange(laterProg.start, laterProg.stop),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCell4Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    } else {
                                        Text("-", color = if (isCell4Focused) MaterialTheme.colorScheme.background.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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

private val timeFormatter get() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun formatTimeRange(startMs: Long, stopMs: Long): String {
    val startStr = timeFormatter.format(Instant.ofEpochMilli(startMs))
    val stopStr = timeFormatter.format(Instant.ofEpochMilli(stopMs))
    return "$startStr - $stopStr"
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvOnboardingScreen(uiState: IptvUiState, viewModel: TvViewModel) {
    var manualMode by remember { mutableStateOf<String?>(null) }
    var dispatcharrUrlInput by remember { mutableStateOf("") }
    var playlistUrlInput by remember { mutableStateOf("") }
    var epgUrlInput by remember { mutableStateOf("") }

    val defaultFocusRequester = remember { FocusRequester() }
    val dispatcharrFocusRequester = remember { FocusRequester() }
    val customFocusRequester = remember { FocusRequester() }
    val connectButtonFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(manualMode) {
        when (manualMode) {
            null -> defaultFocusRequester.requestFocus()
            "dispatcharr" -> dispatcharrFocusRequester.requestFocus()
            "custom" -> customFocusRequester.requestFocus()
        }
    }

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
                text = "Easy Setup with Mobile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
            when (manualMode) {
                null -> {
                    Text(
                        text = "Manual Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                }
                "dispatcharr" -> {
                    Text(
                        text = "Dispatcharr Server",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                            .focusRequester(dispatcharrFocusRequester)
                            .focusProperties {
                                down = connectButtonFocusRequester
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            connectButtonFocusRequester.requestFocus()
                        }),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(connectButtonFocusRequester)
                        ) {
                            Text("Connect")
                        }
                        Button(
                            onClick = { manualMode = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Back")
                        }
                    }
                }
                "custom" -> {
                    Text(
                        text = "Custom URLs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = epgUrlInput,
                        onValueChange = { epgUrlInput = it },
                        label = { Text("EPG XMLTV URL (Optional)") },
                        placeholder = { Text("http://...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties {
                                down = saveButtonFocusRequester
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            saveButtonFocusRequester.requestFocus()
                        }),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(saveButtonFocusRequester)
                        ) {
                            Text("Save & Load")
                        }
                        Button(
                            onClick = { manualMode = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Back")
                        }
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
fun TvTopBarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                        else if (isFocused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) 
                        else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTopBar(
    selectedTab: TvTab,
    onTabSelected: (TvTab) -> Unit,
    firstItemFocusRequester: FocusRequester,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Watcharr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvTopBarItem(
                label = "Start",
                isSelected = selectedTab == TvTab.CHANNELS,
                onClick = { onTabSelected(TvTab.CHANNELS) },
                modifier = Modifier.focusRequester(firstItemFocusRequester)
            )
            TvTopBarItem(
                label = "TV Guide",
                isSelected = selectedTab == TvTab.EPG,
                onClick = { onTabSelected(TvTab.EPG) }
            )
            TvTopBarItem(
                label = "Setup",
                isSelected = selectedTab == TvTab.SETTINGS,
                onClick = { onTabSelected(TvTab.SETTINGS) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .width(180.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}

data class CarouselItem(val program: ProgramEntity, val channel: ChannelEntity)

private val programDayTimeFormatter get() = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private val programDayFormatter get() = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun formatProgramDayTime(timeMs: Long): String {
    return programDayTimeFormatter.format(Instant.ofEpochMilli(timeMs))
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHeroCarousel(
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
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { 
                carouselItems.getOrNull(activeIndex)?.let {
                    onSelectProgramDetail(it.program, it.channel)
                }
            }
            .background(
                color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Crossfade(
            targetState = activeItem,
            animationSpec = tween(durationMillis = 800),
            label = "HeroCarouselTransition"
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
                                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
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
                        text = "${item.channel.name}, ${formatProgramDayTime(item.program.start)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            carouselItems.forEachIndexed { idx, _ ->
                Box(
                    modifier = Modifier
                        .size(if (idx == activeIndex) 8.dp else 6.dp)
                        .background(
                            color = if (idx == activeIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TvNowLiveRow(
    liveItems: List<CarouselItem>,
    onSelectChannel: (ChannelEntity) -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit,
    state: LazyListState = rememberLazyListState(),
    firstVisibleIndex: Int = 0,
    firstItemFocusRequester: FocusRequester? = null
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10.seconds)
            now = System.currentTimeMillis()
        }
    }

    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        itemsIndexed(liveItems, key = { index, item -> item.channel.url }) { index, item ->
            var isFocused by remember { mutableStateOf(false) }
            val total = item.program.stop - item.program.start
            val progress = if (total > 0) (now - item.program.start).toFloat() / total else 0f

            Column(
                modifier = Modifier
                    .width(178.dp)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .then(
                            if (index == firstVisibleIndex && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .combinedClickable(
                            onClick = { onSelectChannel(item.channel) },
                            onLongClick = { onSelectProgramDetail(item.program, item.channel) }
                        )
                        .background(
                            color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {
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
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!item.channel.logoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = item.channel.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = item.channel.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )

                    if (!item.channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = item.channel.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 8.dp, bottom = 12.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                .padding(2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.program.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.channel.name}, ${formatTimeRange(item.program.start, item.program.stop)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvChannelsGrid(
    uiState: IptvUiState,
    viewModel: TvViewModel,
    onSelectChannel: (ChannelEntity) -> Unit,
    onSelectProgramDetail: (ProgramEntity, ChannelEntity) -> Unit
) {
    val liveFirstFocusRequester = remember { FocusRequester() }
    val lazyRowState = rememberLazyListState()
    val firstVisibleIndex by remember { derivedStateOf { lazyRowState.firstVisibleItemIndex } }
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
            .padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isFilterActive) {
                    if (carouselItems.isNotEmpty()) {
                        item {
                            TvHeroCarousel(
                                carouselItems = carouselItems,
                                onSelectProgramDetail = onSelectProgramDetail,
                                modifier = Modifier.focusProperties { down = liveFirstFocusRequester }
                            )
                        }
                    }

                    if (liveItems.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                TvNowLiveRow(
                                    liveItems = liveItems,
                                    onSelectChannel = onSelectChannel,
                                    onSelectProgramDetail = onSelectProgramDetail,
                                    state = lazyRowState,
                                    firstVisibleIndex = firstVisibleIndex,
                                    firstItemFocusRequester = liveFirstFocusRequester
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
                            modifier = Modifier.padding(top = 16.dp)
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
                        }
                        items(uiState.groups, key = { it }) { group ->
                            FilterChip(
                                selected = uiState.selectedGroup == group,
                                onClick = { viewModel.selectGroup(group) }
                            ) {
                                Text(group)
                            }
                        }
                    }
                }

                val columns = 5
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
                    color = if (isFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
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
                    color = if (isFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
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
                                      color = if (isChFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
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
                            color = if (isChFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
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
                                        color = if (isProgFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatTimeRange(program.start, program.stop),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isProgFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.secondary,
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
    uiState: IptvUiState,
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
            color = MaterialTheme.colorScheme.onSurface
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
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!program.iconUrl.isNullOrEmpty()) {
            AsyncImage(
                model = program.iconUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp)
                .padding(start = 48.dp, top = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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
                        color = if (isBackFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isBackFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "< Back",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBackFocused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
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

            Text(
                text = program.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val durationMin = (program.stop - program.start) / 60000
            val dateStr = remember(program.start) {
                try {
                    programDayFormatter.format(Instant.ofEpochMilli(program.start))
                } catch (e: Exception) {
                    ""
                }
            }
            Text(
                text = "$dateStr ${formatTimeRange(program.start, program.stop)}, $durationMin min.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Live / Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            program.desc?.let { description ->
                Box(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun PauseIcon(color: Color = Color.White) {
    Row(
        modifier = Modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(1.dp))
        )
    }
}



@Composable
fun TvPlayerCircularButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    content: @Composable (isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(40.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp && 
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .background(
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        content(isFocused)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerControlsOverlay(
    uiState: IptvUiState,
    viewModel: TvViewModel,
    playheadTime: Long,
    isLive: Boolean,
    onSeekToLive: () -> Unit,
    onCloseOverlay: () -> Unit,
    onShowStats: () -> Unit
) {
    BackHandler(onBack = onCloseOverlay)
    
    val playingChannel = (uiState.playbackState as? PlaybackState.Playing)?.channel ?: return
    
    val currentProgram = remember(uiState.epgData, playingChannel) {
        val programs = uiState.epgData[playingChannel.url] ?: emptyList()
        val now = System.currentTimeMillis()
        programs.firstOrNull { it.start <= now && it.stop >= now }
    }
    
    val channelIndex = remember(uiState.channels, playingChannel) {
        uiState.channels.indexOfFirst { it.url == playingChannel.url }
    }
    val channelNumber = if (channelIndex != -1) "%03d".format(channelIndex + 1) else ""
    
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
    
    var userActivityTrigger by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(userActivityTrigger) {
        delay(5.seconds)
        onCloseOverlay()
    }
    
    val totalDuration = if (currentProgram != null) currentProgram.stop - currentProgram.start else 0L
    
    val pinkProgress = if (currentProgram != null && totalDuration > 0) {
        ((playheadTime - currentProgram.start).toFloat() / totalDuration).coerceIn(0f, 1f)
    } else 0f
    
    val liveProgress = if (currentProgram != null && totalDuration > 0) {
        ((System.currentTimeMillis() - currentProgram.start).toFloat() / totalDuration).coerceIn(0f, 1f)
    } else 0f
    
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullTimeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    val programTimeText = if (currentProgram != null) {
        "${timeFormat.format(Date(currentProgram.start))} - ${timeFormat.format(Date(currentProgram.stop))}"
    } else ""
    
    val wallClockTimeText = fullTimeFormat.format(Date(playheadTime))
    
    val closeFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val statsFocusRequester = remember { FocusRequester() }
    val liveBadgeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        playPauseFocusRequester.requestFocus()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    userActivityTrigger++
                }
                false
            }
            .padding(48.dp)
    ) {
        // Top Left close button
        var isCloseFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .focusRequester(closeFocusRequester)
                .onFocusChanged { isCloseFocused = it.isFocused }
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp && 
                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                    ) {
                        onCloseOverlay()
                        true
                    } else false
                }
                .border(
                    width = 1.dp,
                    color = if (isCloseFocused) Color.Transparent else Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    color = if (isCloseFocused) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                )
                .focusProperties {
                    down = playPauseFocusRequester
                }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCloseFocused) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Bottom details and controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            // Channel logo & name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!playingChannel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = playingChannel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(playingChannel.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text(
                    text = "$channelNumber ${playingChannel.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tab header: Currently Watching
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Currently Watching",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(2.dp)
                            .background(Color(0xFFE91E63))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Program Title
            Text(
                text = currentProgram?.title ?: playingChannel.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Program Time Range
            if (programTimeText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = programTimeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Play / Pause
                TvPlayerCircularButton(
                    onClick = { viewModel.playerEngine.togglePlay() },
                    focusRequester = playPauseFocusRequester,
                    modifier = Modifier.focusProperties {
                        up = closeFocusRequester
                        right = statsFocusRequester
                    }
                ) { isFocused ->
                    if (isPlaying) {
                        PauseIcon(color = if (isFocused) Color.Black else Color.White)
                    } else {
                        Icon(
                             painter = painterResource(id = R.drawable.ic_play_arrow),
                             contentDescription = "Play",
                             tint = if (isFocused) Color.Black else Color.White,
                             modifier = Modifier.size(24.dp)
                         )
                    }
                }

                // Stats Button
                TvPlayerCircularButton(
                    onClick = onShowStats,
                    focusRequester = statsFocusRequester,
                    modifier = Modifier.focusProperties {
                        up = closeFocusRequester
                        left = playPauseFocusRequester
                        if (!isLive) {
                            right = liveBadgeFocusRequester
                        }
                    }
                ) { isFocused ->
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stats),
                        contentDescription = "Stats",
                        tint = if (isFocused) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                ) {
                    // White bar up to live edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(liveProgress)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                    
                    // Pink bar up to playhead
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pinkProgress)
                            .fillMaxHeight()
                            .background(Color(0xFFE91E63), RoundedCornerShape(4.dp))
                    )
                }
                
                // Time Text
                Text(
                    text = wallClockTimeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                // LIVE Badge / Button
                var isLiveBadgeFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .then(
                            if (!isLive) {
                                Modifier
                                    .focusRequester(liveBadgeFocusRequester)
                                    .onFocusChanged { isLiveBadgeFocused = it.isFocused }
                                    .focusable()
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyUp && 
                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                                        ) {
                                            onSeekToLive()
                                            true
                                        } else false
                                    }
                                    .focusProperties {
                                        up = closeFocusRequester
                                        left = statsFocusRequester
                                    }
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            color = if (isLive) {
                                Color(0xFFE91E63) // Pink
                            } else {
                                if (isLiveBadgeFocused) Color.White else Color.White.copy(alpha = 0.2f) // White or semi-trans
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (isLive) Color.White else (if (isLiveBadgeFocused) Color.Black else Color.White),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLive) Color.White else (if (isLiveBadgeFocused) Color.Black else Color.White),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerStatsOverlay(
    viewModel: TvViewModel,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val player = remember(viewModel) { viewModel.playerEngine.getPlayer() }
    var stats by remember { mutableStateOf<io.github.RunC0deRun.shared.playback.StreamStats?>(null) }

    LaunchedEffect(player) {
        while (true) {
            stats = io.github.RunC0deRun.shared.playback.StreamStatsHelper.getStreamStats(player)
            delay(1.seconds)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            onClick = onClose,
            modifier = Modifier
                .width(360.dp)
                .focusRequester(focusRequester)
                .focusable(),
            colors = CardDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.8f),
                contentColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stream Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                    Text(
                        text = "Press BACK to close",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                stats?.let { s ->
                    StatsRow(label = "Resolution", value = s.resolution)
                    StatsRow(label = "Frame Rate", value = s.frameRate)
                    StatsRow(label = "Video Codec", value = s.videoCodec)
                    StatsRow(label = "Video Bitrate", value = s.videoBitrate)
                    StatsRow(label = "Audio Codec", value = s.audioCodec)
                    StatsRow(label = "Audio Bitrate", value = s.audioBitrate)
                    StatsRow(label = "Audio Channels", value = s.audioChannels)
                    StatsRow(label = "Sample Rate", value = s.audioSampleRate)
                    StatsRow(label = "Buffer Ahead", value = s.bufferAhead)
                } ?: run {
                    Text("Loading stream info...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


