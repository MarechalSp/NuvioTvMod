package com.nuvio.app.features.simkl

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object SimklSyncStorage {
    private const val payloadKey = "simkl_sync_snapshot"
    private val store = DesktopStorage.store("nuvio_simkl_sync")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of(payloadKey), payload)
    }

    actual fun removeProfile(profileId: Int) {
        store.remove(ProfileScopedKey.of(payloadKey, profileId))
    }
}
