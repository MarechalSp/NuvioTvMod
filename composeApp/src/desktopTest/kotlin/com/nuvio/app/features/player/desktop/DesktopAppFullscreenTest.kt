package com.nuvio.app.features.player.desktop

import androidx.compose.ui.window.WindowPlacement
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopAppFullscreenTest {
    @Test
    fun `native fullscreen exit lets the window listener restore placement`() {
        val updates = mutableListOf<String>()

        applyMacosComposeFullscreenExit(
            restorePlacement = WindowPlacement.Maximized,
            requestNativeFullscreenExit = {
                updates += "native"
                true
            },
            clearComposeFullscreen = { updates += "compose" },
            setStatePlacement = { updates += "state:$it" },
        )

        assertEquals(listOf("native"), updates)
    }

    @Test
    fun `compose fallback clears fullscreen before restoring maximized placement`() {
        val updates = mutableListOf<Pair<String, WindowPlacement>>()

        applyMacosComposeFullscreenExit(
            restorePlacement = WindowPlacement.Maximized,
            requestNativeFullscreenExit = { false },
            clearComposeFullscreen = { updates += "compose" to WindowPlacement.Floating },
            setStatePlacement = { updates += "state" to it },
        )

        assertEquals(
            listOf(
                "compose" to WindowPlacement.Floating,
                "state" to WindowPlacement.Maximized,
            ),
            updates,
        )
    }

    @Test
    fun `fullscreen cannot be restored as its own exit placement`() {
        val updates = mutableListOf<Pair<String, WindowPlacement>>()

        applyMacosComposeFullscreenExit(
            restorePlacement = WindowPlacement.Fullscreen,
            requestNativeFullscreenExit = { false },
            clearComposeFullscreen = { updates += "compose" to WindowPlacement.Floating },
            setStatePlacement = { updates += "state" to it },
        )

        assertEquals(
            listOf(
                "compose" to WindowPlacement.Floating,
                "state" to WindowPlacement.Floating,
            ),
            updates,
        )
    }
}
