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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.streams.epochMs
import com.nuvio.app.features.tvchannels.TvCatalogSection
import com.nuvio.app.features.tvchannels.TvChannelItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tv_channels_favorite
import nuvio.composeapp.generated.resources.tv_channels_fullscreen
import nuvio.composeapp.generated.resources.tv_channels_unfavorite
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvChannelsCatalogView(
    sections: List<TvCatalogSection>,
    selectedChannel: TvChannelItem?,
    onChannelSelect: (TvChannelItem) -> Unit,
    onWatchFullscreen: (TvChannelItem) -> Unit,
    onToggleFavorite: (TvChannelItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        items(
            items = sections,
            key = { it.id },
        ) { section ->
            TvCatalogSectionRow(
                section = section,
                selectedChannel = selectedChannel,
                onChannelSelect = onChannelSelect,
                onWatchFullscreen = onWatchFullscreen,
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun TvCatalogSectionRow(
    section: TvCatalogSection,
    selectedChannel: TvChannelItem?,
    onChannelSelect: (TvChannelItem) -> Unit,
    onWatchFullscreen: (TvChannelItem) -> Unit,
    onToggleFavorite: (TvChannelItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            section.iconEmoji?.let { emoji ->
                Text(
                    text = emoji,
                    fontSize = 18.sp,
                )
            }
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${section.channels.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = section.channels,
                key = { "${section.id}_${it.stableKey()}" },
            ) { channel ->
                val isSelected = selectedChannel?.stableKey() == channel.stableKey()
                TvCatalogChannelCard(
                    channel = channel,
                    isSelected = isSelected,
                    onClick = { onChannelSelect(channel) },
                    onWatchFullscreen = { onWatchFullscreen(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                )
            }
        }
    }
}

@Composable
private fun TvCatalogChannelCard(
    channel: TvChannelItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onWatchFullscreen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val cardShape = RoundedCornerShape(12.dp)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent.copy(alpha = 0.18f)
            isHovered -> Color.White.copy(alpha = 0.09f)
            else -> Color(0xFF161619).copy(alpha = 0.75f)
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "catalog_card_bg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent.copy(alpha = 0.85f)
            isHovered -> Color.White.copy(alpha = 0.25f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "catalog_card_border",
    )

    val nowMs = epochMs()
    val nowProg = channel.epgInfo?.nowProgram

    Surface(
        modifier = modifier
            .width(200.dp)
            .clip(cardShape)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = cardShape)
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            // Top: Logo Box & Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF202024)),
                contentAlignment = Alignment.Center,
            ) {
                val logoUrl = channel.displayLogo
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(tokens.colors.accent.copy(alpha = 0.35f), Color(0xFF2C2C32)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.80f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                // Favorite Star Button (Top-End)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.60f)),
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(if (channel.isFavorite) Res.string.tv_channels_unfavorite else Res.string.tv_channels_favorite),
                        tint = if (channel.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Watch Fullscreen Icon on Hover / Select (Bottom-End)
                if (isHovered || isSelected) {
                    IconButton(
                        onClick = onWatchFullscreen,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(tokens.colors.accent),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = stringResource(Res.string.tv_channels_fullscreen),
                            tint = tokens.colors.onAccent,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Name
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) tokens.colors.textPrimary else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mini-EPG / Now Playing Section
            if (nowProg != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            text = nowProg.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.accent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

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
            } else {
                // Addon Tag Fallback
                Text(
                    text = channel.addonName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.50f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
