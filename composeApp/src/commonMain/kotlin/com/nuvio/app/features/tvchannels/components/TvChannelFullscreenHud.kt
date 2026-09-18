package com.nuvio.app.features.tvchannels.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.input.pointer.PointerEventPass
import com.nuvio.app.features.tvchannels.TvChannelsRepository
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.PlayerResizeMode
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.tvchannels.TvChannelItem
import com.nuvio.app.features.tvchannels.playableTvUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tv_channels_aspect_fill
import nuvio.composeapp.generated.resources.tv_channels_aspect_fit
import nuvio.composeapp.generated.resources.tv_channels_close_player
import nuvio.composeapp.generated.resources.tv_channels_copy_link
import nuvio.composeapp.generated.resources.tv_channels_exit_fullscreen
import nuvio.composeapp.generated.resources.tv_channels_live_badge
import nuvio.composeapp.generated.resources.tv_channels_next_channel
import nuvio.composeapp.generated.resources.tv_channels_open_external
import nuvio.composeapp.generated.resources.tv_channels_prev_channel
import nuvio.composeapp.generated.resources.tv_channels_stream_label
import nuvio.composeapp.generated.resources.tv_channels_up_next
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvChannelFullscreenHud(
    channel: TvChannelItem,
    streams: List<StreamItem>,
    selectedStreamIndex: Int,
    resizeMode: PlayerResizeMode,
    onResizeModeChange: (PlayerResizeMode) -> Unit,
    onStreamSelected: (Int) -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onExitFullscreen: () -> Unit,
    onClosePreview: () -> Unit = onExitFullscreen,
    onRetry: () -> Unit,
    userActivityTrigger: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    var isHudVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    val activeStream = streams.getOrNull(selectedStreamIndex)
    val playableUrl = activeStream?.playableTvUrl ?: activeStream?.playableDirectUrl

    val scope = rememberCoroutineScope()

    // Auto-hide HUD after 3.5s of inactivity, re-wakes on user interaction or video cursor activity
    LaunchedEffect(lastInteractionTime, userActivityTrigger) {
        isHudVisible = true
        delay(3500L)
        isHudVisible = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Press) {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
            },
    ) {
        // Transparent background layer that toggles HUD on click without consuming or blocking button clicks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        isHudVisible = !isHudVisible
                        if (isHudVisible) {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    },
                ),
        )

        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                // Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.85f),
                                ),
                            ),
                        ),
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Back / Exit Fullscreen Button
                    IconButton(
                        onClick = onExitFullscreen,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.tv_channels_exit_fullscreen),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Channel Logo
                    val logo = channel.displayLogo
                    if (!logo.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.60f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = logo,
                                contentDescription = channel.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }

                    // Channel Name and Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            // Red LIVE Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE50914))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                )
                                Text(
                                    text = stringResource(Res.string.tv_channels_live_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = channel.addonName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f),
                            )

                            channel.primaryGenre?.let { genre ->
                                Text(
                                    text = "• $genre",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.colors.accent,
                                )
                            }

                            channel.epgInfo?.nowProgram?.let { prog ->
                                Text(
                                    text = "• No Ar: ${prog.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Multiple Streams Selector (if > 1)
                    if (streams.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            itemsIndexed(streams) { index, stream ->
                                val isSelected = index == selectedStreamIndex
                                val label = stream.name ?: stream.title ?: stringResource(Res.string.tv_channels_stream_label, index + 1)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onStreamSelected(index) },
                                    label = {
                                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tokens.colors.accent,
                                        selectedLabelColor = tokens.colors.onAccent,
                                        containerColor = Color.Black.copy(alpha = 0.50f),
                                        labelColor = Color.White.copy(alpha = 0.80f),
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.White.copy(alpha = 0.20f),
                                        selectedBorderColor = tokens.colors.accent,
                                    ),
                                )
                            }
                        }
                    }

                    // Aspect Ratio Toggle Button (Fit / Fill)
                    val isFit = resizeMode == PlayerResizeMode.Fit
                    IconButton(
                        onClick = {
                            onResizeModeChange(if (isFit) PlayerResizeMode.Zoom else PlayerResizeMode.Fit)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (!isFit) tokens.colors.accent else Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = stringResource(if (isFit) Res.string.tv_channels_aspect_fit else Res.string.tv_channels_aspect_fill),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Copy / External VLC (if playableUrl is present)
                    if (playableUrl != null) {
                        val uriHandler = LocalUriHandler.current
                        val clipboardManager = LocalClipboardManager.current
                        var copied by remember(playableUrl) { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()

                        IconButton(
                            onClick = { runCatching { uriHandler.openUri(playableUrl) } },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = stringResource(Res.string.tv_channels_open_external),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(playableUrl))
                                copied = true
                                scope.launch {
                                    delay(2000L)
                                    copied = false
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (copied) tokens.colors.accent else Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(Res.string.tv_channels_copy_link),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Close Channel Button (X)
                    IconButton(
                        onClick = {
                            TvChannelsRepository.setFullscreen(false)
                            onClosePreview()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(Res.string.tv_channels_close_player),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Left Edge: Previous Channel Button (⏮)
                IconButton(
                    onClick = onPreviousChannel,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NavigateBefore,
                        contentDescription = stringResource(Res.string.tv_channels_prev_channel),
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Right Edge: Next Channel Button (⏭)
                IconButton(
                    onClick = onNextChannel,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NavigateNext,
                        contentDescription = stringResource(Res.string.tv_channels_next_channel),
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Bottom Info Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val nowMs = com.nuvio.app.features.streams.epochMs()
                    val nowProg = channel.epgInfo?.nowProgram
                    val nextProg = channel.epgInfo?.nextProgram

                    if (nowProg != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 24.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(tokens.colors.accent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.tv_channels_live_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.colors.onAccent,
                                        fontSize = 10.sp,
                                    )
                                }
                                Text(
                                    text = nowProg.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "(${nowProg.formattedTimeRange})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.70f),
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Barra de Progresso
                            val prog = nowProg.progress(nowMs)
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { prog },
                                modifier = Modifier
                                    .width(320.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = tokens.colors.accent,
                                trackColor = Color.White.copy(alpha = 0.20f),
                            )

                            if (!nowProg.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nowProg.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 600.dp),
                                )
                            }

                            if (nextProg != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.tv_channels_up_next, "${nextProg.title} (${nextProg.formattedTimeRange})"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.colors.accent.copy(alpha = 0.90f),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        channel.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))
                    }

                    // Shortcut hints badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "ESC: Sair • ↑ / ↓: Trocar de Canal • F: Tela Cheia",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}
