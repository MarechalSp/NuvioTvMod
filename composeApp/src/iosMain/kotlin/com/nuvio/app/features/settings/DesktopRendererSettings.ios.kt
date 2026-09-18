package com.nuvio.app.features.settings

internal actual object DesktopRendererSettings {
    actual val isSupported: Boolean = false
    actual val useOpenGl: Boolean = false
    actual val isExternallyControlled: Boolean = false

    actual fun setUseOpenGl(enabled: Boolean) = Unit
}
