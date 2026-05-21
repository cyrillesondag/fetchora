package org.mediadownloader.util

object QualityParser {

    private val resolutionRegex = Regex("""(\d+)x(\d+)""")

    fun parseQuality(url: String, fallbackIndex: Int = 1): String {
        val match = resolutionRegex.find(url) ?: return "Quality $fallbackIndex"
        val w = match.groupValues[1].toInt()
        val h = match.groupValues[2].toInt()
        return "${minOf(w, h)}p"
    }
}
