package com.nuvio.app.features.tvchannels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioPlatformExtraTopPadding
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.tvchannels.components.TvAddonsSelectionModal
import com.nuvio.app.features.tvchannels.components.TvChannelListItem
import com.nuvio.app.features.tvchannels.components.TvChannelPreviewPanel
import com.nuvio.app.features.tvchannels.components.TvChannelsCatalogView
import com.nuvio.app.features.tvchannels.components.TvEpgManagementModal
import com.nuvio.app.features.tvchannels.TvViewMode
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Star
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tv_channels_addons
import nuvio.composeapp.generated.resources.tv_channels_addons_selected
import nuvio.composeapp.generated.resources.tv_channels_category_all
import nuvio.composeapp.generated.resources.tv_channels_empty_no_addons_desc
import nuvio.composeapp.generated.resources.tv_channels_empty_no_addons_title
import nuvio.composeapp.generated.resources.tv_channels_empty_no_channels_desc
import nuvio.composeapp.generated.resources.tv_channels_empty_no_channels_title
import nuvio.composeapp.generated.resources.tv_channels_epg_guide
import nuvio.composeapp.generated.resources.tv_channels_favorites
import nuvio.composeapp.generated.resources.tv_channels_live_badge
import nuvio.composeapp.generated.resources.tv_channels_loading
import nuvio.composeapp.generated.resources.tv_channels_refresh
import nuvio.composeapp.generated.resources.tv_channels_search_hint
import nuvio.composeapp.generated.resources.tv_channels_select_addons
import nuvio.composeapp.generated.resources.tv_channels_title
import nuvio.composeapp.generated.resources.tv_channels_view_cards
import nuvio.composeapp.generated.resources.tv_channels_view_list
import org.jetbrains.compose.resources.stringResource

import androidx.compose.foundation.focusable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.features.player.PlayerResizeMode

@Composable
fun TvChannelsScreen(
    modifier: Modifier = Modifier,
    topChromePadding: Dp? = null,
    scrollToTopRequests: Flow<Unit>? = null,
    onWatchFullscreen: (TvChannelItem, StreamItem?) -> Unit = { _, _ -> },
) {
    val tokens = MaterialTheme.nuvio
    val state by TvChannelsRepository.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var resizeMode by rememberSaveable { mutableStateOf(PlayerResizeMode.Fit) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        TvChannelsRepository.initialize()
    }

    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests?.collectLatest {
            listState.animateScrollToItem(0)
        }
    }

    val scope = rememberCoroutineScope()

    val enterFullscreen: () -> Unit = {
        TvChannelsRepository.setFullscreen(true)
    }

    val exitFullscreen: () -> Unit = {
        TvChannelsRepository.setFullscreen(false)
    }

    LaunchedEffect(state.isFullscreen) {
        if (state.isFullscreen) {
            focusRequester.requestFocus()
        }
    }

    PlatformBackHandler(enabled = state.isFullscreen) {
        exitFullscreen()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (state.isFullscreen) 0.dp else (topChromePadding ?: (tokens.spacing.screenTop + statusBarTop + nuvioPlatformExtraTopPadding))

    val rootModifier = if (state.isFullscreen) {
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Escape, Key.F -> {
                        exitFullscreen()
                        true
                    }
                    Key.DirectionUp, Key.PageUp -> {
                        TvChannelsRepository.selectPreviousChannel()
                        true
                    }
                    Key.DirectionDown, Key.PageDown -> {
                        TvChannelsRepository.selectNextChannel()
                        true
                    }
                    else -> false
                }
            }
    } else {
        modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(top = topPadding, bottom = nuvioSafeBottomPadding(tokens.spacing.screenBottom))
    }

    Box(modifier = rootModifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isSplitView = maxWidth >= 840.dp

            if (isSplitView) {
                // Wide Screen: Split Layout (Channels List on Left, Preview Panel on Right)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (state.isFullscreen) 0.dp else if (maxWidth >= 1000.dp) 24.dp else 16.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(if (state.isFullscreen) 0.dp else 18.dp),
                ) {
                    // Left Column: Channels List & Filtering (Hidden in fullscreen mode)
                    if (!state.isFullscreen) {
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight(),
                        ) {
                            // Header Bar (Title, Live Badge, Search, Addons Button, EPG Button, View Mode Toggle)
                            TvChannelsHeader(
                                searchQuery = state.searchQuery,
                                onSearchQueryChange = TvChannelsRepository::setSearchQuery,
                                selectedAddonsCount = state.selectedAddonUrls.size,
                                totalAddonsCount = state.availableAddons.size,
                                viewMode = state.viewMode,
                                onViewModeChange = TvChannelsRepository::setViewMode,
                                onOpenAddonsModal = { TvChannelsRepository.setAddonsModalVisible(true) },
                                onOpenEpgModal = { TvChannelsRepository.setEpgModalVisible(true) },
                                onRefresh = TvChannelsRepository::refresh,
                                isRefreshing = state.isLoadingChannels,
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Categories Bar
                            if (state.categories.isNotEmpty() || state.favoriteKeys.isNotEmpty()) {
                                TvCategoriesFilterRow(
                                    categories = state.categories,
                                    selectedCategory = state.selectedCategory,
                                    hasFavorites = state.favoriteKeys.isNotEmpty(),
                                    onCategorySelected = TvChannelsRepository::setCategory,
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Main List Content Area
                            when {
                                state.selectedAddonUrls.isEmpty() -> {
                                    TvNoAddonsSelectedView(
                                        onSelectAddonsClick = { TvChannelsRepository.setAddonsModalVisible(true) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                state.isLoadingChannels && state.allChannels.isEmpty() -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            NuvioLoadingIndicator(color = tokens.colors.accent)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = stringResource(Res.string.tv_channels_loading),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.70f),
                                            )
                                        }
                                    }
                                }

                                state.filteredChannels.isEmpty() -> {
                                    TvNoChannelsFoundView(
                                        searchQuery = state.searchQuery,
                                        onClearSearch = {
                                            TvChannelsRepository.setSearchQuery("")
                                            TvChannelsRepository.setCategory("")
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                state.viewMode == TvViewMode.CATALOG && state.selectedCategory.isBlank() && state.searchQuery.isBlank() && state.catalogSections.isNotEmpty() -> {
                                    TvChannelsCatalogView(
                                        sections = state.catalogSections,
                                        selectedChannel = state.previewChannel,
                                        onChannelSelect = { TvChannelsRepository.selectChannelForPreview(it) },
                                        onWatchFullscreen = {
                                            TvChannelsRepository.selectChannelForPreview(it)
                                            enterFullscreen()
                                        },
                                        onToggleFavorite = { TvChannelsRepository.toggleFavorite(it) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                else -> {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 24.dp),
                                    ) {
                                        items(
                                            items = state.filteredChannels,
                                            key = { it.stableKey() },
                                        ) { channel ->
                                            val isSelected = state.previewChannel?.stableKey() == channel.stableKey()
                                            TvChannelListItem(
                                                channel = channel,
                                                isSelected = isSelected,
                                                onPreviewClick = {
                                                    TvChannelsRepository.selectChannelForPreview(channel)
                                                },
                                                onWatchClick = {
                                                    TvChannelsRepository.selectChannelForPreview(channel)
                                                    enterFullscreen()
                                                },
                                                onToggleFavorite = {
                                                    TvChannelsRepository.toggleFavorite(channel)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Preview Panel (Expands to 100% in fullscreen mode without reparenting)
                    Box(
                        modifier = if (state.isFullscreen) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .padding(bottom = 24.dp)
                        },
                    ) {
                        TvChannelPreviewPanel(
                            channel = state.previewChannel,
                            streams = state.previewStreams,
                            selectedStreamIndex = state.selectedStreamIndex,
                            isLoading = state.isLoadingPreview,
                            errorMessage = state.previewErrorMessage,
                            isFullscreen = state.isFullscreen,
                            isModalOpen = state.isEpgModalVisible || state.isAddonsModalVisible,
                            resizeMode = resizeMode,
                            onResizeModeChange = { resizeMode = it },
                            onStreamSelected = TvChannelsRepository::selectStreamIndex,
                            onClosePreview = {
                                TvChannelsRepository.setFullscreen(false)
                                TvChannelsRepository.selectChannelForPreview(null)
                            },
                            onToggleFullscreen = enterFullscreen,
                            onExitFullscreen = exitFullscreen,
                            onPreviousChannel = TvChannelsRepository::selectPreviousChannel,
                            onNextChannel = TvChannelsRepository::selectNextChannel,
                            onRetry = {
                                TvChannelsRepository.selectChannelForPreview(state.previewChannel)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                // Compact / Mobile Layout: Single Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (state.isFullscreen) 0.dp else 16.dp,
                        ),
                ) {
                    if (!state.isFullscreen) {
                        // Header Bar
                        TvChannelsHeader(
                            searchQuery = state.searchQuery,
                            onSearchQueryChange = TvChannelsRepository::setSearchQuery,
                            selectedAddonsCount = state.selectedAddonUrls.size,
                            totalAddonsCount = state.availableAddons.size,
                            viewMode = state.viewMode,
                            onViewModeChange = TvChannelsRepository::setViewMode,
                            onOpenAddonsModal = { TvChannelsRepository.setAddonsModalVisible(true) },
                            onOpenEpgModal = { TvChannelsRepository.setEpgModalVisible(true) },
                            onRefresh = TvChannelsRepository::refresh,
                            isRefreshing = state.isLoadingChannels,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Categories Bar
                        if (state.categories.isNotEmpty() || state.favoriteKeys.isNotEmpty()) {
                            TvCategoriesFilterRow(
                                categories = state.categories,
                                selectedCategory = state.selectedCategory,
                                hasFavorites = state.favoriteKeys.isNotEmpty(),
                                onCategorySelected = TvChannelsRepository::setCategory,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Active preview player if channel selected on mobile, or full screen
                    if (state.previewChannel != null || state.isFullscreen) {
                        Box(
                            modifier = if (state.isFullscreen) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .height(310.dp)
                                    .padding(bottom = 12.dp)
                            },
                        ) {
                            TvChannelPreviewPanel(
                                channel = state.previewChannel,
                                streams = state.previewStreams,
                                selectedStreamIndex = state.selectedStreamIndex,
                                isLoading = state.isLoadingPreview,
                                errorMessage = state.previewErrorMessage,
                                isFullscreen = state.isFullscreen,
                                isModalOpen = state.isEpgModalVisible || state.isAddonsModalVisible,
                                resizeMode = resizeMode,
                                onResizeModeChange = { resizeMode = it },
                                onStreamSelected = TvChannelsRepository::selectStreamIndex,
                                onClosePreview = {
                                    TvChannelsRepository.selectChannelForPreview(null)
                                },
                                onToggleFullscreen = enterFullscreen,
                                onExitFullscreen = exitFullscreen,
                                onPreviousChannel = TvChannelsRepository::selectPreviousChannel,
                                onNextChannel = TvChannelsRepository::selectNextChannel,
                                onRetry = {
                                    TvChannelsRepository.selectChannelForPreview(state.previewChannel)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    if (!state.isFullscreen) {
                        // Channels List
                        when {
                            state.selectedAddonUrls.isEmpty() -> {
                                TvNoAddonsSelectedView(
                                    onSelectAddonsClick = { TvChannelsRepository.setAddonsModalVisible(true) },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            state.isLoadingChannels && state.allChannels.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        NuvioLoadingIndicator(color = tokens.colors.accent)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(Res.string.tv_channels_loading),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.70f),
                                        )
                                    }
                                }
                            }

                            state.filteredChannels.isEmpty() -> {
                                TvNoChannelsFoundView(
                                    searchQuery = state.searchQuery,
                                    onClearSearch = {
                                        TvChannelsRepository.setSearchQuery("")
                                        TvChannelsRepository.setCategory("")
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            state.viewMode == TvViewMode.CATALOG && state.selectedCategory.isBlank() && state.searchQuery.isBlank() && state.catalogSections.isNotEmpty() -> {
                                TvChannelsCatalogView(
                                    sections = state.catalogSections,
                                    selectedChannel = state.previewChannel,
                                    onChannelSelect = { TvChannelsRepository.selectChannelForPreview(it) },
                                    onWatchFullscreen = {
                                        TvChannelsRepository.selectChannelForPreview(it)
                                        enterFullscreen()
                                    },
                                    onToggleFavorite = { TvChannelsRepository.toggleFavorite(it) },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                ) {
                                    items(
                                        items = state.filteredChannels,
                                        key = { it.stableKey() },
                                    ) { channel ->
                                        val isSelected = state.previewChannel?.stableKey() == channel.stableKey()
                                        TvChannelListItem(
                                            channel = channel,
                                            isSelected = isSelected,
                                            onPreviewClick = {
                                                TvChannelsRepository.selectChannelForPreview(channel)
                                            },
                                            onWatchClick = {
                                                TvChannelsRepository.selectChannelForPreview(channel)
                                                enterFullscreen()
                                            },
                                            onToggleFavorite = {
                                                TvChannelsRepository.toggleFavorite(channel)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Addons Selection Modal
            TvAddonsSelectionModal(
                visible = state.isAddonsModalVisible,
                addons = state.availableAddons,
                onToggleAddon = TvChannelsRepository::toggleAddonSelection,
                onSelectAll = TvChannelsRepository::selectAllAddons,
                onClearAll = TvChannelsRepository::deselectAllAddons,
                onDismiss = { TvChannelsRepository.setAddonsModalVisible(false) },
            )

            // EPG Management Modal
            TvEpgManagementModal(
                visible = state.isEpgModalVisible,
                onDismiss = { TvChannelsRepository.setEpgModalVisible(false) },
            )
        }
    }
}

@Composable
private fun TvChannelsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedAddonsCount: Int,
    totalAddonsCount: Int,
    viewMode: TvViewMode,
    onViewModeChange: (TvViewMode) -> Unit,
    onOpenAddonsModal: () -> Unit,
    onOpenEpgModal: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 540.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isNarrow) {
                // Modo Compacto: Linha 1 com título e refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TvTitleAndLiveBadge()
                    TvRefreshButton(onRefresh = onRefresh, isRefreshing = isRefreshing)
                }

                // Linha 2 com botões de ação bem distribuídos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TvViewModeToggle(
                        viewMode = viewMode,
                        onViewModeChange = onViewModeChange,
                    )
                    TvEpgButton(
                        onOpenEpgModal = onOpenEpgModal,
                        modifier = Modifier.weight(1f),
                    )
                    TvAddonsButton(
                        selectedCount = selectedAddonsCount,
                        totalCount = totalAddonsCount,
                        onOpenAddonsModal = onOpenAddonsModal,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                // Modo Amplo (Desktop): Título à esquerda e grupo de botões alinhados à direita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TvTitleAndLiveBadge()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TvViewModeToggle(
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                        )
                        TvEpgButton(onOpenEpgModal = onOpenEpgModal)
                        TvAddonsButton(
                            selectedCount = selectedAddonsCount,
                            totalCount = totalAddonsCount,
                            onOpenAddonsModal = onOpenAddonsModal,
                        )
                        TvRefreshButton(onRefresh = onRefresh, isRefreshing = isRefreshing)
                    }
                }
            }

            // Barra de Busca: Espaçosa, confortável e perfeitamente alinhada
            TvSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
            )
        }
    }
}

@Composable
private fun TvTitleAndLiveBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.tv_channels_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        // Red LIVE pill badge
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
}

@Composable
private fun TvViewModeToggle(
    viewMode: TvViewMode,
    onViewModeChange: (TvViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1E22),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Modo Cartões
            val isCatalog = viewMode == TvViewMode.CATALOG
            val catalogBg by animateColorAsState(
                if (isCatalog) tokens.colors.accent else Color.Transparent
            )
            val catalogContentColor by animateColorAsState(
                if (isCatalog) tokens.colors.onAccent else Color.White.copy(alpha = 0.65f)
            )

            Surface(
                onClick = { onViewModeChange(TvViewMode.CATALOG) },
                shape = RoundedCornerShape(7.dp),
                color = catalogBg,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = stringResource(Res.string.tv_channels_view_cards),
                        tint = catalogContentColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = stringResource(Res.string.tv_channels_view_cards),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isCatalog) FontWeight.Bold else FontWeight.Medium,
                        color = catalogContentColor,
                        maxLines = 1,
                    )
                }
            }

            // Modo Lista
            val isList = viewMode == TvViewMode.LIST
            val listBg by animateColorAsState(
                if (isList) tokens.colors.accent else Color.Transparent
            )
            val listContentColor by animateColorAsState(
                if (isList) tokens.colors.onAccent else Color.White.copy(alpha = 0.65f)
            )

            Surface(
                onClick = { onViewModeChange(TvViewMode.LIST) },
                shape = RoundedCornerShape(7.dp),
                color = listBg,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = stringResource(Res.string.tv_channels_view_list),
                        tint = listContentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.tv_channels_view_list),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isList) FontWeight.Bold else FontWeight.Medium,
                        color = listContentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvEpgButton(
    onOpenEpgModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        onClick = onOpenEpgModal,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1E22),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(Res.string.tv_channels_epg_guide),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TvAddonsButton(
    selectedCount: Int,
    totalCount: Int,
    onOpenAddonsModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val isLightAccent = tokens.colors.onAccent == Color(0xFF111111)
    Surface(
        onClick = onOpenAddonsModal,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1E22),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(Res.string.tv_channels_addons),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isLightAccent) Color.White else tokens.colors.accent.copy(alpha = 0.18f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "$selectedCount/$totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLightAccent) Color(0xFF111111) else tokens.colors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TvRefreshButton(
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "refresh_angle",
    )

    Surface(
        onClick = onRefresh,
        enabled = !isRefreshing,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1E22),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(Res.string.tv_channels_refresh),
                tint = Color.White.copy(alpha = if (isRefreshing) 0.40f else 0.85f),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        if (isRefreshing) {
                            rotationZ = rotation
                        }
                    },
            )
        }
    }
}

@Composable
private fun TvSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1E22))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(tokens.colors.accent),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.tv_channels_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.40f),
                        )
                    }
                    innerTextField()
                },
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Limpar busca",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TvCategoriesFilterRow(
    categories: List<String>,
    selectedCategory: String,
    hasFavorites: Boolean = false,
    onCategorySelected: (String) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "Todos" Chip
        val isAllSelected = selectedCategory.isBlank()
        FilterChip(
            selected = isAllSelected,
            onClick = { onCategorySelected("") },
            label = {
                Text(
                    text = stringResource(Res.string.tv_channels_category_all),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isAllSelected) tokens.colors.onAccent else Color.White.copy(alpha = 0.85f),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = tokens.colors.accent,
                selectedLabelColor = tokens.colors.onAccent,
                containerColor = Color.White.copy(alpha = 0.05f),
                labelColor = Color.White.copy(alpha = 0.70f),
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isAllSelected,
                borderColor = Color.White.copy(alpha = 0.08f),
                selectedBorderColor = tokens.colors.accent,
            ),
        )

        // "⭐ Favoritos" Chip (if any channel is favorited)
        if (hasFavorites) {
            val isFavSelected = selectedCategory.equals("favoritos", ignoreCase = true)
            FilterChip(
                selected = isFavSelected,
                onClick = { onCategorySelected("favoritos") },
                label = {
                    Text(
                        text = stringResource(Res.string.tv_channels_favorites),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isFavSelected) tokens.colors.onAccent else Color(0xFFFFD700),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tokens.colors.accent,
                    selectedLabelColor = tokens.colors.onAccent,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    labelColor = Color(0xFFFFD700),
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isFavSelected,
                    borderColor = Color(0xFFFFD700).copy(alpha = 0.30f),
                    selectedBorderColor = tokens.colors.accent,
                ),
            )
        }

        // Category Chips
        categories.forEach { category ->
            val isSelected = selectedCategory.equals(category, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) tokens.colors.onAccent else Color.White.copy(alpha = 0.85f),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tokens.colors.accent,
                    selectedLabelColor = tokens.colors.onAccent,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    labelColor = Color.White.copy(alpha = 0.70f),
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.White.copy(alpha = 0.08f),
                    selectedBorderColor = tokens.colors.accent,
                ),
            )
        }
    }
}

@Composable
private fun TvNoAddonsSelectedView(
    onSelectAddonsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(tokens.colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(Res.string.tv_channels_empty_no_addons_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.tv_channels_empty_no_addons_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.60f),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(360.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSelectAddonsClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.colors.accent,
                    contentColor = tokens.colors.onAccent,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.tv_channels_select_addons),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TvNoChannelsFoundView(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(54.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(Res.string.tv_channels_empty_no_channels_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.tv_channels_empty_no_channels_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.50f),
                textAlign = TextAlign.Center,
            )

            if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = "Limpar busca e filtros")
                }
            }
        }
    }
}
