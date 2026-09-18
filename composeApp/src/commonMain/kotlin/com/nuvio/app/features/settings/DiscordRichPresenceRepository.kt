package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object DiscordRichPresenceRepository {
    val isSupported: Boolean
        get() = DiscordRichPresencePlatform.isSupported

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        _enabled.value = DiscordRichPresenceStorage.loadEnabled() ?: false
    }

    fun setEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_enabled.value == enabled) return
        _enabled.value = enabled
        DiscordRichPresenceStorage.saveEnabled(enabled)
    }
}
