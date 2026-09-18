package com.nuvio.app.features.tvchannels.epg

import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val category: String? = null,
    val iconUrl: String? = null,
) {
    fun isLive(nowMs: Long): Boolean {
        return nowMs in startEpochMs until endEpochMs
    }

    fun progress(nowMs: Long): Float {
        if (nowMs <= startEpochMs) return 0f
        if (nowMs >= endEpochMs) return 1f
        val duration = endEpochMs - startEpochMs
        if (duration <= 0L) return 0f
        return ((nowMs - startEpochMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    val durationMinutes: Int
        get() = max(1, ((endEpochMs - startEpochMs) / 60000L).toInt())

    fun remainingMinutes(nowMs: Long): Int {
        if (nowMs >= endEpochMs) return 0
        return max(0, ((endEpochMs - nowMs) / 60000L).toInt())
    }

    val formattedTimeRange: String
        get() {
            val startStr = formatEpochToTime(startEpochMs)
            val endStr = formatEpochToTime(endEpochMs)
            return "$startStr - $endStr"
        }
}

data class ChannelEpgInfo(
    val channelKey: String,
    val channelId: String? = null,
    val nowProgram: EpgProgram? = null,
    val nextProgram: EpgProgram? = null,
    val todayPrograms: List<EpgProgram> = emptyList(),
) {
    val hasPrograms: Boolean
        get() = nowProgram != null || todayPrograms.isNotEmpty()
}

@Serializable
data class EpgSourceConfig(
    val id: String,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val lastUpdatedEpochMs: Long = 0L,
    val programCount: Int = 0,
)

/**
 * Formata um epoch milissegundos para formato HH:mm (hora local)
 */
fun formatEpochToTime(epochMs: Long): String {
    // Cálculo aproximado de hora:minuto local usando aritmética direta
    // Para precisão sem dependência pesada de kotlinx-datetime em commonMain
    val totalMinutes = (epochMs / 60000L)
    val minuteOfDay = ((totalMinutes % 1440) + 1440) % 1440
    val hour = (minuteOfDay / 60).toInt()
    val minute = (minuteOfDay % 60).toInt()
    val hourStr = if (hour < 10) "0$hour" else hour.toString()
    val minStr = if (minute < 10) "0$minute" else minute.toString()
    return "$hourStr:$minStr"
}
