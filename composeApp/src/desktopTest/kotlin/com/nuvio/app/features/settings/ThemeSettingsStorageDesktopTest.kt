package com.nuvio.app.features.settings

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeSettingsStorageDesktopTest {
    @Test
    fun `device language resolves to captured system locale`() {
        val systemLocale = Locale.forLanguageTag("en-US")

        assertEquals(systemLocale, resolveDesktopAppLocale(AppLanguage.DEVICE.code, systemLocale))
    }

    @Test
    fun `explicit language resolves from its language tag`() {
        val systemLocale = Locale.forLanguageTag("en-US")

        assertEquals(
            Locale.forLanguageTag("pt-BR"),
            resolveDesktopAppLocale("pt-BR", systemLocale),
        )
    }
}
