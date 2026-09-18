package com.nuvio.app.features.settings

import com.nuvio.app.features.player.desktop.DesktopHostOs

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal object MacAppIconUpdater {
    fun updateAsync(icon: AppIconOption, onComplete: () -> Unit) {
        Thread({
            try {
                update(icon)
            } finally {
                onComplete()
            }
        }, "Nuvio macOS app icon updater").apply { isDaemon = false }.start()
    }

    private fun update(icon: AppIconOption) {
        if (DesktopHostOs.current != DesktopHostOs.MACOS) return
        runCatching {
            val appBundle = applicationBundle() ?: return@runCatching
            val destination = appBundle.resolve("Contents/Resources/nuvio-app-icon.icns")
            val resource = "icons/app-icon-${icon.key}-transparent.icns"
            Thread.currentThread().contextClassLoader.getResourceAsStream(resource)?.use { input ->
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun restartAsync() {
        Thread({
            runCatching {
                Thread.sleep(900)
                val appBundle = applicationBundle() ?: return@runCatching
                ProcessBuilder("open", appBundle.toString()).start()
                kotlin.system.exitProcess(0)
            }
        }, "Nuvio macOS app restart").apply {
            isDaemon = false
            start()
        }
    }

    private fun applicationBundle(): Path? {
        var path = System.getProperty("compose.application.home")?.let(Path::of) ?: return null
        while (true) {
            if (path.fileName?.toString()?.endsWith(".app") == true) return path
            path = path.parent ?: return null
        }
    }
}