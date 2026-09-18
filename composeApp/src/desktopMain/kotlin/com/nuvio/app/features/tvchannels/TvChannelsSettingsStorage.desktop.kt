package com.nuvio.app.features.tvchannels

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TvChannelsSettingsStorage {
    private val store = DesktopStorage.store("nuvio_tv_channels_settings")
    private const val keySelectedAddons = "selected_tv_addon_urls"
    private const val keyFavorites = "favorite_channel_keys"
    private const val keyViewMode = "tv_view_mode"
    private const val keyEpgSources = "epg_sources_config"

    actual fun loadSelectedAddonUrls(): Set<String>? {
        val payload = store.getString(ProfileScopedKey.of(keySelectedAddons)) ?: return null
        return payload.split("\n").filter { it.isNotBlank() }.toSet()
    }

    actual fun saveSelectedAddonUrls(urls: Set<String>) {
        val payload = urls.joinToString("\n")
        store.putString(ProfileScopedKey.of(keySelectedAddons), payload)
    }

    actual fun loadFavoriteChannelKeys(): Set<String> {
        val payload = store.getString(ProfileScopedKey.of(keyFavorites)) ?: return emptySet()
        return payload.split("\n").filter { it.isNotBlank() }.toSet()
    }

    actual fun saveFavoriteChannelKeys(keys: Set<String>) {
        val payload = keys.joinToString("\n")
        store.putString(ProfileScopedKey.of(keyFavorites), payload)
    }

    actual fun loadViewMode(): String? {
        return store.getString(ProfileScopedKey.of(keyViewMode))
    }

    actual fun saveViewMode(mode: String) {
        store.putString(ProfileScopedKey.of(keyViewMode), mode)
    }

    actual fun loadEpgSourcesJson(): String? {
        return store.getString(ProfileScopedKey.of(keyEpgSources))
    }

    actual fun saveEpgSourcesJson(json: String) {
        store.putString(ProfileScopedKey.of(keyEpgSources), json)
    }
}
