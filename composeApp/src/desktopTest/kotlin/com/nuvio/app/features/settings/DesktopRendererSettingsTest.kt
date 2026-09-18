package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRendererSettingsTest {
    @Test
    fun `stored OpenGL preference applies on Windows x64`() {
        assertTrue(
            shouldApplyStoredOpenGlRenderer(
                osName = "Windows 11",
                osArchitecture = "amd64",
                environmentRenderer = null,
                systemPropertyRenderer = null,
                storedOpenGlEnabled = true,
            ),
        )
    }

    @Test
    fun `renderer overrides take precedence over stored preference`() {
        assertFalse(
            shouldApplyStoredOpenGlRenderer(
                osName = "Windows 11",
                osArchitecture = "amd64",
                environmentRenderer = "DIRECT3D",
                systemPropertyRenderer = null,
                storedOpenGlEnabled = true,
            ),
        )
        assertFalse(
            shouldApplyStoredOpenGlRenderer(
                osName = "Windows 11",
                osArchitecture = "amd64",
                environmentRenderer = null,
                systemPropertyRenderer = "SOFTWARE",
                storedOpenGlEnabled = true,
            ),
        )
    }

    @Test
    fun `OpenGL setting is unavailable on Windows ARM64`() {
        assertFalse(supportsWindowsOpenGlRenderer("Windows 11", "aarch64"))
        assertFalse(supportsWindowsOpenGlRenderer("Windows 11", "arm64"))
    }

    @Test
    fun `OpenGL setting is unavailable outside Windows`() {
        assertFalse(supportsWindowsOpenGlRenderer("Mac OS X", "aarch64"))
        assertFalse(supportsWindowsOpenGlRenderer("Linux", "amd64"))
    }
}
