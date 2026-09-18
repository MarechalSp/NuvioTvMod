package com.nuvio.app.features.tvchannels.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.tvchannels.TvChannelsSettingsRepository
import com.nuvio.app.features.tvchannels.epg.EpgSourceConfig
import com.nuvio.app.features.tvchannels.epg.TvEpgRepository

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_close
import nuvio.composeapp.generated.resources.tv_channels_done
import nuvio.composeapp.generated.resources.tv_channels_epg_active_sources
import nuvio.composeapp.generated.resources.tv_channels_epg_add_button
import nuvio.composeapp.generated.resources.tv_channels_epg_add_title
import nuvio.composeapp.generated.resources.tv_channels_epg_cancel
import nuvio.composeapp.generated.resources.tv_channels_epg_clear_all
import nuvio.composeapp.generated.resources.tv_channels_epg_name_hint
import nuvio.composeapp.generated.resources.tv_channels_epg_no_sources
import nuvio.composeapp.generated.resources.tv_channels_epg_programs_loaded
import nuvio.composeapp.generated.resources.tv_channels_epg_refresh_tooltip
import nuvio.composeapp.generated.resources.tv_channels_epg_save
import nuvio.composeapp.generated.resources.tv_channels_epg_subtitle
import nuvio.composeapp.generated.resources.tv_channels_epg_supported_formats
import nuvio.composeapp.generated.resources.tv_channels_epg_title
import nuvio.composeapp.generated.resources.tv_channels_epg_updating
import nuvio.composeapp.generated.resources.tv_channels_epg_url_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvEpgManagementModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val tokens = MaterialTheme.nuvio
    val sources by TvChannelsSettingsRepository.epgSources.collectAsStateWithLifecycle()
    val epgUiState by TvEpgRepository.uiState.collectAsStateWithLifecycle()

    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var isAddingOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                color = Color(0xFF141417),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(tokens.colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LiveTv,
                                    contentDescription = null,
                                    tint = tokens.colors.accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(Res.string.tv_channels_epg_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = stringResource(Res.string.tv_channels_epg_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.60f),
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White.copy(alpha = 0.70f)),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(Res.string.compose_player_close),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync Status Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (epgUiState.isLoading) {
                                    stringResource(Res.string.tv_channels_epg_updating)
                                } else {
                                    stringResource(Res.string.tv_channels_epg_programs_loaded, epgUiState.totalProgramsLoaded)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            epgUiState.errorMessage?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF5252),
                                )
                            } ?: run {
                                Text(
                                    text = stringResource(Res.string.tv_channels_epg_supported_formats),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.50f),
                                )
                            }
                        }

                        IconButton(
                            onClick = { TvEpgRepository.refresh() },
                            enabled = !epgUiState.isLoading,
                            modifier = Modifier.size(34.dp),
                        ) {
                            if (epgUiState.isLoading) {
                                NuvioLoadingIndicator(color = tokens.colors.accent, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(Res.string.tv_channels_epg_refresh_tooltip),
                                    tint = tokens.colors.accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sources List
                    Text(
                        text = stringResource(Res.string.tv_channels_epg_active_sources),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (sources.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.tv_channels_epg_no_sources),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(sources, key = { it.id }) { source ->
                                EpgSourceItemRow(
                                    source = source,
                                    onToggle = { TvChannelsSettingsRepository.toggleEpgSource(source.id) },
                                    onRemove = { TvChannelsSettingsRepository.removeEpgSource(source.id) },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Custom Source Form Toggle
                    AnimatedVisibility(visible = isAddingOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, tokens.colors.accent.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.tv_channels_epg_add_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Name Input
                            EpgCustomInput(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = stringResource(Res.string.tv_channels_epg_name_hint),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // URL Input
                            EpgCustomInput(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                placeholder = stringResource(Res.string.tv_channels_epg_url_hint),
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedButton(
                                    onClick = { isAddingOpen = false },
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Text(stringResource(Res.string.tv_channels_epg_cancel), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newUrl.isNotBlank()) {
                                            TvChannelsSettingsRepository.addEpgSource(newName, newUrl)
                                            newName = ""
                                            newUrl = ""
                                            isAddingOpen = false
                                            TvEpgRepository.refresh()
                                        }
                                    },
                                    enabled = newUrl.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tokens.colors.accent,
                                        contentColor = tokens.colors.onAccent,
                                    ),
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.tv_channels_epg_save),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    if (!isAddingOpen) {
                        OutlinedButton(
                            onClick = { isAddingOpen = true },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.colors.accent),
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(Res.string.tv_channels_epg_add_button), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(
                            onClick = {
                                TvChannelsSettingsRepository.resetEpgSourcesToDefault()
                                TvEpgRepository.refresh()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.75f)),
                        ) {
                            Icon(imageVector = Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(Res.string.tv_channels_epg_clear_all), fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.colors.accent,
                                contentColor = tokens.colors.onAccent,
                            ),
                        ) {
                            Text(
                                text = stringResource(Res.string.tv_channels_done),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgSourceItemRow(
    source: EpgSourceConfig,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (source.isEnabled) Color.White else Color.White.copy(alpha = 0.40f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (source.isDefault) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tokens.colors.accent.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "Recomendada",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.accent,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            Text(
                text = source.url,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Switch(
                checked = source.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                ),
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White.copy(alpha = 0.50f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remover",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EpgCustomInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222226))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = SolidColor(tokens.colors.accent),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }
                inner()
            },
        )
    }
}
