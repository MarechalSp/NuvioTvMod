package com.nuvio.app.features.tvchannels

import com.nuvio.app.features.tvchannels.epg.EpgSourceConfig
import com.nuvio.app.features.tvchannels.epg.TvEpgRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class TvViewMode {
    CATALOG,
    LIST,
}

object TvChannelsSettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private val _selectedAddonUrls = MutableStateFlow<Set<String>?>(null)
    val selectedAddonUrls: StateFlow<Set<String>?> = _selectedAddonUrls.asStateFlow()

    private val _favoriteChannelKeys = MutableStateFlow<Set<String>>(emptySet())
    val favoriteChannelKeys: StateFlow<Set<String>> = _favoriteChannelKeys.asStateFlow()

    private val _viewMode = MutableStateFlow(TvViewMode.CATALOG)
    val viewMode: StateFlow<TvViewMode> = _viewMode.asStateFlow()

    private val _epgSources = MutableStateFlow<List<EpgSourceConfig>>(emptyList())
    val epgSources: StateFlow<List<EpgSourceConfig>> = _epgSources.asStateFlow()

    private var isLoaded = false

    fun ensureLoaded() {
        if (isLoaded) return
        isLoaded = true
        _selectedAddonUrls.value = TvChannelsSettingsStorage.loadSelectedAddonUrls()
        _favoriteChannelKeys.value = TvChannelsSettingsStorage.loadFavoriteChannelKeys()

        val savedMode = TvChannelsSettingsStorage.loadViewMode()
        if (!savedMode.isNullOrBlank()) {
            runCatching { TvViewMode.valueOf(savedMode) }.getOrNull()?.let {
                _viewMode.value = it
            }
        }

        val savedSourcesJson = TvChannelsSettingsStorage.loadEpgSourcesJson()
        if (!savedSourcesJson.isNullOrBlank()) {
            val parsed = runCatching {
                json.decodeFromString<List<EpgSourceConfig>>(savedSourcesJson)
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) {
                // Filter out legacy default sources (e.g. default_iptv_org_br) so old cache doesn't bring back the default EPG
                val cleaned = parsed.filterNot { it.id == "default_iptv_org_br" || it.isDefault }
                _epgSources.value = cleaned
                if (cleaned.size != parsed.size) {
                    saveEpgSources(cleaned)
                }
            }
        }
    }

    fun setSelectedAddonUrls(urls: Set<String>) {
        ensureLoaded()
        _selectedAddonUrls.value = urls
        TvChannelsSettingsStorage.saveSelectedAddonUrls(urls)
    }

    fun toggleAddon(url: String, defaultUrlsIfUnset: Set<String>) {
        ensureLoaded()
        val current = _selectedAddonUrls.value ?: defaultUrlsIfUnset
        val updated = if (current.contains(url)) {
            current - url
        } else {
            current + url
        }
        setSelectedAddonUrls(updated)
    }

    fun selectAll(allUrls: Set<String>) {
        setSelectedAddonUrls(allUrls)
    }

    fun deselectAll() {
        setSelectedAddonUrls(emptySet())
    }

    // --- FAVORITOS ---
    fun toggleFavorite(channelKey: String) {
        ensureLoaded()
        val current = _favoriteChannelKeys.value
        val updated = if (current.contains(channelKey)) {
            current - channelKey
        } else {
            current + channelKey
        }
        _favoriteChannelKeys.value = updated
        TvChannelsSettingsStorage.saveFavoriteChannelKeys(updated)
    }

    fun isFavorite(channelKey: String): Boolean {
        ensureLoaded()
        return _favoriteChannelKeys.value.contains(channelKey)
    }

    // --- MODO DE VISUALIZAÇÃO ---
    fun setViewMode(mode: TvViewMode) {
        ensureLoaded()
        _viewMode.value = mode
        TvChannelsSettingsStorage.saveViewMode(mode.name)
    }

    // --- FONTES EPG ---
    fun addEpgSource(name: String, url: String) {
        ensureLoaded()
        val trimmedUrl = url.trim()
        val trimmedName = name.trim().ifBlank { "Lista EPG Personalizada" }
        if (trimmedUrl.isBlank()) return

        val newSource = EpgSourceConfig(
            id = "custom_${System.currentTimeMillis()}",
            name = trimmedName,
            url = trimmedUrl,
            isEnabled = true,
            isDefault = false,
        )
        val updated = _epgSources.value + newSource
        saveEpgSources(updated)
    }

    fun removeEpgSource(sourceId: String) {
        ensureLoaded()
        val updated = _epgSources.value.filterNot { it.id == sourceId }
        saveEpgSources(updated)
    }

    fun toggleEpgSource(sourceId: String) {
        ensureLoaded()
        val updated = _epgSources.value.map {
            if (it.id == sourceId) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveEpgSources(updated)
    }

    fun resetEpgSourcesToDefault() {
        ensureLoaded()
        saveEpgSources(TvEpgRepository.DEFAULT_EPG_SOURCES)
    }

    private fun saveEpgSources(sources: List<EpgSourceConfig>) {
        _epgSources.value = sources
        val payload = runCatching { json.encodeToString(sources) }.getOrNull() ?: ""
        TvChannelsSettingsStorage.saveEpgSourcesJson(payload)
    }
}
