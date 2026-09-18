package io.github.kdroidfilter.composemediaplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals

class WebVttParserTest {
    @Test
    fun parsesCuesWithSettings() {
        val content =
            """
            WEBVTT

            STYLE
            ::cue(.combine) { text-combine-upright: all; }

            00:00:01.000 --> 00:00:03.000 line:0
            Top

            00:00:04.000 --> 00:00:06.000 vertical:rl line:5%
            Vertical
            """.trimIndent()

        val subtitles = WebVttParser.parse(content)

        assertEquals(2, subtitles.cues.size)
        assertEquals(1_000, subtitles.cues[0].startTime)
        assertEquals(3_000, subtitles.cues[0].endTime)
        assertEquals("Top", subtitles.cues[0].text)
        assertEquals(4_000, subtitles.cues[1].startTime)
        assertEquals(6_000, subtitles.cues[1].endTime)
        assertEquals("Vertical", subtitles.cues[1].text)
    }
}
