package com.nuvio.app.features.tvchannels

import com.nuvio.app.features.tvchannels.epg.EpgProgram
import com.nuvio.app.features.tvchannels.epg.TvEpgRepository
import com.nuvio.app.features.tvchannels.epg.XmlTvParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TvChannelsEpgTest {

    @Test
    fun testParseXmlTvDate() {
        val dateStr = "20260913210000 -0300"
        val epoch = XmlTvParser.parseXmlTvDate(dateStr)
        assertNotNull(epoch)
        assertTrue(epoch > 0)
    }

    @Test
    fun testParseXmlTvContent() {
        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv generator-info-name="Test">
              <channel id="Globo.br">
                <display-name>Globo SP</display-name>
                <display-name>Rede Globo</display-name>
              </channel>
              <programme start="20260913200000 -0300" stop="20260913213000 -0300" channel="Globo.br">
                <title>Jornal Nacional</title>
                <desc>Notícias do Brasil e do mundo.</desc>
                <category>Notícias</category>
              </programme>
              <programme start="20260913213000 -0300" stop="20260913224500 -0300" channel="Globo.br">
                <title>Novela das Nove</title>
                <desc>Capítulo de hoje.</desc>
                <category>Novela</category>
              </programme>
            </tv>
        """.trimIndent()

        // Usa data de referência no meio do primeiro programa
        val startEpoch = XmlTvParser.parseXmlTvDate("20260913200000 -0300")!!
        val stopEpoch = XmlTvParser.parseXmlTvDate("20260913213000 -0300")!!
        val refEpoch = startEpoch + 1800_000L // 30 min depois do início

        val result = XmlTvParser.parse(sampleXml, referenceEpochMs = refEpoch)
        assertEquals(1, result.channels.size)
        assertTrue(result.channels.containsKey("Globo.br"))
        assertEquals(2, result.channels["Globo.br"]?.displayNames?.size)

        val progs = result.programsByChannelId["Globo.br"]
        assertNotNull(progs)
        assertEquals(2, progs.size)
        assertEquals("Jornal Nacional", progs[0].title)
        assertEquals("Novela das Nove", progs[1].title)

        // Valida isLive e progress
        assertTrue(progs[0].isLive(refEpoch))
        assertFalse(progs[1].isLive(refEpoch))
        assertTrue(progs[0].progress(refEpoch) > 0.3f && progs[0].progress(refEpoch) < 0.4f)
    }

    @Test
    fun testChannelNameNormalization() {
        val raw = "SporTV 2 HD [BR]"
        val norm = TvEpgRepository.normalizeName(raw)
        val simple = TvEpgRepository.simplifyChannelName(raw)

        assertTrue(norm.contains("sportv"))
        assertTrue(simple.contains("sportv"))
    }

    @Test
    fun testBuildCatalogSections() {
        val ch1 = TvChannelItem(
            id = "1",
            type = "tv",
            name = "SporTV",
            genres = listOf("Esportes"),
            addonName = "IPTV Brasil",
            addonManifestUrl = "https://addon.com/manifest.json",
            catalogId = "esportes",
            catalogName = "Canais Esportivos",
            isFavorite = true,
        )
        val ch2 = TvChannelItem(
            id = "2",
            type = "tv",
            name = "CNN Brasil",
            genres = listOf("Notícias"),
            addonName = "IPTV Brasil",
            addonManifestUrl = "https://addon.com/manifest.json",
            catalogId = "noticias",
            catalogName = "Canais de Notícias",
            isFavorite = false,
        )

        val sections = TvChannelsRepository.buildCatalogSections(
            channels = listOf(ch1, ch2),
            favoriteKeys = setOf(ch1.stableKey()),
        )

        // Deve conter seção de favoritos
        assertTrue(sections.any { it.id == "favorites" })
        assertEquals(1, sections.first { it.id == "favorites" }.channels.size)

        // Deve conter esportes
        assertTrue(sections.any { it.id == "sports" })

        // Deve conter notícias
        assertTrue(sections.any { it.id == "news" })
    }
}
