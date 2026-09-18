package com.nuvio.app.features.tvchannels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.PlatformPlayerSurface
import com.nuvio.app.features.player.PlayerControlsAction
import com.nuvio.app.features.player.PlayerControlsState
import com.nuvio.app.features.player.PlayerResizeMode
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.tvchannels.TvChannelItem
import com.nuvio.app.features.tvchannels.playableTvUrl
import kotlinx.coroutines.delay

@Composable
fun TvChannelVideoPlayer(
    channel: TvChannelItem?,
    stream: StreamItem?,
    isLoading: Boolean,
    errorMessage: String?,
    resizeMode: PlayerResizeMode,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onClosePreview: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onUserActivity: () -> Unit = {},
) {
    val tokens = MaterialTheme.nuvio
    val playableUrl = stream?.playableTvUrl ?: stream?.playableDirectUrl
    var controlsVisible by remember(channel?.id) { mutableStateOf(true) }
    var controlsActivityTick by remember(channel?.id) { mutableLongStateOf(0L) }

    LaunchedEffect(controlsVisible, controlsActivityTick) {
        if (!controlsVisible) return@LaunchedEffect
        delay(3500L)
        controlsVisible = false
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Press) {
                            controlsVisible = true
                            controlsActivityTick += 1
                            onUserActivity()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    NuvioLoadingIndicator(color = tokens.colors.accent)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Carregando transmissão...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            errorMessage != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Tentar novamente",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            playableUrl != null -> {
                val proxyHeaders = stream?.behaviorHints?.proxyHeaders?.request.orEmpty()
                val channelControlsState = remember(channel?.name, channel?.addonName, controlsVisible) {
                    PlayerControlsState(
                        title = channel?.name.orEmpty(),
                        streamTitle = channel?.addonName.orEmpty(),
                        controlsVisible = controlsVisible,
                    )
                }
                PlatformPlayerSurface(
                    sourceUrl = playableUrl,
                    sourceHeaders = proxyHeaders,
                    playWhenReady = true,
                    resizeMode = resizeMode,
                    useNativeController = true,
                    playerControlsState = channelControlsState,
                    onPlayerControlsAction = { action ->
                        when (action) {
                            PlayerControlsAction.Back -> {
                                onClosePreview()
                                true
                            }
                            else -> false
                        }
                    },
                    onPlayerControlsEvent = { type, _ ->
                        when (type) {
                            "cursorActivity",
                            "keepChromeVisible" -> {
                                controlsVisible = true
                                controlsActivityTick += 1
                                onUserActivity()
                                true
                            }
                            "hideChrome" -> {
                                controlsVisible = false
                                true
                            }
                            "toggleChrome" -> {
                                controlsVisible = !controlsVisible
                                if (controlsVisible) {
                                    controlsActivityTick += 1
                                    onUserActivity()
                                }
                                true
                            }
                            "toggleFullscreen" -> {
                                onToggleFullscreen()
                                true
                            }
                            else -> false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onControllerReady = {},
                    onSnapshot = {},
                    onError = {},
                )
            }

            channel == null -> {
                Text(
                    text = "Selecione um canal para assistir",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }

            else -> {
                Text(
                    text = "Nenhuma transmissão disponível",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
        }
    }
}
