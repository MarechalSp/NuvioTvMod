package com.nuvio.app.core.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSentryMetadataTest {
    @Test
    fun createsMacosArm64Metadata() {
        val metadata = desktopSentryMetadata(
            osName = "Mac OS X",
            osArchitecture = "aarch64",
            versionName = "1.4.2",
            versionCode = 31,
        )

        assertEquals("macos", metadata.platform)
        assertEquals("arm64", metadata.architecture)
        assertEquals("com.nuvio.media.desktop@1.4.2+31", metadata.release)
        assertEquals("31-macos-arm64", metadata.distribution)
    }

    @Test
    fun createsWindowsX64Metadata() {
        val metadata = desktopSentryMetadata(
            osName = "Windows 11",
            osArchitecture = "amd64",
            versionName = "2.0.0",
            versionCode = 45,
        )

        assertEquals("windows", metadata.platform)
        assertEquals("x86_64", metadata.architecture)
        assertEquals("com.nuvio.media.desktop@2.0.0+45", metadata.release)
        assertEquals("45-windows-x86_64", metadata.distribution)
    }

    @Test
    fun preservesUnknownArchitecture() {
        val metadata = desktopSentryMetadata(
            osName = "Linux",
            osArchitecture = "riscv64",
            versionName = "1.0.0",
            versionCode = 1,
        )

        assertEquals("linux", metadata.platform)
        assertEquals("riscv64", metadata.architecture)
    }
}
