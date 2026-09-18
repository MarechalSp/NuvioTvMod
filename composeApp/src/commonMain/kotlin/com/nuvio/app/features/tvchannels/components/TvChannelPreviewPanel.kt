package com.nuvio.app.features.tvchannels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.PlayerResizeMode
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.tvchannels.TvChannelItem
import com.nuvio.app.features.tvchannels.playableTvUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tv_channels_available_sources
import nuvio.composeapp.generated.resources.tv_channels_copy_link
import nuvio.composeapp.generated.resources.tv_channels_fullscreen
import nuvio.composeapp.generated.resources.tv_channels_link_copied
import nuvio.composeapp.generated.resources.tv_channels_live_badge
import nuvio.composeapp.generated.resources.tv_channels_open_external
import nuvio.composeapp.generated.resources.tv_channels_preview_title
import nuvio.composeapp.generated.resources.tv_channels_select_to_preview
import nuvio.composeapp.generated.resources.tv_channels_stream_label
import nuvio.composeapp.generated.resources.tv_channels_up_next
import nuvio.composeapp.generated.resources.tv_channels_watch_full
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvChannelPreviewPanel(
    channel: TvChannelItem?,
    streams: List<StreamItem>,
    selectedStreamIndex: Int,
    isLoading: Boolean,
    errorMessage: String?,
    isFullscreen: Boolean = false,
    isModalOpen: Boolean = false,
    resizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    onResizeModeChange: (PlayerResizeMode) -> Unit = {},
    onStreamSelected: (Int) -> Unit,
    onClosePreview: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit = {},
    onPreviousChannel: () -> Unit = {},
    onNextChannel: () -> Unit = {},
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val panelShape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    var userActivityTrigger by remember { mutableLongStateOf(0L) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(panelShape)
            .then(
                if (!isFullscreen) {
                    Modifier.border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = panelShape)
                } else {
                    Modifier
                },
            ),
        color = if (isFullscreen) Color.Black else Color(0xFF141416),
        shape = panelShape,
    ) {
        if (channel == null) {
            // Empty Preview State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.tv_channels_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.70f),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(Res.string.tv_channels_select_to_preview),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.40f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else if (channel != null) {
            // Active Channel Content (Continuous single player slot)
            val activeStream = streams.getOrNull(selectedStreamIndex)
            val playableUrl = activeStream?.playableTvUrl ?: activeStream?.playableDirectUrl

            Column(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                },
            ) {
                // Header Bar above video (Preview mode only): status badge and clickable Close [X] button
                if (!isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE50914))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Row(
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

                            Text(
                                text = "Pré-visualização",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.70f),
                            )
                        }

                        // Close Preview Button - Located outside video HWND surface so clicks are always captured
                        IconButton(
                            onClick = onClosePreview,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Color.White.copy(alpha = 0.85f),
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Fechar pré-visualização",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // Video Container: Expands to full screen in fullscreen mode, or maintains 16:9 ratio in preview
                Box(
                    modifier = if (isFullscreen) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isModalOpen && !isFullscreen) {
                        // Modal popup (EPG or Addons) is open: Dismount HWND video surface to fix airspace overlap
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D0D10)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                val logo = channel.displayLogo
                                if (!logo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = logo,
                                        contentDescription = channel.name,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E22))
                                            .padding(4.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Transmissão pausada (menu aberto)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.50f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        // Constant underlying player surface
                        TvChannelVideoPlayer(
                            channel = channel,
                            stream = activeStream,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            resizeMode = resizeMode,
                            onClosePreview = onClosePreview,
                            onToggleFullscreen = onToggleFullscreen,
                            onUserActivity = { userActivityTrigger = System.currentTimeMillis() },
                            modifier = Modifier.fillMaxSize(),
                            onRetry = onRetry,
                        )
                    }

                    if (isFullscreen) {
                        // Fullscreen HUD overlay with channel zapping, aspect ratio toggle, sources, etc.
                        TvChannelFullscreenHud(
                            channel = channel,
                            streams = streams,
                            selectedStreamIndex = selectedStreamIndex,
                            resizeMode = resizeMode,
                            onResizeModeChange = onResizeModeChange,
                            onStreamSelected = onStreamSelected,
                            onPreviousChannel = onPreviousChannel,
                            onNextChannel = onNextChannel,
                            onExitFullscreen = onExitFullscreen,
                            onClosePreview = onClosePreview,
                            onRetry = onRetry,
                            userActivityTrigger = userActivityTrigger,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Details Area: Rendered only when NOT in fullscreen mode
                if (!isFullscreen) {
                    val uriHandler = LocalUriHandler.current
                    val clipboardManager = LocalClipboardManager.current
                    var copied by remember(playableUrl) { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    Spacer(modifier = Modifier.height(12.dp))

                    // Channel Header & Meta + Discrete Action Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val logo = channel.displayLogo
                        if (!logo.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E22)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = logo,
                                    contentDescription = channel.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = channel.addonName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.60f),
                                )

                                channel.primaryGenre?.let { genre ->
                                    Text(
                                        text = "• $genre",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.colors.accent,
                                    )
                                }
                            }
                        }

                        // Action Icons: Fullscreen, External Player & Copy Link
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Discrete Icon: Expand to Fullscreen
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f)),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White.copy(alpha = 0.85f),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInFull,
                                    contentDescription = stringResource(Res.string.tv_channels_fullscreen),
                                    modifier = Modifier.size(17.dp),
                                )
                            }

                            if (playableUrl != null) {
                                // Discrete Icon: Open in External Player / VLC
                                IconButton(
                                    onClick = {
                                        runCatching { uriHandler.openUri(playableUrl) }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = Color.White.copy(alpha = 0.85f),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = stringResource(Res.string.tv_channels_open_external),
                                        modifier = Modifier.size(17.dp),
                                    )
                                }

                                // Discrete Icon: Copy Stream URL
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (copied) tokens.colors.accent.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.08f),
                                        ),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = if (copied) tokens.colors.accent else Color.White.copy(alpha = 0.85f),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(if (copied) Res.string.tv_channels_link_copied else Res.string.tv_channels_copy_link),
                                        modifier = Modifier.size(17.dp),
                                    )
                                }         }
                            }
                        }
                    }

                    // EPG Program Info Card (if available)
                    val nowMs = com.nuvio.app.features.streams.epochMs()
                    val nowProg = channel.epgInfo?.nowProgram
                    val nextProg = channel.epgInfo?.nextProgram

                    if (nowProg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, tokens.colors.accent.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE50914)),
                                )
                                Text(
                                    text = "No Ar: ${nowProg.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = nowProg.formattedTimeRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.60f),
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Progress Bar
                            val prog = nowProg.progress(nowMs)
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { prog },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = tokens.colors.accent,
                                trackColor = Color.White.copy(alpha = 0.12f),
                            )

                            if (!nowProg.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = nowProg.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (nextProg != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.tv_channels_up_next, "${nextProg.title} (${nextProg.formattedTimeRange})"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.colors.accent,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        // Channel Description fallback
                        channel.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.60f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Multiple Streams Selector
                    if (streams.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(Res.string.tv_channels_available_sources, streams.size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(streams) { index, stream ->
                                val isSelectedStream = index == selectedStreamIndex
                                val defaultLabel = stringResource(Res.string.tv_channels_stream_label, index + 1)
                                val label = stream.name ?: stream.title ?: defaultLabel
                                val isLightAccent = tokens.colors.onAccent == Color(0xFF111111)
                                FilterChip(
                                    selected = isSelectedStream,
                                    onClick = { onStreamSelected(index) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelectedStream) tokens.colors.onAccent else Color.White.copy(alpha = 0.85f),
                                            fontWeight = if (isSelectedStream) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isLightAccent) Color.White else tokens.colors.accent.copy(alpha = 0.25f),
                                        selectedLabelColor = tokens.colors.onAccent,
                                        containerColor = Color.White.copy(alpha = 0.06f),
                                        labelColor = Color.White.copy(alpha = 0.70f),
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelectedStream,
                                        borderColor = Color.White.copy(alpha = 0.08f),
                                        selectedBorderColor = tokens.colors.accent.copy(alpha = 0.6f),
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Watch in Fullscreen Button
                    Button(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.colors.accent,
                            contentColor = tokens.colors.onAccent,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInFull,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(Res.string.tv_channels_watch_full),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
