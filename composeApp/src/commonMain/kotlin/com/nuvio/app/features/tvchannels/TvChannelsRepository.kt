package com.nuvio.app.features.tvchannels

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.addons.buildAddonResourceUrl
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.fetchAddonResponseText
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamParser
import com.nuvio.app.features.tvchannels.epg.TvEpgRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object TvChannelsRepository {
    private val log = Logger.withTag("TvChannelsRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(TvChannelsUiState())
    val uiState: StateFlow<TvChannelsUiState> = _uiState.asStateFlow()

    private var loadChannelsJob: Job? = null
    private var loadStreamJob: Job? = null
    private var isInitialized = false

    // In-memory caches to make loading instant
    private val channelsMemoryCache = mutableMapOf<Set<String>, List<TvChannelItem>>()
    private val streamMemoryCache = mutableMapOf<String, List<StreamItem>>()

    private val TV_CATALOG_TYPES = setOf(
        "tv", "channel", "channels", "iptv", "live", "livetv", "live_tv",
        "stream", "streams", "broadcast", "broadcasts", "radio", "sports",
        "sport", "events", "event", "news", "cctv", "feed", "feeds", "other"
    )

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        TvChannelsSettingsRepository.ensureLoaded()
        TvEpgRepository.initialize()

        scope.launch {
            combine(
                AddonRepository.uiState,
                TvChannelsSettingsRepository.selectedAddonUrls,
            ) { addonsState, selectedUrls ->
                addonsState.addons to selectedUrls
            }.collectLatest { (addons, explicitSelectedUrls) ->
                handleAddonsOrSelectionChanged(addons.enabledAddons(), explicitSelectedUrls)
            }
        }

        // Observa mudanças nos favoritos
        scope.launch {
            TvChannelsSettingsRepository.favoriteChannelKeys.collectLatest { favKeys ->
                _uiState.update { current ->
                    val enrichedAll = current.allChannels.map { ch ->
                        ch.copy(isFavorite = favKeys.contains(ch.stableKey()))
                    }
                    val enrichedFiltered = current.filteredChannels.map { ch ->
                        ch.copy(isFavorite = favKeys.contains(ch.stableKey()))
                    }
                    val sections = buildCatalogSections(enrichedFiltered, favKeys)
                    current.copy(
                        allChannels = enrichedAll,
                        filteredChannels = enrichedFiltered,
                        favoriteKeys = favKeys,
                        catalogSections = sections,
                    )
                }
            }
        }

        // Observa mudanças no modo de exibição (Catálogos vs Lista)
        scope.launch {
            TvChannelsSettingsRepository.viewMode.collectLatest { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }

        // Observa atualizações do EPG para enriquecer canais em tempo real
        scope.launch {
            TvEpgRepository.uiState.collectLatest { _ ->
                _uiState.update { current ->
                    val enrichedAll = current.allChannels.map { ch ->
                        ch.copy(epgInfo = TvEpgRepository.findEpgForChannel(ch))
                    }
                    val enrichedFiltered = current.filteredChannels.map { ch ->
                        ch.copy(epgInfo = TvEpgRepository.findEpgForChannel(ch))
                    }
                    val sections = buildCatalogSections(enrichedFiltered, current.favoriteKeys)
                    current.copy(
                        allChannels = enrichedAll,
                        filteredChannels = enrichedFiltered,
                        catalogSections = sections,
                    )
                }
            }
        }
    }

    private fun handleAddonsOrSelectionChanged(
        enabledAddons: List<ManagedAddon>,
        explicitSelectedUrls: Set<String>?,
    ) {
        val availableOptions = enabledAddons.map { addon ->
            val hasTvCatalogs = isTvAddon(addon)
            TvAddonFilterOption(
                manifestUrl = addon.manifestUrl,
                addonName = addon.displayTitle,
                logoUrl = addon.manifest?.logoUrl,
                catalogCount = addon.manifest?.catalogs.orEmpty().size,
                isSelected = explicitSelectedUrls?.contains(addon.manifestUrl)
                    ?: hasTvCatalogs,
            )
        }

        val effectiveSelectedUrls = if (explicitSelectedUrls != null) {
            explicitSelectedUrls
        } else {
            // Default: select addons that are TV addons or have TV catalogs, or all if none do
            val withTv = availableOptions.filter { it.isSelected }.map { it.manifestUrl }.toSet()
            withTv.ifEmpty { availableOptions.map { it.manifestUrl }.toSet() }
        }

        _uiState.update { current ->
            current.copy(
                availableAddons = availableOptions.map { option ->
                    option.copy(isSelected = effectiveSelectedUrls.contains(option.manifestUrl))
                },
                selectedAddonUrls = effectiveSelectedUrls,
            )
        }

        loadChannels(enabledAddons, effectiveSelectedUrls)
    }

    private fun isTvCatalog(type: String, id: String = "", name: String = ""): Boolean {
        val t = type.lowercase().trim()
        val i = id.lowercase().trim()
        val n = name.lowercase().trim()
        if (TV_CATALOG_TYPES.contains(t)) return true
        if (i.contains("tv") || i.contains("channel") || i.contains("iptv") || i.contains("live") || i.contains("aovivo") || i.contains("stream") || i.contains("canal") || i.contains("canais")) return true
        if (n.contains("tv") || n.contains("canal") || n.contains("canais") || n.contains("channel") || n.contains("iptv") || n.contains("live") || n.contains("ao vivo") || n.contains("transmiss") || n.contains("esporte") || n.contains("futebol") || n.contains("24h") || n.contains("aberto")) return true
        return false
    }

    private fun isTvAddon(addon: ManagedAddon): Boolean {
        val manifest = addon.manifest ?: return false
        val id = manifest.id.lowercase()
        val name = manifest.name.lowercase()
        val desc = (manifest.description ?: "").lowercase()
        if (id.contains("iptv") || id.contains("tv") || id.contains("channel") || id.contains("live") || id.contains("brazuca") || id.contains("sexta")) return true
        if (name.contains("iptv") || name.contains("tv") || name.contains("channel") || name.contains("canal") || name.contains("canais") || name.contains("ao vivo") || name.contains("live")) return true
        if (desc.contains("iptv") || desc.contains("canal") || desc.contains("canais") || desc.contains("ao vivo") || desc.contains("live tv") || desc.contains("tv ao vivo")) return true
        return manifest.catalogs.any { isTvCatalog(it.type, it.id, it.name) }
    }

    fun refresh() {
        channelsMemoryCache.clear()
        streamMemoryCache.clear()
        val enabledAddons = AddonRepository.uiState.value.addons.enabledAddons()
        val selectedUrls = _uiState.value.selectedAddonUrls
        loadChannels(enabledAddons, selectedUrls, forceRefresh = true)
    }

    private fun loadChannels(
        allAddons: List<ManagedAddon>,
        selectedUrls: Set<String>,
        forceRefresh: Boolean = false,
    ) {
        loadChannelsJob?.cancel()

        if (selectedUrls.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    allChannels = emptyList(),
                    filteredChannels = emptyList(),
                    categories = emptyList(),
                    isLoadingChannels = false,
                    channelsErrorMessage = null,
                )
            }
            return
        }

        // Instant cache display to eliminate waiting times
        val cached = channelsMemoryCache[selectedUrls]
        if (cached != null && !forceRefresh) {
            val allCategories = cached.flatMap { it.genres }.filter { it.isNotBlank() }.distinct().sorted()
            _uiState.update { current ->
                val filtered = filterChannels(cached, current.selectedCategory, current.searchQuery)
                current.copy(
                    allChannels = cached,
                    filteredChannels = filtered,
                    categories = allCategories,
                    isLoadingChannels = false,
                    channelsErrorMessage = null,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoadingChannels = cached == null, channelsErrorMessage = null) }

        loadChannelsJob = scope.launch {
            val selectedAddons = allAddons.filter { selectedUrls.contains(it.manifestUrl) }

            // Puxa TODOS os catálogos declarados de cada addon selecionado
            val catalogTasks = selectedAddons.flatMap { addon ->
                val manifest = addon.manifest ?: return@flatMap emptyList()
                manifest.catalogs.map { catalog -> addon to catalog }
            }

            // Fetch all catalogs concurrently in parallel with Dispatchers.IO
            val catalogResults = coroutineScope {
                catalogTasks.map { (addon, catalog) ->
                    async(Dispatchers.IO) {
                        val items = runCatching {
                            fetchAllItemsFromCatalog(
                                transportUrl = addon.manifest?.transportUrl ?: addon.manifestUrl,
                                catalog = catalog,
                                forceRefresh = forceRefresh,
                            )
                        }.onFailure { err ->
                            log.w(err) { "Failed to load TV catalog ${catalog.name} from ${addon.displayTitle}" }
                        }.getOrNull().orEmpty()

                        Triple(addon, catalog, items)
                    }
                }.awaitAll()
            }

            val loadedChannels = mutableListOf<TvChannelItem>()
            val seenKeys = mutableSetOf<String>()
            for ((addon, catalog, items) in catalogResults) {
                val catName = catalog.name.ifBlank { catalog.id }
                for (meta in items) {
                    val genres = if (meta.genres.isNotEmpty()) {
                        meta.genres
                    } else if (catName.isNotBlank() && !catName.equals("tv", ignoreCase = true) && !catName.equals("canais", ignoreCase = true)) {
                        listOf(catName)
                    } else {
                        emptyList()
                    }

                    val channel = TvChannelItem(
                        id = meta.id,
                        type = meta.type,
                        name = meta.name,
                        poster = meta.poster,
                        logo = meta.logo,
                        genres = genres,
                        description = meta.description,
                        addonName = addon.displayTitle,
                        addonManifestUrl = addon.manifestUrl,
                        addonLogo = addon.manifest?.logoUrl,
                        catalogId = catalog.id,
                        catalogName = catName,
                    )
                    if (seenKeys.add(channel.stableKey())) {
                        loadedChannels.add(channel)
                    }
                }
            }

            val favKeys = TvChannelsSettingsRepository.favoriteChannelKeys.value
            val enrichedChannels = loadedChannels.map { channel ->
                channel.copy(
                    isFavorite = favKeys.contains(channel.stableKey()),
                    epgInfo = TvEpgRepository.findEpgForChannel(channel),
                )
            }

            // Store in memory cache
            channelsMemoryCache[selectedUrls] = enrichedChannels

            val allCategories = enrichedChannels
                .flatMap { ch -> ch.genres + listOf(ch.catalogName) }
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("tv", ignoreCase = true) && !it.equals("canais", ignoreCase = true) }
                .distinct()
                .sorted()

            _uiState.update { current ->
                val filtered = filterChannels(
                    channels = enrichedChannels,
                    category = current.selectedCategory,
                    query = current.searchQuery,
                )
                val sections = buildCatalogSections(filtered, favKeys)
                current.copy(
                    allChannels = enrichedChannels,
                    filteredChannels = filtered,
                    catalogSections = sections,
                    favoriteKeys = favKeys,
                    categories = allCategories,
                    isLoadingChannels = false,
                    channelsErrorMessage = if (enrichedChannels.isEmpty() && selectedAddons.isNotEmpty()) {
                        "Nenhum canal encontrado nos addons selecionados."
                    } else null,
                )
            }
        }
    }

    private suspend fun fetchAllItemsFromCatalog(
        transportUrl: String,
        catalog: AddonCatalog,
        forceRefresh: Boolean,
    ): List<MetaPreview> {
        val allItems = mutableListOf<MetaPreview>()
        val seenKeys = mutableSetOf<String>()
        var currentSkip: Int? = null
        var pageCount = 0
        val maxPages = 15 // Permite carregar até 15 páginas (~1500 a 3000 itens por catálogo)

        while (pageCount < maxPages) {
            pageCount++
            val page = runCatching {
                fetchCatalogPage(
                    manifestUrl = transportUrl,
                    type = catalog.type,
                    catalogId = catalog.id,
                    skip = currentSkip,
                    maxItems = 250,
                    forceRefresh = forceRefresh,
                )
            }.getOrNull() ?: break

            if (page.items.isEmpty()) break

            var addedAny = false
            for (item in page.items) {
                if (seenKeys.add(item.stableKey())) {
                    allItems.add(item)
                    addedAny = true
                }
            }

            val nextSkip = page.nextSkip
            if (!addedAny || nextSkip == null || nextSkip <= (currentSkip ?: 0) || page.rawItemCount == 0) {
                break
            }
            currentSkip = nextSkip
        }

        return allItems
    }

    fun setCategory(category: String) {
        _uiState.update { current ->
            val updated = filterChannels(current.allChannels, category, current.searchQuery)
            val sections = buildCatalogSections(updated, current.favoriteKeys)
            current.copy(
                selectedCategory = category,
                filteredChannels = updated,
                catalogSections = sections,
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val updated = filterChannels(current.allChannels, current.selectedCategory, query)
            val sections = buildCatalogSections(updated, current.favoriteKeys)
            current.copy(
                searchQuery = query,
                filteredChannels = updated,
                catalogSections = sections,
            )
        }
    }

    private fun filterChannels(
        channels: List<TvChannelItem>,
        category: String,
        query: String,
    ): List<TvChannelItem> {
        val trimmedQuery = query.trim().lowercase()
        return channels.filter { channel ->
            val matchesCategory = when {
                category.isBlank() -> true
                category.equals("favoritos", ignoreCase = true) -> channel.isFavorite
                else -> channel.genres.any { it.equals(category, ignoreCase = true) } ||
                    channel.catalogName.equals(category, ignoreCase = true)
            }

            val matchesQuery = trimmedQuery.isBlank() ||
                channel.name.lowercase().contains(trimmedQuery) ||
                channel.addonName.lowercase().contains(trimmedQuery) ||
                channel.catalogName.lowercase().contains(trimmedQuery) ||
                channel.genres.any { it.lowercase().contains(trimmedQuery) }

            matchesCategory && matchesQuery
        }
    }

    fun setAddonsModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isAddonsModalVisible = visible) }
    }

    fun toggleAddonSelection(manifestUrl: String) {
        val currentSelected = _uiState.value.selectedAddonUrls
        val updated = if (currentSelected.contains(manifestUrl)) {
            currentSelected - manifestUrl
        } else {
            currentSelected + manifestUrl
        }
        _uiState.update { current ->
            current.copy(
                availableAddons = current.availableAddons.map { addon ->
                    if (addon.manifestUrl == manifestUrl) addon.copy(isSelected = updated.contains(addon.manifestUrl)) else addon
                },
                selectedAddonUrls = updated,
            )
        }
        TvChannelsSettingsRepository.setSelectedAddonUrls(updated)
    }

    fun selectAllAddons() {
        val allAvailable = _uiState.value.availableAddons.map { it.manifestUrl }.toSet()
        _uiState.update { current ->
            current.copy(
                availableAddons = current.availableAddons.map { it.copy(isSelected = true) },
                selectedAddonUrls = allAvailable,
            )
        }
        TvChannelsSettingsRepository.setSelectedAddonUrls(allAvailable)
    }

    fun deselectAllAddons() {
        _uiState.update { current ->
            current.copy(
                availableAddons = current.availableAddons.map { it.copy(isSelected = false) },
                selectedAddonUrls = emptySet(),
            )
        }
        TvChannelsSettingsRepository.setSelectedAddonUrls(emptySet())
    }

    fun selectChannelForPreview(channel: TvChannelItem?) {
        loadStreamJob?.cancel()

        if (channel == null) {
            _uiState.update {
                it.copy(
                    previewChannel = null,
                    previewStreams = emptyList(),
                    selectedStreamIndex = 0,
                    isLoadingPreview = false,
                    previewErrorMessage = null,
                    isFullscreen = false,
                )
            }
            return
        }

        // Instant stream cache check
        val cached = streamMemoryCache[channel.stableKey()]
        if (cached != null && cached.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    previewChannel = channel,
                    previewStreams = cached,
                    selectedStreamIndex = 0,
                    isLoadingPreview = false,
                    previewErrorMessage = null,
                )
            }
            prefetchAdjacentStreams(channel)
            return
        }

        _uiState.update {
            it.copy(
                previewChannel = channel,
                previewStreams = emptyList(),
                selectedStreamIndex = 0,
                isLoadingPreview = true,
                previewErrorMessage = null,
            )
        }

        loadStreamJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val url = buildAddonResourceUrl(
                    manifestUrl = channel.addonManifestUrl,
                    resource = "stream",
                    type = channel.type,
                    id = channel.id,
                )
                val payload = fetchAddonResponseText(url = url, forceRefresh = false)
                val parsed = StreamParser.parse(
                    payload = payload,
                    addonName = channel.addonName,
                    addonId = channel.addonManifestUrl,
                    addonLogo = channel.addonLogo,
                )
                parsed.mapNotNull { stream ->
                    val resolvedUrl = resolvePlayableTvStreamUrl(stream)
                    if (!resolvedUrl.isNullOrBlank()) {
                        if (stream.url != resolvedUrl) {
                            stream.copy(url = resolvedUrl)
                        } else {
                            stream
                        }
                    } else {
                        null
                    }
                }
            }.fold(
                onSuccess = { streams ->
                    streamMemoryCache[channel.stableKey()] = streams
                    _uiState.update { current ->
                        if (current.previewChannel?.stableKey() == channel.stableKey()) {
                            current.copy(
                                previewStreams = streams,
                                selectedStreamIndex = 0,
                                isLoadingPreview = false,
                                previewErrorMessage = if (streams.isEmpty()) {
                                    "Nenhuma transmissão encontrada para este canal"
                                } else {
                                    null
                                },
                            )
                        } else {
                            current
                        }
                    }
                    prefetchAdjacentStreams(channel)
                },
                onFailure = { err ->
                    log.w(err) { "Failed to load stream for channel ${channel.name}" }
                    _uiState.update { current ->
                        if (current.previewChannel?.stableKey() == channel.stableKey()) {
                            current.copy(
                                previewStreams = emptyList(),
                                isLoadingPreview = false,
                                previewErrorMessage = err.message ?: "Falha ao carregar transmissão",
                            )
                        } else {
                            current
                        }
                    }
                },
            )
        }
    }

    private fun prefetchAdjacentStreams(currentChannel: TvChannelItem) {
        val channels = _uiState.value.filteredChannels
        val index = channels.indexOfFirst { it.stableKey() == currentChannel.stableKey() }
        if (index == -1) return
        val nextIndex = (index + 1).takeIf { it in channels.indices }
        val prevIndex = (index - 1).takeIf { it in channels.indices }

        listOfNotNull(nextIndex, prevIndex).map { channels[it] }.forEach { adjChannel ->
            if (!streamMemoryCache.containsKey(adjChannel.stableKey())) {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val url = buildAddonResourceUrl(
                            manifestUrl = adjChannel.addonManifestUrl,
                            resource = "stream",
                            type = adjChannel.type,
                            id = adjChannel.id,
                        )
                        val payload = fetchAddonResponseText(url = url, forceRefresh = false)
                        val parsed = StreamParser.parse(
                            payload = payload,
                            addonName = adjChannel.addonName,
                            addonId = adjChannel.addonManifestUrl,
                            addonLogo = adjChannel.addonLogo,
                        )
                        val resolved = parsed.mapNotNull { stream ->
                            val resolvedUrl = resolvePlayableTvStreamUrl(stream)
                            if (!resolvedUrl.isNullOrBlank()) {
                                if (stream.url != resolvedUrl) {
                                    stream.copy(url = resolvedUrl)
                                } else {
                                    stream
                                }
                            } else {
                                null
                            }
                        }
                        if (resolved.isNotEmpty()) {
                            streamMemoryCache[adjChannel.stableKey()] = resolved
                        }
                    }
                }
            }
        }
    }

    fun selectStreamIndex(index: Int) {
        _uiState.update { current ->
            if (index in current.previewStreams.indices) {
                current.copy(selectedStreamIndex = index)
            } else {
                current
            }
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun selectNextChannel() {
        val current = _uiState.value
        val list = current.filteredChannels
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.stableKey() == current.previewChannel?.stableKey() }
        val nextIndex = if (currentIndex in list.indices) {
            (currentIndex + 1) % list.size
        } else {
            0
        }
        selectChannelForPreview(list[nextIndex])
    }

    fun selectPreviousChannel() {
        val current = _uiState.value
        val list = current.filteredChannels
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.stableKey() == current.previewChannel?.stableKey() }
        val prevIndex = if (currentIndex in list.indices) {
            if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
        } else {
            list.size - 1
        }
        selectChannelForPreview(list[prevIndex])
    }

    fun toggleFavorite(channel: TvChannelItem) {
        TvChannelsSettingsRepository.toggleFavorite(channel.stableKey())
    }

    fun setViewMode(mode: TvViewMode) {
        TvChannelsSettingsRepository.setViewMode(mode)
    }

    fun setEpgModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isEpgModalVisible = visible) }
    }

    fun buildCatalogSections(
        channels: List<TvChannelItem>,
        favoriteKeys: Set<String>,
    ): List<TvCatalogSection> {
        if (channels.isEmpty()) return emptyList()

        val sections = mutableListOf<TvCatalogSection>()

        // 1. Favoritos
        val favorites = channels.filter { it.isFavorite || favoriteKeys.contains(it.stableKey()) }
        if (favorites.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "favorites",
                    title = "Meus Favoritos",
                    iconEmoji = "⭐",
                    channels = favorites,
                )
            )
        }

        // 2. Esportes & Futebol
        val sports = channels.filter { ch ->
            matchesTheme(ch, setOf("esporte", "esportes", "sport", "sports", "futebol", "premiere", "espn", "sportv", "dazn", "combate", "band sports", "caze"))
        }
        if (sports.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "sports",
                    title = "Esportes & Futebol",
                    iconEmoji = "⚽",
                    channels = sports,
                )
            )
        }

        // 3. Notícias & Jornalismo
        val news = channels.filter { ch ->
            matchesTheme(ch, setOf("noticia", "noticias", "news", "jornal", "globonews", "cnn", "bandnews", "record news", "jovem pan"))
        }
        if (news.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "news",
                    title = "Notícias & Jornalismo",
                    iconEmoji = "📰",
                    channels = news,
                )
            )
        }

        // 4. Filmes & Séries
        val movies = channels.filter { ch ->
            matchesTheme(ch, setOf("filme", "filmes", "serie", "series", "cinema", "movie", "movies", "telecine", "hbo", "cinemax", "megapix", "warner", "sony", "universal", "paramount", "axn", "tnt", "space", "star"))
        }
        if (movies.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "movies",
                    title = "Filmes & Séries",
                    iconEmoji = "🍿",
                    channels = movies,
                )
            )
        }

        // 5. Infantil & Família
        val kids = channels.filter { ch ->
            matchesTheme(ch, setOf("infantil", "desenho", "desenhos", "kids", "animacao", "cartoon", "nick", "disney", "discovery kids", "gloob", "toonavi"))
        }
        if (kids.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "kids",
                    title = "Infantil & Desenhos",
                    iconEmoji = "🧸",
                    channels = kids,
                )
            )
        }

        // 6. TV Aberta & Variedades
        val openTv = channels.filter { ch ->
            matchesTheme(ch, setOf("aberta", "globo", "sbt", "record", "band", "rede tv", "cultura", "tv brasil", "gazeta"))
        }
        if (openTv.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "opentv",
                    title = "TV Aberta & Variedades",
                    iconEmoji = "📺",
                    channels = openTv,
                )
            )
        }

        // 7. Catálogos específicos dos Addons
        val channelsByCatalog = channels.groupBy { it.catalogName }
        for ((catalogName, catalogChannels) in channelsByCatalog) {
            if (catalogChannels.isNotEmpty() && catalogName.isNotBlank()) {
                val secId = "catalog_${catalogName.lowercase().replace(" ", "_")}"
                if (sections.none { it.id == secId || it.title.equals(catalogName, ignoreCase = true) }) {
                    sections.add(
                        TvCatalogSection(
                            id = secId,
                            title = catalogName,
                            iconEmoji = "📡",
                            channels = catalogChannels,
                        )
                    )
                }
            }
        }

        // 8. Garante que qualquer canal que não tenha entrado em seções anteriores seja exibido
        val coveredKeys = sections.flatMap { it.channels }.map { it.stableKey() }.toSet()
        val remainingChannels = channels.filter { !coveredKeys.contains(it.stableKey()) }
        if (remainingChannels.isNotEmpty()) {
            sections.add(
                TvCatalogSection(
                    id = "more_channels",
                    title = "Outros Canais",
                    iconEmoji = "📺",
                    channels = remainingChannels,
                )
            )
        }

        return sections
    }

    private fun matchesTheme(channel: TvChannelItem, keywords: Set<String>): Boolean {
        val nameLower = channel.name.lowercase()
        val catLower = channel.catalogName.lowercase()
        val genres = channel.genres.map { it.lowercase() }
        for (kw in keywords) {
            if (nameLower.contains(kw) || catLower.contains(kw) || genres.any { it.contains(kw) }) {
                return true
            }
        }
        return false
    }
}
