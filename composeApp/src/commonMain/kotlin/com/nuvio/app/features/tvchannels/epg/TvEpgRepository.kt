package com.nuvio.app.features.tvchannels.epg

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.fetchAddonResponseText
import com.nuvio.app.features.streams.epochMs
import com.nuvio.app.features.tvchannels.TvChannelItem
import com.nuvio.app.features.tvchannels.TvChannelsSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TvEpgUiState(
    val isLoading: Boolean = false,
    val lastSyncEpochMs: Long = 0L,
    val totalProgramsLoaded: Int = 0,
    val errorMessage: String? = null,
    val epgByChannelKey: Map<String, ChannelEpgInfo> = emptyMap(),
)

object TvEpgRepository {
    private val log = Logger.withTag("TvEpgRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(TvEpgUiState())
    val uiState: StateFlow<TvEpgUiState> = _uiState.asStateFlow()

    // Cache interno de canais e programas XMLTV por ID
    private var cachedChannels = mutableMapOf<String, XmlTvChannel>()
    private var cachedProgramsByChannelId = mutableMapOf<String, List<EpgProgram>>()

    // Índice de canais normalizados: normalizedName -> xmlChannelId
    private val normalizedNameToChannelId = mutableMapOf<String, String>()

    private var syncJob: Job? = null
    private var tickerJob: Job? = null
    private var isInitialized = false

    // Fontes recomendadas por padrão (vazio para não ter nenhuma pré-setada)
    val DEFAULT_EPG_SOURCES = emptyList<EpgSourceConfig>()

    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        // Observa mudanças nas fontes de EPG configuradas
        scope.launch {
            TvChannelsSettingsRepository.epgSources.collectLatest { sources ->
                val activeSources = sources.filter { it.isEnabled }
                if (activeSources.isNotEmpty()) {
                    loadEpgFromSources(activeSources, forceRefresh = false)
                } else {
                    cachedChannels.clear()
                    cachedProgramsByChannelId.clear()
                    normalizedNameToChannelId.clear()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalProgramsLoaded = 0,
                            epgByChannelKey = emptyMap(),
                            errorMessage = null,
                        )
                    }
                }
            }
        }

        // Ticker que roda a cada 30 segundos para atualizar "No Ar" e barra de progresso
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(30_000L)
                updateEpgNowPlaying()
            }
        }
    }

    fun refresh() {
        val sources = TvChannelsSettingsRepository.epgSources.value.filter { it.isEnabled }
        if (sources.isNotEmpty()) {
            loadEpgFromSources(sources, forceRefresh = true)
        }
    }

    private fun loadEpgFromSources(sources: List<EpgSourceConfig>, forceRefresh: Boolean) {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val allChannels = mutableMapOf<String, XmlTvChannel>()
            val allPrograms = mutableMapOf<String, MutableList<EpgProgram>>()
            var totalPrograms = 0

            for (source in sources) {
                try {
                    log.i { "Downloading EPG from: ${source.name} (${source.url})" }
                    val xmlPayload = fetchAddonResponseText(url = source.url, forceRefresh = forceRefresh)
                    if (xmlPayload.isNotBlank()) {
                        val parsed = XmlTvParser.parse(
                            xmlContent = xmlPayload,
                            referenceEpochMs = epochMs(),
                        )
                        allChannels.putAll(parsed.channels)
                        for ((channelId, progs) in parsed.programsByChannelId) {
                            allPrograms.getOrPut(channelId) { mutableListOf() }.addAll(progs)
                        }
                        totalPrograms += parsed.totalProgramsParsed
                    }
                } catch (e: Exception) {
                    log.w(e) { "Failed to load EPG from source: ${source.name}" }
                }
            }

            cachedChannels = allChannels
            cachedProgramsByChannelId = allPrograms.mapValues { (_, list) ->
                list.sortedBy { it.startEpochMs }
            }.toMutableMap()

            buildNormalizedIndex()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    lastSyncEpochMs = epochMs(),
                    totalProgramsLoaded = totalPrograms,
                    errorMessage = if (totalPrograms == 0 && sources.isNotEmpty()) {
                        "Nenhuma programação encontrada nas fontes EPG ativas."
                    } else null,
                )
            }

            updateEpgNowPlaying()
        }
    }

    private fun buildNormalizedIndex() {
        normalizedNameToChannelId.clear()
        for ((id, channel) in cachedChannels) {
            val normId = normalizeName(id)
            if (normId.isNotBlank()) {
                normalizedNameToChannelId[normId] = id
            }
            for (name in channel.displayNames) {
                val normName = normalizeName(name)
                if (normName.isNotBlank()) {
                    normalizedNameToChannelId[normName] = id
                }
            }
        }
    }

    /**
     * Tenta encontrar o EPG correspondente para um TvChannelItem
     */
    fun findEpgForChannel(channel: TvChannelItem, nowMs: Long = epochMs()): ChannelEpgInfo {
        val matchedChannelId = findMatchedXmlTvId(channel)
        if (matchedChannelId == null) {
            // Fallback: se o próprio canal trouxer descrição no addon, exibe como programa único
            val fallbackDesc = channel.description?.takeIf { it.isNotBlank() }
            if (fallbackDesc != null) {
                val mockProg = EpgProgram(
                    id = "addon_meta:${channel.stableKey()}",
                    channelId = channel.id,
                    title = channel.name,
                    description = fallbackDesc,
                    startEpochMs = nowMs - 1800_000L,
                    endEpochMs = nowMs + 1800_000L,
                    category = channel.primaryGenre,
                )
                return ChannelEpgInfo(
                    channelKey = channel.stableKey(),
                    channelId = channel.id,
                    nowProgram = mockProg,
                )
            }
            return ChannelEpgInfo(channelKey = channel.stableKey())
        }

        val programs = cachedProgramsByChannelId[matchedChannelId].orEmpty()
        val nowProg = programs.firstOrNull { it.isLive(nowMs) }
        val nextProg = programs.firstOrNull { it.startEpochMs >= (nowProg?.endEpochMs ?: nowMs) }

        return ChannelEpgInfo(
            channelKey = channel.stableKey(),
            channelId = matchedChannelId,
            nowProgram = nowProg,
            nextProgram = nextProg,
            todayPrograms = programs,
        )
    }

    private fun findMatchedXmlTvId(channel: TvChannelItem): String? {
        // 1. Match direto por ID
        if (cachedChannels.containsKey(channel.id)) {
            return channel.id
        }

        // 2. Match por nome normalizado exato
        val normName = normalizeName(channel.name)
        normalizedNameToChannelId[normName]?.let { return it }

        // 3. Match com sufixos removidos
        val simplifiedName = simplifyChannelName(channel.name)
        normalizedNameToChannelId[simplifiedName]?.let { return it }

        // 4. Match por contenção (substring matching)
        for ((key, id) in normalizedNameToChannelId) {
            if (key.length >= 4 && (simplifiedName.contains(key) || key.contains(simplifiedName))) {
                return id
            }
        }

        return null
    }

    private fun updateEpgNowPlaying() {
        val nowMs = epochMs()
        val currentKeys = _uiState.value.epgByChannelKey
        if (currentKeys.isEmpty()) return

        val updatedMap = currentKeys.mapValues { (_, info) ->
            val channelId = info.channelId ?: return@mapValues info
            val programs = cachedProgramsByChannelId[channelId].orEmpty()
            val nowProg = programs.firstOrNull { it.isLive(nowMs) }
            val nextProg = programs.firstOrNull { it.startEpochMs >= (nowProg?.endEpochMs ?: nowMs) }
            info.copy(nowProgram = nowProg, nextProgram = nextProg)
        }

        _uiState.update { it.copy(epgByChannelKey = updatedMap) }
    }

    fun normalizeName(input: String): String {
        return input.lowercase()
            .replace(Regex("""[áàâãä]"""), "a")
            .replace(Regex("""[éèêë]"""), "e")
            .replace(Regex("""[íìîï]"""), "i")
            .replace(Regex("""[óòôõö]"""), "o")
            .replace(Regex("""[úùûü]"""), "u")
            .replace(Regex("""[ç]"""), "c")
            .replace(Regex("""[^a-z0-9]"""), "")
            .trim()
    }

    fun simplifyChannelName(raw: String): String {
        var s = raw.lowercase()
        // Substitui pontuações e delimitadores por espaços
        s = s.replace(Regex("""[\[\]\(\)\-_.]"""), " ")
        val isolatedWords = setOf(
            "1080p", "720p", "480p", "4k", "uhd", "fhd", "hd", "sd",
            "br", "brasil", "leg", "legendado", "dub", "dublado",
            "ao", "vivo", "live", "canal"
        )
        val words = s.split(Regex("""\s+""")).filter { word ->
            word.isNotBlank() && word !in isolatedWords
        }
        return normalizeName(words.joinToString(""))
    }
}
