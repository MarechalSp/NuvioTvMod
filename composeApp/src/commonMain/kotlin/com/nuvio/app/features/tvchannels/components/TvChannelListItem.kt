package com.nuvio.app.features.tvchannels.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.tvchannels.TvChannelItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tv_channels_favorite
import nuvio.composeapp.generated.resources.tv_channels_preview_button
import nuvio.composeapp.generated.resources.tv_channels_unfavorite
import nuvio.composeapp.generated.resources.tv_channels_watch_full
import org.jetbrains.compose.resources.stringResource

import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.LinearProgressIndicator
import com.nuvio.app.features.streams.epochMs

@Composable
fun TvChannelListItem(
    channel: TvChannelItem,
    isSelected: Boolean,
    onPreviewClick: () -> Unit,
    onWatchClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val cardShape = RoundedCornerShape(12.dp)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent.copy(alpha = 0.16f)
            isHovered -> Color.White.copy(alpha = 0.08f)
            else -> Color(0xFF161619).copy(alpha = 0.70f)
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "channel_item_bg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent.copy(alpha = 0.65f)
            isHovered -> Color.White.copy(alpha = 0.20f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "channel_item_border",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = cardShape)
            .hoverable(interactionSource)
            .clickable(onClick = onPreviewClick),
        color = backgroundColor,
        shape = cardShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Channel Logo / Avatar Card
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222226)),
                contentAlignment = Alignment.Center,
            ) {
                val logoUrl = channel.displayLogo
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        tokens.colors.accent.copy(alpha = 0.45f),
                                        Color(0xFF2C2C32),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Small active dot indicator when currently selected for preview
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(tokens.colors.accent),
                    )
                }
            }

            // Channel Title & Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) tokens.colors.textPrimary else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Addon Source Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = channel.addonName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Genre / Category Tag
                    channel.primaryGenre?.let { genre ->
                        val isLightAccent = tokens.colors.onAccent == Color(0xFF111111)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isLightAccent) Color.White else tokens.colors.accent.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLightAccent) Color(0xFF111111) else tokens.colors.accent,
                                fontWeight = if (isLightAccent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                val nowMs = epochMs()
                val nowProg = channel.epgInfo?.nowProgram

                // Mini-EPG Section
                if (nowProg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914)),
                        )
                        Text(
                            text = "${nowProg.title} (${nowProg.formattedTimeRange})",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.accent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val prog = nowProg.progress(nowMs)
                    LinearProgressIndicator(
                        progress = { prog },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = tokens.colors.accent,
                        trackColor = Color.White.copy(alpha = 0.10f),
                    )
                }
            }

            // Action Buttons Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Favorite Star Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (channel.isFavorite) {
                            Color(0xFFFFD700).copy(alpha = 0.15f)
                        } else {
                            Color.White.copy(alpha = 0.06f)
                        },
                        contentColor = if (channel.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.65f),
                    ),
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(if (channel.isFavorite) Res.string.tv_channels_unfavorite else Res.string.tv_channels_favorite),
                        modifier = Modifier.size(17.dp),
                    )
                }

                // Quick Preview Button
                IconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isSelected) {
                            tokens.colors.accent.copy(alpha = 0.25f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        contentColor = if (isSelected) tokens.colors.accent else Color.White.copy(alpha = 0.85f),
                    ),
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.PlayArrow else Icons.Rounded.Visibility,
                        contentDescription = stringResource(Res.string.tv_channels_preview_button),
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Watch Fullscreen Button
                IconButton(
                    onClick = onWatchClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = stringResource(Res.string.tv_channels_watch_full),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
