package com.nuvio.app.features.tvchannels

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object TvChannelsSettingsStorage {
    private const val keySelectedAddons = "selected_tv_addon_urls"
    private const val keyFavorites = "favorite_channel_keys"
    private const val keyViewMode = "tv_view_mode"
    private const val keyEpgSources = "epg_sources_config"

    actual fun loadSelectedAddonUrls(): Set<String>? {
        val payload = NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(keySelectedAddons)) ?: return null
        return payload.split("\n").filter { it.isNotBlank() }.toSet()
    }

    actual fun saveSelectedAddonUrls(urls: Set<String>) {
        val payload = urls.joinToString("\n")
        NSUserDefaults.standardUserDefaults.setObject(
            payload,
            forKey = ProfileScopedKey.of(keySelectedAddons),
        )
    }

    actual fun loadFavoriteChannelKeys(): Set<String> {
        val payload = NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(keyFavorites)) ?: return emptySet()
        return payload.split("\n").filter { it.isNotBlank() }.toSet()
    }

    actual fun saveFavoriteChannelKeys(keys: Set<String>) {
        val payload = keys.joinToString("\n")
        NSUserDefaults.standardUserDefaults.setObject(
            payload,
            forKey = ProfileScopedKey.of(keyFavorites),
        )
    }

    actual fun loadViewMode(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(keyViewMode))
    }

    actual fun saveViewMode(mode: String) {
        NSUserDefaults.standardUserDefaults.setObject(
            mode,
            forKey = ProfileScopedKey.of(keyViewMode),
        )
    }

    actual fun loadEpgSourcesJson(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(keyEpgSources))
    }

    actual fun saveEpgSourcesJson(json: String) {
        NSUserDefaults.standardUserDefaults.setObject(
            json,
            forKey = ProfileScopedKey.of(keyEpgSources),
        )
    }
}
