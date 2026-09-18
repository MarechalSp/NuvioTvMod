package com.nuvio.app.features.settings

internal expect object DiscordRichPresencePlatform {
    val isSupported: Boolean
}

internal expect object DiscordRichPresenceStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
}
