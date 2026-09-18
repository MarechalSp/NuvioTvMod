package com.nuvio.app.features.tvchannels

import com.nuvio.app.features.streams.StreamItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TvChannelsModelsTest {

    @Test
    fun `channel stable key and logo fallbacks work correctly`() {
        val channelWithLogo = TvChannelItem(
            id = "discovery_hd",
            type = "tv",
            name = "Discovery Channel HD",
            poster = "https://example.com/poster.jpg",
            logo = "https://example.com/logo.png",
            genres = listOf("Documentary", "Science"),
            addonName = "Live IPTV",
            addonManifestUrl = "https://iptv.example/manifest.json",
            catalogId = "iptv_doc",
            catalogName = "Documentaries",
        )

        assertEquals("https://example.com/logo.png", channelWithLogo.displayLogo)
        assertEquals("Documentary", channelWithLogo.primaryGenre)
        assertEquals("https://iptv.example/manifest.json:tv:discovery_hd", channelWithLogo.stableKey())

        val channelWithoutLogo = channelWithLogo.copy(logo = null)
        assertEquals("https://example.com/poster.jpg", channelWithoutLogo.displayLogo)

        val channelWithoutPosterOrLogo = channelWithLogo.copy(logo = null, poster = null, genres = emptyList())
        assertNull(channelWithoutPosterOrLogo.displayLogo)
        assertNull(channelWithoutPosterOrLogo.primaryGenre)
    }

    @Test
    fun `tv ui state active stream and counts work as expected`() {
        val channel1 = TvChannelItem(
            id = "ch_1",
            type = "tv",
            name = "Channel 1",
            addonName = "Addon A",
            addonManifestUrl = "https://a.test/manifest.json",
            catalogId = "cat_1",
            catalogName = "Live",
        )
        val channel2 = TvChannelItem(
            id = "ch_2",
            type = "tv",
            name = "Channel 2",
            addonName = "Addon B",
            addonManifestUrl = "https://b.test/manifest.json",
            catalogId = "cat_2",
            catalogName = "News",
        )

        val state = TvChannelsUiState(
            allChannels = listOf(channel1, channel2),
            filteredChannels = listOf(channel1),
            selectedCategory = "Live",
            isFullscreen = true,
        )

        assertEquals(2, state.totalChannelsCount)
        assertEquals(1, state.filteredChannelsCount)
        assertNull(state.activeStream)
        assertTrue(state.isFullscreen)
    }

    @Test
    fun `tv addon filter option represents selection properly`() {
        val option = TvAddonFilterOption(
            manifestUrl = "https://example.test/manifest.json",
            addonName = "Brasil TV",
            catalogCount = 3,
            isSelected = true,
        )

        assertTrue(option.isSelected)
        assertEquals(3, option.catalogCount)
        assertEquals("Brasil TV", option.addonName)
    }

    @Test
    fun `isSupportedTvProtocol correctly recognizes live and streaming protocols`() {
        // HLS / Dash / HTTP(S)
        assertTrue(isSupportedTvProtocol("http://example.com/live.m3u8"))
        assertTrue(isSupportedTvProtocol("https://server.tv/stream.mpd"))
        assertTrue(isSupportedTvProtocol("https://iptv.provider/live/user/pass/123.ts"))

        // RTMP / RTSP / UDP / RTP / MMS
        assertTrue(isSupportedTvProtocol("rtmp://edge.tv/live/channel1"))
        assertTrue(isSupportedTvProtocol("rtmps://edge.tv/live/channel1"))
        assertTrue(isSupportedTvProtocol("rtsp://cam.local/h264"))
        assertTrue(isSupportedTvProtocol("rtsps://cam.local/h264"))
        assertTrue(isSupportedTvProtocol("udp://@239.255.0.1:1234"))
        assertTrue(isSupportedTvProtocol("rtp://@239.255.0.1:1234"))
        assertTrue(isSupportedTvProtocol("mms://stream.provider/channel"))
        assertTrue(isSupportedTvProtocol("mmsh://stream.provider/channel"))

        // Negative protocols
        assertFalse(isSupportedTvProtocol("magnet:?xt=urn:btih:123456"))
        assertFalse(isSupportedTvProtocol("torrent://example.com/file.torrent"))
        assertFalse(isSupportedTvProtocol("ftp://files.org/video.mp4"))
        assertFalse(isSupportedTvProtocol(""))
    }

    @Test
    fun `resolvePlayableTvStreamUrl resolves various streaming sources`() {
        val dummyAddon = "Test Addon"
        val dummyId = "https://test.addon/manifest.json"

        // 1. Direct standard URL
        val streamWithDirectUrl = StreamItem(
            url = "https://stream.tv/live/index.m3u8",
            addonName = dummyAddon,
            addonId = dummyId,
        )
        assertEquals("https://stream.tv/live/index.m3u8", resolvePlayableTvStreamUrl(streamWithDirectUrl))
        assertEquals("https://stream.tv/live/index.m3u8", streamWithDirectUrl.playableTvUrl)

        // 2. Stream where url is blank but externalUrl contains IPTV m3u8 stream
        val streamWithExternalUrl = StreamItem(
            url = "",
            externalUrl = "https://edge.iptv.com/sports.m3u8",
            addonName = dummyAddon,
            addonId = dummyId,
        )
        assertEquals("https://edge.iptv.com/sports.m3u8", resolvePlayableTvStreamUrl(streamWithExternalUrl))

        // 3. Stream where url is blank, externalUrl is null, but sources has live endpoint
        val streamWithSources = StreamItem(
            url = null,
            sources = listOf("rtmp://broadcast.tv/live/main", "https://backup.tv/live.ts"),
            addonName = dummyAddon,
            addonId = dummyId,
        )
        assertEquals("rtmp://broadcast.tv/live/main", resolvePlayableTvStreamUrl(streamWithSources))

        // 4. Torrent / Magnet stream should be rejected as playable TV url
        val torrentStream = StreamItem(
            url = "magnet:?xt=urn:btih:abc123def456",
            addonName = dummyAddon,
            addonId = dummyId,
        )
        assertNull(resolvePlayableTvStreamUrl(torrentStream))
        assertNull(torrentStream.playableTvUrl)
    }

    @Test
    fun `selectChannelForPreview null resets isFullscreen and clears preview channel`() {
        TvChannelsRepository.setFullscreen(true)
        assertTrue(TvChannelsRepository.uiState.value.isFullscreen)

        TvChannelsRepository.selectChannelForPreview(null)
        assertFalse(TvChannelsRepository.uiState.value.isFullscreen)
        assertNull(TvChannelsRepository.uiState.value.previewChannel)
        assertTrue(TvChannelsRepository.uiState.value.previewStreams.isEmpty())
    }
}
