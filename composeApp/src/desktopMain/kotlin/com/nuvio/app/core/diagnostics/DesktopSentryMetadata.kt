package com.nuvio.app.core.diagnostics

import com.nuvio.app.core.build.AppVersionConfig
import java.util.Locale

internal data class DesktopSentryMetadata(
    val platform: String,
    val architecture: String,
    val release: String,
    val distribution: String,
)

internal fun currentDesktopSentryMetadata(): DesktopSentryMetadata =
    desktopSentryMetadata(
        osName = System.getProperty("os.name").orEmpty(),
        osArchitecture = System.getProperty("os.arch").orEmpty(),
        versionName = AppVersionConfig.DESKTOP_VERSION_NAME,
        versionCode = AppVersionConfig.DESKTOP_VERSION_CODE,
    )

internal fun desktopSentryMetadata(
    osName: String,
    osArchitecture: String,
    versionName: String,
    versionCode: Int,
): DesktopSentryMetadata {
    val platform = normalizeDesktopPlatform(osName)
    val architecture = normalizeDesktopArchitecture(osArchitecture)
    return DesktopSentryMetadata(
        platform = platform,
        architecture = architecture,
        release = "com.nuvio.media.desktop@$versionName+$versionCode",
        distribution = "$versionCode-$platform-$architecture",
    )
}

private fun normalizeDesktopPlatform(osName: String): String {
    val normalized = osName.lowercase(Locale.ROOT)
    return when {
        normalized.contains("mac") -> "macos"
        normalized.contains("win") -> "windows"
        normalized.contains("linux") -> "linux"
        else -> "other"
    }
}

private fun normalizeDesktopArchitecture(osArchitecture: String): String =
    when (osArchitecture.lowercase(Locale.ROOT)) {
        "aarch64", "arm64" -> "arm64"
        "amd64", "x64", "x86_64" -> "x86_64"
        "x86", "i386", "i686" -> "x86"
        else -> osArchitecture.lowercase(Locale.ROOT).ifBlank { "unknown" }
    }
