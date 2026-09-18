package com.nuvio.app.features.tvchannels.epg

data class XmlTvChannel(
    val id: String,
    val displayNames: List<String> = emptyList(),
    val iconUrl: String? = null,
)

data class XmlTvParseResult(
    val channels: Map<String, XmlTvChannel>,
    val programsByChannelId: Map<String, List<EpgProgram>>,
    val totalProgramsParsed: Int,
)

object XmlTvParser {

    /**
     * Faz o parse de um conteúdo XMLTV com filtragem em tempo real para manter
     * a memória otimizada.
     */
    fun parse(
        xmlContent: String,
        referenceEpochMs: Long = SystemCurrentTimeProvider.nowMs(),
        windowPastHours: Long = 4L,
        windowFutureHours: Long = 36L,
    ): XmlTvParseResult {
        val minEpoch = referenceEpochMs - (windowPastHours * 3600_000L)
        val maxEpoch = referenceEpochMs + (windowFutureHours * 3600_000L)

        val channels = mutableMapOf<String, XmlTvChannel>()
        val programsMap = mutableMapOf<String, MutableList<EpgProgram>>()
        var totalParsed = 0

        // Parse channels
        val channelTagRegex = Regex("""<channel\s+id="([^"]+)"[^>]*>(.*?)</channel>""", RegexOption.DOT_MATCHES_ALL)
        val displayNameRegex = Regex("""<display-name[^>]*>(.*?)</display-name>""", RegexOption.IGNORE_CASE)
        val channelIconRegex = Regex("""<icon\s+src="([^"]+)"[^>]*/>""", RegexOption.IGNORE_CASE)

        for (match in channelTagRegex.findAll(xmlContent)) {
            val id = match.groupValues[1].trim()
            val body = match.groupValues[2]
            val names = displayNameRegex.findAll(body).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
            val icon = channelIconRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
            channels[id] = XmlTvChannel(id = id, displayNames = names, iconUrl = icon)
        }

        // Parse programmes: <programme start="..." stop="..." channel="...">...</programme>
        val programmeTagRegex = Regex(
            """<programme\s+start="([^"]+)"\s+stop="([^"]+)"\s+channel="([^"]+)"[^>]*>(.*?)</programme>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        val titleRegex = Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE)
        val descRegex = Regex("""<desc[^>]*>(.*?)</desc>""", RegexOption.IGNORE_CASE)
        val categoryRegex = Regex("""<category[^>]*>(.*?)</category>""", RegexOption.IGNORE_CASE)
        val progIconRegex = Regex("""<icon\s+src="([^"]+)"[^>]*/>""", RegexOption.IGNORE_CASE)

        for (match in programmeTagRegex.findAll(xmlContent)) {
            val startStr = match.groupValues[1].trim()
            val stopStr = match.groupValues[2].trim()
            val channelId = match.groupValues[3].trim()
            val body = match.groupValues[4]

            val startEpoch = parseXmlTvDate(startStr) ?: continue
            val stopEpoch = parseXmlTvDate(stopStr) ?: continue

            // Filtro de janela de tempo
            if (stopEpoch < minEpoch || startEpoch > maxEpoch) {
                continue
            }

            val title = titleRegex.find(body)?.groupValues?.getOrNull(1)?.decodeXml()?.trim()
            if (title.isNullOrBlank()) continue

            val desc = descRegex.find(body)?.groupValues?.getOrNull(1)?.decodeXml()?.trim()
            val category = categoryRegex.find(body)?.groupValues?.getOrNull(1)?.decodeXml()?.trim()
            val icon = progIconRegex.find(body)?.groupValues?.getOrNull(1)?.trim()

            val program = EpgProgram(
                id = "$channelId:$startEpoch",
                channelId = channelId,
                title = title,
                description = desc,
                startEpochMs = startEpoch,
                endEpochMs = stopEpoch,
                category = category,
                iconUrl = icon,
            )

            programsMap.getOrPut(channelId) { mutableListOf() }.add(program)
            totalParsed++
        }

        // Ordena os programas cronologicamente
        val sortedPrograms = programsMap.mapValues { (_, list) ->
            list.sortedBy { it.startEpochMs }
        }

        return XmlTvParseResult(
            channels = channels,
            programsByChannelId = sortedPrograms,
            totalProgramsParsed = totalParsed,
        )
    }

    /**
     * Decodifica entidades comuns de XML (&amp;, &lt;, &gt;, &quot;, &#39;)
     */
    private fun String.decodeXml(): String {
        return this
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#34;", "\"")
    }

    /**
     * Faz o parse da data XMLTV: "YYYYMMDDHHMMSS +/-ZZZZ" ou "YYYYMMDDHHMMSS"
     * Exemplo: "20260913203000 -0300"
     */
    fun parseXmlTvDate(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        if (trimmed.length < 14) return null

        return runCatching {
            val year = trimmed.substring(0, 4).toInt()
            val month = trimmed.substring(4, 6).toInt()
            val day = trimmed.substring(6, 8).toInt()
            val hour = trimmed.substring(8, 10).toInt()
            val minute = trimmed.substring(10, 12).toInt()
            val second = trimmed.substring(12, 14).toInt()

            var tzOffsetMinutes = 0
            if (trimmed.length >= 19) {
                val tzPart = trimmed.substring(14).trim()
                if (tzPart.isNotEmpty()) {
                    val sign = if (tzPart.startsWith("-")) -1 else 1
                    val tzDigits = tzPart.removePrefix("+").removePrefix("-").trim()
                    if (tzDigits.length >= 4) {
                        val tzHours = tzDigits.substring(0, 2).toIntOrNull() ?: 0
                        val tzMins = tzDigits.substring(2, 4).toIntOrNull() ?: 0
                        tzOffsetMinutes = sign * (tzHours * 60 + tzMins)
                    }
                }
            }

            // Calcula os dias desde 1970-01-01 (Unix Epoch)
            val epochDays = daysFromCivil(year, month, day)
            val totalSeconds = (epochDays * 86400L) + (hour * 3600L) + (minute * 60L) + second - (tzOffsetMinutes * 60L)
            totalSeconds * 1000L
        }.getOrNull()
    }

    /**
     * Algoritmo de conversão de data civil (ano, mês, dia) para dias desde a época Unix (1970-01-01).
     * Padrão astronômico/calendárico gregoriano (Howard Hinnant algorithm).
     */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        var y = year
        val m = month
        val d = day
        y -= if (m <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }
}

/**
 * Fornece o horário atual em millisegundos de forma multiplataforma
 */
object SystemCurrentTimeProvider {
    fun nowMs(): Long = com.nuvio.app.features.streams.epochMs()
}
