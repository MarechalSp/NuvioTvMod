package com.nuvio.app.features.tvchannels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.tvchannels.TvAddonFilterOption
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tv_channels_catalogs_count
import nuvio.composeapp.generated.resources.tv_channels_clear_all
import nuvio.composeapp.generated.resources.tv_channels_done
import nuvio.composeapp.generated.resources.tv_channels_manage_addons_modal_subtitle
import nuvio.composeapp.generated.resources.tv_channels_manage_addons_modal_title
import nuvio.composeapp.generated.resources.tv_channels_no_addons_installed
import nuvio.composeapp.generated.resources.tv_channels_select_all
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvAddonsSelectionModal(
    visible: Boolean,
    addons: List<TvAddonFilterOption>,
    onToggleAddon: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val dismissSheet: () -> Unit = {
        coroutineScope.launch {
            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            Text(
                text = stringResource(Res.string.tv_channels_manage_addons_modal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.tv_channels_manage_addons_modal_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.60f),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Select All / Clear All Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.tv_channels_select_all),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                OutlinedButton(
                    onClick = onClearAll,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.tv_channels_clear_all),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Addons List
            if (addons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.tv_channels_no_addons_installed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.50f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(addons, key = { it.manifestUrl }) { addon ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleAddon(addon.manifestUrl) },
                            color = if (addon.isSelected) tokens.colors.accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Addon Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF242428)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!addon.logoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = addon.logoUrl,
                                            contentDescription = addon.addonName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Extension,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = addon.addonName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (addon.catalogCount > 0) {
                                        Text(
                                            text = stringResource(Res.string.tv_channels_catalogs_count, addon.catalogCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.50f),
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = addon.isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = tokens.colors.accent,
                                        checkmarkColor = tokens.colors.onAccent,
                                        uncheckedColor = Color.White.copy(alpha = 0.40f),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = dismissSheet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.colors.accent,
                    contentColor = tokens.colors.onAccent,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.tv_channels_done),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
