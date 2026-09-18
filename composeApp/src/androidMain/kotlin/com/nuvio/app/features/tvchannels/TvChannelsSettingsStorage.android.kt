package com.nuvio.app.features.tvchannels

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TvChannelsSettingsStorage {
    private const val preferencesName = "nuvio_tv_channels_settings"
    private const val keySelectedAddons = "selected_tv_addon_urls"
    private const val keyFavorites = "favorite_channel_keys"
    private const val keyViewMode = "tv_view_mode"
    private const val keyEpgSources = "epg_sources_config"
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadSelectedAddonUrls(): Set<String>? {
        val payload = preferences?.getString(ProfileScopedKey.of(keySelectedAddons), null) ?: return null
        return payload.split("\n").filter { it.isNotBlank() }.toSet()
    }

    actual fun saveSelectedAddonUrls(urls: Set<String>) {
        val payload = urls.joinToString("\n")
        preferences?.edit()?.putString(ProfileScopedKey.of(keySelectedAddons), payload)?.apply()
    }

    actual fun loadFavoriteChannelKeys(): Set<String> {
        val payload = preferences?.getString(ProfileScopedKey.of(keyFavorites), null) ?: return emptySet()
        return payload.split("\n").filter { it.isNotBlank() }.toSet()
    }

    actual fun saveFavoriteChannelKeys(keys: Set<String>) {
        val payload = keys.joinToString("\n")
        preferences?.edit()?.putString(ProfileScopedKey.of(keyFavorites), payload)?.apply()
    }

    actual fun loadViewMode(): String? {
        return preferences?.getString(ProfileScopedKey.of(keyViewMode), null)
    }

    actual fun saveViewMode(mode: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(keyViewMode), mode)?.apply()
    }

    actual fun loadEpgSourcesJson(): String? {
        return preferences?.getString(ProfileScopedKey.of(keyEpgSources), null)
    }

    actual fun saveEpgSourcesJson(json: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(keyEpgSources), json)?.apply()
    }
}
