package com.nuvio.app.features.tvchannels

import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.tvchannels.epg.ChannelEpgInfo

data class TvChannelItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val logo: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    val addonName: String,
    val addonManifestUrl: String,
    val addonLogo: String? = null,
    val catalogId: String,
    val catalogName: String,
    val isFavorite: Boolean = false,
    val epgInfo: ChannelEpgInfo? = null,
) {
    val displayLogo: String?
        get() = logo ?: poster

    val primaryGenre: String?
        get() = genres.firstOrNull { it.isNotBlank() }

    fun stableKey(): String = "$addonManifestUrl:$type:$id"
}

data class TvCatalogSection(
    val id: String,
    val title: String,
    val iconEmoji: String? = null,
    val channels: List<TvChannelItem>,
)

data class TvAddonFilterOption(
    val manifestUrl: String,
    val addonName: String,
    val logoUrl: String? = null,
    val catalogCount: Int = 0,
    val isSelected: Boolean = false,
)

data class TvChannelsUiState(
    val allChannels: List<TvChannelItem> = emptyList(),
    val filteredChannels: List<TvChannelItem> = emptyList(),
    val catalogSections: List<TvCatalogSection> = emptyList(),
    val availableAddons: List<TvAddonFilterOption> = emptyList(),
    val selectedAddonUrls: Set<String> = emptySet(),
    val favoriteKeys: Set<String> = emptySet(),
    val viewMode: TvViewMode = TvViewMode.CATALOG,
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val searchQuery: String = "",
    val isLoadingChannels: Boolean = false,
    val channelsErrorMessage: String? = null,
    val previewChannel: TvChannelItem? = null,
    val previewStreams: List<StreamItem> = emptyList(),
    val selectedStreamIndex: Int = 0,
    val isLoadingPreview: Boolean = false,
    val previewErrorMessage: String? = null,
    val isAddonsModalVisible: Boolean = false,
    val isEpgModalVisible: Boolean = false,
    val isFullscreen: Boolean = false,
) {
    val activeStream: StreamItem?
        get() = previewStreams.getOrNull(selectedStreamIndex)

    val totalChannelsCount: Int
        get() = allChannels.size

    val filteredChannelsCount: Int
        get() = filteredChannels.size
}

/**
 * Resolves any playable media stream URL for live TV channels, supporting:
 * - HLS (.m3u8, .m3u)
 * - MPEG-TS (.ts)
 * - DASH (.mpd)
 * - RTMP / RTMPS
 * - RTSP / RTSPS
 * - HTTP/HTTPS direct media (MP4, MKV, FLV, WebM, AAC, MP3, etc.)
 * - Xtream Codes dynamic live endpoints
 * - URLs located in stream.url, stream.externalUrl, or stream.sources
 */
val StreamItem.playableTvUrl: String?
    get() = resolvePlayableTvStreamUrl(this)

fun resolvePlayableTvStreamUrl(stream: StreamItem): String? {
    // 1. Direct playback URL (standard Stremio SDK)
    val direct = stream.playableDirectUrl?.trim()
    if (!direct.isNullOrBlank() && isSupportedTvProtocol(direct)) {
        return direct
    }

    // 2. Raw URL if not torrent/magnet
    val rawUrl = stream.url?.trim()
    if (!rawUrl.isNullOrBlank() && isSupportedTvProtocol(rawUrl) && !isTorrentOrMagnet(rawUrl)) {
        return rawUrl
    }

    // 3. External URL (many IPTV addons pass .m3u8 / .ts in externalUrl)
    val extUrl = stream.externalUrl?.trim()
    if (!extUrl.isNullOrBlank() && isSupportedTvProtocol(extUrl) && !isTorrentOrMagnet(extUrl)) {
        return extUrl
    }

    // 4. Any source listed in sources
    val fromSources = stream.sources.firstOrNull { src ->
        val trimmed = src.trim()
        isSupportedTvProtocol(trimmed) && !isTorrentOrMagnet(trimmed)
    }?.trim()
    if (!fromSources.isNullOrBlank()) {
        return fromSources
    }

    return null
}

fun isSupportedTvProtocol(url: String): Boolean {
    val u = url.lowercase().trim()
    return u.startsWith("http://") ||
        u.startsWith("https://") ||
        u.startsWith("rtmp://") ||
        u.startsWith("rtmps://") ||
        u.startsWith("rtsp://") ||
        u.startsWith("rtsps://") ||
        u.startsWith("udp://") ||
        u.startsWith("rtp://") ||
        u.startsWith("mms://") ||
        u.startsWith("mmsh://")
}

private fun isTorrentOrMagnet(url: String): Boolean {
    val u = url.lowercase().trim()
    return u.startsWith("magnet:") || u.startsWith("torrent://")
}

