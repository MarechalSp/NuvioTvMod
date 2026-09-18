package com.nuvio.app.features.tvchannels

internal expect object TvChannelsSettingsStorage {
    fun loadSelectedAddonUrls(): Set<String>?
    fun saveSelectedAddonUrls(urls: Set<String>)

    fun loadFavoriteChannelKeys(): Set<String>
    fun saveFavoriteChannelKeys(keys: Set<String>)

    fun loadViewMode(): String?
    fun saveViewMode(mode: String)

    fun loadEpgSourcesJson(): String?
    fun saveEpgSourcesJson(json: String)
}
